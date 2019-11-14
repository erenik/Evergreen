package erenik.seriousgames.evergreen.auth;

import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import erenik.util.Printer;
// import java.util.Base64;

public class dangel_auth {


    /// If converting from strng, use .getBytes("UTF-8") probably.
    public static byte[] encrypt(byte[] key, byte[] initVector, byte[] value) {
        try {
            // Initiate the objects that creates ciphering instance (IV and secret key)
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            // Create ciphering instance
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            // set encryption mode
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            // Performs actual encryption
            byte[] encrypted = cipher.doFinal(value);
            // Convert to Base64, as some generated symbols (hex-codes) may not be displayable usually
            // Base64 format makes all symbols displayable.
//            String encroded = Base64.getEncoder().encodeToString(encrypted);
            // return the base64 encoded, encrypted content
            return encrypted;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /// Use .getBytes("UTF-8") if needed.
    public static byte[] decrypt(byte[] key, byte[] initVector, byte[] encrypted) {
        try {
            // Initiate the objects that creates ciphering instance (IV and secret key)
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            // Create ciphering instance
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            // set decryption mode
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            // decode from base64, and then use instance to decrypt
            byte[] original = cipher.doFinal(encrypted);
            // return the original string (pw?)
            return original;
            // new String(bytes) to get result later.
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        // STATICALLY defines key and IV right now, this has to be changed before implementation obv
        String key = "Dangel12Dangel12"; // 128 bit key
        String initVector = "RandomInitVector"; // 16 bytes IV
        // Executes the code, hopefully works without errors (may need minor modification)
        String testString = "Fear the Danglor";
        byte[] encrypted = encrypt(key.getBytes("UTF-8"), initVector.getBytes(("UTF-8")), testString.getBytes(("UTF-8")));
        System.out.print("Encrypted: ");
        for (int i = 0; i < encrypted.length; ++i)
        {
            System.out.print(" "+(int) encrypted[i]);
        }
        Printer.out();
        Printer.out("ENCRYPTED: " + encrypted);
        byte[] decrypted = decrypt(key.getBytes(("UTF-8")), initVector.getBytes(("UTF-8")), encrypted);
        String outputString = new String(decrypted);
        Printer.out("DECRYPTED: " + outputString);
        if (outputString.equals(testString))
        {
            Printer.out("EQUALS");
        }
        else
            Printer.out("NOT EQUALSSS!!!!!");

    }

}
