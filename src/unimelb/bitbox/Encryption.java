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
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.security.Security;


import static com.google.common.base.Preconditions.checkArgument;

public class Encryption {
    private static final String SSH_MARKER = "ssh-rsa";

    private static Logger log = Logger.getLogger(Encryption.class.getName());

    static String getSharedKey() {
        try {
            //generating AES Key
            KeyGenerator kg = null;
            kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            SecretKey secretKey = kg.generateKey();
            byte[] bK = secretKey.getEncoded();
            return Arrays.toString(bK);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    static String encryptSharedKey(String identity, String sharedKeyByteString) {
        try {
            PublicKey publicKey = getPublicKey(identity);

            String[] byteValues = sharedKeyByteString.substring(1, sharedKeyByteString.length() - 1).split(",");
            byte[] bK = new byte[byteValues.length];
            for (int i = 0, len = bK.length; i < len; i++) {
                bK[i] = Byte.parseByte(byteValues[i].trim());
            }


            Cipher cipher = Cipher.getInstance("RSA");
            //encrypt mode
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            String encryptedSharedKey = java.util.Base64.getEncoder().encodeToString(cipher.doFinal(bK));
            return encryptedSharedKey;
        } catch (Exception e) {
            log.warning("Encrypt shared key failed");
            log.warning(e.toString());
            return null;
        }
    }

    static String decryptSharedKey(String encryptedSharedKey, String path) {
        try {
            /**
             * the main algorithm of decryption is from
             * https://blog.csdn.net/xietansheng/article/details/88389515
             */
            PrivateKey privateKey = getPrivateKey(path);
            Cipher cipher = Cipher.getInstance("RSA");//java default: "RSA"="RSA/ECB/PKCS1Padding"
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            String sharedKey = Arrays.toString(cipher.doFinal(Base64.getDecoder().decode(encryptedSharedKey)));
            return sharedKey;
        } catch (Exception e) {
            log.warning("Decrypt shared key failed");
            log.warning(e.toString());
            return null;
        }
    }

    static String encryptMessage(String msg, String sharedKeyByteString) {
        String[] byteValues = sharedKeyByteString.substring(1, sharedKeyByteString.length() - 1).split(",");
        byte[] bytes = new byte[byteValues.length];
        for (int i = 0, len = bytes.length; i < len; i++) {
            bytes[i] = Byte.parseByte(byteValues[i].trim());
        }
        Key sharedKey = new SecretKeySpec(bytes, 0, bytes.length, "AES");
        try {
            /**
             * the main algorithm of decryption of massage is from
             * https://blog.csdn.net/xietansheng/article/details/88389515
             */
            Cipher cipher = Cipher.getInstance("AES");//java默认"RSA"="RSA/ECB/PKCS1Padding"
            cipher.init(Cipher.ENCRYPT_MODE, sharedKey);
            byte[] output = cipher.doFinal(msg.getBytes());
            String encrypteMsg = java.util.Base64.getEncoder().encodeToString(output);
            return encrypteMsg;
        } catch (Exception e) {
            log.warning("Encrypting the message with shared key failed");
            log.warning(e.toString());
            return null;
        }
    }

    static String decryptMessage(String encryptedMsg, String str) {
        String[] byteValues = str.substring(1, str.length() - 1).split(",");
        byte[] bytes = new byte[byteValues.length];
        for (int i = 0, len = bytes.length; i < len; i++) {
            bytes[i] = Byte.parseByte(byteValues[i].trim());
        }
        Key sharedKey = new SecretKeySpec(bytes, 0, bytes.length, "AES");
        try {
            /**
             * the main algorithm of encryption of massage is from
             * https://blog.csdn.net/xietansheng/article/details/88389515
             */
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, sharedKey);
            //BASE64Decoder decoder = new BASE64Decoder();
            byte[] c = java.util.Base64.getDecoder().decode(encryptedMsg);
            byte[] result = cipher.doFinal(c);
            String plainText = new String(result, StandardCharsets.UTF_8);
            return plainText;
        } catch (Exception e) {
            log.warning("Decrypting the message with shared key failed");
            log.warning(e.toString());
            return null;
        }
    }

    private static PublicKey getPublicKey(String identity) {
        String keys = Configuration.getConfigurationValue("authorized_keys").trim();
        String[] keylist = keys.split(",");

        boolean foundIdentity = false;
        String RSApublicKeyString = "";

        for (String rsaPubKey : keylist) {
            String[] keyComponent = rsaPubKey.split(" ");
            if (keyComponent[2].equals(identity)) {
                RSApublicKeyString = keyComponent[1];
                foundIdentity = true;
                break;
            }
        }

        if (!foundIdentity) {
            log.warning("Identity not found!");
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
        } catch (Exception e) {
            log.warning("Generating public key failed!");
            log.warning(e.toString());
            return null;
        }
    }

    private static byte[] readLengthFirst(InputStream in) throws IOException {
        int[] bytes = new int[]{in.read(), in.read(), in.read(), in.read()};
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

    private static PrivateKey getPrivateKey(String filename) {
        /**
         * Private conversion algorithm found at
         * https://stackoverflow.com/questions/3243018/how-to-load-rsa-private-key-from-file
         */
        try {
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
        } catch (Exception e) {
            log.warning("Generating private key failed!");
            log.warning(e.toString());
            return null;
        }
    }


//    public static void main(String[] args) throws Exception {
//		String Msg = "this is a test message";
//		String sharedKey = getSharedKey();
//		log.info("sent: " + sharedKey);
//		String s = encryptSharedKey("zhouguozhi@xuandeMacBook-Air.local",sharedKey);
//		//System.out.println(s);
//		String str = decryptSharedKey(s,"id_rsa");
//		log.info("received shared Key: " + str);
//		String TestMsg = encryptMessage(Msg, str);
//		//log.info(TestMsg);
//		String RMsg = decryptMessage(TestMsg, str);
//		System.out.println(RMsg);
//	}

}


