import naga.NIOSocket;
import naga.SocketObserverAdapter;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*Client part of the peer. Connects to a ServerAdapter or PeerServerAdapter.
* Message parsing is done in NetworkThread.
*
*/
public class NetworkProtocol extends SocketObserverAdapter{
	public NetworkThread master;
	public Peer sender;
	Encryption encryption  =  new Encryption();
	public NetworkProtocol(NetworkThread master2, Peer peer) {
		master = master2;
		sender = peer;
	}


       public JSONObject addHeader(JSONObject json,int type)
       {

           try {
               return(json.put("col", Integer.toString(master.column)).put("src",master.ID).put("type", Integer.toString(type)));

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
	                  FileWriter fstream = new FileWriter("text.html");
	                  BufferedWriter out = new BufferedWriter(fstream);
	                  
	                  out.write(text);
	                  //Close the output stream
	                  out.close();
	              }catch (Exception e)
	              {//Catch exception if any
	              System.err.println("Error: " + e.getMessage());
	              }
	        
	    }
	
	
	

	public void connectionOpened(NIOSocket nioSocket) 
	{

        sender.socket = nioSocket;
        sender.setAesKey(encryption.generateSymmetricKey());
        //sender.setActive(true);
       master.timer.remove(master.timerTasks.get("connecttoup"));

            try {


                if(sender.name.equals("up"))
                {

                    sender.setActive(true);
                    master.timerTasks.put("upkeyexchange", new Runnable() {
                        public void run() {
                            try {
                                if(!master.exchangedKeysWithUp)
                                    master.keyExchange(sender);
                            } catch (Exception e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    });
                    master.timer.scheduleAtFixedRate(master.timerTasks.get("upkeyexchange"), 0, 3, TimeUnit.SECONDS);
                }/*
                if(sender.name.equals("upRight"))
                {
                    master.sendLeftConnection();
                }     */
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
          
        

	}

	@Override
	public void connectionBroken(NIOSocket nioSocket, Exception exception)
    {
        if(!master.state.equals("repairing"))
        {
            sender.setActive(false);
            //master.timer.shutdown();
            //master.timer = new ScheduledThreadPoolExecutor(2);
            //master.timer.scheduleAtFixedRate(master.timerTasks.get("connecttoup"),0,3,TimeUnit.SECONDS);
            master.deadPeer(sender);
        }


    }

	public JSONObject stripHeader(JSONObject temp)
	{
	    temp.remove("src");
	    temp.remove("type");
	    temp.remove("col");
	    return temp;
	}
	@Override
    public void packetReceived(NIOSocket socket, byte[] packet)
    {
	    try {
            master.parse(new String(packet),sender);
        } catch (Exception e) {
            
            e.printStackTrace();
        } 
    }
}
