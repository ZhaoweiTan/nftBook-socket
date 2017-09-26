package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    EditText serverAddr, clientPort, serverPort;
    Button buttonConnect;
    TextView outputText;
    DatagramSocket SendSocket;
    UdpSendThread udpSendThread;
    Boolean isRunning = false;
    private SensorManager mSensorManager;
    private Sensor mLight;

    private final static int MAXPKTSIZE = 99999;
    private static final String TAG = "UDPSocket";
    private static final String TAG2 = "Receiver";

    private boolean new_mac = false;
    private float mac_loss;
    private float mac_retx_delay;
    private boolean new_rlc = false;
    private float rlc_loss;
    private float rlc_retx_delay;

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

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        IntentFilter mac_loss_filter = new IntentFilter("MobileInsight.LteMacAnalyzer.MAC_RETX");
        IntentFilter rlc_loss_filter = new IntentFilter("MobileInsight.LteMacAnalyzer.RLC_RETX");
        registerReceiver(MobileInsight_Receiver, mac_loss_filter);
        registerReceiver(MobileInsight_Receiver, rlc_loss_filter);
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

        int currentFrame = -1;
        int last_packet_id = 0;
        Date ulPktTime;
        Date firstPktTime;

        private static final int packet_max = 2;


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

                // prepare the file
                String filename = "results.csv";
                FileOutputStream outputStream = null;
                try {
                    outputStream = openFileOutput(filename, MODE_APPEND);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                // receiving the message
                while(running) {
                    ulPktTime = new Date();

                    // send the message
                    DatagramPacket p = new DatagramPacket(message, msg_length, ServerAddr, ServerPort);
                    SendSocket.send(p);
                    // Log.d(TAG2, "message sent " + firstPktTime.toString());

                    // Log.i(TAG2, "Start running");
                    byte[] buf = new byte[MAXPKTSIZE];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);


                    int i = 0;
                    for (; i < packet_max; i++) {
                        SendSocket.receive(packet);
                        if (i == 0) {
                            firstPktTime = new Date();
                        }
                        String receive_string = new String(packet.getData(), 0, packet.getLength() );
                        Log.d(TAG2, "the string is " + receive_string);
                    }

                    // Log.d(TAG2, "last packet " + new Date().toString());

                    long timeDif = (new Date()).getTime() - ulPktTime.getTime();
                    long timeDif2 = (new Date()).getTime() - firstPktTime.getTime();

                    if (!new_mac) {
                        mac_loss = 0;
                        mac_retx_delay = 0;
                    } else {
                        new_mac = false;
                    }

                    if (!new_rlc) {
                        rlc_loss = 0;
                        rlc_retx_delay = 0;
                    } else {
                        new_mac = false;
                    }
                    Log.i(TAG2, "RTT " + String.valueOf(timeDif) + "ms, Trans delay " + String.valueOf(timeDif2) + "ms, MAC loss " + String.valueOf(mac_loss) + " packets, average delay is " + String.valueOf(mac_retx_delay) + " ,RLC loss " + String.valueOf(rlc_loss) + " ,RLC delay " + String.valueOf(rlc_retx_delay));

                    String csvString = "";
                    csvString += String.valueOf(timeDif) + ",";
                    csvString += String.valueOf(timeDif2) + ",";
                    csvString += String.valueOf(mac_loss) + ",";
                    csvString += String.valueOf(mac_retx_delay) + ",";
                    csvString += String.valueOf(rlc_loss) + ",";
                    csvString += String.valueOf(rlc_retx_delay) + ",";
                    csvString += "\n";

                    try {
                        outputStream.write(csvString.getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    sleep(100);

//                    String frame_id_string = receive_string.substring(0, 1);
//                    String packet_id_string = receive_string.substring(1, 2);
//
//                    int frame_id = Integer.parseInt(frame_id_string);
//                    int packet_id = Integer.parseInt(packet_id_string);
//                    // Log.d(TAG2, "frame id is: " + String.valueOf(frame_id) + " packet id is: " + String.valueOf(packet_id));

//                    if (currentFrame == frame_id) {
//                        if (packet_id == last_packet_id + 1) {
//                            last_packet_id = packet_id;
//                            if (packet_id == packet_max - 1) {
//                                if (!new_mac) {
//                                    mac_loss = 0;
//                                    mac_retx_delay = 0;
//                                } else {
//                                    new_mac = false;
//                                }
//                                float timeDif = (new Date()).getTime() - firstPktTime.getTime();
//                                Log.i(TAG2, "The transmission delay is " + String.valueOf(timeDif) + "ms, MAC loss " + String.valueOf(mac_loss) + " packets, average delay is " + String.valueOf(mac_retx_delay) );
//                            }
//                        }
//                    } else if (currentFrame < frame_id || frame_id == 0) {
//                        if (packet_id == 0) {
//                            firstPktTime = new Date();
//                            currentFrame = frame_id;
//                            last_packet_id = 0;
//                        }
//                    }

                }
            } catch (Exception e) {
                Log.e(TAG2, "exception", e);
            } finally {
                closeSockets();
            }
        }
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Do something with this sensor value.
        // Log.d(TAG, Float.toString(x) + ' ' + Float.toString(y) + ' ' + Float.toString(z));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    private final BroadcastReceiver MobileInsight_Receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.appwidget.action.APPWIDGET_ENABLED")) {
            //TODO: ???
        } else if (intent.getAction().equals("MobileInsight.LteMacAnalyzer.MAC_RETX")) {
            new_mac = true;
            mac_loss = Float.parseFloat(intent.getStringExtra("packet loss (pkt/s)"));
            mac_retx_delay =  Float.parseFloat(intent.getStringExtra("retransmission delay (ms/pkt)"));
        } else if (intent.getAction().equals("MobileInsight.LteMacAnalyzer.RLC_RETX")) {
            new_rlc = true;
            rlc_loss = Float.parseFloat(intent.getStringExtra("packet loss (pkt/s)"));
            rlc_retx_delay =  Float.parseFloat(intent.getStringExtra("retransmission delay (ms/pkt)"));
        }
    }
};
}
