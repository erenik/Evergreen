package erenik.weka.transport;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by Emil on 2017-03-06.
 */

public class TransportOccurrence implements Serializable {
    TransportOccurrence(){}

    public TransportOccurrence(TransportType transport, long duration, DurationType dt){
        this.transport = transport;
        this.duration = duration;
        this.dt = dt;
    }

    TransportOccurrence(TransportType transport, long startTimeMs, long durationMs){
        this.transport = transport;
        this.startTimeMs = startTimeMs;
        this.duration = durationMs;
        this.dt = DurationType.Milliseconds;
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(transport);
        out.writeLong(startTimeMs);
        out.writeLong(duration);
        out.writeFloat(ratioUsed);
        out.writeInt(dt.ordinal());
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        transport = (TransportType) in.readObject();
        startTimeMs = in.readLong();
        duration = in.readLong();
        ratioUsed = in.readFloat();
        dt = DurationType.values()[in.readInt()]; // Read in the ordinal and use it as index.
    }

    public TransportType transport;
    long startTimeMs; // Time stamp when the measuring window started.
    public long duration; // Duration in milliseconds for this occurrence.
    public DurationType dt = DurationType.Unknown; // Default milliseconds?
    public float ratioUsed; // percentage, decimal form 0.0 to 1.0

    public long DurationMinutes() {
        return DurationSeconds() / 60;
    }

    public long DurationSeconds() {
        switch (dt){
            case Milliseconds: return duration / 1000;
            case Seconds: return duration;
            case Minutes: return duration * 60;
        }
        return 0;
    }

    public long DurationMillis() {
        switch (dt) {
            case Milliseconds: return duration;
            case Seconds: return duration * 1000;
            case Minutes: return duration * 1000 * 60;
        }
        return 0;
    }

    public void AddMillis(long millis) {
        switch (dt){
            case Milliseconds: duration += millis; break;
            default:
                System.out.println("Bad dt");
                System.exit(3);
        }
    }
}
