import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

import org.JSON.JSONArray;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import naga.ConnectionAcceptor;
import naga.NIOSocket;
import naga.SocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;

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


       public JSONObject addHeader(JSONObject json,int type)
       {

           try {
               return(json.put("col", Integer.toString(master.column)).put("id",master.ID).put("type", Integer.toString(type)));

           } catch (JSONException e) {
               
               e.printStackTrace();
           }
           return json;
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
        sender.aesKey = encryption.generateSymmetricKey().getEncoded();
        sender.active = true;
            try {
                master.keyExchange(sender);
                master.sendPubKey(sender);
                master.getColumn();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
          
        

	}

	@Override
	public void connectionBroken(NIOSocket nioSocket, Exception exception)
    {
	    master.deadPeer(sender);


    }

	public JSONObject stripHeader(JSONObject temp)
	{
	    temp.remove("id");
	    temp.remove("type");
	    temp.remove("col");
	    return temp;
	}
	@Override
    public void packetReceived(NIOSocket socket, byte[] packet)
    {
	    try {
            master.parse(new String(packet),sender);
        } catch (Exception e) {
            
            e.printStackTrace();
        } 
    }
}
