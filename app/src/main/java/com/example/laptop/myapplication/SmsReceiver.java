package com.example.laptop.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.*;
import android.os.Bundle;
import android.util.*;
/**
 * Created by laptop on 4/15/2015.
 */
public class SmsReceiver extends BroadcastReceiver{
    SmsManager smsm = SmsManager.getDefault();
    MainActivity main_act;

    public void onReceive(Context c, Intent i)
    {
        Log.i("SMSR", "Received");
        Bundle bundle = i.getExtras();
        if(bundle != null)
        {
            /*
            This is apparently not the right way to receive test messages. You're supposed to
            create a loop and iterate through the data array. Perhaps that's for multi-message
            text messages? Whatever, this works for one text message for now.
             */
            Object[] data = (Object[]) bundle.get("pdus");
            SmsMessage msg = SmsMessage.createFromPdu((byte[]) data[0]);
            Log.i("SMSR", msg.getDisplayMessageBody());
            main_act.changeText(msg.getDisplayMessageBody());
        }
    }

    public void setCallingActivity(MainActivity act) {
        main_act = act;
    }
}
