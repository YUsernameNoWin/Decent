import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.*;
/* TODO Work on fixing left right connections. Other than that, all encryption issues are done.*/
/* Main class. Initiates Peers.*/
public class runner extends Thread{

    /**
     * @param args
     * @throws IOException 
     * @throws UnknownHostException 
     */
	static PublicKey publicKey;
	static PrivateKey privateKey;
    static Encryption e = new Encryption();
    public static void main(String[] args) throws UnknownHostException, Exception {
        
    	JettyTestServer server = new JettyTestServer();
    	server.start();
        getKey2();
        KeyPair top =  new KeyPair(publicKey,privateKey);
        KeyPair keys1 = getKey();
        KeyPair keys2 = getKey();
        KeyPair keys3 = getKey();
       Master master =new Master(top,keys1);
        ArrayList<NetworkThread> peers = new ArrayList<NetworkThread>();
           master.start();
           //KeyPair news = e.generateKey();
           //saveKey2(news.getPublic(),news.getPrivate());
 
           int temp = 4;
        for(int i=1;i<=30;i++)
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
        Scanner scan =  new Scanner(System.in);
       scan.next();
        for(NetworkThread a:peers)
        {
        	a.getKeyStatus();
        	a.getSocketStatus();
        	
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
			
			Scanner scan = new Scanner(new File("C:/Users/Quinn/Disporic/Decent/out.txt"));
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
    public static void getKey2(){
		KeyPair pair = null;
		try{

			String temp = "";
			String key  = "";
			
			Scanner scan = new Scanner(new File("C:/Users/Quinn/Disporic/Decent/out2.txt"));
			while(!(temp = scan.next()).contains("PrivKey")){
				key +=temp;

			}
			publicKey = e.getPublicKeyFromString(key);
			key = "";
			while(scan.hasNext()){
				key+=scan.next();
			}
			privateKey = e.getPrivateKeyFromString(key);
		

		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
	public static void saveKey2(PublicKey publicKey, PrivateKey privateKey)
	{
		  try
		  {
			  // Create file 
				  FileWriter fstream = new FileWriter("out2.txt",true);
				  BufferedWriter out = new BufferedWriter(fstream);
				  
				  out.write(e.getKeyAsString(publicKey));
				  out.newLine();
				  out.write("PrivKey");
				  out.newLine();
				  out.write(e.getKeyAsString(privateKey));
				  //Close the output stream
				  out.close();
		  }catch (Exception e) {//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
		  }
		
	}
}
