package erenik.weka.transport;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import erenik.util.EList;
import erenik.weka.WClassifier;
import erenik.weka.WekaManager;

/**
 * Android-based service.
 * Created by Emil on 2017-03-04.
 */

public class TransportDetectionService extends Service {

    private static TransportDetectionService instance = null;
    WekaManager wekaMan = null;
    TransportDetectorThread dataSamplerThread = null;

    public EList<SensingFrame> GetLastSensingFrames(int maxNum){
        EList<SensingFrame> sfs = new EList<>();
        for (int i = dataSamplerThread.sensingFrames.size() - 1; i >= 0; --i){
            SensingFrame frame = new SensingFrame();
            SensingFrame sf2 = dataSamplerThread.sensingFrames.get(i);
            frame.startTimeMs = sf2.startTimeMs;
            frame.accAvg = sf2.accAvg;
            frame.gyroAvg = sf2.gyroAvg;
            frame.transportString = sf2.transportString;
            sfs.add(frame);
            if (sfs.size() >= maxNum)
                return sfs; // Return early.. lol.
        }
        return sfs;
    }

    public void SetHistorySetSize(int newSize){
        historySetSize = newSize;
        System.out.println("History set size: "+historySetSize);
    }
    public void SetSleepSessions(int newVal) {
        sleepSessions = newVal;
        System.out.println("Sleep sessions: "+sleepSessions);
    }

    int sleepSessions = 3;
    int historySetSize = 3; // Do 5 samples, calc average
    boolean forceAverageBeforeSleep = true; // Then sleep, if true.

    public WClassifier classifier = null,
        accOnlyClassifier = null,
        gyroOnlyClassifier = null;
//    private EList<TransportOccurrence> transportOccurrences = new EList<>(); // All transport frame IDs
    public int msTotalTimeAnalyzedSinceThreadStart = 0; // o-o

    /// All transport data combined!
    TransportData transportData;

    /// Limit for storing sensing frames. This is also the limit that is applied to converting one sensing frame into a time-frame.
    int secondLimitSensingFrames;
    public boolean sleeping = false; // When false, is sleeping?


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
        instance = this;
    }

    @Override
    public void onDestroy() {
        System.out.println("onDestroy called D:");
        Save();
        super.onDestroy();
        instance = null;
    }

    /// Save to preferences.
    private static final String preferencesFileName = "TransportDetectionData.save";
    boolean Save(){
        ObjectOutputStream objectOut = null;
        FileOutputStream fileOut = null;
        try {
            fileOut = openFileOutput(preferencesFileName, Activity.MODE_PRIVATE);
            objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(transportData);
            fileOut.getFD().sync();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close();
                } catch (IOException e2) {
                    // do nowt
                    System.out.println("Failed to save");
                    return false;
                }
            }
        };
        return true;
    }
    /// Load from preferences.
    boolean LoadSavedData(){
        ObjectInputStream objectIn = null;
        FileInputStream fileIn = null;
        try {
            fileIn = getBaseContext().openFileInput(preferencesFileName);
            objectIn = new ObjectInputStream(fileIn);
            transportData = (TransportData) objectIn.readObject();
//            transportData.PrintAllData();
            fileIn.getFD().sync();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e2) {
                    // do nowt
                    System.out.println("Failed to save");
                    return false;
                }
            }
        };
        return true;
    }

    private void StartDetection() {
        /// Load data if saved, assuming the service had been killed earlier?
        LoadSavedData();
        if (transportData == null)
            transportData = TransportData.BuildPrimaryTree();

        // Create it.
        if (wekaMan == null)
            wekaMan = new WekaManager();
        // Spawn thread to gather samples!
        if (dataSamplerThread == null) {
            dataSamplerThread = new TransportDetectorThread(this);
            dataSamplerThread.start();
        }
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
            // New minute?
            if (minutes % 3 == 0){
                PrintData("Last 3 minutes: ", GetDataSeconds(180));
            }
            if (minutes % 5 == 0){
                PrintData("Last 5 minutes: ", GetDataSeconds(300));
            }
            if (minutes % 10 == 0){
                PrintData("Last 10 minutes: ", GetDataSeconds(600));
            }
            if (minutes % 15 == 0){
                PrintData("Last 15 minutes: ", GetDataSeconds(900));
            }
        }
        if (lastHour != hour){
            PrintData("Last hour: ", GetDataSeconds(3600));
            lastHour = hour;
        }
    }

    private void PrintData(String headerText, EList<TransportOccurrence> transportOccurrences) {
        System.out.println(headerText+" nr samples: "+transportOccurrences.size());
        // New array of 0s for each transport.
        EList<TransportOccurrence> totalTransportDurationUsages = GetTotalStatsForData(transportOccurrences);
        for (int i = 0; i < totalTransportDurationUsages.size(); ++i){
            TransportOccurrence to = totalTransportDurationUsages.get(i); // Just print it.
            System.out.println(" "+to.transport.name()+" # "+to.DurationSeconds()+"s, % "+to.ratioUsed);
        }
    }


    private EList<TransportOccurrence> GetDataSeconds(int nrOfSecondsToInclude) {
        EList<TransportOccurrence> newArr = new EList<>();
        long nowMs = System.currentTimeMillis();
        long msToInclude = nrOfSecondsToInclude * 1000;
        long thresh = nowMs - msToInclude;
        EList<TransportOccurrence> transportOccurrences = transportData.GetDataSeconds(nrOfSecondsToInclude);
        for (int i = transportOccurrences.size() - 1; i >= 0; --i){ // Search from the newest occurrence of data and backwards, grab all where start time is after the threshold period.
            TransportOccurrence to = transportOccurrences.get(i);
            if (to.startTimeMs < thresh)
                break; // Break loop if too old data.
            newArr.add(to);
        }
        return newArr;
    }

    public boolean ShouldSleep() {
        if (sleepSessions <= 0)
            return false;
        if (forceAverageBeforeSleep){
            if (classifier.valuesHistory.size() >= this.historySetSize){
                // Do sleep.
                return true;
            }
        }
        else
            return true;
        return false;
    }

    public static TransportDetectionService GetInstance() {
        return instance;
    }

    public String GetStatus() {
        return sleeping? "Sleeping" : "Active";
    }

    public String CurrentTransport() {
        TransportOccurrence lastEntry = transportData.LastEntry();
        if (lastEntry == null)
            return "None";
        return lastEntry.transport.name();
    }

    private EList<TransportOccurrence> GetTotalStatsForData(EList<TransportOccurrence> transportOccurrences) {
        // New array of 0s for each transport.
        EList<TransportOccurrence> totalTransportDurationUsages = new EList<>();
        int durationTotalMs = 0;
        /// Create a new counter for each transport.
        for (int i = 0; i < TransportType.values().length; ++i){
            TransportOccurrence occ = new TransportOccurrence(TransportType.values()[i], 0, DurationType.Milliseconds);
            totalTransportDurationUsages.add(occ);
        }
        // Increment.
        for (int i = transportOccurrences.size() - 1; i >= 0; --i){
            TransportOccurrence to = transportOccurrences.get(i);
            totalTransportDurationUsages.get(to.transport.ordinal()).AddMillis(to.DurationMillis());
            durationTotalMs += to.DurationMillis();
        }
        // Calculate ratio of them used and remove those whose duration is 0.
        for (int i = 0; i < totalTransportDurationUsages.size(); ++i){
            TransportOccurrence to = totalTransportDurationUsages.get(i);
            if (to.DurationMillis() == 0) {
                totalTransportDurationUsages.remove(i);
                --i;
                continue;
            }
            to.ratioUsed = to.DurationMillis() / (float) durationTotalMs;
            //        System.out.println(" "+classifier.trainingData.classAttribute().value(i)+" # "+to.durationMs+"ms, % "+to.ratioUsed);
        }
        return totalTransportDurationUsages;
    }


    /// Returns an array with one entry for each transport, that holds sums of the duration of each transport, as well as the ratio of each within.
    public EList<TransportOccurrence> GetTotalStatsForDataSeconds(long dataSecondsToAnalyze) {
        long dataMillisecondsToAnalyze = dataSecondsToAnalyze * 1000;
        long nowMs = System.currentTimeMillis();
        EList<TransportOccurrence> transportOccurrences = transportData.GetDataSeconds(nowMs - dataMillisecondsToAnalyze);
        return GetTotalStatsForData(transportOccurrences);
    }

    public String GetTransportString(int value) {
        if (classifier == null || classifier.trainingData == null)
            return "Null";
        int numValues = classifier.trainingData.classAttribute().numValues();
        if (value < 0 || value >= numValues)
            return "";
        return classifier.trainingData.classAttribute().value(value);
    }

    public void AddTransportOccurrence(TransportOccurrence transportOccurrence) {
        transportData.AddData(transportOccurrence, null); // Add it to the object which will be presisted between sessions and interruptions.
    }

}
