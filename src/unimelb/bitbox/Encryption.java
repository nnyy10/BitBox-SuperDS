package unimelb.bitbox;
import com.google.common.io.ByteStreams;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import unimelb.bitbox.util.Configuration;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.security.Security;


import static com.google.common.base.Preconditions.checkArgument;

public class Encryption {
	private static final String SSH_MARKER = "ssh-rsa";

	private static Logger log = Logger.getLogger(Encryption.class.getName());

	static String encryptSharedKey(String identity) throws Exception {
		try {
			PublicKey publicKey= getPublicKey(identity);
			String LocalKey = Configuration.getConfigurationValue("identity");
			KeyGenerator kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			SecretKey secretKey = kg.generateKey();
			byte[] bK = secretKey.getEncoded();
			log.info("sent: " + Arrays.toString(bK));

			Cipher cipher = Cipher.getInstance("RSA");
			//encrypt mode
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			String encryptedSharedKey = java.util.Base64.getEncoder().encodeToString(cipher.doFinal(bK));
			return encryptedSharedKey;
		}
		catch (Exception e){
			log.info("Encrypt shared key fail");
		}
		return null;
	}

	
	static String decryptSharedKey(String encryptedKey,String path) throws Exception {
		try {
			PrivateKey privateKey = getPrivateKey(path);
			Cipher cipher = Cipher.getInstance("RSA");//java默认"RSA"="RSA/ECB/PKCS1Padding"
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			String sharedKey = Arrays.toString(cipher.doFinal(Base64.getDecoder().decode(encryptedKey)));
			//String sharedKey = java.util.Base64.getEncoder().encodeToString(output);
			return sharedKey;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static String encryptMessage(String msg, String str){
		String[] byteValues = str.substring(1, str.length() - 1).split(",");
		byte[] bytes = new byte[byteValues.length];
		Key sharedKey = new SecretKeySpec(bytes, 0, bytes.length,"AES");
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
	}
	
	static String decryptMessage(String encryptedMsg, String str){
		String[] byteValues = str.substring(1, str.length() - 1).split(",");
		byte[] bytes = new byte[byteValues.length];
		Key sharedKey = new SecretKeySpec(bytes, 0, bytes.length,"AES");
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
	}

	private static PublicKey getPublicKey(String identity) {
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
			log.info("identity not found!");
			return null;
		}
		/**
		 * Public key conversion algorithm found at
		 * https://stackoverflow.com/questions/47816938/java-ssh-rsa-string-to-public-key
		 */
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

    private static PrivateKey getPrivateKey(String filename) throws Exception {
		/**
		 * Private conversion algorithm found at
		 * https://stackoverflow.com/questions/3243018/how-to-load-rsa-private-key-from-file
		 */
		String keyPath = filename;
		BufferedReader br = new BufferedReader(new FileReader(keyPath));
		Security.addProvider(new BouncyCastleProvider());
		PEMParser pp = new PEMParser(br);
		PEMKeyPair pemKeyPair = (PEMKeyPair) pp.readObject();
		pp.close();
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemKeyPair.getPrivateKeyInfo().getEncoded());
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey key = keyFactory.generatePrivate(keySpec);
		return key;
	}


    public static void main(String[] args) throws Exception {
		String Msg = "this is a test message";
		//System.out.println("the key is: " + str + " finished");
		String s = encryptSharedKey("zhouguozhi@xuandeMacBook-Air.local");
		//System.out.println(s);
		//Key pk = getPrivateKey("/");
		String str = decryptSharedKey(s,"id_rsa");
		log.info("received: " + str);
		String TestMsg = encryptMessage(Msg, str);
		//log.info(TestMsg);
		String RMsg = decryptMessage(TestMsg, str);
		System.out.println(RMsg);
	}

}


