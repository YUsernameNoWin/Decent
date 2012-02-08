

import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;


import sun.misc.*;

/**
 *
 * <p>Title: RSAEncryptUtil</p>
 * <p>Description: Utility class that helps encrypt and decrypt strings using RSA algorithm</p>
 * @author Aviran Mordo http://aviran.mordos.com
 * @version 1
 */
public class FileEncryption
{
    protected  final String ALGORITHM = "RSA";

  



    /**
     * Generate key which contains a pair of privae and public key using 1024 bytes
     * @return key pair
     * @throws NoSuchAlgorithmException
     */
    public  KeyPair generateKey() throws NoSuchAlgorithmException
    {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(1024);
        KeyPair key = keyGen.generateKeyPair();
        return key;
    }


    /**
     * Encrypt a text using public key.
     * @param text The original unencrypted text
     * @param key The public key
     * @return Encrypted text
     * @throws java.lang.Exception
     */
    public  byte[] encrypt(byte[] text, PublicKey key) throws Exception
    {
        byte[] cipherText = null;
        try
        {
            //
            // get an RSA cipher object and print the provider
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            
            // encrypt the plaintext using the public key
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(text);
        }
        catch (Exception e)
        {
 
            throw e;
        }
        return cipherText;
    }

    /**
     * Encrypt a text using public key. The result is enctypted BASE64 encoded text
     * @param text The original unencrypted text
     * @param key The public key
     * @return Encrypted text encoded as BASE64
     * @throws java.lang.Exception
     */
    public  String encrypt(String text, PublicKey key) throws Exception
    {
        String encryptedText;
        try
        {
            byte[] cipherText = encrypt(text.getBytes("UTF8"),key);
            encryptedText = encodeBASE64(cipherText);

        }
        catch (Exception e)
        {

            throw e;
        }
        return encryptedText;
    }

    /**
     * Decrypt text using private key
     * @param text The encrypted text
     * @param key The private key
     * @return The unencrypted text
     * @throws java.lang.Exception
     */
    public  byte[] decrypt(byte[] text, PrivateKey key) throws Exception
    {
        byte[] dectyptedText = null;
        try
        {
            // decrypt the text using the private key
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

            cipher.init(Cipher.DECRYPT_MODE, key);
            dectyptedText = cipher.doFinal(text);
        }
        catch (Exception e)
        {

            throw e;
        }
        return dectyptedText;

    }
    public SecretKeySpec generateSymmetricKey(){
    	   SecretKeySpec skeySpec = null;
    	try {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			SecretKey skey = kgen.generateKey();
		       byte[] raw = skey.getEncoded();
		    skeySpec =   new SecretKeySpec(raw, "AES");
		       
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
     	return skeySpec;
    	
    }
    public byte[] decryptAES(byte[] input,SecretKeySpec key){
 	
    	byte[] output = null; 
      try {
    	  Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, key);
		output = cipher.doFinal(input);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
      return output;
 	
 }
    public String getAESString(SecretKeySpec key){
     	
    	return key.toString();
 	
 }
    public SecretKeySpec getAESFromString(byte[] temp){
     	SecretKeySpec skeySpe = null;
    	try {
			skeySpe =   new SecretKeySpec(temp, "AES");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
 	return skeySpe;
 }
    public SecretKeySpec getAESFromString(String temp){
     	SecretKeySpec skeySpe = null;
    	try {
			skeySpe =   new SecretKeySpec(temp.getBytes(), "AES");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
 	return skeySpe;
 }
    public byte[] encryptAES(String input,SecretKeySpec key){
     	
    	byte[] output = null; 
      try {
    	  Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		output = cipher.doFinal(input.getBytes());
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
      return output;
 	
 }
    public String asHex (byte buf[]) {
        StringBuffer strbuf = new StringBuffer(buf.length * 2);
        int i;

        for (i = 0; i < buf.length; i++) {
         if (((int) buf[i] & 0xff) < 0x10)
  	    strbuf.append("0");

         strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
        }

        return strbuf.toString();
       }
    /**
     * Decrypt BASE64 encoded text using private key
     * @param text The encrypted text, encoded as BASE64
     * @param key The private key
     * @return The unencrypted text encoded as UTF8
     * @throws java.lang.Exception
     */
    public  String decrypt(String text, PrivateKey key) throws Exception
    {
        String result;
        try
        {
            // decrypt the text using the private key
            byte[] dectyptedText = decrypt(decodeBASE64(text),key);
            result = new String(dectyptedText, "UTF8");

        }
        catch (Exception e)
        {

            throw e;
        }
        return result;

    }

    /**
     * Convert a Key to string encoded as BASE64
     * @param key The key (private or public)
     * @return A string representation of the key
     */
    public  static String getKeyAsString(Key key)
    {
        // Get the bytes of the key

        return new String(key.getEncoded());
    }

    /**
     * Generates Private Key from BASE64 encoded string
     * @param key BASE64 encoded string which represents the key
     * @return The PrivateKey
     * @throws java.lang.Exception
     */
    public  PrivateKey getPrivateKeyFromString(String key) throws Exception
    {
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        BASE64Decoder b64 = new BASE64Decoder();
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(b64.decodeBuffer(key));
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        return privateKey;
    }

    /**
     * Generates Public Key from BASE64 encoded string
     * @param key BASE64 encoded string which represents the key
     * @return The PublicKey
     * @throws java.lang.Exception
     */
    public  PublicKey getPublicKeyFromString(String key) throws Exception
    {
        BASE64Decoder b64 = new BASE64Decoder();
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(b64.decodeBuffer(key));
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        return publicKey;
    }

    /**
     * Encode bytes array to BASE64 string
     * @param bytes
     * @return Encoded string
     */
    private  String encodeBASE64(byte[] bytes)
    {
        BASE64Encoder b64 = new BASE64Encoder();
        return b64.encode(bytes);
    }

    /**
     * Decode BASE64 encoded string to bytes array
     * @param text The string
     * @return Bytes array
     * @throws IOException
     */
    private  byte[] decodeBASE64(String text) throws IOException
    {
        BASE64Decoder b64 = new BASE64Decoder();
        return b64.decodeBuffer(text);
    }

    /**
     * Encrypt file using 1024 RSA encryption
     *
     * @param srcFileName Source file name
     * @param destFileName Destination file name
     * @param key The key. For encryption this is the Private Key and for decryption this is the public key
     * @param cipherMode Cipher Mode
     * @throws Exception
     */

    /**
     * Encrypt file using 1024 RSA encryption
     *
     * @param srcFileName Source file name
     * @param destFileName Destination file name
     * @param publicKey The key. For encryption this is the Private Key and for decryption this is the public key
     * @param cipherMode Cipher Mode
     * @throws Exception
     */
    public String  encryptFile(String input, PublicKey publicKey) throws Exception
    {
        return encryptDecryptFile(input, publicKey, Cipher.ENCRYPT_MODE);
    }

    /**
     * Decrypt file using 1024 RSA encryption
     *
     * @param srcFileName Source file name
     * @param destFileName Destination file name
     * @param key The key. For encryption this is the Private Key and for decryption this is the public key
     * @param cipherMode Cipher Mode
     * @throws Exception
     */
    public String decryptFile(String input, PrivateKey key) throws Exception
    {
        return encryptDecryptFile(input, key, Cipher.DECRYPT_MODE);
    }
    public String encryptDecryptFile(String input, Key key, int cipherMode) throws Exception
    {
        OutputStream outputWriter = null;
        InputStream inputReader = null;
        try
        {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            String textLine = null;
            //RSA encryption data size limitations are slightly less than the key modulus size,
            //depending on the actual padding scheme used (e.g. with 1024 bit (128 byte) RSA key,
            //the size limit is 117 bytes for PKCS#1 v 1.5 padding. (http://www.jensign.com/JavaScience/dotnet/RSAEncrypt/)
            byte[] buf = cipherMode == Cipher.ENCRYPT_MODE? new byte[100] : new byte[128];
            int bufl;
            // init the Cipher object for Encryption...
            cipher.init(cipherMode, key);
            String done = "";
            // start FileIO
            while ( input != "")
            {
            	if(input.length()>buf.length){
            		buf = input.substring(0,buf.length).getBytes();
            		input = input.substring(0,buf.length);
            		bufl = buf.length;
            	}
            	else {
            		buf = input.getBytes();
            		bufl = input.length();
            		input = "";
            	}
                byte[] encText = null;
                if (cipherMode == Cipher.ENCRYPT_MODE)
                {
                      encText = encrypt(copyBytes(buf,bufl),(PublicKey)key);
                }
                else
                {

                    encText = decrypt(copyBytes(buf,bufl),(PrivateKey)key);
                }
                done+= new String(encText);

            }
            return done;

        }
        catch (Exception e)
        {

            throw e;
        }
       
    }

    public  byte[] copyBytes(byte[] arr, int length)
    {
        byte[] newArr = null;
        if (arr.length == length)
        {
            newArr = arr;
        }
        else
        {
            newArr = new byte[length];
            for (int i = 0; i < length; i++)
            {
                newArr[i] = (byte) arr[i];
            }
        }
        return newArr;
    }

}
