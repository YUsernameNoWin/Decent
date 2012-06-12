import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.UUID;


import org.JSON.JSONException;
import org.JSON.JSONObject;

import naga.ConnectionAcceptor;
import naga.NIOServerSocket;
import naga.NIOService;
import naga.NIOSocket;
import naga.SocketObserver;
import naga.SocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetreader.RawPacketReader;
import naga.packetwriter.RawPacketWriter;

/*Works as the main manager of the network. Manages peer map, peer connections, dead peers and new peers.
 * 
 */
public class Master extends Thread{
    Encryption encryption  =  new Encryption();

	public ArrayList<UUID> leftList = new ArrayList<UUID>();
	public ArrayList<UUID> downList = new ArrayList<UUID>();
	public ArrayList<UUID> rightList = new ArrayList<UUID>();
	public CountableArrayList <ArrayList<Peer>> map= new CountableArrayList<ArrayList<Peer>>();
	public Queue <Hole>holes = new LinkedList<Hole>();
	public Queue <Hole>openSlots = new LinkedList<Hole>();
	public HashMap<String,Peer> IDMap =  new HashMap<String,Peer>();
	public PublicKey publicKey= null;
	public PrivateKey privateKey = null;
	public NIOService service;
	public InetAddress leftAd,rightAd,downAd;
	public NIOServerSocket leftServer, rightServer, downServer;

	public int peerNum = 0;
	public final int MAX_PEER =10;
	String clearText;
	String AESKEY;
	char type;
	int col;
	public Master()
	{
	    
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
          //  map.get(3).add(new Peer());
        
        getKey();
        try {
           service = new NIOService();
           leftServer = service.openServerSocket(510);
           leftServer.listen(new ServerAdapter(this,510,map.get(0).get(0)));
           leftServer.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
           
           rightServer = service.openServerSocket(511);
           rightServer.listen(new ServerAdapter(this,511,map.get(1).get(0)));
           rightServer.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
           
           downServer = service.openServerSocket(512);
           downServer.listen(new ServerAdapter(this,512,map.get(2).get(0)));
           downServer.setConnectionAcceptor(ConnectionAcceptor.ALLOW);



                  // Handle IO until we quit the program.
                  while (true)
                  {
                        service.selectBlocking(1000);
                     //   System.out.println(holes);
                  }
        } catch (IOException e1) {
            
            e1.printStackTrace();
        }

	}
	
	/*public JSONObject<String,Peer> getPeers2(Peer peer){
        HashMap <String,Peer>temp = new HashMap<String,Peer>();
        //System.out.println("Server: " + peer.port);
        //printMap();
        if(peer.x == 0)
            {

                //check if first row
                if(peer.y==0)
                {
                    temp.put("right",map.get(0).get(peer.y+1));
                    temp.put("left",map.get(map.activeSize-1).get(peer.y));
                    temp.put("downRight",map.get(map.activeSize-1).get(peer.y+1));
                    temp.put("downLeft",map.get(peer.x+1).get(peer.y+1));
                    temp.put("down",map.get(1).get(peer.y));
                }
                else if(peer.y >= map.get(col).activeSize-1)
                {
                    temp.put("up",map.get(0).get(peer.y-1));
                    temp.put("upRight",map.get(map.activeSize-1).get(peer.y-1));
                    temp.put("upLeft",map.get(peer.x+1).get(peer.y-1));
                    temp.put("downRight",map.get(1).get(peer.y-1));
                    temp.put("right",map.get(1).get(peer.y));
                }
                //any other rows
                else
                {
                    temp.put("up",map.get(0).get(peer.y-1));
                    temp.put("down",map.get(0).get(peer.y+1));
                    temp.put("upRight",map.get(1).get(peer.y-1));
                    temp.put("right",map.get(1).get(peer.y));
                    temp.put("downRight",map.get(1).get(peer.y+1));
                    temp.put("left",map.get(map.activeSize-1).get(peer.y));
                    temp.put("upLeft",map.get(map.activeSize-1).get(peer.y-1));
                    temp.put("downLeft",map.get(map.activeSize-1).get(peer.y+1));
                }
            }
            else if(peer.x == map.activeSize-1)
            {
                if(peer.y==0)
                {
                    temp.put("downLeft",map.get(0).get(peer.y+1));
                    temp.put("left",map.get(map.activeSize-2).get(peer.y));
                    temp.put("right",map.get(0).get(peer.y));
                    temp.put("downRight",map.get(peer.x-1).get(peer.y+1));
                    temp.put("down",map.get(map.activeSize-1).get(peer.y+1));
                }
                else if(peer.y ==map.get(col).activeSize-1)
                {
                    temp.put("up",map.get(map.activeSize-1).get(peer.y-1));
                    temp.put("left",map.get(peer.x-1).get(peer.y));
                    temp.put("upLeft",map.get(0).get(peer.y-1));
                    temp.put("upRight",map.get(peer.x-1).get(peer.y-1));
                    temp.put("right",map.get(0).get(peer.y));
    
                }
                else
                {
                    temp.put("down",map.get(map.activeSize-1).get(peer.y+1));
                    temp.put("up",map.get(map.activeSize-1).get(peer.y-1));
                    temp.put("left",map.get(peer.x-1).get(peer.y));
                    temp.put("downLeft",map.get(0).get(peer.y+1));
                    temp.put("upLeft",map.get(peer.x-1).get(peer.y-1));
                    temp.put("right",map.get(0).get(peer.y));
                    temp.put("downRight",map.get(peer.x-1).get(peer.y+1));
                    temp.put("upRight",map.get(0).get(peer.y-1));
    
                }
                
            }
            else
            {   
                if(peer.y==0)
                {
                    temp.put("downLeft",map.get(peer.x-1).get(peer.y+1));
                    temp.put("left",map.get(peer.x-1).get(peer.y));
                    temp.put("down",map.get(peer.x).get(peer.y+1));
                    temp.put("downRight",map.get(peer.x+1).get(peer.y+1));
                    temp.put("right",map.get(peer.x+1).get(peer.y));
                }

                else if(peer.y == map.get(col).activeSize-1)
                {
                    temp.put("upLeft",map.get(peer.x+1).get(peer.y-1));
                    temp.put("left",map.get(peer.x-1).get(peer.y));
                    temp.put("up",map.get(peer.x).get(peer.y-1));
                    temp.put("upRight",map.get(peer.x-1).get(peer.y-1));
                    temp.put("right",map.get(peer.x+1).get(peer.y));
                }
                else
                {
                    temp.put("upLeft",map.get(peer.x-1).get(peer.y-1));
                    temp.put("left",map.get(peer.x-1).get(peer.y));
                    temp.put("downLeft",map.get(peer.x-1).get(peer.y+1));
                    temp.put("down",map.get(peer.x).get(peer.y+1));
                    temp.put("up",map.get(peer.x).get(peer.y-1));
                    temp.put("upRight",map.get(peer.x+1).get(peer.y-1));
                    temp.put("right",map.get(peer.x+1).get(peer.y));
                    temp.put("downRight",map.get(peer.x+1).get(peer.y+1));
                }
            }
        Iterator<String> iterate = temp.keySet().iterator();
        JSONObject<String,Peer> output = new JSONObject<String,Peer>();
        while(iterate.hasNext())
        {
            String  current = iterate.next();
            Peer value = temp.get(current);
            if(value != null && value.active)
            {
                try {
                    output.put(current, value);
                } catch (JSONException e) {
                    
                    e.printStackTrace();
                }
            }
        }
        return output;      
    }*/
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
	            System.out.print("X ");
	        }
	        System.out.println();
	    }
	    //System.out.println(peerNum);
	    
	}
	/*	@Deprecated
	public JSONObject getPeerKeyList2(Peer peer){
			HashMap <String,Peer>temp = new HashMap<String,Peer>();
			System.out.println("no fucking way");
			//printMap();
			if(peer.x == 0)
				{
	
					//check if first row
					if(peer.y==0)
					{
						temp.put("right",map.get(0).get(peer.y+1));
						temp.put("left",map.get(map.activeSize-1).get(peer.y));
						temp.put("downLeft",map.get(map.activeSize-1).get(peer.y+1));
						temp.put("downRight",map.get(1).get(peer.y+1));
						temp.put("down",map.get(1).get(peer.y));
					}
					else if(peer.y >= map.get(col).activeSize-1)
	                {
	                    temp.put("up",map.get(0).get(peer.y-1));
	                    temp.put("right",map.get(map.activeSize-1).get(peer.y));
	                    temp.put("upLeft",map.get(map.activeSize-1).get(peer.y-1));
	                    temp.put("downRight",map.get(1).get(peer.y-1));
	                    temp.put("right",map.get(1).get(peer.y));
	                }
					//any other rows
					else
					{
						temp.put("up",map.get(0).get(peer.y-1));
						temp.put("down",map.get(0).get(peer.y+1));
						temp.put("upRight",map.get(1).get(peer.y-1));
						temp.put("right",map.get(1).get(peer.y));
						temp.put("downRight",map.get(1).get(peer.y+1));
						temp.put("left",map.get(map.activeSize-1).get(peer.y));
						temp.put("upLeft",map.get(map.activeSize-1).get(peer.y-1));
						temp.put("downLeft",map.get(map.activeSize-1).get(peer.y+1));
					}
				}
				else if(peer.x == map.activeSize-1)
				{
				    if(peer.y==0)
	                {
	                    temp.put("downLeft",map.get(map.activeSize-2).get(peer.y+1));
	                    temp.put("left",map.get(map.activeSize-2).get(peer.y));
	                    temp.put("right",map.get(0).get(peer.y));
	                    temp.put("downRight",map.get(0).get(peer.y+1));
	                    temp.put("down",map.get(map.activeSize-1).get(peer.y+1));
	                }
				    else if(peer.y ==map.get(col).activeSize-1)
					{
						temp.put("up",map.get(map.activeSize-1).get(peer.y-1));
						temp.put("left",map.get(peer.x-1).get(peer.y));
						temp.put("upLeft",map.get(peer.x-1).get(peer.y-1));
						temp.put("upRight",map.get(0).get(peer.y-1));
						temp.put("right",map.get(0).get(peer.y));
		
					}
					else
					{
						temp.put("down",map.get(map.activeSize-1).get(peer.y+1));
						temp.put("up",map.get(map.activeSize-1).get(peer.y-1));
						temp.put("left",map.get(peer.x-1).get(peer.y));
						temp.put("downLeft",map.get(peer.x-1).get(peer.y+1));
						temp.put("upLeft",map.get(peer.x-1).get(peer.y-1));
						temp.put("right",map.get(0).get(peer.y));
						temp.put("downRight",map.get(0).get(peer.y+1));
						temp.put("upRight",map.get(0).get(peer.y-1));
		
					}
					
				}
				else
				{	
				    if(peer.y==0)
	                {
	                    temp.put("downLeft",map.get(peer.x-1).get(peer.y+1));
	                    temp.put("left",map.get(peer.x-1).get(peer.y));
	                    temp.put("down",map.get(peer.x).get(peer.y+1));
	                    temp.put("downRight",map.get(peer.x+1).get(peer.y+1));
	                    temp.put("right",map.get(peer.x+1).get(peer.y));
	                }
	
					else if(peer.y == map.get(col).activeSize-1)
	                {
	                    temp.put("upLeft",map.get(peer.x-1).get(peer.y-1));
	                    temp.put("left",map.get(peer.x-1).get(peer.y));
	                    temp.put("up",map.get(peer.x).get(peer.y-1));
	                    temp.put("upRight",map.get(peer.x+1).get(peer.y-1));
	                    temp.put("right",map.get(peer.x+1).get(peer.y));
	                }
					else
					{
						temp.put("upLeft",map.get(peer.x-1).get(peer.y-1));
						temp.put("left",map.get(peer.x-1).get(peer.y));
						temp.put("downLeft",map.get(peer.x-1).get(peer.y+1));
						temp.put("down",map.get(peer.x).get(peer.y+1));
						temp.put("up",map.get(peer.x).get(peer.y-1));
						temp.put("upRight",map.get(peer.x+1).get(peer.y-1));
						temp.put("right",map.get(peer.x+1).get(peer.y));
						temp.put("downRight",map.get(peer.x+1).get(peer.y+1));
					}
				}
			Iterator<String> iterate = temp.keySet().iterator();
			JSONObject output = new JSONObject();
			try {
	            output.put("cont","keylist");
	        } catch (JSONException e1) {
	            
	            e1.printStackTrace();
	        }
			while(iterate.hasNext())
			{
		         String  current = iterate.next();
			    Peer value = temp.get(current);
			    if(value != null && value.active && value.publicKey != null)
			    {
	                try {
	                    output.put(current, encryption.getKeyAsString(value.publicKey));
	                } catch (JSONException e) {
	                    
	                    e.printStackTrace();
	                }
			    }
			}
			return output;		
		}*/
	    public JSONObject<String,Peer> getPeers(Peer peer){	
	        JSONObject<String,Peer> output = new JSONObject<String,Peer>();
	        try {
	            output.put("right", map.get(peer.x-1).get(peer.y));
	            output.put("left", map.get(peer.x+1).get(peer.y));
	
	            if(peer.y -1 > 0)
	            {
	                output.put("up", map.get(peer.x).get(peer.y-1));
	                output.put("upRight", map.get(peer.x-1).get(peer.y - 1));
	                output.put("upLeft", map.get(peer.x+1).get(peer.y - 1));
	            }
	            if(peer.y + 1 < map.get(peer.x).size()-1)
	            {
	            	try{
	                   output.put("downLeft", map.get(peer.x + 1).get(peer.y + 1));
	            	}
	            	catch(Exception e)
	            	{
	            		
	            		System.out.println("BAD PEER::::::: " + peer.ID);
	            	}
	                   
	                   output.put("downRight", map.get(peer.x - 1).get(peer.y + 1));
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
			
			//Find shortest list
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
	        }
		}
		IDMap.put(peer.ID,peer);
        peerNum++;
	     //printMap();
		return peer.x;
	}
	public void addBasePeer(List<Peer> list,int index)
	{
	    Peer base = list.get(0);
	    try {
	        base.serverSock = service.openServerSocket(510+index);
	        base.serverSock.listen(new ServerAdapter(this,index,base));
	        base.serverSock.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
	    } catch (IOException e) {
	        
	        e.printStackTrace();
	    }
	
	}





	public void removePeer(Peer hashed) {

       holes.add(new Hole(hashed.y,hashed.x,map.get(hashed.y).get(hashed.x-1)));
       map.get(hashed.x).remove(hashed.y);
       IDMap.remove(hashed.ID);
      peerNum--;
      //printMap();
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
	public JSONObject addHeader(JSONObject json,int type,Peer peer )
	{

		try {
		    return(json.put("col", Integer.toString(peer.x)).put("id", peer.ID).put("type", Integer.toString(type)));

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
				  out.write("PrivKeyion");
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
