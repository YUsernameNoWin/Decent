import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;
import java.io.IOException;
import java.net.InetAddress;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.ActionResponse;
import net.sbbi.upnp.messages.UPNPResponseException;
import javax.crypto.spec.SecretKeySpec;


public class Main {

	/**
	 * @param args
	 * @throws Exception 
	 */
    Encryption encryption  =  new Encryption();
	public static void main(String[] args) throws Exception {
		int discoveryTiemout = 5000; // 5 secs
	    try {
	      InternetGatewayDevice[] IGDs = InternetGatewayDevice.getDevices( discoveryTiemout );
	      if ( IGDs != null ) {
	        for ( int i = 0; i < IGDs.length; i++ ) {
	          InternetGatewayDevice testIGD = IGDs[i];
	          System.out.println( "Found device " + testIGD.getIGDRootDevice().getModelName() );
	          // now let's open the port
	          String localHostIP = InetAddress.getLocalHost().getHostAddress();
	          boolean mapped = testIGD.addPortMapping( "Some mapping description", 
	                                                   null, 6000, 6000,
	                                                   localHostIP, 0, "TCP" );
	          if ( mapped ) {

	            System.out.println( "Port 9090 mapped to " + localHostIP );
	            System.out.println( "Current mappings count is " + testIGD.getNatMappingsCount() );
	            // checking on the device
	            ActionResponse resp = testIGD.getSpecificPortMappingEntry( null, 9090, "TCP" );
	            if ( resp != null ) {
	              System.out.println( "Port 9090 mapping confirmation received from device" );
	            }
	          }
	        }
	      } else {
	        System.out.println( "Unable to find IGD on your network" );
	      }
	    } catch ( IOException ex ) {
	      System.err.println( "IOException occured during discovery or ports mapping " +
	                          ex.getMessage() );
	    } catch( UPNPResponseException respEx ) {
	      System.err.println( "UPNP device unhappy " + respEx.getDetailErrorCode() + 
	                          " " + respEx.getDetailErrorDescription() );
	    }
	
	}
	public static String splitEN(PublicKey text,String key) throws Exception{
		String encrypt = "";
		for(int a = 0;a<1+(key.length()/117);a++){
			if(key.length() > 117){
				encrypt+=new String(encryption.encryptRSA(text,key.substring(0,117)));
				key = key.substring(0,117);
			}
			else {
				encrypt+=new String(encryption.encryptRSA(text,key));
			}
			
		}
		byte[]gb = encrypt.getBytes();
		return encrypt;
	}
	public static String splitDE(PrivateKey text,String key) throws Exception{
		String encrypt = "";
		for(int a = 0;a<key.length()/128;a++){
				encrypt+=new String(encryption.decryptRSA(text,key.substring(0,128)));
				key = key.substring(0,128);
			
		}
		return encrypt;
	}
}



