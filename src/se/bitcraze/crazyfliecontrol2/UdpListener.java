package se.bitcraze.crazyfliecontrol2;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class UdpListener implements Runnable {

    private int port = 5000;
    private String ipAddress = "192.168.4.1";
    WifiManager wifiManager;

    UdpListener(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    @Override
    public void run() {
        boolean run = true;
        while (run) {
            try {
//                Solution 2
//                Socket client = new Socket();
//                int serverIP = this.wifiManager.getDhcpInfo().serverAddress;
//                String stringIP = android.text.format.Formatter.formatIpAddress(serverIP);
//                InetAddress address = InetAddress.getByName(stringIP);
//                SocketAddress socketAddress = new InetSocketAddress(address, 5000);
//                client.connect(socketAddress, 5000);

//                Solution 1
                byte[] messageByte = new byte[512];
                Socket socket = new Socket(ipAddress, 5000);
                DataInputStream inpuStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                inpuStream.read(messageByte);


//                Solution 0
//                InetAddress ip = InetAddress.getByName(ipAddress);
//                DatagramSocket udpSocket = new DatagramSocket();
//                udpSocket.setSoTimeout(10000);
//                udpSocket.setBroadcast(true);
//                byte[] message = new byte[512];
//                DatagramPacket packet = new DatagramPacket(message,message.length);
//                Log.i("UDP client: ", "about to wait to receive");
//                udpSocket.receive(packet);

                Log.d("Received data", "...");
            } catch (Exception e) {
                Log.e("UDP client Error", "error: ", e);
                run = false;
            }
        }
    }
}
