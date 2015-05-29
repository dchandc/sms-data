package com.example.laptop.myapplication;

import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Dennis on 5/22/2015.
 */
public class SmsTask extends AsyncTask<String, Void, String> {
    TextView tv;
    public SmsTask(TextView view) {
        tv = view;
    }
    @Override
    protected String doInBackground(String... urls) {
        SmsManager smsManager = SmsManager.getDefault();
        String str = "Sent";
        try {
            //String phoneNo = "5556";
            String phoneNo = "16262158107";
            String msg;

            byte [] all = new byte[64];
            all[0] = 100;
            all[1] = 1;
            all[2] = 2;
            for(int i = 3; i < 64; i++)
            {
                all[i] = (byte) i;
            }
            msg = Base64.encodeToString(all, Base64.DEFAULT);
            Log.i("sms", "Sending message: " + msg);
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Log.i("sms", "Message sent");

            all[0] = 101;
            all[1] = 2;
            all[2] = 3;
            msg = Base64.encodeToString(all, Base64.DEFAULT);
            Log.i("sms", "Sending message: " + msg);
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Log.i("sms", "Message sent");

            all[0] = 102;
            all[1] = 1;
            all[2] = 1;
            msg = Base64.encodeToString(all, Base64.DEFAULT);
            Log.i("sms", "Sending message: " + msg);
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Log.i("sms", "Message sent");

            all[0] = 101;
            all[1] = 1;
            all[2] = 3;
            msg = Base64.encodeToString(all, Base64.DEFAULT);
            Log.i("sms", "Sending message: " + msg);
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Log.i("sms", "Message sent");

            all[0] = 101;
            all[1] = 3;
            all[2] = 3;
            msg = Base64.encodeToString(all, Base64.DEFAULT);
            Log.i("sms", "Sending message: " + msg);
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Log.i("sms", "Message sent");

        } catch (Exception e) {
            e.printStackTrace();
            Log.i("sms", "Exception");
        }

        return str;
    }

    @Override
    protected void onPostExecute(String s) {
        tv.setText(s);
    }
}

