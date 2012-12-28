import org.JSON.JSONException;
import org.JSON.JSONObject;

/**
 * User: Quinn
 * Date: 9/2/12
 * Time: 5:51 PM
 */
public class Message {
    long time = 0;
    JSONObject contents = null;
    int id = 0;
    int state = 0;
    Peer owner = null;
    boolean fromPeer = false;
    ServerAdapter fromServer;
    public long timeout = 0;
    public boolean isPublicKeyEncrypted = false;
    public Message(JSONObject contents , Peer owner)
    {
        this.contents = contents;
        this.owner = owner;
        time = System.currentTimeMillis();
        timeout = System.currentTimeMillis();

    }
    public void peerMessage(ServerAdapter from)
    {
        fromServer = from;
        fromPeer = true;
    }
    public void updateTime()
    {
        time = System.currentTimeMillis() - time;
        timeout = System.currentTimeMillis() - timeout;
    }
    public void resetTime()
    {
        time = System.currentTimeMillis();
    }
    public String toString()
    {
        return contents.toString();
    }

}
