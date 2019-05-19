package unimelb.bitbox;

import unimelb.bitbox.util.Configuration;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.SecretKey;
import java.util.logging.Logger;

public class Encryption {
	private static Logger log = Logger.getLogger(Encryption.class.getName());
	static String encryptSharedKey(String identity) throws Exception {
		//TODO generate a shared key, encrypt the shared key with the public key of the associated identity and encode it as base 64
		String LocalKey = Configuration.getConfigurationValue("identity");
		if(LocalKey.contains(identity)){
			String[] substr = LocalKey.split(",");
			for (String s : substr) {
				if (s.contains(identity)) {
					String[] identityKey = s.split(" ");
					String publicKey = identityKey[identityKey.length - 2];
				}
			}
			KeyGenerator kg = KeyGenerator.getInstance("AES");
			kg.init(128);
			SecretKey secretKey = kg.generateKey();
			byte[] bK = secretKey.getEncoded();
			String sharedKey = java.util.Base64.getEncoder().encodeToString(bK);
//			String encryptSharedKey;
			return "encryptSharedKey";
		}
		else{
			log.info("No such a public Key");
			//JSON_process.AUTH_RESPONSE(false," ");
			return null;
		}

	}
	
	static String decryptSharedKey(String encryptedKey){
		//TODO decode the encrypted key as base 64, decrypt the key with your private key stored in bitboxclient_rsa
		return "decryptedSharedKey";
	}
	
	static String encryptMessage(String msg, String sharedKey){
		//TODO encrypt the message with the shared key
		String encryptedMsg = msg;
		return encryptedMsg;
	}
	
	static String decryptMessage(String encryptedMsg, String sharedKey){
		//TODO decrypt the message with the shared key
		String msg = encryptedMsg;
		return msg;
	}
	
}
