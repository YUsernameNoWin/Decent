import naga.NIOSocket;
import naga.ServerSocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/* Peer Server Class. Handles server connections between peers - Connects to a Network Protocol
 * Message parsing is done in NetworkThread.*/
public class PeerServerAdapter extends ServerSocketObserverAdapter{
	public NetworkThread master;
	Encryption encryption  =  new Encryption();
    Peer sender;
    private int currentSender = 0;
	public PeerServerAdapter(NetworkThread master2,Peer sender) {
		master = master2;
		this.sender = sender;

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
		if(!sender.isActive())
		    nioSocket.listen(new NetworkProtocol(master,sender));
		    

				



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
