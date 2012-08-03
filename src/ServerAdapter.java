import naga.NIOSocket;
import naga.ServerSocketObserverAdapter;
import naga.SocketObserver;
import naga.SocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;

/* Main server component for handling messages. 
 * Ties to master class for referencing peers and handling management of the peer map*/
public class ServerAdapter extends ServerSocketObserverAdapter {
	public Master master;
	Peer peer;
	Encryption encryption  =  new Encryption();
	int port;
    int column;
    private KeyPair keyPair;
	public ServerAdapter(Master master2, int col, Peer self, KeyPair keys ) {
		master = master2;
        this.column = col;
		peer = self;
		try {
		   keyPair = keys;
		   
		}catch(Exception e)
		{
		    e.printStackTrace();
		}
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
					JSONObject<?, ?> encryptedPacket =  new JSONObject<Object, Object>(new String(packet));
					JSONObject<?, ?> outPacket =  new JSONObject<Object, Object>();
					JSONObject<?, ?> clearPacket =  new JSONObject<Object, Object>();
					//master.printMap();
					//if(UUID.fromString(encryptedPacket.getString("src")) == null)
					  //  return;
					String id = encryptedPacket.getString("src");
                    String name  =  (String) encryptedPacket.remove("name");
					Peer hashed = master.IDMap.get(encryptedPacket.getString("src"));

                    //If peer is not connected yet
					if(hashed == null)
                    {
                        master.IDMap.put(id,new Peer());
                        hashed = master.IDMap.get(id);
                    }

					if(encryptedPacket.getString("dest").equals(encryption.getKeyAsString(keyPair.getPublic())))
					{
						peerParse(encryptedPacket,hashed);
					}
					else if(encryptedPacket.getString("dest").equals(encryption.getKeyAsString(master.publicKey)))
					{
                       if(hashed.getAesKey() == null)
                       {
                           if(encryptedPacket.has("aeskey"))
                           {

                               if(registerPeer(encryptedPacket,hashed, socket, id))
                               {
                               hashed.name =  name;
                                   outPacket = new JSONObject();
                                   outPacket.put("gotpubkey",true);
                                   outPacket = master.addHeader(
                                           encryption.AESencryptJSON(outPacket,hashed.getAesKey()),2,hashed);
                                   master.forwardMessage(hashed,outPacket.toString(),"topkeyreceived");
                               }
                           }
                       }
					   clearPacket = encryption.AESdecryptJSON(encryptedPacket,hashed.getAesKey());
					   if(clearPacket.has("connectionbroken"))
					   {
					       peerConnectionBroken(clearPacket, hashed);
					   }
					   if(clearPacket.has("bounce"))
					   {
					       bounceMessage(encryption.getPublicKeyFromString(clearPacket.getString("peerkey")),clearPacket.getString("bounce"));
					   }
					   if(clearPacket.has("needkeylist"))
					   {
						   master.sendKeyList(hashed);
					   }
                        if(clearPacket.has("needkeylist") && hashed.needsUpdate)
                        {
                           hashed.needsUpdate = false;
                        }
                        if(clearPacket.has("heart"))
                        {
                            master.heartBeat(hashed);
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
						   peerAddPublicKey(hashed, clearPacket, socket);
					   }
					   else if(clearPacket.has("get"))
					   {
						   NIOSocket sock = null;
							try {
								sock = master.service.openSocket("127.0.0.1",1337);
								sock.listen(new internalSocket(hashed,clearPacket.getString("get"),master));
								
							} catch (IOException e) {
					
								e.printStackTrace();
							}
						    
					   }
					   if(clearPacket.has("uprightip") && clearPacket.has("uprightport"))
					   {
						   sendUpRightConnection(hashed, clearPacket);
					   }
					   if(clearPacket.has("upleftip") && clearPacket.has("upleftport"))
					   {
						   sendUpLeftConnection(hashed,clearPacket);
					   }
						
						
					}
					//System.out.println("Server: "+"SENT PACKET " + outPacket.toString());
				} catch (JSONException e1) {
					
					e1.printStackTrace();
				} catch (Exception e) {
					
					e.printStackTrace();
				}
				
			}

			public void peerParse(JSONObject<?, ?> encryptedPacket, Peer sender)throws Exception
		    {
		        JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>();
		        //1 == send up. 2 == broadcast. 3 == forward down. 4 == send down one. 5 == Reroute.
		        int type = encryptedPacket.getInt("type");
		        //test send up to master, add UUID to senders list in order to send back response
		        //get
		        //System.out.println();
		        
		        if(type == 1 || type == 2)
		        {
		            if(!sender.isPeerActive)
		            {
		                 peerActivate(encryptedPacket, sender);
		            }
                    else {
                     clearPacket = encryption.AESdecryptJSON(encryptedPacket,sender.getPeerAesKey());
                     if(clearPacket.has("needcol"))
                        sendCol(sender);
                    }
		            
		        }

		        //forward response downwards or if it is meant for us, decrypt it.
		        else if( type == 2)
		        {
		        	
		        }
		            
		    }

			private void sendUpLeftConnection(Peer hashed, JSONObject<?, ?> clearPacket) {
                // master.printMap();
                JSONObject<?, ?> peers = master.getPeers(hashed);
                JSONObject<?, ?> outPacket = new JSONObject<Object, Object>();
                try {
                  if(peers.has("upLeft"))
                  {
                  
                      Peer upLeft = (Peer)peers.get("upLeft");
                      if(upLeft.publicKey != null && upLeft.y != 0)
                      {
                          outPacket.put("connect", "upleft");
                          outPacket.put("ip",clearPacket.getString("upleftip"));
                          outPacket.put("port", clearPacket.get("upleftport"));
                          outPacket.put("publickey",encryption.getKeyAsString(hashed.publicKey));
                          outPacket = encryption.AESencryptJSON(outPacket, upLeft.getAesKey());
                          outPacket  = master.addHeader(outPacket, 2, upLeft);
                          master.forwardMessage(master.map.get(upLeft.x).get(0).socket,outPacket.toString(),"sendupleftConnection");
                      }
                  }
                }catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
            public JSONObject<?, ?> addPeerHeader(JSONObject<?, ?> json,int type,Peer peer )
            {

                try {
                    return(json
                            .put("col", Integer.toString(peer.x))
                            .put("src", encryption.getKeyAsString(keyPair.getPublic()))
                            .put("dest", encryption.getKeyAsString(peer.publicKey))
                            .put("type", Integer.toString(type)));

                } catch (JSONException e) {

                    e.printStackTrace();
                }
                return json;
            }
            private void sendUpRightConnection(Peer hashed, JSONObject<?, ?> clearPacket) {
              //master.printMap();
                try {
                    JSONObject<?, ?> peers = master.getPeers(hashed);
                    JSONObject<?, ?> outPacket = new JSONObject<Object, Object>();
                    if(peers.has("upRight"))
                    {
                        
                        Peer upRight =(Peer) peers.get("upRight");
                        if(upRight.publicKey != null && upRight.y != 0)
                        {
                        	
                            //System.out.println("Upright id: " + upRight.ID);
                            outPacket.put("connect", "upright");
                            outPacket.put("ip",clearPacket.getString("uprightip"));
                            outPacket.put("port", clearPacket.get("uprightport"));
                            outPacket.put("publickey", encryption.getKeyAsString(hashed.publicKey));
                            outPacket = encryption.AESencryptJSON(outPacket, upRight.getAesKey());
                            outPacket  = master.addHeader(outPacket, 2, upRight);
                            master.forwardMessage(master.map.get(upRight.x).get(0).socket,outPacket.toString(),"senduprightConnection");
                        }
                    }
                }catch(Exception e)
                {
                    e.printStackTrace();
                }
                
            }

            private void peerAddPublicKey(Peer hashed, JSONObject<?, ?> clearPacket, NIOSocket socket) {
                try {
                    JSONObject<?, ?> outPacket = new JSONObject<Object, Object>();
                    master.keyMap.put(encryption.getPublicKeyFromString(clearPacket.getString("publickey")), hashed);
                    hashed.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
                    outPacket.put("keylist",true);
                    outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket, hashed.getAesKey()), 2, hashed);
                    master.forwardMessage(socket,outPacket.toString(),"peeraddpublickey");
                }catch(Exception e)
                {
                    e.printStackTrace();
                }
            }



            private void peerConnectionBroken(JSONObject<?, ?> clearPacket, Peer hashed) {
                try
                {
                    JSONObject<?, ?> peers = master.getPeers(hashed);
                    if(peers.has(clearPacket.getString("connectionbroken")))
                    {
                           Peer dead =  ((Peer) peers.get(clearPacket.getString("connectionbroken")));
                           dead.connectionBrokenCount++;
            
                        if(dead.connectionBrokenCount > 1)
                        {
                            master.removePeer(dead);
    
                        }
                    }
                }catch(Exception e)
                {
                    e.printStackTrace();
                }
            }

            private void peerActivate(JSONObject<?, ?> encryptedPacket,Peer sender) {
            	JSONObject clearPacket = new JSONObject();
            	try {
                    //clearPacket =  encryption.RSAdecryptJSON(encryptedPacket,master.privateKey);
                   if(encryptedPacket.has("aeskey"))
                   {

                       byte[] key = encryption.decryptRSA(keyPair.getPrivate(), encryptedPacket.getString("aeskey").getBytes());
                       sender.setPeerAesKeyFromBase64(key);
                       encryptedPacket.remove("aeskey");
                       clearPacket = encryption.AESdecryptJSON(encryptedPacket,sender.getPeerAesKey());
                       if(clearPacket.has("publickey"))
                       {
                       	sender.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
       	                sender.isPeerActive = true;
       	                sender.ID = clearPacket.getString("src");
                        JSONObject outPacket = new JSONObject();
                        outPacket.put("gotpubkey",true);
                        outPacket = addPeerHeader(encryption.AESencryptJSON(outPacket, sender.getPeerAesKey()), 2, sender);
                        master.forwardMessage(peer, outPacket.toString(), "gotpubkey");
                       }
                   }
               }catch(Exception e)
               {
                   e.printStackTrace();
               }
                
            }

            private boolean registerPeer(JSONObject<?, ?> encryptedPacket, Peer hashed, NIOSocket socket,String id) {
                try {
                     JSONObject<?, ?> outPacket = new JSONObject<Object, Object>();
                     JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>();
                     byte[] key = Base64.decode(encryption.decryptRSA(master.privateKey, encryptedPacket.getString("aeskey").getBytes()));
                     hashed.setAesKey(key);

                     encryptedPacket.remove("aeskey");
                     clearPacket = encryption.AESdecryptJSON(encryptedPacket,key);
                     hashed.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("src"));
                     hashed.socket = socket;
                     hashed.setActive(true);
                     hashed.x = Integer.parseInt(clearPacket.getString("col"));
                     int added = master.addPeer(hashed);
                     JSONObject<String,Peer> peers = master.getPeers(hashed);
                     
                     //Notify user that they are connected
              /*       outPacket.put("connected", "yes");
                     outPacket = encryption.AESencryptJSON(outPacket, key);
                     outPacket = master.addHeader(outPacket, 2, hashed);
                     master.forwardMessage(newPeer.socket,outPacket.toString(),"connectionSuccess");*/
                     
                     //wrong column. Make the peer reconnect
                     if(added !=  port)
                     {
                          outPacket
                         .put("repair",true)
                         .put("port", added + 510);
                          
                       //   outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket,key),2,hashed);
    
    
                     }
                }catch(Exception e)
                {
                    e.printStackTrace();
                    return false;
                }

             return true;
                
            }
       private void bounceMessage(PublicKey key,String encryptedPacket)
       {
           Peer peer = master.keyMap.get(key);
           master.forwardMessage(peer,encryptedPacket,"BOUNCE FROM " + peer.ID);
       }



      });

    }

    private void sendCol(Peer sender) {
        JSONObject outPacket = new JSONObject();
        try {
            outPacket.put("col",column);
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        outPacket = addPeerHeader(encryption.AESencryptJSON(outPacket, sender.getPeerAesKey()), 2, sender);
        master.forwardMessage(peer,outPacket.toString(),"sendcolumn");
    }

    @Override
	public void acceptFailed(IOException exception) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void serverSocketDied(Exception exception) {
		// TODO Auto-generated method stub
		
	}
    public JSONObject<?, ?> addPeerHeader(JSONObject<?, ?> json,int type,Peer peer )
    {

        try {
            return(json
                    .put("col", Integer.toString(peer.x))
                    .put("src", encryption.getKeyAsString(keyPair.getPublic()))
                    .put("dest", encryption.getKeyAsString(peer.publicKey))
                    .put("type", Integer.toString(type)));

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
class internalSocket implements SocketObserver{
	Peer peer = null;
	String request = null;
	Master master = null;
	String response = "";
	public internalSocket(Peer peer, String request, Master master)
	{
		this.peer = peer;
		this.request = request;
		this.master = master;
	}
	@Override
	public void connectionOpened(NIOSocket nioSocket) {
		nioSocket.write(("GET " + request +" HTTP/1.0\r\n\r\n").getBytes());
		
	}

	@Override
	//TODO Fix up who the to send the connbroken packet to...
	public void connectionBroken(NIOSocket nioSocket, Exception exception) {
		// TODO Auto-generated method stub
		JSONObject<?, ?> outPacket = new JSONObject<Object, Object>();
		try {
			outPacket.put("response",response);
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
	    outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket, peer.getAesKey()), 2, peer);
	    master.forwardMessage(peer,outPacket.toString(),"getRequest");

	}

	@Override
	public void packetReceived(NIOSocket socket, byte[] packet) {
		response += new String(packet);
		
	}
	
}
