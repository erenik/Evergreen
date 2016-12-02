package erenik.seriousgames.evergreen.auth;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class dangel_auth {

    
    public static String encrypt(String key, String initVector, String value) {
        try {
            // Initiate the objects that creates ciphering instance (IV and secret key)
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            // Create ciphering instance
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            // set encryption mode
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            // Performs actual encryption
            byte[] encrypted = cipher.doFinal(value.getBytes());
            // Convert to Base64, as some generated symbols (hex-codes) may not be displayable usually
            // Base64 format makes all symbols displayable.
            String encroded = Base64.getEncoder().encodeToString(encrypted);
            // return the base64 encoded, encrypted content
            return encroded;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String key, String initVector, String encrypted) {
        try {
            // Initiate the objects that creates ciphering instance (IV and secret key)
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            // Create ciphering instance
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            // set decryption mode
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            // decode from base64, and then use instance to decrypt
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            // return the original string (pw?)
            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) {
        // STATICALLY defines key and IV right now, this has to be changed before implementation obv
        String key = "Dangel12Dangel12"; // 128 bit key
        String initVector = "RandomInitVector"; // 16 bytes IV
        // Executes the code, hopefully works without errors (may need minor modification)
        String encrypted = encrypt(key, initVector, "Fear the Danglor");
        System.out.println("ENCRYPTED: " + encrypted);
        System.out.println("DECRYPTED: " + decrypt(key, initVector, encrypted));
    }

}
