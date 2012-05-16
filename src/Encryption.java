import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.*;
import javax.crypto.spec.*;

import org.JSON.JSONArray;
import org.JSON.JSONException;
import org.JSON.JSONObject;

import sun.misc.BASE64Decoder;

import java.io.*;
import java.util.*;
public class Encryption {

	/* Main encryption class. Encrypts with AES or RSA
    * 
	 */

    public void writeToFile(String input)
    {
        try
        {
            // Create file 
                FileWriter fstream = new FileWriter("test.txt",true);
                BufferedWriter out = new BufferedWriter(fstream);
                
                out.write(input);
                out.newLine();
                out.close();
        }catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }
    public JSONObject RSAdecryptJSON(JSONObject encrypted, PrivateKey privKey)
    {  
        JSONObject clearPacket = new JSONObject();
        try {
            clearPacket.putOpt("id", encrypted.remove("id"));
            clearPacket.putOpt("type", encrypted.remove("type"));
            clearPacket.putOpt("col", encrypted.remove("col"));
            clearPacket.putOpt("debug", encrypted.remove("debug"));
            clearPacket.putOpt("exchange", encrypted.remove("exchange"));
            for(int i =0;i<encrypted.names().length();i++)
            {
                String decryptedName = new String(decryptRSA(privKey,encrypted.names().getString(i).getBytes()));
                String decryptedContent = new String(decryptRSA(privKey,encrypted.getString(encrypted.names().getString(i)).getBytes()));
                
                clearPacket.put(decryptedName,decryptedContent);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println(encrypted);
            e.printStackTrace();
        }
        return clearPacket;
        
    }
    
    
   /* Main encryption class. Encrypts with AES or RSA
	* 
	 */
	
	public JSONObject RSAencryptJSON(JSONObject decrypted,PublicKey publicKey)
   {
       JSONObject encrypted = new JSONObject();
       if(decrypted.names() == null)
           return decrypted;
       for(int a=0;a<decrypted.names().length();a++){
           try {
               encrypted.put(new String(encryptRSA(publicKey, decrypted.names().getString(a).getBytes())),
                       new String(encryptRSA(publicKey, decrypted.getString(decrypted.names().getString(a)))));
           } catch (Exception e) {
               e.printStackTrace();
           }
       }
       return encrypted;
   }
	public JSONObject AESencryptJSON(JSONObject decrypted,byte[] aesKey)
{
    JSONObject encrypted = new JSONObject();
    if(decrypted.names() == null)
        return decrypted;
    for(int a=0;a<decrypted.names().length();a++){
        try {
            encrypted.put(new String(encryptAES(aesKey, decrypted.names().getString(a).getBytes())),
            		new String(encryptAES(aesKey, decrypted.get(decrypted.names().getString(a)).toString().getBytes())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    return encrypted;
}
/* Main encryption class. Encrypts with AES or RSA
* 
 */

public JSONObject AESdecryptJSON(JSONObject encrypted, byte[] AESkey)
{  
    //Disable for debugging
    JSONObject clearPacket = new JSONObject();
    try {
        clearPacket.putOpt("id", encrypted.remove("id"));
        clearPacket.putOpt("type", encrypted.remove("type"));
        clearPacket.putOpt("col", encrypted.remove("col"));
        clearPacket.putOpt("debug", encrypted.remove("debug"));
        JSONArray names = encrypted.names();
        if(names == null)
            return clearPacket;
        for(int i =0;i<names.length();i++)
        {
            try {
          String decryptedName = new String(decryptAES(AESkey, names.getString(i).getBytes()));
          String decryptedContent = new String(decryptAES(AESkey, encrypted.getString(names.getString(i)).getBytes()));
          clearPacket.put(decryptedName, decryptedContent);
            }catch(Exception e) {
                e.printStackTrace();
                writeToFile(names.getString(i));
            }



        }
    }
    catch(Exception e)
    {
    	e.printStackTrace();
        writeToFile(new String (AESkey)  + " " + encrypted.toString());
    }
    return clearPacket;
    
}
	/**
     * Generates Private Key from BASE64 encoded string
     * @param key BASE64 encoded string which represents the key
     * @return The PrivateKey
     * @throws java.lang.Exception
     */
    
    public PrivateKey getPrivateKeyFromString(String key) throws Exception
    {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(key));
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        return privateKey;
    }
    public byte[] getAESFromString(byte[] temp){
     	byte[] skeySpe = null;
    	try {
			skeySpe =   Base64.decode(temp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
 	return skeySpe;
 }
    public  byte[] getAESFromString(String temp){
     	byte[] skeySpe = null;
    	try {
			skeySpe =   Base64.decode(temp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
 	return skeySpe;
 }
    public   String getKeyAsString(Key key)
    {
        // Get the bytes of the key

        return new String(Base64.encodeBytesToBytes(key.getEncoded()));
    }
    /**
     * Generates Public Key from BASE64 encoded string
     * @param key BASE64 encoded string which represents the key
     * @return The PublicKey
     * @throws java.lang.Exception
     */
    public  PublicKey getPublicKeyFromString(String key) throws Exception
    {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(key));
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        return publicKey;
    }
	   public  SecretKeySpec generateSymmetricKey(){
    	   SecretKeySpec skeySpec = null;
    	try {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			kgen.init(128);
			SecretKey skey = kgen.generateKey();
		       byte[] raw = skey.getEncoded();
		    skeySpec =   new SecretKeySpec(raw, "AES");
		    
		       
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
     	return skeySpec;
    	
    }
	 public   KeyPair generateKey() throws NoSuchAlgorithmException
	    {
	        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	        keyGen.initialize(1024);
	        KeyPair key = keyGen.generateKeyPair();
	        return key;
	    }
	public  byte[] encryptRSA( PublicKey key, byte[] text) throws Exception
    { 
        byte[] dectyptedText = null;

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        dectyptedText = Base64.encodeBytesToBytes(cipher.doFinal(text));
        return dectyptedText;
    }
	public  byte[] encryptRSA( PublicKey key, String text) throws Exception
    { 
		
        byte[] dectyptedText = null;

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        dectyptedText = Base64.encodeBytesToBytes(cipher.doFinal(text.getBytes()));
        return dectyptedText;
   
    }



    /**
     * Decode BASE64 encoded string to bytes array
     * @param text The string
     * @return Bytes array
     * @throws IOException
     */

	public  byte[] decryptRSA( PrivateKey key, byte[] text) throws Exception
    { 
        byte[] decryptedText = null;

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        decryptedText = cipher.doFinal(Base64.decode(text));
        return decryptedText;
    }
	public  byte[] decryptRSA( PrivateKey key, String text) throws Exception
    { 
        byte[] dectyptedText = null;

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        dectyptedText = cipher.doFinal(Base64.decode(text.getBytes()));
        return text.getBytes();
    }
	   public  byte[] encryptAES(byte[] key, byte[] text) throws Exception {   
			   SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");


		       // Instantiate the cipher

		       Cipher cipher = Cipher.getInstance("AES");

		       cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
	        byte[] encValue = cipher.doFinal(text);
	        byte[] encryptedValue = Base64.encodeBytesToBytes(encValue);
	        return encryptedValue;
	     }
	public  byte[] decryptAES(byte[] key, byte[] text) throws Exception{   
			 SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
	        Cipher c = Cipher.getInstance("AES");
	        c.init(Cipher.DECRYPT_MODE, skeySpec);
	        byte[] decordedValue = Base64.decode(text);
	        byte[] decValue = c.doFinal(decordedValue);
	        return decValue;

     }

	private  byte[] GetKey(byte[] suggestedKey)
    {
        byte[] kRaw = suggestedKey;
        ArrayList<Byte> kList = new  ArrayList<Byte>();

        for (int i = 0; i < 128; i += 8)
        {
            kList.add(kRaw[(i / 8) % kRaw.length]);
        }

        byte[] byteArray = new byte[kList.size()];
        for(int i = 0; i<kList.size(); i++){
          byteArray[i] = kList.get(i);
        }
        return byteArray;
    }




}
