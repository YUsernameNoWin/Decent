import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedSelectorException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;
import naga.*;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;
import org.JSON.JSONException;
import org.JSON.JSONObject;

/* Handles message parsing in function Parse().
 * Equivalent of Master for ServerAdapter
 * Tracks peers and connections.
 */
public class NetworkThread extends Thread{
    Encryption encryption  =  new Encryption();
	Peer down,left,right,up,upLeft,upRight,master,downLeft,downRight,top;

	
	
	public PrivateKey privateKey;
	public PublicKey publicKey;
	public ArrayList<UUID> leftList = new ArrayList<UUID>();
	public ArrayList<UUID> downList = new ArrayList<UUID>();
	public ArrayList<UUID> rightList = new ArrayList<UUID>();
	protected PrintWriter response;
	protected BufferedReader send;
	public NIOService service;
	public int column;
	public UUID ID = UUID.randomUUID();
	public int id;
	public NIOServerSocket leftServer,downServer,rightServer;
	ServerSocket serverSocket = null;
    int port =0;
	public NetworkThread(int port,int id) throws Exception{
	    this.port = port;
		this.id = id;
	}
	public void run()
	{
	    

	    try {
            try {
                service = new NIOService();
            } catch (IOException e) {
                
                e.printStackTrace();
            }

            up = new Peer("127.0.0.1", port,"up");

            upLeft = new Peer("127.0.0.1", port+1,"upLeft");

            upRight = new Peer("127.0.0.1", port+2,"upRight");
 
            right = new Peer("127.0.0.1", port,"right");
    
            left = new Peer("127.0.0.1", port,"left");
      
            downRight= new Peer("127.0.0.1", port+3,"downRight");
           
            downLeft = new Peer("127.0.0.1", port+4,"downLeft");
         
            top = new Peer(encryption.generateSymmetricKey().getEncoded());
            top.name  = "top";
            
            getKey();
           
            up.socket = service.openSocket(up.address,port);
            up.socket.setPacketReader(new AsciiLinePacketReader());
            up.socket.setPacketWriter(new RawPacketWriter());
            up.socket.listen(new NetworkProtocol(this,up));
            up.active = true;
            down = new Peer("127.0.0.1", port + 10,"down");
            down.serverSock = service.openServerSocket(down.port);
            down.serverSock.listen(new PeerServerAdapter(this,down));
            down.serverSock.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
            
            masterKeyExchange();
            sendLeftRightConnection();
            updatePort();
         
            get("index.html");

            
            while(true){
                try {
                service.selectBlocking(500);
                }catch(ClosedSelectorException e)
                {
                    
                }
            }

                 
            

        } catch (Exception e) {
            
            e.printStackTrace();
        }
	}
	public void sendPubKey(Peer sender)
	{
	    JSONObject packet = new JSONObject();
	    try {
            packet.put("publickey", encryption.getKeyAsString(publicKey));
            packet = encryption.AESencryptJSON(packet, sender.aesKey);
            packet = addHeader(packet,1);
            forwardMessage(sender,packet.toString(),"sendPubkey");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	public void sendLeftRightConnection() 
	{
	    
       JSONObject clearPacket = new JSONObject();
       try {
       clearPacket =  clearPacket.put("uprightip", "127.0.0.1");
       clearPacket =  clearPacket.put("uprightport", port+10);
       clearPacket =  clearPacket.put("upleftip", "127.0.0.1");
       clearPacket =  clearPacket.put("upleftport", port+10);
       clearPacket= encryption.AESencryptJSON(clearPacket,top.aesKey);
       clearPacket = addHeader(clearPacket,1);
       }catch(Exception e) {
           e.printStackTrace();
       }
       forwardMessage(up,clearPacket.toString(),"sendconnections");
        
    }
	public void updatePort() throws JSONException
	{
        JSONObject clearPacket = new JSONObject();
        clearPacket =  clearPacket.put("port",port);
        clearPacket= encryption.AESencryptJSON(clearPacket,top.aesKey);
        clearPacket = addHeader(clearPacket,1);
       forwardMessage(up,clearPacket.toString(),"port"); 
	}
	public void getKeyList() throws JSONException
	{
        JSONObject clearPacket = new JSONObject();
        clearPacket =  clearPacket.put("needkeylist",true);
        clearPacket= encryption.AESencryptJSON(clearPacket,top.aesKey);
        clearPacket = addHeader(clearPacket,1);
       forwardMessage(up,clearPacket.toString(),"getkeylist"); 
	}
    public void getColumn() throws JSONException
    {     

        JSONObject clearPacket = new JSONObject(); 
       clearPacket =  clearPacket.put("needcol",true); 
       clearPacket= encryption.AESencryptJSON(clearPacket,up.aesKey);
       clearPacket = addHeader(clearPacket,2);
       clearPacket.put("exchange", true);
        forwardMessage(up,clearPacket.toString(),"getcolumn");    
    }
    
    public void keyExchange(Peer peer) throws JSONException
    {
	     JSONObject message = new JSONObject();
	     message.put("aeskey", new String(peer.aesKey));

	     message = encryption.RSAencryptJSON(message, peer.publicKey);
	     message = addHeader(message, 1);
	     message.put("exchange", true);
	     forwardMessage(peer,message.toString(),"keyexchange,sendaeskey");
        
        message = new JSONObject();
        message.put("publickey", encryption.getKeyAsString(publicKey));
        message = encryption.AESencryptJSON(message, peer.aesKey);
        message = addHeader(message, 2);
        message.put("exchange", true);
       forwardMessage(peer,message.toString(),"keyexchange,sendpubkey");
        
    }
	 public void masterKeyExchange() throws JSONException
	    {
	        JSONObject clearPacket;
	        clearPacket = new JSONObject();
	        clearPacket =  clearPacket.put("aeskey", new String(Base64.encodeBytes(top.aesKey)));
	        clearPacket= encryption.RSAencryptJSON(clearPacket,top.publicKey);
	        clearPacket = addHeader(clearPacket,1);

	         forwardMessage(up,clearPacket.toString(),"masterexchange,sendaeskey");
	         
	        clearPacket = new JSONObject();
	       

	     clearPacket.put("publickey", encryption.getKeyAsString(publicKey));
	        clearPacket = encryption.AESencryptJSON(clearPacket,top.aesKey);
	     // System.out.println("DECRYPTED AES TEST " +  master.ID +" "  +encryption.encryption.AESdecryptJSON(clearPacket,master.top.aesKey) );
	        clearPacket = addHeader(clearPacket,1);

	      
	        forwardMessage(up,clearPacket.toString(),"masterexchange,sendpubkey");
	    }
    public String parse(String input, Peer sender)throws Exception
	    {
	        JSONObject encryptedPacket = new JSONObject(input);
	        JSONObject clearPacket = new JSONObject();
	        //1 == send up. 2 == broadcast. 3 == forward down. 4 == send down one. 5 == Reroute.
	        int type = encryptedPacket.getInt("type");
	        //test send up to master, add UUID to senders list in order to send back response
	        //get
	        

	        if(type == 1)
	        {
	            if(!encryptedPacket.has("exchange"))
	            sendUp(input);
	            if(!sender.active)
	            {
	                try {
	                    clearPacket =  encryption.RSAdecryptJSON(encryptedPacket,privateKey);
	                    if(clearPacket.has("aeskey"))
	                    {
	                        sender.aesKey = clearPacket.getString("aeskey").getBytes();
	                        sender.active = true;
	                        sender.ID = clearPacket.getString("id");
	                        //System.out.println("Peer "  + id + " connected to " + sender.name + ":" + sender.ID);
	                    }
	                }catch(Exception e)
	                {
	                    e.printStackTrace();
	                }
	            }
	            else
	            {
    	            try 
    	            {
                        clearPacket =  encryption.AESdecryptJSON(encryptedPacket,sender.aesKey);
                         if(clearPacket.has("publickey"))
                            sender.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
                        else if(clearPacket.has("needcol"))
                        {
                            JSONObject outPacket = new JSONObject();
                            outPacket.put("col", column);
                            outPacket = encryption.AESencryptJSON(outPacket, sender.aesKey);
                            outPacket = addHeader(outPacket,2);
                            forwardMessage(sender,outPacket.toString(),"replycol");
                        }
                        else if(clearPacket.has("col"))
                        {
                            column = clearPacket.getInt("col");
                        }
                        else if(clearPacket.has("response")){
                        	System.out.println("RESPONSE:" + clearPacket.getString("response"));
                        }

    	            }catch(Exception e)
    	            {
    	              
    	                System.out.println("notforme");
    	            }
	            
	            }
	            
	        }

	        //forward response downwards or if it is meant for us, decrypt it.
	        else if( type == 2)
	        {

	                clearPacket =  encryption.AESdecryptJSON(encryptedPacket,top.aesKey);
	                if(Integer.toString(id).equals(clearPacket.getString("id")))
	                {
	                    if(clearPacket.has("repair"))
	                    {
	                        JSONObject  out = new JSONObject();
	                        out.put("dc", "disconnected");
	                        out = addHeader(encryption.AESencryptJSON(out,top.aesKey),1);
	                        forwardMessage(up, out.toString(),"repairnotice");
	                        repair(clearPacket.getInt("port"));
	                    }
	                    if(clearPacket.has("connect"))
	                    {
	                       // System.out.println("hello");
	                            if(clearPacket.getString("connect").equals("downleft")  && !downLeft.active)
	                            {
	                                downLeft.address = clearPacket.getString("ip");
                                    downLeft.port = clearPacket.getInt("port");
                                    downLeft.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey")); 
	                                downLeft.socket = service.openSocket(downLeft.address,downLeft.port);
	                                downLeft.socket.setPacketReader(new AsciiLinePacketReader());
	                                downLeft.socket.setPacketWriter(new RawPacketWriter());
	                                downLeft.socket.listen(new NetworkProtocol(this,downLeft));
	                                downLeft.active = true;
	                            //    System.out.println(id + " connected to downLeft");
	                            }
	                            if(clearPacket.getString("connect").equals("downright") && !downRight.active)
	                            {
	                                downRight.address = clearPacket.getString("ip");
	                                downRight.port = clearPacket.getInt("port");
	                                downRight.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey")); 
	                                downRight.socket = service.openSocket(downRight.address,downRight.port);
	                                downRight.socket.setPacketReader(new AsciiLinePacketReader());
	                                downRight.socket.setPacketWriter(new RawPacketWriter());
	                                downRight.socket.listen(new NetworkProtocol(this,downRight));
	                                downRight.active = true;
	                               // System.out.println(id + " connected to downRight");
	                            }
	                    }
	                    if( clearPacket.has("keylist")) 
	                    {
	                        if(clearPacket.has("left"))
	                            left.publicKey = encryption.getPublicKeyFromString((String) clearPacket.opt("left"));
	                        if(clearPacket.has("upleft"))
	                            upLeft.publicKey = encryption.getPublicKeyFromString((String) clearPacket.opt("upleft"));
	                        if(clearPacket.has("downleft"))
	                            downLeft.publicKey = encryption.getPublicKeyFromString((String) clearPacket.opt("downleft"));
	                        if(clearPacket.has("upright"))
	                            upRight.publicKey = encryption.getPublicKeyFromString((String) clearPacket.opt("upright"));
	                        if(clearPacket.has("right"))
	                            right.publicKey = encryption.getPublicKeyFromString((String) clearPacket.opt("right"));
	                        if(clearPacket.has("downright"))
	                            downRight.publicKey = encryption.getPublicKeyFromString((String) clearPacket.opt("downright"));
	                        if(clearPacket.has("down"))
	                            down.publicKey = encryption.getPublicKeyFromString((String) clearPacket.opt("down"));
	                        if(clearPacket.has("up"))
	                            up.publicKey = encryption.getPublicKeyFromString((String) clearPacket.opt("up"));
	                        

	                    }

	                }
	                else
	                {
	                    if(down.publicKey !=  null)
	                    forwardMessage(down,input,"senddown");
	                }
	        }

	           
	                
	            
	            
	                
	        
	        

	            

	            
	        return input;
	            
	    }
    public void deadPeer(Peer sender)
    {
        JSONObject packet = new JSONObject();
        try {
            packet.put("connectionbroken", sender.name);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        packet = encryption.AESencryptJSON(packet, top.aesKey);
        packet = addHeader(packet, 1);
        sendUp(packet.toString());
        
    }
    public void close()
    {
        service.close();
    }
	public boolean sendUp(String input){
		if(!forwardMessage(up,input,"sendUp"))
			if(!forwardMessage(upLeft,input,"sendUp"))
				if(!forwardMessage(upRight,input,"sendUp"))		
					return false;
		return true;

	}
	public void sendDown(String input){
		forwardMessage(down,input,"sendUp");
		forwardMessage(left,input,"sendUp");
		forwardMessage(right,input,"sendUp");

	}

	

	public String waitFor(BufferedReader in) throws IOException{
		String temp;
		String content = "";
	       while((temp = in.readLine()) != null && !temp.equals("end"))
	       {
	    	   content+=temp;
	    	   
	       }
	       return content;
	}
	public void broadcast(){
		
	}
	
	public void repair(int port) {

        try {
            this.port  = port;
            run();
        } catch (Exception e) {
            
            e.printStackTrace();
        }
    }

    public void getKey(){
		try{
			String temp = "";
			String key  = "";
			
			Scanner scan = new Scanner(new File("out.txt"));
			while(!(temp = scan.next()).contains("PrivKey")){
				key +=temp;

			}
			publicKey = encryption.getPublicKeyFromString(key);
			top.publicKey = publicKey;
			up.publicKey = publicKey;
			
			key = "";
			while(scan.hasNext()){
				key+=scan.next();
			}
			privateKey = encryption.getPrivateKeyFromString(key);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
    public JSONObject addHeader(JSONObject json,int type)
    {

        try {
            return(json.put("col", Integer.toString(column)).put("id", Integer.toString(id)).put("type", Integer.toString(type)));

        } catch (JSONException e) {
            
            e.printStackTrace();
        }
        return json;
    }
	public void networkLoop(){
		while(true)
			try {
				service.selectBlocking();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		
	}


	public void get(String input){
		JSONObject request = new JSONObject();
		try {
			request.put("get", input);
			request = addHeader(encryption.AESencryptJSON(request, up.aesKey), 2);
			forwardMessage(up,request.toString(),"get");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
			
	}

	public boolean forwardMessage(Peer dest,String content , String type){
	    try {
	        JSONObject temp = new JSONObject(content);
	        temp.put("debug", type);
			return dest.socket.write((temp.toString()+"\n").getBytes());

	    } catch (Exception e) {
            
            e.printStackTrace();
        }
	    return false;
	}
	public boolean getDown(){
		try {
			Socket dest = new Socket(down.address,6001);
			return true;
		} catch (IOException e) {
			
			System.out.println("Peer: No down");
			
		}
		try {
			Socket dest = new Socket(left.address,6001);
			return true;
		} catch (IOException e) {
			
			System.out.println("Peer: No Left");
		}
		try {
			Socket dest = new Socket(right.address,6001);
			return true;
		} catch (IOException e) {
			
			System.out.println("Peer: No Right");
		}
		return false;
	}
	
}
