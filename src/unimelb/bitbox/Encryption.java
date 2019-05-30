package unimelb.bitbox;
import com.google.common.io.ByteStreams;
import unimelb.bitbox.util.Configuration;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.SecretKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkArgument;

public class Encryption {
	private static final String SSH_MARKER = "ssh-rsa";

	private static Logger log = Logger.getLogger(Encryption.class.getName());
	static String encryptSharedKey(String identity) throws Exception {
		//do i need code the Json message sending part here or this part may
		//return more than one parameter like a status about if this works
		//TODO generate a shared key, encrypt the shared key with the public key of the associated identity and encode it as base 64
		PublicKey publicKey=null;
		String LocalKey = Configuration.getConfigurationValue("identity");
		if(LocalKey.contains(identity)){
			String[] substr = LocalKey.split(",");
			for (String s : substr) {
				if (s.contains(identity)) {
					String identityKey = s;
					//publicKey = getPublicKey(identityKey[-2：-1]);
				}
			}
			KeyGenerator kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			SecretKey secretKey = kg.generateKey();
			byte[] bK = secretKey.getEncoded();
			//String sharedKey = java.util.Base64.getEncoder().encodeToString(bK);
//			String encryptSharedKey;

			Cipher cipher = Cipher.getInstance("RSA");
			//加密模式
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			String encryptedSharedKey = java.util.Base64.getEncoder().encodeToString(cipher.doFinal(bK));
			return encryptedSharedKey;
		}
		else{
			log.info("No such a public Key");
			//JSON_process.AUTH_RESPONSE(false," ");
			return null;
		}

	}
	
	static String decryptSharedKey(String encryptedKey,String path) throws Exception {
		//TODO decode the encrypted key as base 64, decrypt the key with your private key stored in bitboxclient_rsa
		//byte[] encryptedMsg =
		try {
			PrivateKey privateKey = getPrivateKey(path);
			Cipher cipher = Cipher.getInstance("RSA");//java默认"RSA"="RSA/ECB/PKCS1Padding"
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			String sharedKey = Arrays.toString(cipher.doFinal(Base64.getDecoder().decode(encryptedKey)));
			//String sharedKey = java.util.Base64.getEncoder().encodeToString(output);
			return sharedKey;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}
		//return "decryptedSharedKey";
	}
	
	static String encryptMessage(String msg, Key sharedKey){
		//TODO encrypt the message with the shared key
		try{
			Cipher cipher = Cipher.getInstance("AES");//java默认"RSA"="RSA/ECB/PKCS1Padding"
			cipher.init(Cipher.ENCRYPT_MODE, sharedKey);
			byte[] output = cipher.doFinal(msg.getBytes());
			String encrypteMsg = java.util.Base64.getEncoder().encodeToString(output);
			return encrypteMsg;
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
		//return "encryptedMsg";
	}
//		String encryptedMsg = msg;
//		return encryptedMsg;
	
	static String decryptMessage(String encryptedMsg, Key sharedKey){
		//TODO decrypt the message with the shared key
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, sharedKey);
			//BASE64Decoder decoder = new BASE64Decoder();
			byte[] c = java.util.Base64.getDecoder().decode(encryptedMsg);
			byte[] result = cipher.doFinal(c);
			String plainText = new String(result, StandardCharsets.UTF_8);
			return plainText;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
//		String msg = encryptedMsg;
//		return msg;
	}

	private static PublicKey getPublicKey(String identity) throws Exception {
		//TODO getPublicKey from a string read from the configuration file
		String keys = Configuration.getConfigurationValue("authorized_keys").trim();
		String[] keylist = keys.split(",");

		boolean foundIdentity = false;
		String RSApublicKeyString = "";

		for(String rsaPubKey: keylist) {
			String[] keyComponent = rsaPubKey.split(" ");
			if (keyComponent[2].equals(identity)) {
				RSApublicKeyString = keyComponent[1];
				foundIdentity = true;
				break;
			}
		}

		if(!foundIdentity){
			return null;
		}



		try {
			InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(RSApublicKeyString));
			String marker = new String(readLengthFirst(stream));
			checkArgument(SSH_MARKER.equals(marker), "looking for marker %s but received %s", SSH_MARKER, marker);
			BigInteger publicExponent = new BigInteger(readLengthFirst(stream));
			BigInteger modulus = new BigInteger(readLengthFirst(stream));
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, publicExponent);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");

			return keyFactory.generatePublic(keySpec);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		/*System.out.println(RSApublicKeyString);
		try{
			byte[] byteKey = Base64.getDecoder().decode(RSApublicKeyString);
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(byteKey);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");

			PublicKey pkey = keyFactory.generatePublic(keySpec);
		}
		catch(Exception e){
			e.printStackTrace();
		}
*/
/*		byte[] keyBytes;
		keyBytes = java.util.Base64.getDecoder().decode(key);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = keyFactory.generatePublic(keySpec);
		return publicKey;*/
	}
	private static byte[] readLengthFirst(InputStream in) throws IOException {
		int[] bytes = new int[]{ in.read(), in.read(), in.read(), in.read() };
		int length = 0;
		int shift = 24;
		for (int i = 0; i < bytes.length; i++) {
			length += bytes[i] << shift;
			shift -= 8;
		}
		byte[] val = new byte[length];
		ByteStreams.readFully(in, val);
		return val;
	}

    private static RSAPrivateKey getPrivateKey(String filename) throws Exception {
//		File f = new File(filename);
//		FileInputStream fis = new FileInputStream(f);
//		DataInputStream dis = new DataInputStream(fis);
//		byte[] keyBytes = new byte[(int) f.length()];
//		dis.readFully(keyBytes);
//		dis.close();
//
//		String temp = new String(keyBytes);
//		String privKeyPEM = temp.replace("-----BEGIN PRIVATE KEY-----\n", "");
//		privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "");
//		//System.out.println("Private key\n"+privKeyPEM);
//
//		Base64 b64 = new Base64();
//		byte [] decoded = b64.decode(privKeyPEM);
//
//		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
//		KeyFactory kf = KeyFactory.getInstance(algorithm);
//		return kf.generatePrivate(spec);
		return null;
    }

    public static void main(String[] args) throws Exception {
//		RSAPrivateKey privateKey = getPrivateKey("bitboxclient_rsa");
//		String privatek = privateKey.toString();
//		System.out.println(privatek);
//		String str = encryptSharedKey("derekxuan@outlook.com");
		Key str = getPublicKey("derekxuan@outlook.com");
		System.out.println("the key is: " + str + "finished");

	}

}


