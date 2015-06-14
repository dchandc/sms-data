package com.example.laptop.smsdataproxy;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class SmsTask extends AsyncTask<Void, Void, Void> {
    Context context;
    TextView tv;
    String phoneNo;
    SmsManager smsManager = SmsManager.getDefault();

    public SmsTask(Context context, TextView tv, String phoneNo) {
        this.context = context;
        this.tv = tv;
        this.phoneNo = phoneNo;
    }

    @Override
    protected Void doInBackground(Void... urls) {
        try {
            String msg;

            /*
            byte [] all = new byte[100];
            for(int i = 0; i < 100; i++)
            {
                all[i] = (byte) i;
            }
            msg = Base64.encodeToString(all, Base64.DEFAULT);
            Log.i("sms", "Sending message [" + msg.length() + "]: " + msg);
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Log.i("sms", "Message sent");
            */

            byte [] all = new byte[64];
            all[0] = 100;
            all[1] = 1;
            all[2] = 2;
            for(int i = 3; i < 64; i++)
            {
                all[i] = (byte) i;
            }
            msg = Base64.encodeToString(all, Base64.NO_WRAP);
            Log.i("sms", "Sending message [" + msg.length() + "]: " + msg);
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Log.i("sms", "Message sent");

            all[0] = 100;
            all[1] = 2;
            all[2] = 2;
            for(int i = 3; i < 64; i++)
            {
                all[i] = (byte) i;
            }
            msg = Base64.encodeToString(all, Base64.NO_WRAP);
            Log.i("sms", "Sending message [" + msg.length() + "]: " + msg);
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Log.i("sms", "Message sent");

        } catch (Exception e) {
            e.printStackTrace();
            Log.i("sms", "Exception");
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void ignored) {
        Toast.makeText(context, "Sent SMS to " + phoneNo, Toast.LENGTH_SHORT).show();
    }
}