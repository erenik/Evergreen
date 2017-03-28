package erenik.weka.transport;


import erenik.util.EList;

/**
 * A frame of time for sensing which is later analyzed.
 * Sensors include magnitude of acc. and gyroscope.
 * Calculations include avg, min, max and stddev on both.
 * Created by Emil on 2017-02-23.
 */

public class SensingFrame {
    EList<MagnitudeData> accMagns = new EList<>(),
        gyroMagns = new EList<>();

    /// When transport has not been determined?
    SensingFrame(){
        startTimeMs = System.currentTimeMillis();
        durationMs = 5000;
    }
    SensingFrame(String transport){
        startTimeMs = System.currentTimeMillis();
        durationMs = 5000;
        transportString = transport;
    }
    public long StartTimeMs(){
        return startTimeMs;
    };
    // Stored on creation, System.currentTimeMillis();
    long startTimeMs;
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
        return "AccS "+accMagns.size()+" GyroS "+gyroMagns.size()+" "+(startTimeMs/1000)+","+accString(",")+","+gyroString(",")+","+transportString+"\n";
    }
    public String shortString(){
        return "AccAvg: "+accAvg+", GyroAvg: "+gyroAvg+" Transport: "+transportString;
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
