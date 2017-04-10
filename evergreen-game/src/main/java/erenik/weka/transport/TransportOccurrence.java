package erenik.weka.transport;

import java.io.IOException;
import java.io.Serializable;

import erenik.util.EList;
import erenik.util.Printer;

/**
 * Created by Emil on 2017-03-06.
 */

public class TransportOccurrence implements Serializable {
    private static final long serialVersionUID = 1L;

    TransportOccurrence(){}

    public TransportOccurrence(TransportType transport, long duration, DurationType dt){
        this.transport = transport;
        this.duration = duration;
        this.dt = dt;
    }

    TransportOccurrence(TransportType transport, long startTimeSystemMs, long durationMs){
        this.transport = transport;
        this.startTimeSystemMs = startTimeSystemMs;
        this.duration = durationMs;
        this.dt = DurationType.Milliseconds;
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(transport);
        out.writeLong(startTimeSystemMs);
        out.writeLong(duration);
        out.writeFloat(ratioUsed);
        out.writeInt(dt.ordinal());
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        transport = (TransportType) in.readObject();
        startTimeSystemMs = in.readLong();
        duration = in.readLong();
        ratioUsed = in.readFloat();
        dt = DurationType.values()[in.readInt()]; // Read in the ordinal and use it as index.
    }

    public TransportType transport;
    long startTimeSystemMs; // Time stamp when the measuring window started.
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
                Printer.out("Bad dt");
                System.exit(3);
        }
    }

    public static long TotalTimeMs(EList<TransportOccurrence> transportOccurrences) {
        long totDur = 0;
        for (int i = 0; i < transportOccurrences.size(); ++i){
            totDur += transportOccurrences.get(i).DurationMillis();
        }
        return totDur;
    }
}
