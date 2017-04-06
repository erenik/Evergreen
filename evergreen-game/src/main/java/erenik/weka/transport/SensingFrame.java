package erenik.weka.transport;


import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import erenik.util.EList;

/**
 * A frame of time for sensing which is later analyzed.
 * Sensors include magnitude of acc. and gyroscope.
 * Calculations include avg, min, max and stddev on both.
 * Created by Emil on 2017-02-23.
 */

public class SensingFrame implements Serializable {
    private static final long serialVersionUID = 1L;

    EList<MagnitudeData> accMagns = new EList<>(),
        gyroMagns = new EList<>();

    int postPoned = 0;

    // Serializable
    static int version = 0;
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeLong(startTimeSystemMs);
        out.writeInt(durationMs);
        out.writeFloat(accMin);
        out.writeFloat(accMax);
        out.writeFloat(accAvg);
        out.writeFloat(accStdev);
        out.writeFloat(gyroMin);
        out.writeFloat(gyroMax);
        out.writeFloat(gyroAvg);
        out.writeFloat(gyroStdev);
        out.writeObject(transportString);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        startTimeSystemMs = in.readLong();
        durationMs = in.readInt();
        accMin = in.readFloat();
        accMax = in.readFloat();
        accAvg = in.readFloat();
        accStdev = in.readFloat();
        gyroMin = in.readFloat();
        gyroMax = in.readFloat();
        gyroAvg = in.readFloat();
        gyroStdev = in.readFloat();
        transportString = (String) in.readObject();
    }
    private void readObjectNoData() throws ObjectStreamException
    {}

    /// When transport has not been determined?
    SensingFrame(){
        durationMs = 5000;
        InitArrays();
    }

    SensingFrame(String transport){
//        startTimeSensorMs = startTimeSystemMs = 0;
        durationMs = 5000;
        transportString = transport;
        InitArrays();
    }
    private void InitArrays() {
        accMagns = new EList<>();
        gyroMagns = new EList<>();
    }

    public long StartTimeSystemMs(){
        return startTimeSystemMs;
    };
    // Stored on creation, System.currentTimeMillis();
    long startTimeAccSensorMs = 0, // Start-time as defined by the sensor.
        startTimeGyroSensorMs = 0,
        startTimeSystemMs = 0; // Start-time as defined by the System.currentTimeMillis().
    /// Default 5000
    int durationMs = 5000;
    float accMin, accMax, accAvg, accStdev,
        gyroMin, gyroMax, gyroAvg, gyroStdev;

    String transportString = "";

    void CalcStats(){
        if (accMagns.size() > 0) {
            accMin = Min(accMagns);
            accMax = Max(accMagns);
            accAvg = Avg(accMagns);
            accStdev = Stddev(accMagns, accAvg);
        }
        if (gyroMagns.size() > 0) {
            gyroMin = Min(gyroMagns);
            gyroMax = Max(gyroMagns);
            gyroAvg = Avg(gyroMagns);
            gyroStdev = Stddev(gyroMagns, gyroAvg);
        }
    }
    void ClearMagnitudeData(){
        accMagns = null;
        gyroMagns = null;
    }

    float Min(EList<MagnitudeData> data){
        float min = data.get(0).magnitude;
        for (int i = 1; i < data.size(); ++i)
            if (data.get(i).magnitude < min)
                min = data.get(i).magnitude;
        return min;
    }

    float Max(EList<MagnitudeData> data){
        float max = data.get(0).magnitude;
        for (int i = 1; i < data.size(); ++i)
            if (data.get(i).magnitude > max)
                max = data.get(i).magnitude;
        return max;
    }
    float Avg(EList<MagnitudeData> data){
        float tot = data.get(0).magnitude;
        for (int i = 1; i < data.size(); ++i)
            tot += data.get(i).magnitude;
        return tot / data.size();
    }
    float Stddev(EList<MagnitudeData> data, float avg){
        int totalDiffs = 0;
        for(int i = 0; i < data.size(); i++){
            float diff = data.get(i).magnitude - avg;
            totalDiffs += diff * diff; // this is the calculation for summing up all the values
        }
        return (float) Math.sqrt(totalDiffs);
    }
    public String toString(){
        if (accMagns != null && gyroMagns != null)
            return "AccS "+accMagns.size()+" GyroS "+gyroMagns.size()+" "+(startTimeSystemMs/1000)+","+accString(",")+","+gyroString(",")+","+transportString+"\n";
        return shortString();
    }
    public String longString(){
        if (transportString.length() <= 1)
            return "To be evaluated";
        // String.format("%.1f", values[i])
        return "AccAvg: "+String.format("%.2f", accAvg)+", GyroAvg: "+String.format("%.2f", gyroAvg)+" Transport: "+transportString;
    }
    public String shortString(){
        if (transportString.length() <= 1)
            return "To be evaluated";
        // String.format("%.1f", values[i])
        return "AA: "+String.format("%.2f", accAvg)+", GA: "+String.format("%.2f", gyroAvg)+" T: "+transportString;
    }
    public String accString(String glue) {
        return accMin+glue+accMax+glue+accAvg+glue+accStdev;
    }
    public String gyroString(String glue) {
        return gyroMin+glue+gyroMax+glue+gyroAvg+glue+gyroStdev;
    }

    public static String CSVHeaders() {
        return "startTimeSecond,accMin,accMax,accAvg,accStdev,gyroMin,gyroMax,gyroAvg,gyroStdev,transport";
    }
}
