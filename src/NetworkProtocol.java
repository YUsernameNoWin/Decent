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
*
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
	
	
	public void masterKeyExchange() throws JSONException
	{
	    JSONObject clearPacket;
        clearPacket = new JSONObject();
        clearPacket =  clearPacket.put("aeskey", new String(Base64.encodeBytes(master.topSymKey)));
        clearPacket= encryption.RSAencryptJSON(clearPacket,master.upPubKey);
       //System.out.println("DECRYPTED RSA TEST "  +RSAdecryptJSON(clearPacket,master.privateKey));
        clearPacket = addHeader(clearPacket,1);
        //System.out.println("Peer: " +  master.port  + " " + clearPacket.toString());
         master.forwardMessage(sender,clearPacket.toString());
         
        clearPacket = new JSONObject();
       

     clearPacket.put("publickey", encryption.getKeyAsString(master.publicKey));
        clearPacket = encryption.AESencryptJSON(clearPacket,master.topSymKey);
     // System.out.println("DECRYPTED AES TEST " +  master.ID +" "  +encryption.encryption.AESdecryptJSON(clearPacket,master.topSymKey) );
        clearPacket = addHeader(clearPacket,1);
      //System.out.println("Peer: " +  master.port  + " " + clearPacket.toString());
      
    //  System.out.println("PEER KEY " +new String(master.topSymKey));
      
        master.forwardMessage(sender,clearPacket.toString());
	}
	public void getKeysAndColumn() throws JSONException
	{     
	    JSONObject clearPacket = new JSONObject();
	    clearPacket =  clearPacket.put("needkeylist",true);
	    clearPacket= encryption.AESencryptJSON(clearPacket,master.topSymKey);
	    clearPacket = addHeader(clearPacket,1);
	    master.forwardMessage(sender,clearPacket.toString());
       
	    clearPacket = new JSONObject();
       clearPacket =  clearPacket.put("uprightip", "127.0.0.1");
       clearPacket =  clearPacket.put("uprightport", master.port+10);
       clearPacket =  clearPacket.put("upleftip", "127.0.0.1");
       clearPacket =  clearPacket.put("upleftport", master.port+10);
       clearPacket= encryption.AESencryptJSON(clearPacket,master.topSymKey);
       clearPacket = addHeader(clearPacket,1);
       master.forwardMessage(sender,clearPacket.toString());
       
       clearPacket =  clearPacket.put("needcol",true); 
       clearPacket= encryption.AESencryptJSON(clearPacket,master.up.aesKey);
       clearPacket = addHeader(clearPacket,1);
        master.forwardMessage(sender,clearPacket.toString());    
	}
	public void connectionOpened(NIOSocket nioSocket) 
	{
        sender.socket = nioSocket;
		try{
		    masterKeyExchange();
		    getKeysAndColumn();
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
	    try {
            master.parse(new String(packet));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
    }
}
