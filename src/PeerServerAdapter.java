import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.JSON.JSONArray;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import naga.ConnectionAcceptor;
import naga.NIOSocket;
import naga.ServerSocketObserverAdapter;
import naga.SocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;


public class PeerServerAdapter extends ServerSocketObserverAdapter{
	public NetworkThread master;
	public Peer sender;
	Encryption encryption  =  new Encryption();
	public PeerServerAdapter(NetworkThread master2, Peer peer) {
		master = master2;
		sender = peer;
	}
	public void acceptFailed(IOException exception)
	{
	}

	public void serverSocketDied(Exception e)
	{
	}

	public void newConnection(NIOSocket nioSocket)
    {
		if(sender.socket == null)
		    sender.socket = nioSocket;
    	nioSocket.setPacketReader(new AsciiLinePacketReader());
		nioSocket.setPacketWriter(new RawPacketWriter());
      // Set our socket observer to listen to the new socket.
		nioSocket.listen(new SocketObserverAdapter()
		{
			public void packetReceived(NIOSocket socket, byte[] packet)
			{
			//System.out.println(new String(packet));
			
				try {
					/*if(!sender.active)
					{
					    JSONObject clearPacket = new JSONObject(new String(packet));
					    clearPacket = encryption.RSAdecryptJSON(clearPacket, master.privateKey);
					    if(clearPacket.has("aeskey"))
					    {
					        if(!sender.name.equals("down"))
					            //System.out.println(sender.name);
    					    sender.aesKey  = Base64.decode(clearPacket.getString("aeskey"));
    					    sender.active = true;
                            sender.ID = clearPacket.getString("id");
                            System.out.println("Peer "  + master.port + " connected to " + sender.name + ":" + sender.ID);
					    }
					}
					if(sender.publicKey == null)
					{
	                       JSONObject clearPacket = new JSONObject(new String(packet));
	                        clearPacket = encryption.AESdecryptJSON(clearPacket, sender.aesKey);
	                        if(clearPacket.has("publickey"))
	                        {
	                            sender.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
	                        }
					}*/
					    
					
                    master.parse(new String(packet),sender);
				} catch (Exception e) {
					
					e.printStackTrace();
				}
				
	
				
			}

      });
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
