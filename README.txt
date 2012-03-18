Decent(working name) is an attempt to create self scaling anonymous networks. The structure of a network starts with a master at the top that acts a coordinator for connecting peers and serving content. From there, peers who connect to the master are organized into layers. It is suggested that you own at least the  first layer of peers to ensure the security of the master. Peers connect to other peers who will then forward messages to the master. The master will check to see what peers need to connect to who and send connect messages to those peers.  When a peer that is farther down the in the levels requests content, it will funnel its way up to the master.  If the content is public and an upstream peer already has the content, that upstream peer will relay back the content.  This allows for massive scaling when there is sudden traffic.
 Here's an example of a network.
 M = master
 P = peer
| = up connection
/ = connection from right to left. If on the edge of the network, it connects to the one on the right on the other side.
\ = connection from left to right. If on the edge of the network, it connects to the one on the left on the other side.
                        M
                    /    |    \
                    p   p    p
                \ | /\  |  /\ | /
                   p   p    p
                \ | /\  |  /\ | /
                   p   p    p								
					
The key files in the src folder are the master class, and the networkthread class. The runner is the main class.