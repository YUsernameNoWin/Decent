import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Scanner;

/* TODO Work on fixing left right connections. Other than that, all encryption issues are done.*/
/* Main class. Initiates Peers.*/
public class runner extends Thread{
    //TODO reimplement TCP handshake over multiple nodes. Make sure to change state only when acknowledgement is recieved.
    /**
     * @param args
     * @throws IOException 
     * @throws UnknownHostException 
     */
	static PublicKey publicKey;
	static PrivateKey privateKey;
    static Encryption e = new Encryption();
    static Master master = null;
    public static void main(String[] args) throws UnknownHostException, Exception {
        Scanner scan =  new Scanner(System.in);
    	JettyTestServer server = new JettyTestServer();
    	server.start();
        getKey2();
        KeyPair top =  new KeyPair(publicKey,privateKey);
        KeyPair keys1 = getKey();
        KeyPair keys2 = getKey();
        KeyPair keys3 = getKey();
        int port = 529;
        master =new Master(top,keys1);
        ArrayList<NetworkThread> peers = new ArrayList<NetworkThread>();
           master.start();
           //KeyPair news = e.generateKey();
           //saveKey2(news.getPublic(),news.getPrivate());
           sleep(200);
           int temp = 7;
        NetworkThread invitor = new NetworkThread(master.port ,temp,keys1,(keys1 = e.generateKey()),top);
        invitor.bridging = false;
        invitor.start();
        temp++;
    }
        /*for(int i = 1; i <= 3;i++)
            {
                peers.add(new NetworkThread(port ,temp,keys1,(keys1 = e.generateKey()),top));
                temp++;
            }
            for(NetworkThread thread: peers)
            {
                thread.start();
            }

        }

        for(int i=1;i<=3;i++)
        {
            peers.add(new NetworkThread(i*10+500,temp,keys1,(keys1 = e.generateKey()),top));
            temp++;
            peers.add(new NetworkThread(i*10+501,temp,keys2,(keys2 = e.generateKey()),top));
            temp++;
            peers.add(new NetworkThread(i*10+502,temp,keys3,(keys3 = e.generateKey()),top));
            temp++;

        }

        System.out.println("start spawning");
        int time = 1000;
        for(int i = 0; i < peers.size(); i++)
        {
            if(i%3 == 0)
            {
                sleep(time);
                time+= 1000;

            }
            NetworkThread a  =  peers.get(i);
            a.start();

            //scan.next();
            //System.out.print(a.port + " ");

        }
        scan.next();
        Peer dead = master.IDMap.get(e.getKeyAsString(peers.get(2).publicKey));

        master.repair(dead,dead.port,dead.x,dead.y);
        /*for(NetworkThread a:peers)
        {

            a.getKeyStatus();
            //a.getSocketStatus();

        }*/
    //scan.next();
    // peers.get(5).close();

    /*
            new NetworkThread(master.holes.remove().up.port + 10,8213).start();
            sleep(100);
            new NetworkThread(master.holes.remove().up.port + 10,80).start();
            sleep(100);
            //peers.get(1).close();
           new NetworkThread(master.holes.remove().up.port + 10,90).start();

    */


    public static KeyPair getKey(){
		try{
			String temp = "";
			String key  = "";
			
			Scanner scan = new Scanner(new File("out.txt"));
            while(!(temp = scan.next()).contains("PrivKey")){
                key +=temp;

            }
            publicKey = e.getPublicKeyFromString(key);
            key = "";
            while(scan.hasNext()){
                key+=scan.next();
            }
            privateKey = e.getPrivateKeyFromString(key);
			KeyPair a = new KeyPair(publicKey, privateKey);
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
			
			Scanner scan = new Scanner(new File("out2.txt"));
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
