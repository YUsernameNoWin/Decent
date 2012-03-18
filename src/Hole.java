/*
 * Holes represent places in the peer map where new peers should be placed when they connect.
 */
public class Hole{
        Hole(int x, int y){
            this.x = x;
            this.y  =y;
        }
        int x,y;
    }