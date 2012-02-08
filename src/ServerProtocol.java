import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Scanner;

import javax.crypto.spec.SecretKeySpec;

//TODO add repair method. Make a live map of network. Parse needs more work. Change to nonblocking sockets+channels?
//Actually make webserver part.
public class ServerProtocol {
	private SecretKeySpec[][] map;
	protected Socket conn;
	private PublicKey parent,leftParent,rightParent,child,rightChild,leftChild, publicKey;
	private PrivateKey privateKey;
	private SecretKeySpec symKey;
	protected PrintWriter out;
	protected BufferedReader in;
	protected Master server;
	public String content;
	public Peer sender;
	public ServerProtocol(Socket sock){
		
	}
	public ServerProtocol(Socket sock, Peer init, Master master)
			throws Exception {
		String outContent = "";
		out = new PrintWriter(sock.getOutputStream(), true);
        in = new BufferedReader(
                    new InputStreamReader(
                    sock.getInputStream()));
        privateKey=master.privateKey;
	       symKey = Encryption.getAESFromString(Encryption.decryptRSA(privateKey, content.getBytes()));
	       outContent = new String(Encryption.encryptAES(symKey.getEncoded(), "keypls".getBytes()));
	       out.println(outContent);
	       content = waitFor(in);
	       init.publicKey =  Encryption.getPublicKeyFromString(new String(Encryption.decryptAES(symKey.getEncoded(),content.getBytes())));
	       out.close();
	       in.close();
        

	}
	public String waitFor(BufferedReader in) throws IOException{
		String temp;
		String content = "";
	       while((temp = in.readLine()) != null){
	    	   content+=temp;
	    	   
	       }
	       return content;
	}
	public String parse(String input){
		System.out.println(input);
		//1 == send up. 2 == broadcast. 3 == forward. 
		if(input.charAt(0) == '1'){
			try {
				System.out.println(Encryption.decryptRSA(server.privateKey,input.substring(input.indexOf("content:"+8),input.indexOf("UUID"))));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		else 
			input ="";
		return input;
	}

}
