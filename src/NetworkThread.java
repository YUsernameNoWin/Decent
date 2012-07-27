import naga.ConnectionAcceptor;
import naga.NIOServerSocket;
import naga.NIOService;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedSelectorException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.UUID;

/* Handles message parsing in function Parse().
 * Equivalent of Master for ServerAdapter
 * Tracks peers and connections.
 */
public class NetworkThread extends Thread{
    Encryption encryption  =  new Encryption();
    
	Peer down,left,right,up,upLeft,upRight,master,downLeft,downRight,top,futureUp;

	
	//initialization booleans
    public boolean exchangedKeysWithUp, recievedColFromUp, exchangedKeysWithTop, openedServerSocket, SentLeftRightConnections, initialized;
	public PrivateKey privateKey;
	public PublicKey publicKey;
    private LinkedList <Peer> prepSockets =  new LinkedList<Peer>();
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
	public NetworkThread(int port,int id, KeyPair keys,KeyPair newKeys, KeyPair topKeys) throws Exception{
	    this.port = port;
		this.id = id;
        top = new Peer(encryption.generateSymmetricKey());
        top.name  = "top";
        top.publicKey = topKeys.getPublic();
		this.publicKey = newKeys.getPublic();
		this.privateKey = newKeys.getPrivate();
		up = new Peer("127.0.0.1", port,"up");
		up.publicKey = keys.getPublic();
	}
	public void run()
	{
	    

	    try {
            try {
                service = new NIOService();
            } catch (IOException e) {
                
                e.printStackTrace();
            }

            

            upLeft = new Peer("127.0.0.1", port+1,"upLeft");

            upRight = new Peer("127.0.0.1", port+2,"upRight");
 
            right = new Peer("127.0.0.1", port,"right");
    
            left = new Peer("127.0.0.1", port,"left");
      
            downRight= new Peer("127.0.0.1", port+3,"downRight");
           
            downLeft = new Peer("127.0.0.1", port+4,"downLeft");
         

            
            //getKey();
           
            up.socket = service.openSocket(up.address,port);
            up.socket.setPacketReader(new AsciiLinePacketReader());
            up.socket.setPacketWriter(new RawPacketWriter());
            up.socket.listen(new NetworkProtocol(this,up));
            up.setActive(true);

            down = new Peer("127.0.0.1", port + 10,"down");
            openServerSocket();
            


            //masterKeyExchange();
            sleep((int)(Math.random() * 500) + 200);

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
	    JSONObject<?, ?> packet = new JSONObject<Object, Object>();
	    try {
            packet.put("publickey", encryption.getKeyAsString(publicKey));
            packet = encryption.AESencryptJSON(packet, sender.getAesKey());
            packet = addHeader(packet,1,sender);
            forwardMessage(sender,packet.toString(),"sendPubkey");
        } catch (JSONException e) {
            e.printStackTrace();
        }
	}
	public void sendLeftConnection()
	{
	    
       JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>();
       try {
	       clearPacket =  clearPacket.put("upleftip", "127.0.0.1");
	       clearPacket =  clearPacket.put("upleftport", port+10);
	       clearPacket= encryption.AESencryptJSON(clearPacket, top.getAesKey());
	       clearPacket = addHeader(clearPacket,1,top);
       }catch(Exception e) {
           e.printStackTrace();
       }
       forwardMessage(up,clearPacket.toString(),"leftconnection");
        
    }
    public void sendRightConnection()
    {

        JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>();
        try {
            clearPacket =  clearPacket.put("uprightip", "127.0.0.1");
            clearPacket =  clearPacket.put("uprightport", port+10);
            clearPacket= encryption.AESencryptJSON(clearPacket,top.getAesKey());
            clearPacket = addHeader(clearPacket,1,top);
        }catch(Exception e) {
            e.printStackTrace();
        }
        forwardMessage(up,clearPacket.toString(),"rightconnection");

    }
	public void updatePort() throws JSONException
	{
        JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>();
        clearPacket =  clearPacket.put("port",port);
        clearPacket= encryption.AESencryptJSON(clearPacket,top.getAesKey());
        clearPacket = addHeader(clearPacket,1,top);
       forwardMessage(up,clearPacket.toString(),"port"); 
	}
	public void getKeyList() throws JSONException
	{
        JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>();
        clearPacket =  clearPacket.put("needkeylist",true);
        clearPacket= encryption.AESencryptJSON(clearPacket,up.getAesKey());
        clearPacket = addHeader(clearPacket,1,top);
       forwardMessage(up,clearPacket.toString(),"getkeylist");
	}

    
    public void getColumn(Peer sender) throws JSONException
	{     
	
	    JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>(); 
	   clearPacket =  clearPacket.put("needcol",true); 
	   clearPacket= encryption.AESencryptJSON(clearPacket,sender.getAesKey());
	   clearPacket = addHeader(clearPacket,2,sender);
	
	    forwardMessage(sender,clearPacket.toString(),"getcolumn");
	}
	public void keyExchange(Peer peer) throws Exception
    {
    	JSONObject<?, ?> clearPacket;
    	
        peer.setAesKey(encryption.generateSymmetricKey());
        clearPacket = new JSONObject<Object, Object>();
        clearPacket.put("publickey", encryption.getKeyAsString(publicKey));
        clearPacket = encryption.AESencryptJSON(clearPacket,peer.getAesKey());
        String yes = new String(encryption.encryptRSA(peer.publicKey,peer.getAesKeyInBase64()));
        clearPacket =  clearPacket.put("aeskey", yes);
        clearPacket = addHeader(clearPacket,1,peer);

       forwardMessage(up,clearPacket.toString(),"keyexchange from "+ this.id+" to " + peer.name);
        
    }

    public String parse(String input, Peer sender)throws Exception
	    {
	        JSONObject<?, ?> encryptedPacket = new JSONObject<Object, Object>(input);
	        JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>();
	        //1 == send up. 2 == broadcast. 3 == forward down. 4 == send down one. 5 == Reroute.
	        int type = encryptedPacket.getInt("type");
	        //test send up to master, add UUID to senders list in order to send back response
	        //get
	        //System.out.println();
            activateSender(clearPacket,encryptedPacket,sender);
            if(!initialized)
              notReadyParse(encryptedPacket,sender);
            if(initialized)
            {
                if(encryptedPacket.getString("debug").equals("13"))
                    System.out.println("13 at " + id);
                String name  =  (String) encryptedPacket.remove("name");
                if(type == 1)
                {
                   parseDownStream(encryptedPacket,sender);
                }

                //forward response downwards or if it is meant for us, decrypt it.
                else if( type == 2)
                {
                    parseUpStream(sender,encryptedPacket);
                }
            }
	        return input;
	            
	    }

    private void parseUpStream(Peer sender, JSONObject<?, ?> encryptedPacket) throws Exception {
        JSONObject clearPacket = new JSONObject();
        //TODO figure out how to mask the src from everyone but the top.
        //maybe do some sort of 1 time id
        //message from master
        if(encryption.getKeyAsString(publicKey).equals(encryptedPacket.getString("dest")))
        {
            //System.out.println();
            try {
                if(encryptedPacket.getString("src").equals(encryption.getKeyAsString(top.publicKey)))
                    clearPacket =  encryption.AESdecryptJSON(encryptedPacket,top.getAesKey());
                else if(encryptedPacket.getString("src").equals(encryption.getKeyAsString(up.publicKey)))
                    clearPacket =  encryption.AESdecryptJSON(encryptedPacket,up.getAesKey());
            }catch(Exception e)
            {
                System.out.println("FUCK");
            }
            if(clearPacket.has("repair"))
            {
                repair(clearPacket);
            }
            if(clearPacket.has("connect"))
            {
                connectToPeer(clearPacket);
            }
            if( clearPacket.has("keylist"))
            {
                processKeyList(clearPacket);

            }
            if(clearPacket.has("response"))
            {
                //System.out.println("RESPONSE " + clearPacket.getString("response"));
                saveHTML(clearPacket.getString("response"));
            }
        }
        else
        {
            int tempCol = Integer.parseInt(encryptedPacket.getString("col"));
            if(tempCol > column)
                if(downLeft.publicKey !=  null)
                    forwardMessage(downLeft,encryptedPacket.toString(),encryptedPacket.getString("debug"));
                else if(upLeft.publicKey != null)
                    forwardMessage(upLeft,encryptedPacket.toString(),encryptedPacket.getString("debug"));
                else if(tempCol < column)
                    if(downRight.publicKey !=  null)
                        forwardMessage(downRight,encryptedPacket.toString(),encryptedPacket.getString("debug"));
                    else if(upRight.publicKey != null)
                        forwardMessage(upRight,encryptedPacket.toString(),encryptedPacket.getString("debug"));
                    else if( tempCol == column && down.publicKey !=  null)
                    {
                        forwardMessage(down,encryptedPacket.toString(),encryptedPacket.getString("debug"));
                        forwardMessage(up,encryptedPacket.toString(),encryptedPacket.getString("debug"));
                    }
        }
    }

    private void parseDownStream(JSONObject encryptedPacket, Peer sender) throws JSONException {
        JSONObject clearPacket = new JSONObject();
        if(!sender.isActive())
        {
            try{
                activateSender(clearPacket, encryptedPacket, sender);

            }catch(Exception e)
            {

                System.out.println("WTF");
            }
        }
        else if(encryption.getKeyAsString(publicKey).equals(encryptedPacket.getString("dest")))
        {
            processMessageForSelf(clearPacket, encryptedPacket, sender);

        }
        else {
            forwardMessage(up,encryptedPacket.toString(),encryptedPacket.getString("debug"));
        }
    }

    private void notReadyParse(JSONObject<?, ?> encryptedPacket, Peer sender) {
        try{
            if(!exchangedKeysWithUp)
            {   JSONObject clearPacket = encryption.AESdecryptJSON(encryptedPacket, sender.getAesKey());
                if(clearPacket.has("gotpubkey"))
                    exchangedKeysWithUp = true;

            }
            else if(!recievedColFromUp)
            {
                JSONObject clearPacket = encryption.AESdecryptJSON(encryptedPacket,sender.getAesKey());
                getColumn(up);
                setColumn(clearPacket);
            }
            else if(!exchangedKeysWithTop)
            {
                JSONObject clearPacket = encryption.AESdecryptJSON(encryptedPacket,top.getAesKey());
                keyExchange(top);
                exchangedKeysWithTop = activateSender(clearPacket,encryptedPacket,top);
                if(clearPacket.has("gotpubkey"))
                    exchangedKeysWithUp = true;
            }
            else if(!openedServerSocket)
            {
                openServerSocket();
                openedServerSocket = true;
            }
            else if(!SentLeftRightConnections)
            {
                sendLeftConnection();
                sendRightConnection();
                SentLeftRightConnections = true;
                initialized = true;
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void saveHTML(String html)
    {
          try
          {
              // Create file 
                  FileWriter fstream = new FileWriter("success.html",false);
                  BufferedWriter out = new BufferedWriter(fstream);
                  
                  out.write(html);
                  out.close();
                  Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler C://users/quinn/workspace/webserver/success.html");
          }catch (Exception e) {//Catch exception if any
              System.err.println("Error: " + e.getMessage());
          }
          
        
    }
    private void processKeyList(JSONObject<?, ?> clearPacket) throws Exception {
        if(clearPacket.has("left"))
            left.publicKey = encryption.getPublicKeyFromString((String) clearPacket.get("left"));
        if(clearPacket.has("upleft"))
            upLeft.publicKey = encryption.getPublicKeyFromString((String) clearPacket.get("upleft"));
        if(clearPacket.has("downleft"))
            downLeft.publicKey = encryption.getPublicKeyFromString((String) clearPacket.get("downleft"));
        if(clearPacket.has("upright"))
            upRight.publicKey = encryption.getPublicKeyFromString((String) clearPacket.get("upright"));
        if(clearPacket.has("right"))
            right.publicKey = encryption.getPublicKeyFromString((String) clearPacket.get("right"));
        if(clearPacket.has("downright"))
            downRight.publicKey = encryption.getPublicKeyFromString((String) clearPacket.get("downright"));
        if(clearPacket.has("down"))
            down.publicKey = encryption.getPublicKeyFromString((String) clearPacket.get("down"));
        if(clearPacket.has("up"))
            up.publicKey = encryption.getPublicKeyFromString((String) clearPacket.get("up"));
        
        
    }
    private void connectToPeer(JSONObject<?, ?> clearPacket) throws JSONException, IOException, Exception {
        if(clearPacket.getString("connect").equals("upright")  && !downLeft.isActive())
        {
            downLeft.address = clearPacket.getString("ip");
            downLeft.port = clearPacket.getInt("port");
            downLeft.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
            downLeft.socket = service.openSocket(downLeft.address,downLeft.port);
            downLeft.socket.setPacketReader(new AsciiLinePacketReader());
            downLeft.socket.setPacketWriter(new RawPacketWriter());
            downLeft.socket.listen(new NetworkProtocol(this,downLeft));
            downLeft.setActive(true);
        }
        if(clearPacket.getString("connect").equals("upleft") && !downRight.isActive())
        {
            downRight.address = clearPacket.getString("ip");
            downRight.port = clearPacket.getInt("port");
            downRight.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
            downRight.socket = service.openSocket(downRight.address,downRight.port);
            downRight.socket.setPacketReader(new AsciiLinePacketReader());
            downRight.socket.setPacketWriter(new RawPacketWriter());
            downRight.socket.listen(new NetworkProtocol(this,downRight));
            downRight.setActive(true);
        }
        
    }
    private void processMessageForSelf(JSONObject<?, ?> clearPacket,JSONObject<?, ?> encryptedPacket,Peer sender) {
        try 
        {
        	if(encryptedPacket.getString("src").equals(encryption.getKeyAsString(top.publicKey)))
            	clearPacket =  encryption.AESdecryptJSON(encryptedPacket,top.getAesKey());
            else if(down.isReady() && encryptedPacket.getString("src").equals(encryption.getKeyAsString(down.publicKey)))
            	clearPacket =  encryption.AESdecryptJSON(encryptedPacket,down.getAesKey());
            if(clearPacket.has("publickey"))
                sender.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
            else if(clearPacket.has("needcol"))
            {
                JSONObject<?, ?> outPacket = new JSONObject<Object, Object>();
                outPacket.put("col", column);
                outPacket = encryption.AESencryptJSON(outPacket, sender.getAesKey());
                outPacket = addHeader(outPacket,2,sender);
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
          
            e.printStackTrace();
        }
        
    }
    private boolean activateSender(JSONObject<?, ?> clearPacket,JSONObject<?, ?> encryptedPacket,Peer sender) {
        try {
            if(encryptedPacket.has("aeskey"))
            {

                byte[] key = encryption.decryptRSA(privateKey, encryptedPacket.getString("aeskey").getBytes());
                sender.setAesKeyFromBase64(key);
                encryptedPacket.remove("aeskey");
                clearPacket = encryption.AESdecryptJSON(encryptedPacket,sender.getAesKey());
                if(clearPacket.has("publickey"))
                {
                	sender.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
	                sender.setActive(true);
	                sender.ID = clearPacket.getString("src");
                    JSONObject outPacket = new JSONObject();
                    outPacket.put("gotpubkey",true);
                    outPacket = addHeader(encryption.AESdecryptJSON(outPacket,sender.getAesKey()),2,sender);
                    forwardMessage(sender,outPacket.toString(),"gotpubkey");
                    return true;
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
    public void deadPeer(Peer sender)
    {
        JSONObject<?, ?> packet = new JSONObject<Object, Object>();
        try {
            packet.put("connectionbroken", sender.name);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        packet = encryption.AESencryptJSON(packet, top.getAesKey());
        packet = addHeader(packet, 1,top);
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
	public void setColumn(JSONObject clearPacket){
        if(clearPacket.has("col"))
        {
            try {
                column = clearPacket.getInt("col");
                recievedColFromUp = true;
            } catch (JSONException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }
    }
	public void repair(JSONObject<?, ?> clearPacket) throws JSONException {
	    JSONObject outPacket = new JSONObject();
	    try {
	    String publicKey = clearPacket.getString("repair");
	       futureUp = new Peer();
	       futureUp.publicKey = encryption.getPublicKeyFromString(publicKey);
	       futureUp.setAesKey(encryption.generateSymmetricKey());
	    outPacket.put("bounce", encryption.encryptRSA(futureUp.publicKey, futureUp.getAesKeyInBase64()));
	    outPacket.put("peerkey", publicKey);
	    outPacket = encryption.AESdecryptJSON(outPacket, top.getAesKey());
	    outPacket =  addHeader(outPacket,1,top);
	    }catch(Exception e)
	    {
	        e.printStackTrace();
	    }
        
    }
	public void bounceParse(JSONObject encryptedPacket, Peer bouncer)
	{
	    JSONObject outPacket = new JSONObject();
	    JSONObject containerPacket = new JSONObject();
	    if(bouncer.getAesKey() ==  null)
	    {
	       
	        try {
	            encryptedPacket = encryption.RSAdecryptJSON(encryptedPacket, privateKey);
                bouncer.setAesKeyFromBase64(encryptedPacket.getString("bounce").getBytes());
                outPacket.put("ip", InetAddress.getLocalHost().getHostAddress());
                outPacket = encryption.AESencryptJSON(outPacket, bouncer.getAesKey());
                containerPacket.put("bounce", outPacket.toString());
                containerPacket = encryption.AESencryptJSON(containerPacket, top.getAesKey());
                containerPacket = addHeader(containerPacket,1,top);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {

                e.printStackTrace();
            }
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
    public JSONObject<?, ?> addHeader(JSONObject<?, ?> json,int type, Peer dest)
    {
    	if(dest.publicKey == null)
    		System.out.println(json + " " + dest);
        try {
            return(json
            		.put("col", Integer.toString(column))
            		.put("type", Integer.toString(type))
            		.put("src", encryption.getKeyAsString(publicKey))
            		.put("dest", encryption.getKeyAsString(dest.publicKey))
                    .put("name", Integer.toString(id)));

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
		JSONObject<?, ?> request = new JSONObject<Object, Object>();
		try {
			request.put("get", input);
			request = addHeader(encryption.AESencryptJSON(request, top.getAesKey()), 1,top);
			forwardMessage(up,request.toString(),"get");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
			
	}

	public boolean forwardMessage(Peer dest,String content , String type){
        if(!dest.isActive())
        {
            System.out.println(content);
            return false;
        }
	    try {
	        JSONObject<?, ?> temp = new JSONObject<Object, Object>(content);
	        temp.put("debug", (type));

	        //System.out.println(temp.get("debug"));
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
	public void getSocketStatus() {
		ArrayList badSockets = new ArrayList();
		if(up.socket == null)
			badSockets.add(up.name);
		if(upLeft.socket == null)
			badSockets.add(upLeft.name);
		if(upRight.socket == null)
			badSockets.add(upRight.name);

		if(down.socket == null)
			badSockets.add(down.name);
		if(downLeft.socket == null)
			badSockets.add(downLeft.name);
		if(downRight.socket == null)
			badSockets.add(downRight.name);
		System.out.println("Peer " + this.id + " has bad sockets: " + badSockets);
	}
	public void getKeyStatus() {
		ArrayList badKeys = new ArrayList();
		if(up.publicKey == null)
			badKeys.add(up.name);
		if(upLeft.publicKey == null)
			badKeys.add(upLeft.name);
		if(upRight.publicKey == null)
			badKeys.add(upRight.name);

		if(down.publicKey == null)
			badKeys.add(down.name);
		if(downLeft.publicKey == null)
			badKeys.add(downLeft.name);
		if(downRight.publicKey == null)
			badKeys.add(downRight.name);
		System.out.println("Peer " + this.id + " has bad publicKey: " + badKeys);
		
	}

    public void openServerSocket() {
        try {
            down.serverSock = service.openServerSocket(down.port);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        prepSockets.add(down);
        prepSockets.add(upRight);
        prepSockets.add(upLeft);

        down.serverSock.listen(new PeerServerAdapter(this,prepSockets));
        down.serverSock.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
    }
}
