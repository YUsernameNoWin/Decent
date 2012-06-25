import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.PublicKey;
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
    static Encryption e = new Encryption();
    public static void main(String[] args) throws UnknownHostException, Exception {
        
    	JettyTestServer server = new JettyTestServer();
    	server.start();
       Master master =new Master();
        ArrayList<NetworkThread> peers = new ArrayList<NetworkThread>();
           master.start();

           KeyPair top = getKey();
           KeyPair keys1 = getKey();
           KeyPair keys2 = getKey();
           KeyPair keys3 = getKey();
           int temp = 4;
        for(int i=1;i<=5;i++)
        {
          peers.add(new NetworkThread(i*10+500,temp,keys1,(keys1 = e.generateKey()),top));
          temp++;
          peers.add(new NetworkThread(i*10+501,temp,keys2,(keys2 = e.generateKey()),top));
          temp++;
          peers.add(new NetworkThread(i*10+502,temp,keys3,(keys3 = e.generateKey()),top));
          temp++;
        }
        
        

        for(NetworkThread a:peers)
        {
            a.start();
           // scan.next();
            //System.out.print(a.port + " ");
           sleep(300);
        }
        sleep(50000);
        for(NetworkThread a:peers)
        {
        	//if(a.up.publicKey == null || a.down.publicKey == null)
        		System.out.println(a.id);
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
    public static KeyPair getKey(){
		try{
			String temp = "";
			String key  = "";
			
			Scanner scan = new Scanner(new File("out.txt"));
			while(!(temp = scan.next()).contains("PrivKey")){
				key +=temp;

			}
			KeyPair a = new KeyPair(e.getPublicKeyFromString(key), null);
			return a;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }
}
