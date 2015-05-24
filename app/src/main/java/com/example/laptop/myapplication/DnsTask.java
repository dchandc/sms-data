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
            /*
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

            DatagramPacket packet = new DatagramPacket(query, query.length);
            packet.setAddress(googleAddress);
            packet.setPort(googlePort);

            DatagramSocket socket = new DatagramSocket(3333);
            Log.i("dns", "Sending packet");
            StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < query.length; i++) {
                sb.append(String.format("%02x", query[i]) + " ");
            }
            Log.i("dns", "Send packet[" + packet.getLength() + "]: " + sb.toString());
            */

            byte[] ipPacket = {
                    0x45, 0x00, // 0: version, IHL, DSCP, ECN
                    0x00, 0x3c, // 2: total length
                    0x0b, (byte) 0xb1, // 4: ID
                    0x40, 0x00, // 6: flags, fragment offset
                    0x40, 0x11, // 8: TTL, protocol
                    0x2c, (byte) 0xb7, // 10: checksum
                    (byte) 0xa9, (byte) 0xe8, 0x57, (byte) 0xfe, // 12: source address
                    (byte) 0x80, 0x61, (byte) 0x80, 0x01, // 16: dest address

                    (byte) 0x88, 0x22, // 20: source port
                    0x00, 0x35, // 22: dest port
                    0x00, 0x28, // 24: length
                    0x02, (byte) 0x83, // 26: checksum

                    (byte) 0x84, (byte) 0xa7, // 28: ID
                    0x01, 0x00, // 30: options
                    0x00, 0x01, // 32: QDCOUNT
                    0x00, 0x00, // 34: ANCOUNT
                    0x00, 0x00, // 36: NSCOUNT
                    0x00, 0x00, // 38: ARCOUNT

                    0x03, 0x77, 0x77, 0x77, // 40: www
                    0x06, 0x67, 0x6f, 0x6f, 0x67, 0x6c, 0x65, // 44: google
                    0x03, 0x63, 0x6f, 0x6d, // 50: com
                    /*
                    0x03, 0x77, 0x77, 0x77, // www
                    0x0c, 0x6e, 0x6f, 0x72, 0x74, 0x68, 0x65, 0x61, 0x73, 0x74, 0x65, 0x72, 0x6e, // northeastern
                    0x03, 0x65, 0x64, 0x75, //edu
                    */
                    0x00, // 51: END
                    0x00, 0x01, // 53: QTYPE
                    0x00, 0x01 // 55: QCLASS
            };

            int queryLength = ipPacket.length - 28;
            byte[] query = new byte[queryLength];
            System.arraycopy(ipPacket, 28, query, 0, queryLength);

            DatagramPacket packet = new DatagramPacket(query, query.length);
            /*
            String addressString = String.format("%d.%d.%d.%d",
                    ipPacket[16] & 0xff, ipPacket[17] & 0xff,
                    ipPacket[18] & 0xff, ipPacket[19] & 0xff);
            InetAddress destAddress = InetAddress.getByName(addressString);
            int destPort = (ipPacket[22] & 0xff) << 8 | (ipPacket[23] & 0xff);
            packet.setAddress(destAddress);
            packet.setPort(destPort);
            */
            packet.setAddress(googleAddress);
            packet.setPort(googlePort);

            DatagramSocket socket = new DatagramSocket(3333);
            Log.i("dns", "Sending packet");
            StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < query.length; i++) {
                sb.append(String.format("%02x", query[i]) + " ");
            }
            Log.i("dns", "Send packet[" + packet.getLength() + "]: " + sb.toString());

            socket.send(packet);
            Log.i("dns", "Receiving packet");
            byte[] buffer = new byte[512];
            packet.setData(buffer);
            packet.setLength(buffer.length);
            socket.receive(packet);
            sb = new StringBuilder("");
            for (int i = 0; i < packet.getLength(); i++) {
                sb.append(String.format("%02x", buffer[i]) + " ");
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
