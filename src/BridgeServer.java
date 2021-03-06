import naga.NIOSocket;
import naga.ServerSocketObserverAdapter;
import naga.SocketObserver;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Quinn
 * Date: 8/29/12
 * Time: 3:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class BridgeServer extends ServerSocketObserverAdapter {
    NetworkThread master;
    BridgeServer()
    {
        this.master = null;
    }
    BridgeServer(NetworkThread master)
    {
        this.master = master;
    }
    public void acceptFailed(IOException exception)
    {
    }

    public void serverSocketDied(Exception e)
    {
    }

    public void newConnection(NIOSocket nioSocket)
    {
        nioSocket.setPacketReader(new AsciiLinePacketReader());
        nioSocket.setPacketWriter(new RawPacketWriter());
        nioSocket.listen(new SocketObserver() {
            @Override
            public void connectionOpened(NIOSocket nioSocket) {
                JSONObject message = new JSONObject();
                try {
                    message.put("bridge", master.encryption.getKeyAsString(master.top.publicKey));
                    message.put("type",2);
                    message.put("debug","bridge");
                } catch (JSONException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                nioSocket.write((message.toString() +"\n").getBytes());
            }

            @Override
            public void connectionBroken(NIOSocket nioSocket, Exception exception) {

            }

            @Override
            public void packetReceived(NIOSocket socket, byte[] packet) {
                master.sendBridgeMessage(packet,socket);
            }
        });
    }

}
