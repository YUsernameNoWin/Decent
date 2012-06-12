import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.*;

/* Main class. Initiates Peers.*/
public class runner extends Thread{

    /**
     * @param args
     * @throws IOException 
     * @throws UnknownHostException 
     */
    public static void main(String[] args) throws UnknownHostException, Exception {
        // TODO Auto-generated method stub
    	JettyTestServer server = new JettyTestServer();
    	server.start();
       Master master =new Master();
        ArrayList<NetworkThread> peers = new ArrayList<NetworkThread>();
           master.start();
           sleep(1000);


           int temp = 4;
        for(int i=1;i<=50;i++)
        {
           peers.add(new NetworkThread(i*10+500,temp));
           temp++;
          peers.add(new NetworkThread(i*10+501,temp));
          temp++;
          peers.add(new NetworkThread(i*10+502,temp));
          temp++;
        }
        
        

        for(NetworkThread a:peers)
        {
            a.start();
           // scan.next();
            //System.out.print(a.port + " ");
           sleep(200);
        }
        //scan.next();
       // peers.get(4).close();
       // peers.get(5).close();

        /*
        new NetworkThread(master.holes.remove().up.port + 10,8213).start();
        sleep(100);
        new NetworkThread(master.holes.remove().up.port + 10,80).start();
        sleep(100);
        //peers.get(1).close();
       new NetworkThread(master.holes.remove().up.port + 10,90).start();

*/
        
    }

}
