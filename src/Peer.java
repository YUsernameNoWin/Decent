import naga.NIOServerSocket;
import naga.NIOSocket;
import org.JSON.JSONObject;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.Queue;

/*
* Peer datastructure
*/
	public class Peer{
		public PublicKey publicKey;
		public String address;
		private byte[] aesKey;
		public NIOSocket socket;
		public NIOServerSocket serverSock;
		public int port;
		public int x;
		public String name;
		public int y;
        public JSONObject<String, Peer> peers;
		public Queue<String> data = new LinkedList<String>();
		public boolean isPeerActive;
		private boolean active;
		public KeyPair peerKeys;
        public String ID;

		public String secretID;
		public int connectionBrokenCount = 0;
		public int trickleUpCount = 0;
		public Peer()
		{
			
		}
		public Peer(int col, int row){
			this.x = col;
			this.y = row;
			
		}
		public Peer(byte[] aesKey,String ID){
			this.setAesKey(aesKey);
			this.ID = ID;
		}
	      public Peer(byte[] aesKey,PublicKey key){
	            this.setAesKey(aesKey);
	            this.publicKey = key;
	     }
	       public Peer(byte[] aesKey){
               this.setAesKey(aesKey);
        }
		public boolean isActive() {
			
	        return active;
	    }
        public boolean isReady() {

            if(aesKey != null && publicKey != null)
                return true;
            return false;
        }
	    public void setActive(boolean active) {
	            this.active = active;
	        }	       
		public void setAesKey(byte[] aesKey2) {
		    this.aesKey = aesKey2;
            
        }
		public byte[] getAesKey()
		{
		    return aesKey;
		}
        public Peer(byte[] aesKey,String ID,String col,String row){
			this.setAesKey(aesKey);
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
		    return "X: " + x + " Y: " + y;
		}
		/**
		 * 
		 * @return AES key in decoded mode.
		 */
        public byte[] getAesKeyInBase64() {

                    return Base64.encodeBytesToBytes(aesKey);
            
        }
        /**
         * 
         * @param aesKey stored as Base64
         */
        public void setAesKeyFromBase64(byte[] aesKey) {
                try {
                    this.aesKey = Base64.decode(aesKey);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
  
        }
			
		
		
	}