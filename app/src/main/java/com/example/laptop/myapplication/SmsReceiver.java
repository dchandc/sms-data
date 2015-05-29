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

/**
 * Created by laptop on 4/15/2015.
 */
public class SmsReceiver extends BroadcastReceiver{
    MainActivity main_act;
    SmsManager smsManager;
    DatagramSocket socket;
    String googleDns = "8.8.8.8";
    int dnsPort = 53;
    int bytesPerSms = 120;

    public SmsReceiver() {
        smsManager = SmsManager.getDefault();
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
        if(bundle != null)
        {
            Object[] data = (Object[]) bundle.get("pdus");
            for (int j=0; j < data.length; j++){
                SmsMessage sms = SmsMessage.createFromPdu((byte[])data[j]);
                Thread t = new Thread(new SmsRunnable(sms));
                t.start();
            }

            //---display the new SMS message---
            //Toast.makeText(main_act, str, Toast.LENGTH_SHORT).show();
            Toast.makeText(main_act, "Done", Toast.LENGTH_SHORT).show();
        }
    }

    public void setCallingActivity(MainActivity act) {
        main_act = act;
    }

    public class SmsRunnable implements Runnable {
        SmsMessage sms;
        public SmsRunnable(SmsMessage sms) {
            this.sms = sms;
        }

        @Override
        public void run() {
            String from = sms.getOriginatingAddress();
            /*
            String str = "";
            str += "SMS from " + sms.getOriginatingAddress();
            str += " [" + j + "]:";
            str += sms.getMessageBody();
            str += "\n";
            Log.i("sms", str);
            */

            byte[] raw = Base64.decode(sms.getMessageBody(), Base64.DEFAULT);
            /*
            String temp = "";
            for(int k = 0; k < raw.length; k++) {
            temp += String.format("0x%02X", raw[k]) + " ";
            if((k+1)%8 == 0) {
                Log.i("RECV", temp);
                temp = "";
                }
            }
            Log.i("RECV", temp);
            */

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
                    byte[] sub = new byte[bytesPerSms];
                    int offset = i * bytesPerSms;
                    int len = (packet.getLength() - offset) % bytesPerSms;
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
