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
            /*
            SmsMessage msg = SmsMessage.createFromPdu((byte[]) data[0]);
            Log.i("SMSR", msg.getDisplayMessageBody());
            main_act.changeText(msg.getDisplayMessageBody());
            */
            String str = "";
            SmsMessage[] msgs = new SmsMessage[data.length];
            for (int j=0; j<msgs.length; j++){
                msgs[j] = SmsMessage.createFromPdu((byte[])data[j]);
                str += "SMS from " + msgs[j].getOriginatingAddress();
                str += " [" + j + "]:";
                str += msgs[j].getMessageBody();
                Log.i("RECV", msgs[j].getMessageBody());

                byte[] raw = Base64.decode(msgs[j].getMessageBody(), Base64.DEFAULT);
                /*String temp = "";
                for(int k = 0; k < raw.length; k++) {
                    temp += String.format("0x%02X", raw[k]) + " ";
                    if((k+1)%8 == 0) {
                        Log.i("RECV", temp);
                        temp = "";
                    }
                }
                Log.i("RECV", temp);
                */
                str += "\n";
            }
            Log.i("sms", str);
            //---display the new SMS message---
            Toast.makeText(main_act, str, Toast.LENGTH_SHORT).show();
        }
    }

    public void setCallingActivity(MainActivity act) {
        main_act = act;
    }
}
