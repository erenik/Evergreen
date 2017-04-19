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

    /// The detection methods.
    public static int NOT_CLASSIFIED_YET = 0,
            ACC_GYRO = 1,
        ACC_ONLY = 2,
            ACC_GYRO_RF_HSS_12_SS_12_DEFAULT = 3, // Random Forest, hss 24
        ACC_GYRO_RT_HSS_12_SS_12_AN_DEFAULT = 4, // Random Tree, hss 12, ss 12, acceleration values normalized.
        ACC_ONLY_RF_HSS_12_SS_12_DEFAULT = 5,
        ACC_ONLY_RT_HSS_12_SS_12_AN_DEFAULT = 6, // Random Tree, hss 12, ss 12, acceleration values normalized, only accelerometer values, gyroscope not functioning.
        ACC_GYRO_RT_HSS_36_SS_36_AN_DEFAULT = 7, // Random Tree, hss 12, ss 12, acceleration values normalized.
        ACC_ONLY_RT_HSS_36_SS_36_AN_DEFAULT = 8, // Random Tree, hss and ss 36, acceleration values normalized,
        DETECTION_METHODS = 9,


    NOT_LISTED_SETTING = 1000;

    TransportOccurrence(){}

    public TransportOccurrence(TransportType transport, long duration, DurationType dt, int detectionMethodUsed){
        this.transport = transport;
        this.duration = duration;
        this.dt = dt;
        this.detectionMethodUsed = detectionMethodUsed;
  //      PrintDetectionMethod();
    }

    TransportOccurrence(TransportType transport, long startTimeSystemMs, long durationMs, int detectionMethodUsed){
        this.transport = transport;
        this.startTimeSystemMs = startTimeSystemMs;
        this.duration = durationMs;
        this.dt = DurationType.Milliseconds;
        this.detectionMethodUsed = detectionMethodUsed;
//        PrintDetectionMethod();
    }

    private void PrintDetectionMethod() {
        Printer.out("TransportOccurent, DetectionMethod: "+detectionMethodUsed);
        if (detectionMethodUsed == 0){
            new Exception().printStackTrace();
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(transport);
        out.writeLong(startTimeSystemMs);
        out.writeLong(duration);
        out.writeFloat(ratioUsed);
        out.writeInt(dt.ordinal());
        out.writeInt(detectionMethodUsed);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        transport = (TransportType) in.readObject();
        startTimeSystemMs = in.readLong();
        duration = in.readLong();
        ratioUsed = in.readFloat();
        dt = DurationType.values()[in.readInt()]; // Read in the ordinal and use it as index.
        detectionMethodUsed = in.readInt();
    }

    public TransportType transport;
    long startTimeSystemMs; // Time stamp when the measuring window started.
    public long duration; // Duration in milliseconds for this occurrence.
    public DurationType dt = DurationType.Unknown; // Default milliseconds?
    public float ratioUsed; // percentage, decimal form 0.0 to 1.0
    public int detectionMethodUsed = ACC_GYRO;

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

    public static int GetMostUsedDetectionMethod(EList<TransportOccurrence> data) {
        long[] secondsInEach = new long[DETECTION_METHODS];
        int biggestIndex = 0;
        for (int i = 0; i < data.size(); ++i){
            int method = data.get(i).detectionMethodUsed;
            secondsInEach[method] += data.get(i).DurationSeconds();
            if (secondsInEach[method] > secondsInEach[biggestIndex]){
                biggestIndex = method;
            }
        }
        return biggestIndex; // Same as method by now.
    }

}
