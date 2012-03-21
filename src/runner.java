import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Scanner;

/* Main class. Initiats Peers.*/
public class runner {

    /**
     * @param args
     * @throws IOException 
     * @throws UnknownHostException 
     */
    public static void main(String[] args) throws UnknownHostException, Exception {
        // TODO Auto-generated method stub


       Master master =new Master();
        ArrayList<NetworkThread> peers = new ArrayList<NetworkThread>();
           master.start();
           int temp = 1;
        for(int i=1;i<=5;i++)
        {
           peers.add(new NetworkThread(i*10+500,temp));
           temp++;
          peers.add(new NetworkThread(i*10+501,temp));
          temp++;
          peers.add(new NetworkThread(i*10+502,temp));
          temp++;
        }
        
        
        Scanner scan = new Scanner(System.in);
        for(NetworkThread a:peers)
        {
            a.start();
            System.out.print(a.port + " ");
            scan.next();
        }
        
    }

}
