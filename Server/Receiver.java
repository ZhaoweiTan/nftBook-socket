import java.io.IOException;
import java.net.*;
import java.nio.*;

public class Receiver {

    public static void main(String[] args) {
        int port = 48010;
        new Receiver().run(port);
    }

    public void run(int port) {    
      try {
        DatagramSocket serverSocket = new DatagramSocket(port);
        byte[] receiveData = new byte[1146];
     
        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                           receiveData.length);
        System.out.println("Start Listening");
        int total_packet_received = 0;
        while(true)
        {
              serverSocket.receive(receivePacket);
              total_packet_received ++;
              byte[] metadata = {receiveData[0], receiveData[1]}; 
              ByteBuffer wrapped = ByteBuffer.wrap(metadata);

              Short md = wrapped.getShort();
              String sentence = new String( receivePacket.getData(), 0,
                                 receivePacket.getLength() );
              System.out.println("RECEIVED: " + Integer.toString(sentence.length()));
              System.out.println(Integer.toString(md));
              // now send acknowledgement packet back to sender     
              // InetAddress IPAddress = receivePacket.getAddress();
              // String sendString = "ACK";
              // byte[] sendData = sendString.getBytes();
              // System.out.println(IPAddress.toString() +  Integer.toString(receivePacket.getPort()));
              // DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
              //      IPAddress, receivePacket.getPort());
              // serverSocket.send(sendPacket);
        }
      } catch (IOException e) {
              System.out.println(e);
      }
      // should close serverSocket in finally block
    }
}