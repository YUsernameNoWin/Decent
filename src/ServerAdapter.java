import naga.NIOSocket;
import naga.ServerSocketObserverAdapter;
import naga.SocketObserver;
import naga.SocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;

/* Main server component for handling messages. 
 * Ties to master class for referencing peers and handling management of the peer map*/
public class ServerAdapter extends ServerSocketObserverAdapter {
	public Master master;
	Peer peer;
    NIOSocket socket = null;
	Encryption encryption  =  new Encryption();
	int port;
    int column;
    public KeyPair keyPair;
    ServerAdapter self;
	public ServerAdapter(Master master2, int col, Peer self, KeyPair keys ) {
		master = master2;
        this.column = col;
        this.self = this;
		peer = self;
		try {
		   keyPair = keys;
		   
		}catch(Exception e)
		{
		    e.printStackTrace();
		}
	}
      
	public void newConnection(NIOSocket nioSocket)
    {

    	nioSocket.setPacketReader(new AsciiLinePacketReader());
		nioSocket.setPacketWriter(new RawPacketWriter());
		peer.socket = nioSocket;
      // Set our socket observer to listen to the new socket.
        socket = nioSocket;
		nioSocket.listen(new SocketObserverAdapter()
		{
			public void packetReceived(NIOSocket socket, byte[] packet)
			{
				master.parse(new String(packet),self);
			}











      });

    }
    public void bounceMessage(PublicKey key,String encryptedPacket)
    {
        Peer peer = master.keyMap.get(key);
        master.forwardMessage(peer,encryptedPacket,"BOUNCE FROM " + peer.ID);
    }
    public void peerParse(JSONObject<?, ?> encryptedPacket, Peer sender)throws Exception
    {
        JSONObject<?, ?> clearPacket = new JSONObject<Object, Object>();
        //1 == send up. 2 == broadcast. 3 == forward down. 4 == send down one. 5 == Reroute.
        int type = encryptedPacket.getInt("type");
        //test send up to master, add UUID to senders list in order to send back response
        //get
        //System.out.println();

        if(type == 1 || type == 2)
        {
            if(!sender.isPeerActive)
            {
                peerActivate(encryptedPacket, sender);
            }
            else {
                clearPacket = encryption.AESdecryptJSON(encryptedPacket,sender.getPeerAesKey());
                if(clearPacket.has("needcol"))
                    sendCol(sender);
            }

        }

        //forward response downwards or if it is meant for us, decrypt it.
        else if( type == 2)
        {

        }

    }


    public void peerAddPublicKey(Peer hashed, JSONObject<?, ?> clearPacket, NIOSocket socket) {
        try {
            JSONObject<?, ?> outPacket = new JSONObject<Object, Object>();
            master.keyMap.put(encryption.getPublicKeyFromString(clearPacket.getString("publickey")), hashed);
            hashed.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
            outPacket.put("keylist",true);
            outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket, hashed.getAesKey()), 2, hashed);
            master.forwardMessage(socket,outPacket.toString(),"peeraddpublickey");
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }



    public void peerConnectionBroken(JSONObject<?, ?> clearPacket, Peer hashed) {
        try
        {
            JSONObject<?, ?> peers = master.getPeers(hashed);
            if(peers.has(clearPacket.getString("connectionbroken")))
            {
                Peer dead =  ((Peer) peers.get(clearPacket.getString("connectionbroken")));
                dead.connectionBrokenCount++;

                if(dead.connectionBrokenCount > 1)
                {
                    master.removePeer(dead);

                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void peerActivate(JSONObject<?, ?> encryptedPacket,Peer sender) {
        JSONObject clearPacket = new JSONObject();
        try {
            //clearPacket =  encryption.RSAdecryptJSON(encryptedPacket,master.privateKey);
            if(encryptedPacket.has("aeskey"))
            {

                byte[] key = encryption.decryptRSA(keyPair.getPrivate(), encryptedPacket.getString("aeskey").getBytes());
                sender.setPeerAesKeyFromBase64(key);
                encryptedPacket.remove("aeskey");
                clearPacket = encryption.AESdecryptJSON(encryptedPacket,sender.getPeerAesKey());
                if(clearPacket.has("publickey"))
                {
                    sender.publicKey = encryption.getPublicKeyFromString(clearPacket.getString("publickey"));
                    sender.isPeerActive = true;
                    sender.ID = clearPacket.getString("src");
                    JSONObject outPacket = new JSONObject();
                    outPacket.put("gotpubkey",true);
                    outPacket = addPeerHeader(encryption.AESencryptJSON(outPacket, sender.getPeerAesKey()), 2, sender);
                    master.forwardMessage(peer, outPacket.toString(), "gotpubkey");
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }

    }
    private void sendCol(Peer sender) {
        JSONObject outPacket = new JSONObject();
        try {
            outPacket.put("col",column);
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        outPacket = addPeerHeader(encryption.AESencryptJSON(outPacket, sender.getPeerAesKey()), 2, sender);
        master.forwardMessage(peer,outPacket.toString(),"sendcolumn");
    }

    @Override
	public void acceptFailed(IOException exception) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void serverSocketDied(Exception exception) {
		// TODO Auto-generated method stub
		
	}
    public JSONObject<?, ?> addPeerHeader(JSONObject<?, ?> json,int type,Peer peer )
    {

        try {
            return(json
                    .put("col", Integer.toString(peer.x))
                    .put("src", encryption.getKeyAsString(keyPair.getPublic()))
                    .put("dest", encryption.getKeyAsString(peer.publicKey))
                    .put("type", Integer.toString(type)));

        } catch (JSONException e) {

            e.printStackTrace();
        }
        return json;
    }
    public void writeToFile(String text)
    {
          try
          {
              // Create file 
                  FileWriter fstream = new FileWriter("error.txt");
                  BufferedWriter out = new BufferedWriter(fstream);
                  
                  out.append(text);
                  //Close the output stream
                  out.close();
              }catch (Exception e)
              {//Catch exception if any
              System.err.println("Error: " + e.getMessage());
              }
        
    }
}
class internalSocket implements SocketObserver{
	Peer peer = null;
	String request = null;
	Master master = null;
	String response = "";
	public internalSocket(Peer peer, String request, Master master)
	{
		this.peer = peer;
		this.request = request;
		this.master = master;
	}
	@Override
	public void connectionOpened(NIOSocket nioSocket) {
		nioSocket.write(("GET " + request +" HTTP/1.0\r\n\r\n").getBytes());
		
	}

	@Override
	//TODO Fix up who the to send the connbroken packet to...
	public void connectionBroken(NIOSocket nioSocket, Exception exception) {
		// TODO Auto-generated method stub
		JSONObject<?, ?> outPacket = new JSONObject<Object, Object>();
		try {
			outPacket.put("response",response);
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
	    outPacket = master.addHeader(master.encryption.AESencryptJSON(outPacket, peer.getAesKey()), 2, peer);
	    master.forwardMessage(peer,outPacket.toString(),"getRequest");

	}

	@Override
	public void packetReceived(NIOSocket socket, byte[] packet) {
		response += new String(packet);
		
	}
	
}
