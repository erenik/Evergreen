package erenik.weka.transport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.sql.Array;
import java.util.ArrayList;

import erenik.util.Tuple;
import erenik.weka.WClassifier;
import erenik.weka.WekaManager;
import weka.classifiers.trees.RandomForest;

/**
 * Android-based service.
 * Created by Emil on 2017-03-04.
 */

public class TransportDetectionService extends Service {

    WekaManager wekaMan = null;
    TransportDetectorThread dataSamplerThread = null;

    private SensingFrame sensingFrame = new SensingFrame("");
    ArrayList<SensingFrame> sensingFrames = new ArrayList<>(); // Past sensing frames.
    public WClassifier classifier = null;
    public ArrayList<TransportOccurrence> transportOccurrences = new ArrayList<>(); // All transport frame IDs
    public int msPerFrame; // Milliseconds per frame stored in the array above (transportOccurrences).
    public int msTotalTimeAnalyzedSinceThreadStart = 0; // o-o

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /// Uhhh.. Idk.
        System.out.println("onStartCommand");
        StartDetection();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.exit(4); // No binding for now. Only local usage.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("TransportDetectionService onCreate");
//        StartDetection();
    }

    private void StartDetection() {
        // Create it.
        if (wekaMan == null)
            wekaMan = new WekaManager();
        // Spawn thread to gather samples!
        dataSamplerThread = new TransportDetectorThread(this);
        dataSamplerThread.start();

        // Load a saved model for prediction?
        // Or generate it?
//        wekaMan.

        // Callback to re-start data-sampler thread if it has stopped?
    }

    static int lastMinute = 0, lastHour = 0, lastDay = 0;

    public void RecalcAverages() {
        // Check the array.
//        msTotalTimeAnalyzedSinceThreadStart;
        int seconds = msTotalTimeAnalyzedSinceThreadStart / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        int days = hours / 24;
        int weeks = days / 7;
        int day = days - weeks * 7,
                hour = hours - days * 24,
                minute = minutes - hours * 60,
                second = seconds - minutes * 60;

        if (lastMinute != minute){
            PrintData("Last minute: ", GetDataSeconds(60));            // Present statistics for the last minute?
            lastMinute = minute;
        }
        if (lastHour != hour){
            PrintData("Last hour: ", GetDataSeconds(3600));
            lastHour = hour;
        }
    }

    private void PrintData(String headerText, ArrayList<TransportOccurrence> transportOccurrences) {
        System.out.println(headerText+" nr samples: "+transportOccurrences.size());
        int[] transports = new int[10];
        for (int i = 0; i < transportOccurrences.size(); ++i){
            TransportOccurrence to = transportOccurrences.get(i);
            ++transports[to.transport];
        }
        for (int i = 0; i < transports.length; ++i){
            if (transports[i] == 0)
                continue;
            System.out.println(" "+classifier.trainingData.classAttribute().value(i)+" # "+transports[i]+" % "+transports[i]/(float)transportOccurrences.size());
        }
    }

    private ArrayList<TransportOccurrence> GetDataSeconds(int nrOfSecondsToInclude) {
        ArrayList<TransportOccurrence> newArr = new ArrayList<>();
        long nowMs = System.currentTimeMillis();
        long msToInclude = nrOfSecondsToInclude * 1000;
        long thresh = nowMs - msToInclude;
        for (int i = transportOccurrences.size() - 1; i >= 0; --i){
            TransportOccurrence to = transportOccurrences.get(i);
            if (to.timeStampMs < thresh)
                break; // Break loop if too old data.
            newArr.add(to);
        }
        return newArr;
    }
}
