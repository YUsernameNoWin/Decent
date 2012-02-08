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

/*TODO Needs a check to make sure upstream peers aren't trying to flood the network with false broadcasts
*	sign messages with master PrivKey? Hash it and provide checksum? 
*Determine if i need to drop UUID lists and replace fully with column routing.
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
               // TODO Auto-generated catch block
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
	
	public String parse(String input)throws Exception
	{
		JSONObject encryptedPacket = new JSONObject(input);
		//1 == send up. 2 == broadcast. 3 == forward down. 4 == send down one. 5 == Reroute.
		int type = encryptedPacket.getInt("type");
		//test send up to master, add UUID to senders list in order to send back response
		//get
		
		int col = encryptedPacket.getInt("col");

		if(type == 1)
		{

			master.sendUp(input);
		}

		//forward response downwards or if it is meant for us, decrypt it.
		else if( type == 2)
		{
		    String content;
			    if(!encryptedPacket.get("id").equals(master.ID.toString()))
			        return "";
				JSONObject clearPacket = encryptedPacket;
				clearPacket =  encryption.AESdecryptJSON(encryptedPacket,master.topSymKey);
				if(clearPacket.has("cont")) {
				    content  = clearPacket.getString("cont");
				    if(content.equals("repair"))
				    {
				        JSONObject  out = new JSONObject();
				        out.put("dc", "disconnected");
				        out = addHeader(encryption.AESencryptJSON(out,master.topSymKey),2);
				        master.forwardMessage(master.up, out.toString());
				        master.repair(clearPacket.getInt("port"));
				    }
    				if( content.equals("keylist")) 
    				{
    				    if(clearPacket.has("left"))
    				        master.left = new Peer(encryption.getPublicKeyFromString((String) clearPacket.opt("left")));
    				    if(clearPacket.has("upleft"))
    				        master.upLeft = new Peer(encryption.getPublicKeyFromString((String) clearPacket.opt("upleft")));
    				    if(clearPacket.has("downleft"))
    				        master.downLeft = new Peer(encryption.getPublicKeyFromString((String) clearPacket.opt("downleft")));
    				    if(clearPacket.has("upright"))
    				        master.upRight = new Peer(encryption.getPublicKeyFromString((String) clearPacket.opt("upright")));
    				    if(clearPacket.has("right"))
    				        master.right = new Peer(encryption.getPublicKeyFromString((String) clearPacket.opt("right")));
    				    if(clearPacket.has("downright"))
    				        master.downRight = new Peer(encryption.getPublicKeyFromString((String) clearPacket.opt("downright")));
    				    if(clearPacket.has("down"))
    				        master.down = new Peer(encryption.getPublicKeyFromString((String) clearPacket.opt("down")));
    				    if(clearPacket.has("up"))
    				        master.up = new Peer(encryption.getPublicKeyFromString((String) clearPacket.opt("up")));
    				    
    				    JSONObject out = new JSONObject();
    				    
    				    if(master.left != null)
    				    {
    				        master.left.port  =master.port +10;
            				   out.put("leftip",encryption.encryptRSA(master.left.publicKey,InetAddress.getLocalHost().toString()))
            				   .put("leftport",encryption.encryptRSA(master.left.publicKey,Integer.toString(master.left.port).getBytes()));
            				   master.left.serverSock = master.service.openServerSocket(master.left.port);
            				   master.left.serverSock.listen(new PeerServerAdapter(master,master.left));
            		           master.left.serverSock.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
    				    }
    				    if(master.right != null)
    				    {
    				        master.left.port  =master.port +10;
    				        out.put("rightip",encryption.encryptRSA(master.right.publicKey,InetAddress.getLocalHost().toString()))
    				            .put("rightport",encryption.encryptRSA(master.right.publicKey,Integer.toString(master.right.port).getBytes()));
            			    master.right.serverSock = master.service.openServerSocket(master.right.port);
                            master.right.serverSock.listen(new PeerServerAdapter(master,master.right));
                            master.right.serverSock.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
    				    }
    				    out = encryption.AESencryptJSON(out,master.upSymKey);
    				    out = addHeader(out, 1);
    				    
    				    master.forwardMessage(master.up, out.toString());
    				}
    				else if(clearPacket.has("connect") && clearPacket.has("ip") && clearPacket.has("port"))
    				{
    				    if(clearPacket.getString("connect").equals("left") && master.downLeft == null)
    				    {
    				        master.downLeft = new Peer(clearPacket.getString("ip"),clearPacket.getInt("port"));
    				        master.downLeft.socket = master.service.openSocket(master.downLeft.address,master.downLeft.port);
    				        master.downLeft.socket.setPacketReader(new AsciiLinePacketReader());
    				        master.downLeft.socket.setPacketWriter(new RawPacketWriter());
    				        master.downLeft.socket.listen(new NetworkProtocol(master,master.downLeft));
    				    }
    				    else if(clearPacket.getString("connect").equals("right") && master.downRight == null)
    				    {
                            master.downRight = new Peer(clearPacket.getString("ip"),clearPacket.getInt("port"));
                            master.downRight.socket = master.service.openSocket(master.downRight.address,master.downRight.port);
                            master.downRight.socket.setPacketReader(new AsciiLinePacketReader());
                            master.downRight.socket.setPacketWriter(new RawPacketWriter());
                            master.downRight.socket.listen(new NetworkProtocol(master,master.downRight));
    				    }
    				}
				}
    				
			}
				
			
			
				
		
		

			

			
		return input;
			
	}

	@Override
	public void connectionOpened(NIOSocket nioSocket) 
	{
		try{
		    JSONObject clearPacket = new JSONObject();
		   clearPacket =  clearPacket.put("aeskey", new String(Base64.encodeBytesToBytes(master.topSymKey)));
		   clearPacket= encryption.RSAencryptJSON(clearPacket,master.upPubKey);
		  //System.out.println("DECRYPTED RSA TEST "  +RSAdecryptJSON(clearPacket,master.privateKey));
		   clearPacket = addHeader(clearPacket,1);
		   //System.out.println("Peer: " +  master.port  + " " + clearPacket.toString());
			nioSocket.write((clearPacket.toString() + "\n").getBytes());
			
           clearPacket = new JSONObject();
          

        clearPacket.put("publickey", encryption.getKeyAsString(master.publicKey));
           clearPacket = encryption.AESencryptJSON(clearPacket,master.topSymKey);
        // System.out.println("DECRYPTED AES TEST " +  master.ID +" "  +encryption.encryption.AESdecryptJSON(clearPacket,master.topSymKey) );
           clearPacket = addHeader(clearPacket,1);
         //System.out.println("Peer: " +  master.port  + " " + clearPacket.toString());
         
       //  System.out.println("PEER KEY " +new String(master.topSymKey));
         
            nioSocket.write((clearPacket.toString() + "\n").getBytes());
            
           clearPacket = new JSONObject();
          clearPacket =  clearPacket.put("cont", "needkeylist");
         clearPacket= encryption.AESencryptJSON(clearPacket,master.topSymKey);
          clearPacket = addHeader(clearPacket,1);
           //System.out.println("Peer: " +  master.port  + " " + clearPacket.toString());
             nioSocket.write((clearPacket.toString() + "\n").getBytes());
             

			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void connectionBroken(NIOSocket nioSocket, Exception exception) 
	{
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		try {
			//System.out.println("Peer: " +master.port +new String(packet));
			parse(new String(packet));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
