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

/* Main server component for handling messages. 
 * Ties to master class for referencing peers and handling management of the peer map*/
public class ServerAdapter extends ServerSocketObserverAdapter {
	public Master master;
	Peer peer;
	Encryption encryption  =  new Encryption();
	int port;
	public ServerAdapter(Master master2,int col, Peer self) {
		master = master2;
		this.port = col;
		peer = self;
	}
      
	public void newConnection(NIOSocket nioSocket)
    {

    	nioSocket.setPacketReader(new AsciiLinePacketReader());
		nioSocket.setPacketWriter(new RawPacketWriter());
		peer.socket = nioSocket;
      // Set our socket observer to listen to the new socket.
		nioSocket.listen(new SocketObserverAdapter()
		{
			public void packetReceived(NIOSocket socket, byte[] packet)
			{
				try {
					
					JSONObject encryptedPacket =  new JSONObject(new String(packet));
					JSONObject outPacket =  new JSONObject();
					JSONObject clearPacket =  new JSONObject();
					//master.printMap();
					//if(UUID.fromString(encryptedPacket.getString("id")) == null)
					  //  return;
					String id = encryptedPacket.getString("id");
					Peer hashed = master.IDMap.get(encryptedPacket.getString("id"));
					
					if(hashed == null)
					{
						clearPacket = encryption.RSAdecryptJSON(encryptedPacket,master.privateKey);

						if(clearPacket.has("aeskey"))
						{
						   byte[] key = Base64.decode(clearPacket.getString("aeskey"));
						   Peer newPeer = new Peer(key,clearPacket.getString("id"));
						   newPeer.socket = socket;
    						int added = master.addPeer(newPeer);
    						hashed = master.IDMap.get(id);
    						JSONObject<String,Peer> peers = master.getPeers(hashed);
    						//Notify peers of incoming connection

                            //wrong column. Make the peer reconnect
    						if(added !=  port)
    						{
    						     outPacket
    						    .put("repair",true)
    						    .put("port", added + 510);

    						     outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket,key),2,hashed);
    						   //  master.forwardMessage(newPeer.socket,outPacket.toString());

    						}

						return;
						}
						else 
						    return;
					}
					else 
					{

					    clearPacket = encryption.AESdecryptJSON(encryptedPacket,hashed.aesKey);
					    if(clearPacket.has("connectionbroken"))
					    {
					//        System.out.println("Peer : " + hashed.ID + " has lost connection to " + clearPacket.getString("connectionbroken"));
					        JSONObject peers = master.getPeers(hashed);
					        if(peers.has(clearPacket.getString("connectionbroken")))
					        {
    					           Peer dead =  ((Peer) peers.get(clearPacket.getString("connectionbroken")));
    					           dead.connectionBrokenCount++;
    				
    					        if(dead.connectionBrokenCount > 1)
    					        {
    					            master.removePeer(dead);

                                      master.printMap();
    					        }
					        }
					    }
					   
						if(clearPacket.has("needkeylist"))
						{
							outPacket = master.getPeers(hashed);
							outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket,hashed.aesKey),2,hashed);
							master.forwardMessage(socket,outPacket.toString());
						}
						else if(clearPacket.has("port"))
						{
						    hashed.port = clearPacket.getInt("port");
						}
						else if(clearPacket.has("dc"))
						{
						   master.removePeer(hashed);
						}
						else if(clearPacket.has("publickey"))
						{
							hashed.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
							outPacket.put("keylist",true);
							outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket, hashed.aesKey), 2, hashed);
							master.forwardMessage(socket,outPacket.toString());
						}
						else if(clearPacket.has("get"))
						{
						    outPacket.put("get",true);
						    outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket, hashed.aesKey), 2, hashed);
						}
						if(clearPacket.has("uprightip") && clearPacket.has("uprightport"))
						{
						    //master.printMap();
						    JSONObject peers = master.getPeers(hashed);
						    if(peers.has("upRight"))
						    {
						        Peer upRight =(Peer) peers.get("upRight");
						        //System.out.println("Upright id: " + upRight.ID);
						        outPacket.put("connect", "downleft");
						        outPacket.put("ip",clearPacket.getString("uprightip"));
						        outPacket.put("port", clearPacket.get("uprightport"));
						        outPacket.put("publickey", encryption.getKeyAsString(hashed.publicKey));
						        outPacket = encryption.AESencryptJSON(outPacket, upRight.aesKey);
						        outPacket  = master.addHeader(outPacket, 2, upRight);
						        master.forwardMessage(master.map.get(upRight.x).get(0).socket,outPacket.toString());
						        
						    }
						}
						if(clearPacket.has("upleftip") && clearPacket.has("upleftport"))
						{
						   // master.printMap();
	                          JSONObject peers = master.getPeers(hashed);
	                            if(peers.has("upLeft"))
	                            {
	                            
	                                Peer upLeft = (Peer)peers.get("upLeft");
	                                outPacket.put("connect", "downright");
	                                outPacket.put("ip",clearPacket.getString("upleftip"));
	                                outPacket.put("port", clearPacket.get("upleftport"));
	                                outPacket.put("publickey",encryption.getKeyAsString(hashed.publicKey));
	                                outPacket = encryption.AESencryptJSON(outPacket, upLeft.aesKey);
	                                //TODO FIX header function, should be uplef, not left
	                                outPacket  = master.addHeader(outPacket, 2, upLeft);
	                                master.forwardMessage(master.map.get(upLeft.x).get(0).socket,outPacket.toString());
	                                
	                            }
						}
						
						
					}
					//System.out.println("Server: "+"SENT PACKET " + outPacket.toString());
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
}
