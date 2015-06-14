package com.example.laptop.smsdataproxy;

import android.os.AsyncTask;
import android.widget.TextView;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

/**
 * Created by Dennis on 4/27/2015.
 */
public class HttpTask extends AsyncTask<String, Void, String> {
    TextView tv;
    public HttpTask(TextView view) {
        tv = view;
    }
    @Override
    protected String doInBackground(String... urls) {

        HttpClient hclient = new DefaultHttpClient();
        HttpGet hget = new HttpGet(urls[0]);
        try {
            String s = hclient.execute(hget, new BasicResponseHandler());
            return s;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        tv.setText(s);
    }
}
