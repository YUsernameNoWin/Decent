import java.io.*;
import java.net.*;
import java.util.*;

public class Node extends Thread {

    protected DatagramSocket socket = null;
    protected BufferedReader in = null;

    public Node() throws IOException {
	this("Node");
    }

    public Node(String name) throws IOException {
        super(name);
        socket = new DatagramSocket(9999);
    }

    public void run() {
    	long counter = 0;
        while (true) {
            try {
                byte[] buf = new byte[256];
               
                // receive request
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            System.out.println(received);
                // figure out response
            String dString = "<html> <body> <h1>hello</h1> <p>master</p></body></html> UID "+ socket.getLocalPort();
            counter++;
   
            buf = dString.getBytes();

        // send the response to the client at "address" and "port"
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}