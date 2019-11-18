package evergreen.common;

import java.io.IOException;
import java.io.Serializable;

/**
 * Simple integer enumerator, based on long.
 * Created by Emil on 2017-03-23.
 */
public class Enumerator implements Serializable{
    public long value = 0;
    private static final long serialVersionUID = 1L;

    public void writeTo(java.io.ObjectOutputStream out) throws IOException {
        out.writeLong(value);
    }
    public void readFrom(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        value = in.readLong();
    }
}