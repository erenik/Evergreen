package erenik.util;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Emil on 2017-03-25.
 */

public interface ESerializable {
    public ESerializable Construct();
    boolean writeTo(ObjectOutputStream out);
    boolean readFrom(ObjectInputStream out);
}
