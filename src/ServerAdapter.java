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
	int port;
	public ServerAdapter(Master master2,int col) {
		master = master2;
		this.port = col;
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

				try {
					
					JSONObject encryptedPacket =  new JSONObject(new String(packet));
					JSONObject outPacket =  new JSONObject();
					JSONObject clearPacket =  new JSONObject();
					System.out.println(new String(packet));
					if(UUID.fromString(encryptedPacket.getString("id")) == null)
					    return;
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
    						if(peers.has("downleft"))
    						{
    						    outPacket.put("connect",true);
    						    outPacket = encryption.AESencryptJSON(outPacket, ((Peer)peers.get("downleft")).aesKey);
    						    outPacket  = master.addHeader(outPacket, 2, (Peer) peers.get("downleft"));
    						    master.forwardMessage((Peer)peers.get("downleft"),outPacket.toString());
    						}
                            if(peers.has("downright"))
                            {
                                outPacket.put("connect",true);
                                outPacket = encryption.AESencryptJSON(outPacket, ((Peer)peers.get("downright")).aesKey);
                                outPacket  = master.addHeader(outPacket, 2, (Peer) peers.get("downright"));
                                master.forwardMessage((Peer)peers.get("downright"),outPacket.toString());
                            }
                            //wrong column. Make the peer reconnect
    						if(added !=  port)
    						{
    						     outPacket
    						    .put("repair",true)
    						    .put("port", added + 510);
                                // System.out.println(outPacket.toString());
    						     outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket,key),2,hashed);

    	                         master.forwardMessage(socket, outPacket.toString());
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
			              //System.out.println("Server: " + clearPacket.toString());
						
						if(clearPacket.has("needkeylist"))
						{
							outPacket = master.getPeerKeyList(hashed);
							outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket,hashed.aesKey),2,hashed);
							master.forwardMessage(socket,outPacket.toString());
						}
						
						else if(clearPacket.has("dc"))
						{
						   master.holes.add(new Hole(hashed.col,hashed.row));
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
						    JSONObject peers = master.getPeers(hashed);
						    if(peers.has("upRight"))
						    {
						        Peer right =(Peer) peers.get("upRight");
						        outPacket.put("connect", "downleft");
						        outPacket.put("ip",clearPacket.getString("uprightip"));
						        outPacket.put("port", clearPacket.get("uprightport"));
						        outPacket.put("publickey", hashed.publicKey);
						        outPacket = encryption.AESencryptJSON(outPacket, right.aesKey);
						        outPacket  = master.addHeader(outPacket, 2, right);
						        master.forwardMessage(right.socket,outPacket.toString());
						        
						    }
						}
						if(clearPacket.has("downleftip") && clearPacket.has("downleftport"))
						{
	                          JSONObject peers = master.getPeers(hashed);
	                            if(peers.has("downLeft"))
	                            {
	                                Peer left = (Peer)peers.get("downLeft");
	                                outPacket.put("connect", "downright");
	                                outPacket.put("ip",clearPacket.getString("upleftip"));
	                                outPacket.put("port", clearPacket.get("upleftport"));
	                                outPacket.put("publickey", hashed.publicKey);
	                                outPacket = encryption.AESencryptJSON(outPacket, left.aesKey);
	                                outPacket  = master.addHeader(outPacket, 2, left);
	                                master.forwardMessage(left.socket,outPacket.toString());
	                                
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
}
