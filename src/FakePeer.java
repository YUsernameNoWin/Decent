import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;


public class FakePeer {
	public PublicKey publicKey;
	public PrivateKey privateKey;
	public int column;
	public Peer down, downLeft, downRight;
	public Encryption encryption = new Encryption();
	public FakePeer()
	{
		KeyPair pair = null;
		try {
			pair = encryption.generateKey();
			publicKey = pair.getPublic();
			privateKey = pair.getPrivate();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
