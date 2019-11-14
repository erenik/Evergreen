package erenik.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import erenik.util.EList;
import java.util.logging.Level;
import java.util.logging.Logger;

import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.Log;

/**
 * Created by Emil on 2017-03-23.
 */

public class Byter {
    public static byte[] toByteArray(Object obj) {
        try {
            ObjectOutputStream oos = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            return baos.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    public static Object toObject(byte[] bytes) {
        try {
            ObjectInputStream ois = null;
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();
            ois.close();
            return obj;
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static ObjectInputStream getObjectInputStream(byte[] bytes) {
        try {
            ObjectInputStream ois = null;
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bais);
            return ois;
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /*
    public static byte[] GlueBytes(byte[] bytes, byte[] bytes1, byte[] bytes2) {
        try {
            ObjectOutputStream oos = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.write(bytes);
            oos.write();
            oos.writeObject(obj);
            oos.close();
            return baos.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }*/
}
