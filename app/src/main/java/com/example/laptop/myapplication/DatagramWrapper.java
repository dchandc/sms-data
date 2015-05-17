package com.example.laptop.myapplication;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Dennis on 5/17/2015.
 */
public class DatagramWrapper {
    private static final int HEADER_LEN = 6;

    static byte[] getByteArray(DatagramPacket packet) {
        InetAddress inetAddress = packet.getAddress();
        byte[] addressArray = inetAddress.getAddress();
        int port = packet.getPort();
        byte[] portArray = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(port).array();
        byte[] dataArray = packet.getData();
        byte[] byteArray = new byte[dataArray.length + HEADER_LEN];
        System.arraycopy(addressArray, 0, byteArray, 0, 4);
        System.arraycopy(portArray, 2, byteArray, 4, 2);
        System.arraycopy(dataArray, 0, byteArray, HEADER_LEN, dataArray.length);

        return byteArray;
    }

    static DatagramPacket getPacket(byte[] byteArray) {
        if (byteArray.length < HEADER_LEN)
            return null;

        InetAddress inetAddress = null;
        byte[] addressArray = new byte[4];
        System.arraycopy(byteArray, 0, addressArray, 0, 4);
        try {
            inetAddress = InetAddress.getByAddress(addressArray);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        byte[] portArray = new byte[4];
        System.arraycopy(byteArray, 4, portArray, 2, 2);
        int port = ByteBuffer.wrap(portArray).order(ByteOrder.BIG_ENDIAN).getInt();

        int dataLength = byteArray.length - HEADER_LEN;
        byte[] dataArray = new byte[dataLength];
        System.arraycopy(byteArray, HEADER_LEN, dataArray, 0, dataLength);

        DatagramPacket packet = new DatagramPacket(dataArray, dataLength);
        packet.setAddress(inetAddress);
        packet.setPort(port);

        return packet;
    }
}
