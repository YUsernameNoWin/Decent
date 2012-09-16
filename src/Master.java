import naga.ConnectionAcceptor;
import naga.NIOService;
import naga.NIOSocket;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;


/*Works as the main manager of the network. Manages peer map, peer connections, dead peers and new peers.
 * 
 */
public class Master extends Thread{
    Encryption encryption  =  new Encryption();
    private static final Map<String, String> OPPOSITEDIRECTION =
            Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("upRight", "downLeft");
                put("up", "down");
                put("upLeft", "downRight");
                put("downRight", "upLeft");
                put("down", "up");
                put("downLeft", "upRight");
                put("left", "right");
                put("right", "left");
            }});
	public ArrayList<UUID> leftList = new ArrayList<UUID>();
	public ArrayList<UUID> downList = new ArrayList<UUID>();
	public ArrayList<UUID> rightList = new ArrayList<UUID>();
	public CountableArrayList <ArrayList<Peer>> map= new CountableArrayList<ArrayList<Peer>>();
	public Queue <Hole>holes = new LinkedList<Hole>();
	public Queue<Hole> openSlots = new LinkedList<Hole>();
	public HashMap<String,Peer> IDMap =  new HashMap<String,Peer>();
    public HashMap<String,Message> tcpStateTracker = new HashMap<>();
    public HashMap<String,Message> peerTcpStateTracker = new HashMap<>();
	public PublicKey publicKey= null;
	public PrivateKey privateKey = null;
	public KeyPair peerKeys = null;
	public NIOService service;
	public InetAddress leftAd,rightAd,downAd;
	public HashMap<PublicKey,Peer> keyMap = new HashMap<PublicKey,Peer>();
	public int peerNum = 0;
    public int port = 510;
	public final int MAX_PEER = 100, NUM_COL = 5;
	String clearText;
	String AESKEY;
	char type;
	int col;
	public Master(KeyPair top, KeyPair keys1)
	{
	    publicKey = top.getPublic();
	    privateKey = top.getPrivate();
	    peerKeys = keys1;
	}


	
	

	public void run()
	{

          //  map.get(3).add(new Peer());


        for(int i = 0; i < NUM_COL; i++)
        {
            map.add(new CountableArrayList<Peer>(MAX_PEER));
            map.get(i).add(new Peer(i,0));
            map.get(i).get(0).ID = Integer.toString(i);
            map.get(i).get(0).publicKey = peerKeys.getPublic();
            holes.add(new Hole(i, 1));
        }
        //getKey();
        try {
           service = new NIOService();
           addBasePeer(map.get(0),0);
           addBasePeer(map.get(1),1);
           addBasePeer(map.get(2),2);
           addBasePeer(map.get(3),3);
           addBasePeer(map.get(4),4);
           addBasePeer(map.get(5),5);




                  // Handle IO until we quit the program.
                  while (true)
                  {
                        service.selectBlocking(1000);
                        processACKs();
                  }
        } catch (IOException e1) {
            
            e1.printStackTrace();
        }

	}

    private void processACKs() {
        for(Object test: tcpStateTracker.values().toArray())
        {
            Message message = (Message)test;
            message.updateTime();
            if(message.time > 10000)
            {
                sendACK(message,message.owner);
                message.resetTime();
            }
            if(message.timeout > 30000)
            {
                tcpStateTracker.remove(message.id);
            }
        }
        for(Object test: peerTcpStateTracker.values().toArray())
        {
            Message message = (Message)test;
            message.updateTime();
            if(message.time > 10000)
            {
                message.fromServer.sendACK(message,message.owner);
                message.resetTime();
            }
        }
    }




    public void parse(String input, ServerAdapter from)
    {

        try {
            JSONObject<?, ?> encryptedPacket =  new JSONObject<Object, Object>(new String(input));
            JSONObject<?, ?> outPacket =  new JSONObject<Object, Object>();
            JSONObject<?, ?> clearPacket =  new JSONObject<Object, Object>();
            //printMap();
            //if(UUID.fromString(encryptedPacket.getString("src")) == null)
            //  return;
            String id = encryptedPacket.getString("src");
            String messageID = "";
            String name  =  (String) encryptedPacket.remove("name");
            Peer hashed = IDMap.get(encryptedPacket.getString("src"));

            //If peer is not connected yet
            if(hashed == null)
            {
                IDMap.put(id,new Peer());
                hashed = IDMap.get(id);
            }

            if(encryptedPacket.getString("dest").equals(encryption.getKeyAsString(from.keyPair.getPublic())))
            {
                from.peerParse(encryptedPacket,hashed);
            }
            else if(encryptedPacket.getString("dest").equals(encryption.getKeyAsString(publicKey)))
            {
                if(hashed.getAesKey() == null)
                {
                    addPeer(hashed,encryptedPacket,from);

                }

                else
                {
                    clearPacket = encryption.AESdecryptJSON(encryptedPacket,hashed.getAesKey());
                    ACK(clearPacket,hashed);
                    if(clearPacket.has("connectionbroken"))
                    {
                        from.peerConnectionBroken(clearPacket, hashed);
                    }
                    if(clearPacket.has("bounce"))
                    {
                        from.bounceMessage(encryption.getPublicKeyFromString(clearPacket.getString("peerkey")),clearPacket.getString("bounce"));
                    }
                    if(clearPacket.has("needkeylist"))
                    {
                        sendKeyList(hashed);
                    }
                    if(clearPacket.has("needkeylist") && hashed.needsUpdate)
                    {
                        hashed.needsUpdate = false;
                    }
                    if(clearPacket.has("heart"))
                    {
                        heartBeat(hashed);
                    }
                    else if(clearPacket.has("port"))
                    {
                        hashed.port = clearPacket.getInt("port");
                    }
                    else if(clearPacket.has("dc"))
                    {
                        removePeer(hashed);
                    }
                    else if(clearPacket.has("publickey"))
                    {
                        from.peerAddPublicKey(hashed, clearPacket, from.socket);
                    }
                    else if(clearPacket.has("get"))
                    {
                        NIOSocket sock = null;
                        try {
                            sock = service.openSocket("127.0.0.1",1337);
                            sock.listen(new internalSocket(hashed,clearPacket.getString("get"),this));

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

            }
            //System.out.println("Server: "+"SENT PACKET " + outPacket.toString());
        } catch (JSONException e1) {

            e1.printStackTrace();
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public void ACK(JSONObject clearPacket, Peer sender)
    {
        try{
            String messageID = clearPacket.getString("messageid");
            Message message = tcpStateTracker.get(messageID);
            if(clearPacket.has("ack") &&  message != null)
            {
                tcpStateTracker.remove(messageID);
                if(clearPacket.getInt("ack") < 2)
                    sendACK(message,sender);
            }
            else if(tcpStateTracker.get(messageID) == null)
            {
                message = new Message(clearPacket,sender);
                tcpStateTracker.put(messageID,message);
                sendACK(message,sender);
            }


        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void sendACK(Message message, Peer hashed) {
        JSONObject outPacket = new JSONObject();
        try {
            if(message.contents.has("ack"))
                outPacket.put("ack", message.contents.getInt("ack") + 1 );
            else
                outPacket.put("ack", 0);

                outPacket = encryption.AESencryptJSON(outPacket,hashed.getAesKey());
                outPacket.put("messageid",message.id);
                outPacket = addHeader(outPacket,2,hashed);

            forwardMessage(hashed,outPacket.toString(),"ack " + message.id);
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private void bridgeParse(JSONObject<?,?> encryptedPacket, Peer hashed) {

    }
    private void addAESKey( JSONObject encryptedPacket, Peer hashed, ServerAdapter from)
    {
        JSONObject outPacket = new JSONObject();
        JSONObject clearPacket = null;
            try{


                if(encryptedPacket.has("aeskey"))
                {
                    byte[] key = Base64.decode(encryption.decryptRSA(privateKey, encryptedPacket.getString("aeskey").getBytes()));
                    hashed.setAesKey(key);
                    encryptedPacket.remove("aeskey");
                }
            }catch(Exception e)
            {
                e.printStackTrace();
            }

    }
    public void addPublicKey( JSONObject encryptedPacket, Peer hashed, ServerAdapter from,JSONObject clearPacket)
    {
        JSONObject outPacket = new JSONObject();

        try {

            if(clearPacket.has("publickey"))
            {
                hashed.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("src"));
                outPacket.put("gotpubkey",true);
                outPacket = encryption.AESencryptJSON(outPacket,hashed.getAesKey());
                outPacket = addHeader(outPacket,2,hashed);
                forwardMessage(hashed,outPacket.toString(),"topgotpubkey");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    private void sendUpLeftConnection(Peer hashed, JSONObject<?, ?> clearPacket) {
        // printMap();
        JSONObject<?, ?> peers = getPeers(hashed);
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
                    outPacket  = addHeader(outPacket, 2, upLeft);
                    forwardMessage(map.get(upLeft.x).get(0).socket,outPacket.toString(),"sendupleftConnection");
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    private void sendUpRightConnection(Peer hashed, JSONObject<?, ?> clearPacket) {
        //printMap();
        try {
            JSONObject<?, ?> peers = getPeers(hashed);
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
                    outPacket  = addHeader(outPacket, 2, upRight);
                    forwardMessage(map.get(upRight.x).get(0).socket,outPacket.toString(),"senduprightConnection");
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }

    }
    private boolean registerPeerKeys(JSONObject<?, ?> encryptedPacket, Peer hashed, NIOSocket socket, String id) {
        try {
            JSONObject<?, ?> outPacket = new JSONObject<Object, Object>();
            JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>();
            byte[] key = Base64.decode(encryption.decryptRSA(privateKey, encryptedPacket.getString("aeskey").getBytes()));
            hashed.setAesKey(key);

            encryptedPacket.remove("aeskey");
            clearPacket = encryption.AESdecryptJSON(encryptedPacket,key);
            hashed.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("src"));
            hashed.socket = socket;
            hashed.setActive(true);
            hashed.x = Integer.parseInt(clearPacket.getString("col"));

            JSONObject<String,Peer> peers = getPeers(hashed);

            //Notify user that they are connected
            /*       outPacket.put("connected", "yes");
            outPacket = encryption.AESencryptJSON(outPacket, key);
            outPacket = addHeader(outPacket, 2, hashed);
            forwardMessage(newPeer.socket,outPacket.toString(),"connectionSuccess");*/

            //wrong column. Make the peer reconnect
            /*if(added !=  from.port)
            {
                outPacket
                        .put("repair",true)
                        .put("port", added + 510);

                //   outPacket = addHeader(encryption.AESencryptJSON(outPacket,key),2,hashed);


            }      */
        }catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }

        return true;

    }
    public String get(String content){
		NIOSocket sock = null;
		
		
		return content;
		
	}
	public void printMap() 
	{
	    
        System.out.println();
        System.out.print("MAP        ");
        for(int i = 0; i < MAX_PEER;i++)
        	System.out.print(i + " ");
        	        System.out.println();
	    for(int i = 0;i<map.size();i++)
	    {
	        System.out.print("Col: " + i + " ||| ");
	        for(int x =0;x<map.get(i).size();x++) {
	            System.out.print(map.get(i).get(x).name + " ");
	        }
	        System.out.println();
	    }
	    //System.out.println(peerNum);
	    
	}
	    public JSONObject<String,Peer> getPeers(Peer peer){	
	    	//System.out.println("WTF");
	        JSONObject<String,Peer> output = new JSONObject<String,Peer>();
	        try {

	            output.put("right", map.get(peer.x+1).get(peer.y));
	            output.put("left", map.get(peer.x-1).get(peer.y));
	
	            if(peer.y - 1 >= 0)
	            {
	                output.put("up", map.get(peer.x).get(peer.y-1));
	                output.put("upLeft", map.get(peer.x-1).get(peer.y - 1));
	                output.put("upRight", map.get(peer.x+1).get(peer.y - 1));
	            }
	            if(peer.y + 1 <= map.get(peer.x).size())
	            {
	            	try{
	                   output.put("downLeft", map.get(peer.x + 1).get(peer.y + 1));
	            	}
	            	catch(Exception e)
	            	{
	            		
	            		System.out.println("BAD PEER::::::: " + peer.ID);
	            	}
	                   
	                   output.put("downRight", map.get(peer.x + 1).get(peer.y + 1));
	                   output.put("down", map.get(peer.x).get(peer.y + 1));
	            }
                if(peer.y ==  1)
                {
                    output.put("upRight", map.get(peer.x + 2).get(1));
                    output.put("downLeft", map.get(peer.x -2).get(1));

                    output.put("upLeft", map.get(peer.x + 1).get(1));
                    output.put("downRight", map.get(peer.x + 1).get(1));
                }
	        }
	        catch(JSONException e)
	        {
	                e.printStackTrace();
	        }
	        return output;
	    }





	public void addPeer(Peer hashed, JSONObject encryptedPacket, ServerAdapter from)
	{
        JSONObject clearPacket = null;
        addAESKey(encryptedPacket,hashed,from);
        hashed.socket = from.socket;
        try{
            clearPacket = encryption.AESdecryptJSON(encryptedPacket,hashed.getAesKey());
            ACK(clearPacket,hashed);
            addPublicKey(encryptedPacket,hashed,from,clearPacket);
            addPeerToMap(hashed);
            /*if(hashed.ordersGiven)

            else
                giveOrders(hashed,clearPacket,from);*/


            hashed.setActive(true);
            hashed.x = Integer.parseInt(clearPacket.getString("col"));
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        JSONObject<String,Peer> peers = getPeers(hashed);
    }

    public void giveOrders(Peer peer, JSONObject clearPacket, ServerAdapter from) {
        if(!holes.isEmpty())
        {
            Hole hole = holes.remove();
            peer.y  = hole.y;
            peer.x  = hole.x;
            repair(peer,map.get(peer.x).get(peer.y).port,peer.x,peer.y);
            if(map.get(peer.x).size() == peer.y)
                holes.add(new Hole(peer.x, peer.y + 1));
            peer.ordersGiven = true;
        }
    }

    public boolean addPeerToMap(Peer peer)
    {

            peer.y = map.get(peer.x).size();


            if(map.get(peer.x).size() > MAX_PEER)
            {
                if(map.get(map.activeSize-1).size() > MAX_PEER)
                {
                    CountableArrayList newList = new CountableArrayList<Peer>(MAX_PEER);
                    newList.add(peer);
                    map.add(newList);
                    addBasePeer(map.get(map.activeSize - 1), map.activeSize-1 );
                    peer.y = 1;
                }
                else
                {

                    peer.y = map.get(map.activeSize-1).size();
                }
                peer.x = map.activeSize-1;
                int repairPort = 500 +(peer.y * 10) + map.activeSize-1;
                repair(peer,repairPort ,map.activeSize-1,peer.y);
                IDMap.remove(peer.publicKey);
                return false;


            }
            map.get(peer.x).add(peer);
            peerNum++;
            //printMap();
            JSONObject peerList =  getPeers(peer);
            JSONObject peerPubKey =  new JSONObject();
            try{
                for(int i = 0; i < peerList.length();i++)
                {
                    Peer current = (Peer)peerList.get((String)peerList.names().get(i));
                    if(current.publicKey != null && current.getAesKey() != null)
                    {
                        current.needsUpdate = true;
                    }
                }

            }catch(Exception e)
            {
                e.printStackTrace();
            }

        return true;

    }
    public void repair(Peer peer, int port, int x, int y) {
        JSONObject outPacket = new JSONObject();
        try {
            outPacket.put("repair", true);
            outPacket.put("ip","127.0.0.1");
            outPacket.put("port", port);
            outPacket.put("publickey",encryption.getKeyAsString(map.get(x).get(y-1).publicKey));
            outPacket = addHeader(encryption.AESencryptJSON(outPacket,peer.getAesKey()),2,peer);
            forwardMessage(peer,outPacket.toString(),"repair");
            peer.setActive(false);
            peer.setAesKey(null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void addBasePeer(List<Peer> list,int index)
	{
	    Peer base = list.get(0);
	    try {
	    	base.peerKeys = encryption.generateKey();
	    	base.publicKey = peerKeys.getPublic();
	        base.serverSock = service.openServerSocket(510+index);
	        base.serverSock.listen(new ServerAdapter(this,index,base,peerKeys));
	        base.serverSock.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
	        
	    } catch (Exception e) {
	        
	        e.printStackTrace();
	    }
	
	}





	public void removePeer(Peer hashed) {
	   JSONObject<String, Peer> peers = getPeers(hashed);
	   Peer replacement = null;
	   int maxTrickleUp = 99999999;
	   try{
    	   if(peers.has("down") && ((Peer)peers.get("down")).trickleUpCount < maxTrickleUp)
    	   {
    	       replacement = ((Peer)peers.get("down"));
    	   }
           if(peers.has("downRight") && ((Peer)peers.get("downRight")).trickleUpCount < maxTrickleUp)
           {
               replacement = ((Peer)peers.get("downRight"));
           }
           if(peers.has("downLeft") && ((Peer)peers.get("downLeft")).trickleUpCount < maxTrickleUp)
           {
               replacement = ((Peer)peers.get("downLeft"));
           }
           if(replacement != null)
           {
               JSONObject outPacket = new JSONObject();
               if(peers.has("up"))
               {
                   Peer up = (Peer)peers.get("up");
                   outPacket.put("repair", encryption.getKeyAsString(up.publicKey));
                   outPacket = encryption.AESencryptJSON(outPacket, replacement.getAesKey());
                   outPacket = addHeader(outPacket,2,replacement);
               }
           }
	   }catch(JSONException e)
	   {
	       e.printStackTrace();
	   }
       holes.add(new Hole(hashed.y,hashed.x));
       map.get(hashed.x).remove(hashed.y);
       IDMap.remove(hashed.ID);
       peerNum--;
       printMap();
       System.out.println("Peer: " + hashed.ID + " has died");
    }





    public List<Peer> findShortest()
	{
		List<Peer> index =map.get(0);
		for(int i = 0; i < map.size(); i ++)
		{
			ArrayList<Peer>list = map.get(i);
			if(list.size() < index.size())
			    index = list;
		}
		return index;
	}
	public JSONObject<?, ?> addHeader(JSONObject<?, ?> json,int type,Peer peer )
	{   String random = "";
        try {
            if(!json.has("messageid"))
            {
                random = Integer.toString((int) (Math.random() * 1000000));

                    json.put("messageid",random);
                    tcpStateTracker.put(random,new Message(json,peer));
                    json.remove("messageid");
            }
            else
            {
                random = json.getString("messageid");
            }

                json.put(new String(encryption.encryptAES(peer.getAesKey(),"messageid".getBytes())),new String(encryption.encryptAES(peer.getAesKey(),random.getBytes())));
                return(json
                        .put("col", Integer.toString(peer.x))
                        .put("src", encryption.getKeyAsString(publicKey))
                        .put("dest", encryption.getKeyAsString(peer.publicKey))
                        .put("type", Integer.toString(type)));

		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return json;
	}


	public void saveKey()
	{
		  try
		  {
			  // Create file 
				  FileWriter fstream = new FileWriter("out.txt",true);
				  BufferedWriter out = new BufferedWriter(fstream);
				  
				  out.write(encryption.getKeyAsString(publicKey));
				  out.newLine();
				  out.write("PrivKey");
				  out.newLine();
				  out.write(encryption.getKeyAsString(privateKey));
				  //Close the output stream
				  out.close();
		  }catch (Exception e) {//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
		  }
		
	}
    public void sendKeyList(Peer hashed) {
        JSONObject<?, ?> outPacket = new JSONObject();
        JSONObject<String, Peer> peers = getPeers(hashed);
        hashed.peers = peers;

        try {
            outPacket.put("keylist",true);
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        for(int i = 0; i < peers.names().length();i++)
        {
            String key;
            try {
                key = (String)peers.names().get(i);

                outPacket.put(key.toLowerCase(),
                        encryption.getKeyAsString(((Peer)peers.get(key)).publicKey));
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        outPacket = addHeader(encryption.AESencryptJSON(outPacket,hashed.getAesKey()),2,hashed);
        forwardMessage(hashed.socket,outPacket.toString(),"sendkeylist");

    }
	public void saveKey2()
	{
		  try
		  {
			  // Create file 
				  FileWriter fstream = new FileWriter("out2.txt",true);
				  BufferedWriter out = new BufferedWriter(fstream);
				  
				  out.write(encryption.getKeyAsString(publicKey));
				  out.newLine();
				  out.write("PrivKey");
				  out.newLine();
				  out.write(encryption.getKeyAsString(privateKey));
				  //Close the output stream
				  out.close();
		  }catch (Exception e) {//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
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
			key = "";
			while(scan.hasNext()){
				key+=scan.next();
			}
			privateKey = encryption.getPrivateKeyFromString(key);
		

		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
	public void getKey2(){
		try{
			String temp = "";
			String key  = "";
			
			Scanner scan = new Scanner(new File("out2.txt"));
			while(!(temp = scan.next()).contains("PrivKey")){
				key +=temp;

			}
			publicKey = encryption.getPublicKeyFromString(key);
			key = "";
			while(scan.hasNext()){
				key+=scan.next();
			}
			privateKey = encryption.getPrivateKeyFromString(key);
		

		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
    public boolean forwardMessage(NIOSocket dest,String content, String debug){
        try {
            JSONObject<?, ?> temp = new JSONObject<Object, Object>(content);
            temp.put("debug", debug);
            return dest.write((temp.toString() + "\n").getBytes());
        }catch(Exception e) {
            //System.out.println(content + " " + dest);
            e.printStackTrace();
            return false;
        }
    }
    public boolean forwardMessage(Peer dest,String content, String debug){
        try {
            JSONObject<?, ?> temp = new JSONObject<Object, Object>(content);
            temp.put("debug", debug);

            return dest.socket.write((temp.toString()+"\n").getBytes());

        } catch (Exception e) {
            
            e.printStackTrace();
        }
        return false;
    }


    public void heartBeat(Peer hashed) {
        JSONObject outPacket = new JSONObject();
        try {
            outPacket.put("beat", true);
            if(hashed.needsUpdate)
            {
                update(hashed);
            }
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        outPacket = addHeader(encryption.AESencryptJSON(outPacket,hashed.getAesKey()), 2, hashed);
        forwardMessage(hashed,outPacket.toString(),"heartbeat from top");
    }

    private void update(Peer hashed)
    {
        sendKeyList(hashed);
    }
}
