import naga.NIOSocket;
import naga.SocketObserverAdapter;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.TimeUnit;

/*Client part of the peer. Connects to a ServerAdapter or PeerServerAdapter.
* Message parsing is done in NetworkThread.
*
*/
public class NetworkProtocol extends SocketObserverAdapter{
	public NetworkThread master;
	public Peer sender;
	Encryption encryption  =  new Encryption();
	public NetworkProtocol(NetworkThread master2, Peer peer) {
		master = master2;
		sender = peer;
	}
 
    
	
	   public void writeToFile(String text)
	    {
	          try
	          {
	              // Create file 
	                  FileWriter fstream = new FileWriter("text.html");
	                  BufferedWriter out = new BufferedWriter(fstream);
	                  
	                  out.write(text);
	                  //Close the output stream
	                  out.close();
	              }catch (Exception e)
	              {//Catch exception if any
	              System.err.println("Error: " + e.getMessage());
	              }
	        
	    }
	
	
	

	public void connectionOpened(NIOSocket nioSocket) 
	{

        sender.socket = nioSocket;
        //sender.setActive(true);
	}

	@Override
	public void connectionBroken(NIOSocket nioSocket, Exception exception)
    {
	    sender.setActive(false);
	    master.deadPeer(sender);
        if(!master.state.equals("repairing"))
        {
            sender.setActive(false);
            //master.timer.shutdown();
            //master.timer = new ScheduledThreadPoolExecutor(2);
            //master.timer.scheduleAtFixedRate(master.timerTasks.get("connecttoup"),0,3,TimeUnit.SECONDS);
           
        }


    }

	@Override
    public void packetReceived(NIOSocket socket, byte[] packet)
    {
	    try {
	        //System.out.println(new String(packet));
            master.parse(new String(packet),sender);
            
        } catch (Exception e) {
            
            e.printStackTrace();
        } 
    }
}
