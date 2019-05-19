package unimelb.bitbox;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class Encryption {
	static String encryptSharedKey(String identity){
		//TODO generate a shared key, encrypt the shared key with the public key of the associated identity and encode it as base 64
		return "encryptedSharedKey";
	}
	
	static String decryptSharedKey(String encryptedKey){
		//TODO decode the encrypted key as base 64, decrypt the key with your private key stored in bitboxclient_rsa
		return "decryptedSharedKey";
	}
	
	static String encryptMessage(String msg){
		//TODO encrypt the message with the shared key
		String encryptedMsg = msg;
		return encryptedMsg;
	}
	
	static String decryptMessage(String encryptedMsg){
		//TODO decrypt the message with the shared key
		String msg = encryptedMsg;
		return msg;
	}
	
}
