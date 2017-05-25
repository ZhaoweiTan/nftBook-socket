import java.io.IOException;
import java.net.*;

public class Receiver {

    public static void main(String[] args) {
        int port = 48010;
        new Receiver().run(port);
    }

    public void run(int port) {    
      try {
        DatagramSocket serverSocket = new DatagramSocket(port);
        byte[] receiveData = new byte[2000];
     
        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                           receiveData.length);
        System.out.println("Start Listening");

        while(true)
        {
              serverSocket.receive(receivePacket);
              String sentence = new String( receivePacket.getData(), 0,
                                 receivePacket.getLength() );
              System.out.println("RECEIVED: " + Integer.toString(sentence.length()));
              System.out.println(sentence);
              // now send acknowledgement packet back to sender     
              InetAddress IPAddress = receivePacket.getAddress();
              String sendString = "ACK";
              byte[] sendData = sendString.getBytes();
              System.out.println(IPAddress.toString() +  Integer.toString(receivePacket.getPort()));
              DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                   IPAddress, receivePacket.getPort());
              serverSocket.send(sendPacket);
        }
      } catch (IOException e) {
              System.out.println(e);
      }
      // should close serverSocket in finally block
    }
}