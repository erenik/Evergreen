package erenik.evergreen.common;

import java.io.IOException;
import java.io.Serializable;

/**
 * Simple integer enumerator, based on long.
 * Created by Emil on 2017-03-23.
 */
public class Enumerator implements Serializable{
    public long value = 0;
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeLong(value);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        value = in.readLong();
    }
}