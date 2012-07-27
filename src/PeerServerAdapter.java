import naga.NIOSocket;
import naga.ServerSocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/* Peer Server Class. Handles server connections between peers - Connects to a Network Protocol
 * Message parsing is done in NetworkThread.*/
public class PeerServerAdapter extends ServerSocketObserverAdapter{
	public NetworkThread master;
	//public Peer sender;
	Encryption encryption  =  new Encryption();
    Queue <Peer> senders = new LinkedList<Peer>();
    private int currentSender = 0;
	public PeerServerAdapter(NetworkThread master2, LinkedList <Peer> senders) {
		master = master2;
		//sender = peer;
       this.senders.addAll(senders);
	}
	public void acceptFailed(IOException exception)
	{
	}

	public void serverSocketDied(Exception e)
	{
	}

	public void newConnection(NIOSocket nioSocket)
    {
		//if(sender.socket == null)
		  //  sender.socket = nioSocket;
    	nioSocket.setPacketReader(new AsciiLinePacketReader());
		nioSocket.setPacketWriter(new RawPacketWriter());
      // Set our socket observer to listen to the new socket.

		nioSocket.listen(new NetworkProtocol(master,senders.remove()));


				



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
}
