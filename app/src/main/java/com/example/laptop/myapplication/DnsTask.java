package com.example.laptop.myapplication;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Dennis on 5/22/2015.
 */
public class DnsTask extends AsyncTask<String, Void, String> {
    TextView tv;
    public DnsTask(TextView view) {
        tv = view;
    }
    @Override
    protected String doInBackground(String... urls) {
        String str = "Test";
        try {
            InetAddress googleAddress = InetAddress.getByName("8.8.8.8");
            int googlePort = 53;
            byte[] query = {
                    0x11, 0x22, // ID
                    0x01, 0x00, // flags
                    0x00, 0x01, // QDCOUNT
                    0x00, 0x00, // ANCOUNT
                    0x00, 0x00, // NSCOUNT
                    0x00, 0x00, // ARCOUNT
                    0x03, 0x77, 0x77, 0x77, // www
                    0x0c, 0x6e, 0x6f, 0x72, 0x74, 0x68, 0x65, 0x61, 0x73, 0x74, 0x65, 0x72, 0x6e, // northeastern
                    0x03, 0x65, 0x64, 0x75, //edu
                    0x00,
                    0x00, 0x01,
                    0x00, 0x01
            };
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(query, query.length);
            packet.setAddress(googleAddress);
            packet.setPort(googlePort);

            DatagramSocket socket = new DatagramSocket(3333);
            Log.i("dns", "Sending packet");
            StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < query.length; i++) {
                sb.append(String.format("%2x", query[i]) + " ");
            }
            Log.i("dns", "Send packet[" + packet.getLength() + "]: " + sb.toString());
            socket.send(packet);
            Log.i("dns", "Receiving packet");
            packet.setData(buffer);
            packet.setLength(buffer.length);
            socket.receive(packet);
            sb = new StringBuilder("");
            for (int i = 0; i < packet.getLength(); i++) {
                sb.append(String.format("%2x", buffer[i]) + " ");
            }
            Log.i("dns", "Recv packet[" + packet.getLength() + "]: " + sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("dns", "Exception");
        }

        return str;
    }

    @Override
    protected void onPostExecute(String s) {
        tv.setText(s);
    }
}
