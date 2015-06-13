package com.example.laptop.myapplication;

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
    MainActivity main_act;
    SmsManager smsManager;
    String googleDns = "8.8.8.8";
    int dnsPort = 53;
    int bytesPerSms = 115;
    ArrayList<MessageBuffer> mbufList;

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

            String from = SmsMessage.createFromPdu((byte[]) pdus[0]).getOriginatingAddress();
            if (main_act.filterNumber != null && !main_act.filterNumber.equals(from))
                return;

            StringBuilder sb = new StringBuilder("");
            for (int j = 0; j < pdus.length; j++) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdus[j]);
                sb.append(sms.getMessageBody());
            }

            String body = sb.toString();
            main_act.appendText("SMS message from " +
                    from + " [" + body.length() + "]: " +
                    body);

            byte[] raw = Base64.decode(body, Base64.NO_WRAP);
            sb = new StringBuilder("Byte64-decoded SMS message [" + raw.length + "]: ");
            for (int i = 0; i < raw.length; i++) {
                sb.append(String.format("%02x ", raw[i]));
            }
            sb.append("\n");
            main_act.appendText(sb.toString());

            if (raw.length < 3)
                return;

            int seqNum = raw[0];
            int dataNum = raw[1];
            int dataCount = raw[2];
            Log.i("sms", seqNum + " " + dataNum + " " + dataCount);
            // dataNum and dataCount start at 1
            if (seqNum < 0 || dataNum < 1 || dataCount < 1 || dataNum > dataCount)
                return;

            byte[] data = new byte[raw.length - 3];
            System.arraycopy(raw, 3, data, 0, raw.length - 3);

            boolean found = false;
            MessageBuffer mbuf = null;
            int mbufIndex = -1;
            for (int i = 0; i < mbufList.size(); i++) {
                mbuf = mbufList.get(i);
                if (mbuf.seqNum == seqNum) {
                    found = true;
                    mbuf.add(data, dataNum);
                    mbufIndex = i;
                    Log.i("sms", "Found mbuf with seqNum " + seqNum);
                    break;
                }
            }

            if (!found) {
                mbuf = new MessageBuffer(seqNum);
                mbuf.add(data, dataNum);
                mbufList.add(mbuf);
                mbufIndex = mbufList.size() - 1;
                Log.i("sms", "Added mbuf with seqNum " + seqNum);
            }

            if (mbuf.count == dataCount) {
                byte[] mergedData = mbuf.getData();
                sb = new StringBuilder("");
                for (int i = 0; i < mergedData.length; i++) {
                    sb.append(String.format("%02x", mergedData[i]) + " ");
                }
                Log.i("sms", "Merged data[" + mergedData.length + "]: " + sb.toString());
                SmsAsyncTask task = new SmsAsyncTask(seqNum++, from, mergedData);
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


    public class SmsAsyncTask extends AsyncTask<Void, String, Void> {
        String from;
        byte[] mergedData;
        int seqNum;

        public SmsAsyncTask(int seqNum, String from, byte[] mergedData) {
            this.seqNum = seqNum;
            this.from = from;
            this.mergedData = mergedData;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                int queryLength = mergedData.length - 28;
                if (queryLength < 0)
                    return null;

                DatagramSocket socket = new DatagramSocket();
                byte[] query = new byte[queryLength];
                System.arraycopy(mergedData, 28, query, 0, queryLength);
                DatagramPacket packet = new DatagramPacket(query, query.length);

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
                for (int i = 0; i < query.length; i++) {
                    sb.append(String.format("%02x", query[i]) + " ");
                }
                Log.i("SmsAsyncTask", "Send packet [" + packet.getLength() + "]: " + sb.toString());
                socket.send(packet);

                byte[] buffer = new byte[2048];
                packet.setData(buffer);
                packet.setLength(buffer.length);
                socket.receive(packet);
                sb = new StringBuilder("Received data [" + packet.getLength() + "]: ");
                for (int i = 0; i < packet.getLength(); i++) {
                    sb.append(String.format("%02x", buffer[i]) + " ");
                }
                sb.append("\n");
                Log.i("SmsAsyncTask", "Recv packet [" + packet.getLength() + "]: " + sb.toString());
                publishProgress(sb.toString());

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

    public class MessageBuffer {
        ArrayList<Entry> list;
        int count;
        int length;
        int seqNum;

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
            list = new ArrayList<Entry>();
            count = 0;
            length = 0;
        }

        public void add(byte[] data, int dataNum) {
            if (data == null || dataNum < 1)
                return;

            // Reject duplicates
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