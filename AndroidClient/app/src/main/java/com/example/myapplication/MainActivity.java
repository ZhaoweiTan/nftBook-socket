package com.example.myapplication;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    EditText serverAddr, clientPort, serverPort;
    Button buttonConnect;
    TextView outputText;
    DatagramSocket SendSocket;
    UdpSendThread udpSendThread;
    Boolean isRunning = false;

    private final static int MAXPKTSIZE = 99999;
    private static final String TAG = "UDPSocket";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverAddr = (EditText) findViewById(R.id.editTextServerAddr);
        clientPort = (EditText) findViewById(R.id.editTextClientPort);
        serverPort = (EditText) findViewById(R.id.editTextServerPort);

        buttonConnect = (Button) findViewById(R.id.StartClient);
        buttonConnect.setOnClickListener(buttonConnectOnClickListener);

        outputText = (TextView)findViewById(R.id.Output);
        outputText.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    View.OnClickListener buttonConnectOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (!isRunning) {
                // Change the state
                isRunning = true;
                buttonConnect.setText("Disconnect");
                outputText.setText("Start thread");
                serverAddr.setEnabled(false);
                clientPort.setEnabled(false);
                serverPort.setEnabled(false);
                // end

                try {
                    // get port and address
                    int server_port = Integer.parseInt(serverPort.getText().toString());
                    int client_port = Integer.parseInt(clientPort.getText().toString());
                    InetAddress server_addr = InetAddress.getByName(serverAddr.getText().toString());

                    SendSocket = new DatagramSocket(client_port);

                    udpSendThread = new UdpSendThread(SendSocket, server_port, server_addr);
                    udpSendThread.start();
                } catch (Exception e) {
                    Log.e(TAG, "exception", e);
                    return;
                }
            } else {
                // Change the state
                isRunning = false;
                buttonConnect.setText("Connect");
                outputText.setText("Close thread");
                serverAddr.setEnabled(true);
                clientPort.setEnabled(true);
                serverPort.setEnabled(true);

                // kill the thread
                udpSendThread.setRunning(false);
                udpSendThread.interrupt();
                udpSendThread = null;

                // close the socket
                if(SendSocket != null) {
                    SendSocket.close();
                    SendSocket = null;
                }

            }
        }
    };


    private class UdpSendThread extends Thread{

        DatagramSocket SendSocket;
        int ServerPort;
        InetAddress ServerAddr;
        boolean running = true;

        public UdpSendThread(DatagramSocket _sendsocket, int _serverport, InetAddress _serveraddr) throws SocketException {
            super();
            this.SendSocket = _sendsocket;
            this.ServerPort = _serverport;
            this.ServerAddr = _serveraddr;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }

        public void setRunning(boolean _running){
            this.running = _running;
            closeSockets();
        }

        private void closeSockets() {
            if (SendSocket != null) {
                SendSocket.close();
            }
        }

        @Override
        public void run() {
            try {
                // prepare the message
                String messageStr = "Hello Android!";
                int msg_length = messageStr.length();
                byte[] message = messageStr.getBytes();

                // send the message
                DatagramPacket p = new DatagramPacket(message, msg_length, ServerAddr, ServerPort);
                Log.d(TAG, "message sent");
                SendSocket.send(p);

                // receiving the message
                while(running) {
                    byte[] buf = new byte[MAXPKTSIZE];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    SendSocket.receive(packet);
                    String receive_string = new String(packet.getData(), 0, packet.getLength() );
                    Log.d(TAG, "message receive: " + receive_string);
                }
            } catch (Exception e) {
                // Log.e(TAG, "exception", e);
            } finally {
                closeSockets();
            }
        }
    }
}
