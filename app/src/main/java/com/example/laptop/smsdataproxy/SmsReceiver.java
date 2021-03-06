package com.example.laptop.smsdataproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.util.Base64;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SmsReceiver extends BroadcastReceiver{
    public MainActivity main_act;
    private SmsManager smsManager;
    private String googleDns = "8.8.8.8";
    private int dnsPort = 53;
    private int bytesPerSms = 115;
    private ArrayList<MessageBuffer> mbufList;

    public SmsReceiver() {
        smsManager = SmsManager.getDefault();
        mbufList = new ArrayList<>();
    }

    public void onReceive(Context context, Intent intent)
    {
        Log.i("SmsReceiver", "Received");
        Bundle bundle = intent.getExtras();
        if(bundle != null)
        {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus.length < 1)
                return;

            // Ignore unwanted SMS messages if filter number is set.
            String from = SmsMessage.createFromPdu((byte[]) pdus[0]).getOriginatingAddress();
            if (main_act.filterNumber != null && !main_act.filterNumber.equals(from))
                return;

            StringBuilder sb = new StringBuilder("");
            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                sb.append(sms.getMessageBody());
            }

            String body = sb.toString();
            main_act.appendText("SMS message from " +
                    from + " [" + body.length() + "]: " +
                    body + "\n");

            // Base64.decode may throw an exception if the string length is not a multiple of 4.
            byte[] raw;
            try {
                raw = Base64.decode(body, Base64.NO_WRAP);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            sb = new StringBuilder("Byte64-decoded SMS message [" + raw.length + "]: ");
            for (byte b : raw) {
                sb.append(String.format("%02x ", b));
            }
            sb.append("\n");
            main_act.appendText(sb.toString());

            if (raw.length < 3)
                return;

            int seqNum = raw[0];
            int dataNum = raw[1];
            int dataCount = raw[2];
            Log.i("sms", seqNum + " " + dataNum + " " + dataCount);
            // dataNum and dataCount start at 1, not 0.
            if (seqNum < 0 || dataNum < 1 || dataCount < 1 || dataNum > dataCount)
                return;

            byte[] data = new byte[raw.length - 3];
            System.arraycopy(raw, 3, data, 0, raw.length - 3);

            // Search for existing MessageBuffer with missing fragments.
            boolean found = false;
            MessageBuffer mbuf = null;
            int mbufIndex = -1;
            for (int i = 0; i < mbufList.size(); i++) {
                mbuf = mbufList.get(i);
                if (mbuf.seqNum == seqNum) {
                    found = true;
                    mbuf.add(data, dataNum);
                    mbufIndex = i;
                    Log.i("SmsReceiver", "Found mbuf with seqNum " + seqNum);
                    break;
                }
            }

            // Add new MessageBuffer if none found.
            if (!found) {
                mbuf = new MessageBuffer(seqNum);
                mbuf.add(data, dataNum);
                mbufList.add(mbuf);
                mbufIndex = mbufList.size() - 1;
                Log.i("SmsReceiver", "Added mbuf with seqNum " + seqNum);
            }

            // Check MessageBuffer count, merge data, and start task to forward the packet.
            if (mbuf.count == dataCount) {
                byte[] mergedData = mbuf.getData();
                sb = new StringBuilder("");
                for (byte b : mergedData) {
                    sb.append(String.format("%02x", b));
                    sb.append(" ");
                }
                Log.i("SmsReceiver", "Merged data[" + mergedData.length + "]: " + sb.toString());
                SmsAsyncTask task = new SmsAsyncTask(seqNum, from, mergedData);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                else
                    task.execute();
                Log.i("SmsReceiver", "Executed new SmsAsyncTask");
                mbufList.remove(mbufIndex);
            }
        }
    }

    public void setCallingActivity(MainActivity act) {
        main_act = act;
    }

    /**
     * An instance of this class is created for each complete packet received.
     */
    public class SmsAsyncTask extends AsyncTask<Void, String, Void> {
        private int seqNum;
        private String from;
        private byte[] mergedData;

        public SmsAsyncTask(int seqNum, String from, byte[] mergedData) {
            this.seqNum = seqNum;
            this.from = from;
            this.mergedData = mergedData;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Packet must have IP and UDP header present.
                int queryLength = mergedData.length - 28;
                if (queryLength < 0)
                    return null;

                // Without arguments, DatagramSocket() binds to any available port and address.
                DatagramSocket socket = new DatagramSocket();
                byte[] query = new byte[queryLength];
                System.arraycopy(mergedData, 28, query, 0, queryLength);
                DatagramPacket packet = new DatagramPacket(query, query.length);

                // Use Google DNS address for DNS queries, otherwise copy destination address.
                InetAddress destAddress;
                int destPort = (mergedData[22] & 0xff) << 8 | (mergedData[23] & 0xff);
                if (destPort == dnsPort) {
                    destAddress = InetAddress.getByName(googleDns);
                } else {
                    String addressString = String.format("%d.%d.%d.%d",
                            mergedData[16] & 0xff, mergedData[17] & 0xff,
                            mergedData[18] & 0xff, mergedData[19] & 0xff);
                    destAddress = InetAddress.getByName(addressString);
                }
                Log.i("SmsAsyncTask", "Destination address " + destAddress.toString());
                packet.setAddress(destAddress);
                packet.setPort(destPort);

                StringBuilder sb = new StringBuilder("");
                for (byte b : query) {
                    sb.append(String.format("%02x", b));
                    sb.append(" ");
                }
                Log.i("SmsAsyncTask", "Send packet [" + packet.getLength() + "]: " + sb.toString());
                socket.send(packet);

                // Switch buffer to be able to receive maximum UDP packet size.
                byte[] buffer = new byte[2048];
                packet.setData(buffer);
                packet.setLength(buffer.length);
                socket.receive(packet);
                sb = new StringBuilder("Received data [" + packet.getLength() + "]: ");
                for (int i = 0; i < packet.getLength(); i++) {
                    sb.append(String.format("%02x", buffer[i]));
                    sb.append(" ");
                }
                sb.append("\n");
                Log.i("SmsAsyncTask", "Recv packet [" + packet.getLength() + "]: " + sb.toString());
                publishProgress(sb.toString());

                // Fragment packet, encode, and send SMS message.
                int count = (packet.getLength() / bytesPerSms) + 1;
                for (int i = 0; i < count; i++) {
                    int offset = i * bytesPerSms;
                    int len = Math.min(packet.getLength() - offset, bytesPerSms);
                    byte[] sub = new byte[3 + len];
                    sub[0] = (byte) seqNum;
                    sub[1] = (byte) (i + 1);
                    sub[2] = (byte) count;
                    System.arraycopy(buffer, offset, sub, 3, len);
                    String msg = Base64.encodeToString(sub, Base64.NO_WRAP);
                    publishProgress("Byte64-encoded SMS message [" + msg.length() + "]: " +
                            msg + "\n");
                    smsManager.sendTextMessage(from, null, msg, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... message) {
            if (message != null && message.length > 0)
                main_act.appendText(message[0]);
        }
    }

    /**
     * This class holds packet fragments for sorting and merging.
     */
    public class MessageBuffer {
        public ArrayList<Entry> list;
        public int count;
        public int length;
        public int seqNum;

        class Entry {
            byte[] data;
            int dataNum;

            Entry(byte[] data, int dataNum) {
                this.data = data;
                this.dataNum = dataNum;
            }
        }

        public MessageBuffer(int seqNum) {
            this.seqNum = seqNum;
            list = new ArrayList<>();
            count = 0;
            length = 0;
        }

        public void add(byte[] data, int dataNum) {
            if (data == null || dataNum < 1)
                return;

            // Reject duplicates.
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).dataNum == dataNum)
                    return;
            }

            list.add(new Entry(data, dataNum));
            length += data.length;
            count++;
        }

        public byte[] getData() {
            if (list.size() < 1)
                return null;

            Collections.sort(list, new Comparator<Entry>() {
                @Override
                public int compare(Entry first, Entry second) {
                    if (first.dataNum < second.dataNum)
                        return -1;
                    else if (first.dataNum > second.dataNum)
                        return 1;
                    else
                        return 0;
                }
            });

            byte[] mergedData = new byte[length];
            int offset = 0;
            for (int i = 0; i < list.size(); i++){
                byte[] b = list.get(i).data;
                System.arraycopy(b, 0, mergedData, offset, b.length);
                offset += b.length;
            }

            return mergedData;
        }
    }
}