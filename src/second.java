import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.ArrayList;


public class second {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException, Exception {
		// TODO Auto-generated method stub

	   Master master =new Master();
	    ArrayList<NetworkThread> peers = new ArrayList<NetworkThread>();
	    for(int i=1;i<50;i++)
	    {
	       peers.add(new NetworkThread(i*10+500));
	      peers.add(new NetworkThread(i*10+501));
	      peers.add(new NetworkThread(i*10+502));
	    }
	    
        
		master.start();
		for(NetworkThread a:peers)
		{
		    a.start();
		}
		
	}

}
