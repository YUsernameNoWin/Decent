import java.util.List;

/*
 * Holes represent places in the peer map where new peers should be placed when they connect.
 */
public class Hole{
    int x,y;
    public Peer up;
        Hole(int x, int y, Peer up){
            this.x = x;
            this.y  =y;
            this.up = up;
        }
    public String toString()
    {
        return "Hole: Row = " + x + "Column = " + y + "Peer port: " + (up.port+ 10) ; 
    }


}