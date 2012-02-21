import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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

//TODO REWRITE PARSE for 4 and 3
public class NetworkThread extends Thread{
    Encryption encryption  =  new Encryption();
	Peer down,left,right,up,upLeft,upRight,master,downLeft,downRight;

	
	
	public PrivateKey privateKey;
	public PublicKey publicKey;
	public PublicKey upPubKey;
	public ArrayList<UUID> leftList = new ArrayList<UUID>();
	public ArrayList<UUID> downList = new ArrayList<UUID>();
	public ArrayList<UUID> rightList = new ArrayList<UUID>();
	protected PrintWriter response;
	protected BufferedReader send;
	public NIOService service;
	public int column;
	public UUID ID = UUID.randomUUID();
	public NIOServerSocket leftServer,downServer,rightServer;
	ServerSocket serverSocket = null;
    int port =0;
	public byte[] topSymKey = encryption.generateSymmetricKey().getEncoded();
	public NetworkThread(int port) throws Exception{
	    this.port = port;
		
	}
	public void run()
	{
	    
	    try {
            sleep(port*2);
        } catch (InterruptedException e1) {

            e1.printStackTrace();
        }
	    try {
            try {
                service = new NIOService();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            up = new Peer("127.0.0.1", port);
            upLeft = new Peer("127.0.0.1", port);
            upRight = new Peer("127.0.0.1", port);
            right = new Peer("127.0.0.1", port);
            left = new Peer("127.0.0.1", port);
            downRight= new Peer("127.0.0.1", port);
            downLeft = new Peer("127.0.0.1", port);
            getKey();
           
            up.socket = service.openSocket(up.address,port);
            up.socket.setPacketReader(new AsciiLinePacketReader());
            up.socket.setPacketWriter(new RawPacketWriter());
            up.socket.listen(new NetworkProtocol(this,up));
            up.aesKey  = encryption.generateSymmetricKey().getEncoded();
            keyExchange(up);
            
            downLeft.aesKey = encryption.generateSymmetricKey().getEncoded();
            downRight.aesKey = encryption.generateSymmetricKey().getEncoded();
             
            down = new Peer("127.0.0.1", port + 10);
            down.serverSock = service.openServerSocket(down.port);
            down.serverSock.listen(new PeerServerAdapter(this,down));
            down.serverSock.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
            while(true){
                service.selectBlocking();
            }

                 
            

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	 private void keyExchange(Peer peer) throws JSONException {
	     JSONObject message = new JSONObject();
	     message.put("aeskey", new String(Base64.encodeBytes(peer.aesKey)));

	     message = encryption.RSAencryptJSON(message, peer.publicKey);
	       message = addHeader(message, 1);
	     forwardMessage(peer,message.toString());
        
        message = new JSONObject();
        message.put("publickey", encryption.getKeyAsString(publicKey));
        message = encryption.AESencryptJSON(message, peer.aesKey);
        message = addHeader(message, 1);
       forwardMessage(peer,message.toString());
        
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

	            sendUp(input);
	        }

	        //forward response downwards or if it is meant for us, decrypt it.
	        else if( type == 2)
	        {
	            String content;

	                JSONObject clearPacket = encryptedPacket;
	                clearPacket =  encryption.AESdecryptJSON(encryptedPacket,topSymKey);
	                if(clearPacket.has("cont")) {
	                       content  = clearPacket.getString("cont");
	                }

	                    if(clearPacket.has("repair"))
	                    {
	                        JSONObject  out = new JSONObject();
	                        out.put("dc", "disconnected");
	                        out = addHeader(encryption.AESencryptJSON(out,topSymKey),1);
	                        forwardMessage(up, out.toString());
	                        repair(clearPacket.getInt("port"));
	                    }
	                    if(clearPacket.has("connect") && clearPacket.has("ip") && clearPacket.has("port"))
	                        {
	                        System.out.println("hello");
	                            if(clearPacket.getString("connect").equals("downleft") && !downLeft.active)
	                            {
	                                downLeft.address = clearPacket.getString("ip");
                                    downLeft.port = clearPacket.getInt("port");
	                                downLeft.socket = service.openSocket(downLeft.address,downLeft.port);
	                                downLeft.socket.setPacketReader(new AsciiLinePacketReader());
	                                downLeft.socket.setPacketWriter(new RawPacketWriter());
	                                downLeft.socket.listen(new NetworkProtocol(this,downLeft));
	                                downLeft.active = true;
	                            }
	                            else if(clearPacket.getString("connect").equals("downright") && !downRight.active)
	                            {
	                                downRight.address = clearPacket.getString("ip");
	                                downRight.port = clearPacket.getInt("port");
	                                downRight.socket = service.openSocket(downRight.address,downRight.port);
	                                downRight.socket.setPacketReader(new AsciiLinePacketReader());
	                                downRight.socket.setPacketWriter(new RawPacketWriter());
	                                downRight.socket.listen(new NetworkProtocol(this,downRight));
	                                downRight.active = true;
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
	                        
	                        JSONObject out = new JSONObject();
	                        
	                        if(left.publicKey != null)
	                        {
	                               out.put("leftip",encryption.encryptRSA(left.publicKey,InetAddress.getLocalHost().toString()))
	                               .put("leftport",encryption.encryptRSA(left.publicKey,Integer.toString(port+10).getBytes()));
	                        }
	                        if(right.publicKey != null)
	                        {
	                            out.put("rightip",encryption.encryptRSA(right.publicKey,InetAddress.getLocalHost().toString()))
	                                .put("rightport",encryption.encryptRSA(right.publicKey,Integer.toString(right.port).getBytes()));
	                            right.serverSock = service.openServerSocket(port+10);
	                        }
	                        out = encryption.AESencryptJSON(out,topSymKey);
	                        out = addHeader(out, 1);
	                        
	                        forwardMessage(up, out.toString());
	                    }

	                }
	                    
	           
	                
	            
	            
	                
	        
	        

	            

	            
	        return input;
	            
	    }
	public boolean sendUp(String input){
		if(!forwardMessage(up,input))
			if(!forwardMessage(upLeft,input))
				if(!forwardMessage(upRight,input))			
					return false;
		return true;

	}
	public void sendDown(String input){
		forwardMessage(down,input);
		forwardMessage(left,input);
		forwardMessage(right,input);

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
            // TODO Auto-generated catch block
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
			upPubKey = publicKey;
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
            return(json.put("col", Integer.toString(column)).put("id",ID).put("type", Integer.toString(type)));

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return json;
    }
	public void networkLoop(){
		while(true)
			try {
				service.selectBlocking();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}


	public void get(String input){

		
			
	}

	public boolean forwardMessage(Peer dest,String content){
	    try {
	        
			return dest.socket.write((content+"\n").getBytes());

	    } catch (NullPointerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	    return false;
	}
	public boolean getDown(){
		try {
			Socket dest = new Socket(down.address,6001);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Peer: No down");
			
		}
		try {
			Socket dest = new Socket(left.address,6001);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Peer: No Left");
		}
		try {
			Socket dest = new Socket(right.address,6001);
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Peer: No Right");
		}
		return false;
	}
	
}
