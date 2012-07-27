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
                put("upLeft","downRight");
                put("downRight","upLeft");
                put("down","up");
                put("downLeft","upRight");
                put("left","right");
                put("right","left");
            }});
	public ArrayList<UUID> leftList = new ArrayList<UUID>();
	public ArrayList<UUID> downList = new ArrayList<UUID>();
	public ArrayList<UUID> rightList = new ArrayList<UUID>();
	public CountableArrayList <ArrayList<Peer>> map= new CountableArrayList<ArrayList<Peer>>();
	public Queue <Hole>holes = new LinkedList<Hole>();
	public Queue <Hole>openSlots = new LinkedList<Hole>();
	public HashMap<String,Peer> IDMap =  new HashMap<String,Peer>();
	public PublicKey publicKey= null;
	public PrivateKey privateKey = null;
	public KeyPair peerKeys = null;
	public NIOService service;
	public InetAddress leftAd,rightAd,downAd;
	public HashMap<PublicKey,Peer> keyMap = new HashMap<PublicKey,Peer>();
	public int peerNum = 0;
	public final int MAX_PEER = 100;
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
	    map.add(new CountableArrayList<Peer>(MAX_PEER));
        map.add(new CountableArrayList<Peer>(MAX_PEER));
        map.add(new CountableArrayList<Peer>(MAX_PEER));
        //map.add(new CountableArrayList<Peer>(MAX_PEER));
           map.get(0).add(new Peer(0,0));
           map.get(1).add(new Peer(1,0));
           map.get(2).add(new Peer(2,0));
           
           map.get(0).get(0).ID = "1";
           map.get(1).get(0).ID = "2";
           map.get(2).get(0).ID = "3";
           
           map.get(0).get(0).publicKey = peerKeys.getPublic();
           map.get(1).get(0).publicKey = peerKeys.getPublic();
           map.get(2).get(0).publicKey = peerKeys.getPublic();
          //  map.get(3).add(new Peer());
        
        //getKey();
        try {
           service = new NIOService();
           addBasePeer(map.get(0),0);
           addBasePeer(map.get(1),1);
           addBasePeer(map.get(2),2);



                  // Handle IO until we quit the program.
                  while (true)
                  {
                        service.selectBlocking(1000);
                  }
        } catch (IOException e1) {
            
            e1.printStackTrace();
        }

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
	        }
	        catch(JSONException e)
	        {
	                e.printStackTrace();
	        }
	        return output;
	    }





	public int addPeer(Peer peer) 
	{

		if(!holes.isEmpty())
		{
			Hole hole = holes.remove();
			peer.y  = hole.y;
			peer.x  = hole.x;
			if(map.get(peer.x).get(0).socket != null)
			    peer.socket = map.get(peer.x).get(0).socket;
			peer.setActive(true);
			map.get(hole.x).set(hole.y, peer);
			openSlots.add(new Hole(hole.x,hole.y+1,peer));
		}
		else
		{
            peer.y = map.get(peer.x).size();
			 map.get(peer.x).add(peer);

			/*//Find shortest list
	        ArrayList<Peer> index =map.get(0);
	        int x = 0;
	        for(int i = 0; i < map.size(); i ++)
	        {
	            ArrayList<Peer>list = map.get(i);
	            if(list.size() < index.size())
	            {
	                index = list;
	                x = i;
	            }
	        }
	        if(index.size() > MAX_PEER)
	        {
	        	index = new CountableArrayList<Peer>();
	        	peer.x = map.activeSize-1;
	        	peer.y = 0;
	        	map.add(index);
	        	index.add(peer);
	        	addBasePeer(index,peer.y);
	        }
	        else
	        {	        	
			    peer.y = index.size();
			    peer.x = x;
			    index.add(peer);
	        }  */
		}
		IDMap.put(peer.ID,peer);
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
                        sendKeyList(current);
                    }
            }

        }catch(Exception e)
        {
            e.printStackTrace();
        }
		return peer.x;
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
	   int maxTrickleUp = 0;
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
       holes.add(new Hole(hashed.y,hashed.x,map.get(hashed.y).get(hashed.x-1)));
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
	{

		try {
		    return(json
		    		.put("col", Integer.toString(peer.x))
		    		.put("src", encryption.getKeyAsString(publicKey))
            		.put("dest", encryption.getKeyAsString(peer.publicKey))
		    		.put("type", Integer.toString(type)));

		} catch (JSONException e) {
			
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
        if(hashed.name.equals("13"))
        {
            System.out.println(hashed.x);
            hashed.socket = map.get(hashed.x).get(0).socket;
            printMap();

        }
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        outPacket = addHeader(encryption.AESencryptJSON(outPacket,hashed.getAesKey()),2,hashed);
        forwardMessage(hashed.socket,outPacket.toString(),hashed.name);

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

	
}
