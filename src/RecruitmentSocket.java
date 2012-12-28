import naga.NIOSocket;
import naga.ServerSocketObserverAdapter;
import naga.SocketObserverAdapter;
import naga.packetreader.AsciiLinePacketReader;
import naga.packetwriter.RawPacketWriter;

import java.io.IOException;


public class RecruitmentSocket  extends ServerSocketObserverAdapter {
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

        nioSocket.listen(new SocketObserverAdapter()
        {
            public void packetReceived(NIOSocket socket, byte[] packet)
            {
                
            }

      });
    }
}
