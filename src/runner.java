import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/* Main class. Initiates Peers.*/
public class runner extends Thread{

    /**
     * @param args
     * @throws IOException 
     * @throws UnknownHostException 
     */
    public static void main(String[] args) throws UnknownHostException, Exception {
        // TODO Auto-generated method stub


       Master master =new Master();
        ArrayList<NetworkThread> peers = new ArrayList<NetworkThread>();
           master.start();
         /*  sleep(1000);
           for(int i = 0; i < 9; i++)
           {
               master.addPeer(new Peer(Integer.toString(i+4)));
               
           }
           sleep(1000);
           System.out.println(master.map.get(2).get(2));
           while(true)
           {
               master.printMap();
               System.out.println(master.getPeers(master.map.get(scan.nextInt()).get(scan.nextInt())));

           }*/


           Scanner scan = new Scanner(System.in);
           int temp = 4;
        for(int i=1;i<=3;i++)
        {
           peers.add(new NetworkThread(i*10+500,temp));
           temp++;
          peers.add(new NetworkThread(i*10+501,temp));
          temp++;
          peers.add(new NetworkThread(i*10+502,temp));
          temp++;
        }
        
        

        for(NetworkThread a:peers)
        {
            a.start();
           // scan.next();
            System.out.print(a.port + " ");
           sleep(200);
        }
        //scan.next();
       // peers.get(4).close();
       // peers.get(5).close();
        sleep(1000);
        System.out.println("ID " + peers.get(3).id);
        Random rand = new Random();
        peers.get(0).close();

        /*
        new NetworkThread(master.holes.remove().up.port + 10,8213).start();
        sleep(100);
        new NetworkThread(master.holes.remove().up.port + 10,80).start();
        sleep(100);
        //peers.get(1).close();
       new NetworkThread(master.holes.remove().up.port + 10,90).start();

*/
        
    }

}
