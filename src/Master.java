import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
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
import naga.packetreader.RawPacketReader;
import naga.packetwriter.RawPacketWriter;

//TODO FIX UUID ADDING AND ADJUST STRING LENGTHS.
public class Master extends Thread{
    Encryption encryption  =  new Encryption();
	class Hole{
		Hole(int x, int y){
			this.x = x;
			this.y  =y;
		}
		int x,y;
	}
	public ArrayList<UUID> leftList = new ArrayList<UUID>();
	public ArrayList<UUID> downList = new ArrayList<UUID>();
	public ArrayList<UUID> rightList = new ArrayList<UUID>();
	public List <CountableArrayList<Peer>> map= new ArrayList<CountableArrayList<Peer>>();
	public Queue <Hole>holes = new LinkedList<Hole>();
	public HashMap<String,Peer> IDMap =  new HashMap<String,Peer>();
	public PublicKey publicKey= null;
	public PrivateKey privateKey = null;
	public NIOService service;
	public InetAddress leftAd,rightAd,downAd;
	public NIOServerSocket left,down,right;
	public int peerNum = 0;
	public final int MAX_PEER =10;
	String clearText;
	String AESKEY;
	char type;
	int col;
	public Master()
	{
	    
	}

	public byte[] formatPacket(String input,int type,int col){
		input = type + col + UUID.randomUUID().toString()+"C"+input;
		return input.getBytes();
	}
	
	

	public void run()
	{
	    map.add(new CountableArrayList<Peer>(MAX_PEER));
        map.add(new CountableArrayList<Peer>(MAX_PEER));
        map.add(new CountableArrayList<Peer>(MAX_PEER));
        //map.add(new CountableArrayList<Peer>(MAX_PEER));
        for(int i = 0; i< MAX_PEER;i++){
            map.get(0).add(new Peer());
           map.get(1).add(new Peer());
           map.get(2).add(new Peer());
          //  map.get(3).add(new Peer());
        }
        getKey();
        try {
            service = new NIOService();
            addBasePeer(map.get(0),0);
            addBasePeer(map.get(1),1);
            addBasePeer(map.get(2),2);
            //addBasePeer(map.get(3),0);


                  // Handle IO until we quit the program.
                  while (true)
                  {
                  /*  for(int i = 0;i<map.size();i++)
                    {
                        for(int a = 0;a<map.size();a++)
                        {
                        System.out.print(map.get(i).get(a).port);
                        }
                        System.out.println();
                    }*/
    
                        service.selectBlocking();
            
                  }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

	}
	
	public void addBasePeer(List<Peer> list,int index)
	{
	    Peer base = list.get(0);
	    try {
            base.serverSock = service.openServerSocket(510+index);
            base.serverSock.listen(new ServerAdapter(this,index));
            base.serverSock.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	   
	}
	public JSONObject getPeers(Peer peer){
		HashMap <String,Peer>temp = new HashMap<String,Peer>();
		//System.out.println("Server: " + peer.port);
		//printMap();
		if(peer.col == 0)
			{

				//check if first row
				if(peer.row==0)
				{
					temp.put("right",map.get(0).get(peer.row+1));
					temp.put("left",map.get(map.size()-1).get(peer.row));
					temp.put("downLeft",map.get(map.size()-1).get(peer.row+1));
					temp.put("downRight",map.get(1).get(peer.row+1));
					temp.put("down",map.get(1).get(peer.row));
				}
				else if(peer.row >= map.get(col).activeSize()-1)
                {
                    temp.put("up",map.get(0).get(peer.row-1));
                    temp.put("right",map.get(map.size()-1).get(peer.row));
                    temp.put("upLeft",map.get(map.size()-1).get(peer.row-1));
                    temp.put("downRight",map.get(1).get(peer.row-1));
                    temp.put("right",map.get(1).get(peer.row));
                }
				//any other rows
				else
				{
					temp.put("up",map.get(0).get(peer.row-1));
					temp.put("down",map.get(0).get(peer.row+1));
					temp.put("upRight",map.get(1).get(peer.row-1));
					temp.put("right",map.get(1).get(peer.row));
					temp.put("downRight",map.get(1).get(peer.row+1));
					temp.put("left",map.get(map.size()-1).get(peer.row));
					temp.put("upLeft",map.get(map.size()-1).get(peer.row-1));
					temp.put("downLeft",map.get(map.size()-1).get(peer.row+1));
				}
			}
			else if(peer.col == map.size()-1)
			{
			    if(peer.row==0)
                {
                    temp.put("downLeft",map.get(map.size()-2).get(peer.row+1));
                    temp.put("left",map.get(map.size()-2).get(peer.row));
                    temp.put("right",map.get(0).get(peer.row));
                    temp.put("downRight",map.get(0).get(peer.row+1));
                    temp.put("down",map.get(map.size()-1).get(peer.row+1));
                }
			    else if(peer.row ==map.get(col).activeSize()-1)
				{
					temp.put("up",map.get(map.size()-1).get(peer.row-1));
					temp.put("left",map.get(peer.col-1).get(peer.row));
					temp.put("upLeft",map.get(peer.col-1).get(peer.row-1));
					temp.put("upRight",map.get(0).get(peer.row-1));
					temp.put("right",map.get(0).get(peer.row));
	
				}
				else
				{
					temp.put("down",map.get(map.size()-1).get(peer.row+1));
					temp.put("up",map.get(map.size()-1).get(peer.row-1));
					temp.put("left",map.get(peer.col-1).get(peer.row));
					temp.put("downLeft",map.get(peer.col-1).get(peer.row+1));
					temp.put("upLeft",map.get(peer.col-1).get(peer.row-1));
					temp.put("right",map.get(0).get(peer.row));
					temp.put("downRight",map.get(0).get(peer.row+1));
					temp.put("upRight",map.get(0).get(peer.row-1));
	
				}
				
			}
			else
			{	
			    if(peer.row==0)
                {
                    temp.put("downLeft",map.get(peer.col-1).get(peer.row+1));
                    temp.put("left",map.get(peer.col-1).get(peer.row));
                    temp.put("down",map.get(peer.col).get(peer.row+1));
                    temp.put("downRight",map.get(peer.col+1).get(peer.row+1));
                    temp.put("right",map.get(peer.col+1).get(peer.row));
                }

				else if(peer.row == map.get(col).activeSize()-1)
                {
                    temp.put("upLeft",map.get(peer.col-1).get(peer.row-1));
                    temp.put("left",map.get(peer.col-1).get(peer.row));
                    temp.put("up",map.get(peer.col).get(peer.row-1));
                    temp.put("upRight",map.get(peer.col+1).get(peer.row-1));
                    temp.put("right",map.get(peer.col+1).get(peer.row));
                }
				else
				{
					temp.put("upLeft",map.get(peer.col-1).get(peer.row-1));
					temp.put("left",map.get(peer.col-1).get(peer.row));
					temp.put("downLeft",map.get(peer.col-1).get(peer.row+1));
					temp.put("down",map.get(peer.col).get(peer.row+1));
					temp.put("up",map.get(peer.col).get(peer.row-1));
					temp.put("upRight",map.get(peer.col+1).get(peer.row-1));
					temp.put("right",map.get(peer.col+1).get(peer.row));
					temp.put("downRight",map.get(peer.col+1).get(peer.row+1));
				}
			}
		Iterator<String> iterate = temp.keySet().iterator();
		JSONObject output = new JSONObject();
		try {
            output.put("cont","keylist");
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
		    }
		}
		return output;		
	}
	
	
	public String get(String a){
		NIOSocket temp;
		try {
			String end = "";
			temp = service.openSocket("google.com", 80);
			temp.setPacketReader(new RawPacketReader());
			temp.setPacketWriter(new RawPacketWriter());
			
			temp.listen(new SocketObserver(){

				@Override
				public void connectionOpened(NIOSocket nioSocket) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void connectionBroken(NIOSocket nioSocket,
						Exception exception) {
					// TODO Auto-generated method stub
					System.exit(0);
				}

				@Override
				public void packetReceived(NIOSocket socket, byte[] packet) {
					// TODO Auto-generated method stub
				}
				
			});

			temp.write("GET /index.html".getBytes());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return a;
		
	}
	public void printMap() 
	{
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
	    for(int i = 0;i<map.size();i++)
	    {
	        for(int x =0;x<map.get(i).activeSize();x++) {
	            System.out.print("x ");
	        }
	        System.out.println();
	    }
	    System.out.println(peerNum);
	}
	public int addPeer(Peer peer) 
	{

		if(!holes.isEmpty())
		{
			Hole hole = holes.remove();
			map.get(hole.x).set(hole.y, peer);
		}
		else
		{
			List<Peer> list =findShortest();
			peer.col = map.indexOf(list);
			peer.row = ((CountableArrayList<Peer>) list).activeSize();
			peer.active = true;
			System.out.println(peer.col + " " + (map.size()-1));
			if(peer.col == (map.size()-1))
			    addBasePeer(list,peer.col);
			if(list.get(peer.row).serverSock != null) {
			    peer.serverSock = list.get(peer.row).serverSock;
			}
			list.set(peer.row,peer);
			peerNum++;
		}
		IDMap.put(peer.ID,peer);
	     printMap();
		return peer.col;
	}
	public List<Peer> findShortest()
	{
		int min = MAX_PEER;

		List<Peer> index =map.get(0);
		for(int i = 0; i < map.size(); i ++)
		{
			CountableArrayList<Peer>list = map.get(i);
			
			if(list.activeSize()<min)
			{
				min = list.activeSize();
				index = map.get(i);
			}

				
		}
		
		if(((CountableArrayList<Peer>) index).activeSize() == MAX_PEER)
		{
			map.add(new CountableArrayList<Peer>(MAX_PEER));
			index = map.get(map.size()-1);
	        for(int i = 0; i< MAX_PEER;i++){
	          index.add(new Peer());
	        }
			
		}
		return index;
	}
	public JSONObject addHeader(JSONObject json,int type,Peer peer )
	{

		try {
		    return(json.put("col", Integer.toString(peer.col)).put("id", peer.ID).put("type", Integer.toString(type)));

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return json;
	}

	public JSONObject AESencryptJSON(JSONObject decrypted,Peer peer)
	{
		JSONObject encrypted = new JSONObject();
		if(decrypted.names() == null)
		    return decrypted;
		for(int a=0;a<decrypted.names().length();a++){
			try {
			    encrypted.put(new String(encryption.encryptAES(peer.aesKey, decrypted.names().getString(a).getBytes())),
				        new String(encryption.encryptAES(peer.aesKey, decrypted.getString(decrypted.names().getString(a)).getBytes())));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return encrypted;
	}
	public void saveKey()
	{
		  try
		  {
			  // Create file 
				  FileWriter fstream = new FileWriter("out.txt");
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
	public boolean forwardMessage(String content, Socket dest){
		try {
			PrintWriter out = new PrintWriter(dest.getOutputStream(),true);
			out.write(content);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
			
		}
		return true;
	}


    public void removePeer(Peer hashed) {
       map.get(hashed.col).set(hashed.row, new Peer());
        IDMap.remove(hashed.ID);
      peerNum--;
        
        
    }

	
}
