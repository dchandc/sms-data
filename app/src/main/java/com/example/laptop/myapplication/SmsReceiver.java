package com.example.laptop.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import android.util.Base64;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by laptop on 4/15/2015.
 */
public class SmsReceiver extends BroadcastReceiver{
    MainActivity main_act;
    SmsManager smsManager;
    DatagramSocket socket;
    String googleDns = "8.8.8.8";
    int dnsPort = 53;
    int bytesPerSms = 118;
    ArrayList<MessageBuffer> mbufList;

    public SmsReceiver() {
        smsManager = SmsManager.getDefault();
        mbufList = new ArrayList<MessageBuffer>();
        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onReceive(Context context, Intent intent)
    {
        Log.i("SMSR", "Received");
        Bundle bundle = intent.getExtras();
        if(bundle == null)
            return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdus[0]);
        String from = sms.getOriginatingAddress();
        byte[] raw = Base64.decode(sms.getMessageBody(), Base64.DEFAULT);
        if (raw.length < 3)
            return;

        int seqNum = raw[0];
        int dataNum = raw[1];
        int dataCount = raw[2];
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
                break;
            }
        }

        if (!found) {
            mbuf = new MessageBuffer(seqNum);
            mbuf.add(data, dataNum);
        }

        if (mbuf.count == dataCount) {
            byte[] mergedData = mbuf.getData();
            Thread t = new Thread(new SmsRunnable(mergedData, from));
            t.start();
            mbufList.remove(mbufIndex);
        }
    }

    public void setCallingActivity(MainActivity act) {
        main_act = act;
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

    public class SmsRunnable implements Runnable {
        byte[] raw;
        String from;

        public SmsRunnable(byte[] raw, String from) {
            this.raw = raw;
            this.from = from;

            String temp = "";
            for(int k = 0; k < raw.length; k++) {
                temp += String.format("0x%02X", raw[k]) + " ";
                if((k+1)%8 == 0) {
                    Log.i("sms", temp);
                    temp = "";
                }
            }
            Log.i("sms", temp);
        }

        @Override
        public void run() {
            int queryLength = raw.length - 28;
            if (queryLength < 0)
                return;

            byte[] query = new byte[queryLength];
            System.arraycopy(raw, 28, query, 0, queryLength);
            DatagramPacket packet = new DatagramPacket(query, query.length);

            try {
                InetAddress destAddress;
                int destPort = (raw[22] & 0xff) << 8 | (raw[23] & 0xff);
                if (destPort == dnsPort) {
                    destAddress = InetAddress.getByName(googleDns);
                } else {
                    String addressString = String.format("%d.%d.%d.%d",
                            raw[16] & 0xff, raw[17] & 0xff,
                            raw[18] & 0xff, raw[19] & 0xff);
                    destAddress = InetAddress.getByName(addressString);
                }
                Log.i("sms", "Dest address " + destAddress.toString());
                packet.setAddress(destAddress);
                packet.setPort(destPort);

                StringBuilder sb = new StringBuilder("");
                for (int i = 0; i < query.length; i++) {
                    sb.append(String.format("%02x", query[i]) + " ");
                }
                Log.i("dns", "Send packet[" + packet.getLength() + "]: " + sb.toString());
                socket.send(packet);

                byte[] buffer = new byte[2048];
                packet.setData(buffer);
                packet.setLength(buffer.length);
                socket.receive(packet);
                sb = new StringBuilder("");
                for (int i = 0; i < packet.getLength(); i++) {
                    sb.append(String.format("%02x", buffer[i]) + " ");
                }
                Log.i("dns", "Recv packet[" + packet.getLength() + "]: " + sb.toString());

                for (int i = 0; i < (packet.getLength() / bytesPerSms) + 1; i++) {
                    int offset = i * bytesPerSms;
                    int len = (packet.getLength() - offset) % bytesPerSms;
                    byte[] sub = new byte[len];
                    System.arraycopy(buffer, offset, sub, 0, len);
                    String msg = Base64.encodeToString(sub, Base64.DEFAULT);
                    smsManager.sendTextMessage(from, null, msg, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
