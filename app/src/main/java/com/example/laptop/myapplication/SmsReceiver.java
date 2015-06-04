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

public class SmsReceiver extends BroadcastReceiver{
    MainActivity main_act;
    SmsManager smsManager;
    DatagramSocket socket;
    String googleDns = "8.8.8.8";
    int dnsPort = 53;
    int bytesPerSms = 118;

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
        Log.i("SmsReceiver", "Received");
        Bundle bundle = intent.getExtras();
        if(bundle != null)
        {
            Object[] data = (Object[]) bundle.get("pdus");
            for (int j=0; j < data.length; j++){
                SmsMessage sms = SmsMessage.createFromPdu((byte[])data[j]);
                SmsAsyncTask task = new SmsAsyncTask(sms);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                else
                    task.execute();
                Log.i("SmsReceiver", "Executed new SmsAsyncTask");
            }
        }
    }

    public void setCallingActivity(MainActivity act) {
        main_act = act;
    }


    public class SmsAsyncTask extends AsyncTask<Void, String, Void> {
        SmsMessage sms;
        public SmsAsyncTask(SmsMessage sms) {
            this.sms = sms;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String from = sms.getOriginatingAddress();
            String body = sms.getMessageBody();

            StringBuilder sb = new StringBuilder("SMS message from " +
                    from + " [" + body.length() + "]: " +
                    body);
            publishProgress(sb.toString());

            byte[] raw = Base64.decode(sms.getMessageBody(), Base64.DEFAULT);

            sb = new StringBuilder("Byte64-decoded SMS message [" + raw.length + "]: ");
            for (int i = 0; i < raw.length; i++) {
                sb.append(String.format("%02x ", raw[i]));
            }
            sb.append("\n");
            publishProgress(sb.toString());

            int queryLength = raw.length - 28;
            if (queryLength < 0)
                return null;

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
                Log.i("SmsAsyncTask", "Destination address " + destAddress.toString());
                packet.setAddress(destAddress);
                packet.setPort(destPort);

                sb = new StringBuilder("");
                for (int i = 0; i < query.length; i++) {
                    sb.append(String.format("%02x", query[i]) + " ");
                }
                Log.i("SmsAsyncTask", "Send packet [" + packet.getLength() + "]: " + sb.toString());
                socket.send(packet);

                byte[] buffer = new byte[2048];
                packet.setData(buffer);
                packet.setLength(buffer.length);
                socket.receive(packet);
                sb = new StringBuilder("");
                for (int i = 0; i < packet.getLength(); i++) {
                    sb.append(String.format("%02x", buffer[i]) + " ");
                }
                Log.i("SmsAsyncTask", "Recv packet [" + packet.getLength() + "]: " + sb.toString());

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
                return null;
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... message) {
            if (message != null && message.length > 0)
                main_act.appendText(message[0]);
        }
    }
}