import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.Queue;

import javax.crypto.spec.SecretKeySpec;

import naga.*;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RegularPacketWriter;
    /*
     * Peer datastructure
     */
	public class Peer{
		public PublicKey publicKey;
		public String address;
		public byte[] aesKey;
		public NIOSocket socket;
		public NIOServerSocket serverSock;
		public int port;
		public int x;
		public String name;
		public int y;
		public Queue<String> data = new LinkedList<String>();
		public String ID;
		public boolean active;
		public int connectionBrokenCount = 0;
		public Peer()
		{
			
		}
		public Peer(int col, int row){
			this.x = col;
			this.y = row;
			
		}
		public Peer(byte[] aesKey,String ID){
			this.aesKey = aesKey;
			this.ID = ID;
		}
	      public Peer(byte[] aesKey,PublicKey key){
	            this.aesKey = aesKey;
	            this.publicKey = key;
	     }
	       public Peer(byte[] aesKey){
               this.aesKey = aesKey;
        }
		public Peer(byte[] aesKey,String ID,String col,String row){
			this.aesKey = aesKey;
			this.ID = ID;
			this.y = Integer.parseInt(row);
			this.x = Integer.parseInt(col);
		}
		public Peer(PublicKey a){
				publicKey= a;

			
		}
		public Peer(String ip, int port2, String name) {
			address = ip;
			port = port2;
			this.name = name;
		}
	      public Peer(String ID) {
	          this.ID = ID;
	        }
		public String toString()
		{
		    return "Peer: " + ID;
		}
			
		
		
	}