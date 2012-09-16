/*
 * Holes represent places in the peer map where new peers should be placed when they connect.
 */
public class Hole{
    int x,y;
    public Peer up;
        Hole(int x, int y){
            this.x = x;
            this.y  =y;
        }
    public String toString()
    {
        return ("Hole: Row = " + x + "Column = " + y) ;
    }


}