package erenik.weka.transport;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by Emil on 2017-03-06.
 */

public class TransportOccurrence implements Serializable {
    TransportOccurrence(){}
    TransportOccurrence(TransportType transport, long startTimeMs, int durationMs){
        this.transport = transport;
        this.startTimeMs = startTimeMs;
        this.durationMs = durationMs;
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(transport);
        out.writeLong(startTimeMs);
        out.writeLong(durationMs);
        out.writeFloat(ratioUsed);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        transport = (TransportType) in.readObject();
        startTimeMs = in.readLong();
        durationMs = in.readLong();
        ratioUsed = in.readFloat();
    }

    public TransportType transport;
    long startTimeMs; // Time stamp when the measuring window started.
    public long durationMs; // Duration in milliseconds for this occurrence.
    public float ratioUsed; // percentage, decimal form 0.0 to 1.0
}
