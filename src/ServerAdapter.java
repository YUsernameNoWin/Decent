import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import org.JSON.JSONArray;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import naga.NIOSocket;
import naga.ServerSocketObserverAdapter;
import naga.SocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;


public class ServerAdapter extends ServerSocketObserverAdapter {
	public Master master;
	Encryption encryption  =  new Encryption();
	int col;
	public ServerAdapter(Master master2,int col) {
		master = master2;
		this.col = col;
	}

    public void writeToFile(String text)
    {
          try
          {
              // Create file 
                  FileWriter fstream = new FileWriter("error.txt");
                  BufferedWriter out = new BufferedWriter(fstream);
                  
                  out.append(text);
                  //Close the output stream
                  out.close();
              }catch (Exception e)
              {//Catch exception if any
              System.err.println("Error: " + e.getMessage());
              }
        
    }
      
	public void newConnection(NIOSocket nioSocket)
    {
    	nioSocket.setPacketReader(new AsciiLinePacketReader());
		nioSocket.setPacketWriter(new RawPacketWriter());
      // Set our socket observer to listen to the new socket.
		nioSocket.listen(new SocketObserverAdapter()
		{
			public void packetReceived(NIOSocket socket, byte[] packet)
			{
				System.out.println("Server recieved: "+new String(packet));
				try {
					
					JSONObject encryptedPacket =  new JSONObject(new String(packet));
					JSONObject outPacket =  new JSONObject();
					JSONObject clearPacket =  new JSONObject();
					if(UUID.fromString(encryptedPacket.getString("id")) == null)
					    return;
					String id =encryptedPacket.getString("id");
					Peer hashed = master.IDMap.get(encryptedPacket.getString("id"));
					
					if(hashed == null)
					{
						clearPacket = encryption.RSAdecryptJSON(encryptedPacket,master.privateKey);

						if(clearPacket.has("aeskey"))
						{
						   byte[] key =Base64.decode(clearPacket.getString("aeskey"));
			
    						int added = master.addPeer(new Peer(key,clearPacket.getString("id")));
    						hashed = master.IDMap.get(id);
    						if(added !=  col)
    						{
    						     outPacket
    						    .put("cont", "repair")
    						    .put("port", added + 510);
    						     outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket,key),2,hashed);
    	                         socket.write(outPacket.toString().getBytes());
    						}

						return;
						}
						else 
						    return;
					}
					else 
					{
	                       //System.out.println(new String(hashed.aesKey));
					   // System.out.println(new String(encryption.decryptAES(hashed.aesKey, encryptedPacket.getString("publickey").getBytes())));

					    clearPacket = encryption.AESdecryptJSON(encryptedPacket,hashed.aesKey);
						
						
						if(clearPacket.has("cont") && clearPacket.getString("cont").equals("needkeylist"))
						{
							outPacket = master.getPeers(hashed);
							outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket,hashed.aesKey),2,hashed);
						}
						
						if(clearPacket.has("dc"))
						{
						   master.removePeer(hashed);
						}
						if(clearPacket.has("publickey"))
						{
							hashed.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
							outPacket.put("cont", "keylist");
							outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket, hashed.aesKey), 2, hashed);
						}
						if(clearPacket.has("get"))
						{
						    outPacket.put("cont", "get");
						    outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket, hashed.aesKey), 2, hashed);
						}
						if(clearPacket.has("rightip") && clearPacket.has("rightport"))
						{
						    JSONObject peers = master.getPeers(hashed);
						    if(peers.has("right"))
						    {
						        Peer right = (Peer)peers.get("right");
						        outPacket.put("ip",clearPacket.getString("rightip"));
						        outPacket.put("port", clearPacket.getInt("rightport"));
						        
						        
						    }
						}
						if(clearPacket.has("leftip") && clearPacket.has("leftport"))
						{
	                          JSONObject peers = master.getPeers(hashed);
	                            if(peers.has("left"))
	                            {
	                                Peer left = (Peer)peers.get("left");
	                                outPacket.put("ip",clearPacket.getString("rightip"));
	                                outPacket.put("port", clearPacket.getInt("rightport"));
	                                
	                                
	                            }
						}
						
						
					}
					//System.out.println("Server: "+"SENT PACKET " + outPacket.toString());
					socket.write((outPacket.toString()+"\n").getBytes());
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

      });
    }

	@Override
	public void acceptFailed(IOException exception) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void serverSocketDied(Exception exception) {
		// TODO Auto-generated method stub
		
	}
}
