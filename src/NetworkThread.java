import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
	public byte[] upSymKey = encryption.generateSymmetricKey().getEncoded();
	public NetworkThread(int port) throws Exception{
	    this.port = port;
		
	}
	public void run()
	{
	    

	    try {
            sleep(port);
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
            getKey();
            up = new Peer("127.0.0.1", port);
            //upLeft = new Peer("127.0.0.1", port);
            //upRight = new Peer("127.0.0.1", port);
            //right = new Peer("127.0.0.1", port);
           // left = new Peer("127.0.0.1", port);
            //downRight= new Peer("127.0.0.1", port);
            //downLeft = new Peer("127.0.0.1", port);
           // 
            up.socket = service.openSocket(up.address,port);
            up.socket.setPacketReader(new AsciiLinePacketReader());
            up.socket.setPacketWriter(new RawPacketWriter());
            up.socket.listen(new NetworkProtocol(this,up));
            
            down = new Peer("127.0.0.1", port + 10);
            down.serverSock = service.openServerSocket(down.port);
            down.serverSock.listen(new PeerServerAdapter(this,down));
            down.serverSock.setConnectionAcceptor(ConnectionAcceptor.ALLOW);
            while(true){
                service.selectBlocking(100);
            }

                 
            

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	public String parse(String input, Peer sender)throws Exception
	{
		//1 == send up. 2 == broadcast. 3 == forward down. 4 == send down one. 5 == Reroute.
		char type = input.charAt(0);
		//test send up to master, add UUID to senders list in order to send back response
		//get
		UUID ID =  UUID.fromString(input.substring(1,37));
		int col = Character.getNumericValue(input.charAt(38));
		
		if(type == '1')
		{

			if(sender.equals(left))
				leftList.add(ID);
			else if(sender.equals(right))
				rightList.add(ID);
			else if(sender.equals(down))
				downList.add(ID);
			else
				return "";
			sendUp(input);
		}
		//broadcast message to entire network
		else if (type == '2'){
			if(sender.equals(up)||sender.equals(upLeft)||sender.equals(upRight))
			sendDown(input);
		}
		//forward response downwards or if it is meant for us, decrypt it.
		else if( type == '3'){
			if(downList.contains(ID))
				forwardMessage(down,input);
			else if(rightList.contains(ID))
			{
				forwardMessage(right,input);
			}
			else if(leftList.contains(ID))
			{
				forwardMessage(left,input);
			}
			else{
			//its for us
				try{
				input =new String(encryption.decryptAES(topSymKey,input.substring(39).getBytes()));
				}catch(IOException a){
					try{
						input = new String(encryption.decryptRSA(privateKey, input.substring(38,210)));
						if(input.contains("hereKey"))
							sender.publicKey = encryption.getPublicKeyFromString(input.substring(input.indexOf("hereKey")+7));
						
					}
					catch(IOException b){
						b.printStackTrace();
					}
				}
				
				/* Stuff i could get.
				 * 
				 * Request for pub key with AES key:send AESED(pubkey)
				 * NewConn:Check AES ==AES
				 * Exchange of AES key
				 * 
				 */
				
			}
		}
		else if(type == '4'){
			if(sender.equals(down)){
				forwardMessage(upRight,input);
				forwardMessage(upLeft,input);
			}
			else if(sender.equals(up))
			{	

						sendUp(new String("1"+UUID.randomUUID().toString()+encryption.encryptAES(topSymKey,"rightdead".getBytes())));
						sendUp(new String("1"+UUID.randomUUID().toString()+encryption.encryptAES(topSymKey,"leftdead".getBytes())));
				
			}
			else if(sender.equals(left)||sender.equals(right))
			{
			input = "3"+input.substring(1);
			forwardMessage(down,input);
			}
		}
		else if(type == '5')
		{
			if(sender.equals(up)&&!forwardMessage(upLeft,input))
			{
				if(!forwardMessage(upRight,input))
					forwardMessage(down,input);
			}
			else if(sender.equals(upLeft)&&!forwardMessage(up,input))
			{
				if(!forwardMessage(upRight,input))
					forwardMessage(down,input);
			}
			else if(sender.equals(upRight)&&!forwardMessage(up,input))
			{
				if(!forwardMessage(upLeft,input))
					forwardMessage(down,input);
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

	
	public JSONObject encryptJSON(JSONObject decrypted,Peer peer){
		JSONObject encrypted = new JSONObject();
		for(int a=0;a<decrypted.names().length();a++){
			try {
				encrypted.put(new String(encryption.decryptAES(peer.aesKey, decrypted.names().get(a).toString().getBytes())),
						decrypted.getString(decrypted.names().get(a).toString()));
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
        up.socket.closeAfterWrite();
        down.serverSock.close();
        try {
            new NetworkThread(port).start();
            stop();
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
			key = "";
			while(scan.hasNext()){
				key+=scan.next();
			}
			privateKey = encryption.getPrivateKeyFromString(key);
		}catch (Exception e) {
			e.printStackTrace();
		}
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
	    }catch(NullPointerException e) {
	        return false;
	    }
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
