package weka.transport;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import evergreen.util.EList;
import evergreen.util.Printer;
import weka.Settings;
import weka.WClassifier;
import weka.WekaManager;

/**
 * Android-based service.
 * Created by Emil on 2017-03-04.
 */

public class TransportDetectionService extends Service {

    /** use with putExtra int when requesting the service.
     * Available commands are "StartService" which just starts data-collection.
     * "GetTotalStatsForDataSeconds" - which requests data for given amount of seconds - must be specified in a separate putExtra int with DATA_SECONDS
    */
    public static final String REQUEST_TYPE = "RequestType",
        DATA_SECONDS = "DataSeconds", // Argument, Long
            NUM_FRAMES = "NumFrames", // Argument, Int
            SERIALIZABLE_DATA = "SerializableData"; // Argument, Serializable
    public static final int START_SERVICE = 0, // Request-types.
        GET_TOTAL_STATS_FOR_DATA_SECONDS = 1,
            GET_LAST_SENSING_FRAMES = 2;

    private static TransportDetectionService instance = null;
    WekaManager wekaMan = null;
    static TransportDetectorThread dataSamplerThread = null;
    private int totalSensingFramesGathered = 0;
    private int sensingFramesSinceLastSleep = 0;
    /// For requests via Intents. If non-null, send a broadcast with the data for the given seconds and reset this to 0.
    long totalStatSecondsRequested = 0;
    int numLastSensingFramesRequested = 0;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Printer.out("Received: "+intent);
            String status = intent.getStringExtra("STATUS");
            Printer.out("Status: "+status);
            intent.getSerializableExtra("");
        }
    };

    public Settings settings = new Settings();
    // Yeahhh.
    private boolean forceAverageBeforeSleep = true;

    public void SetHistorySetSize(int newSize){
        newSize = 36;
        settings.historySetSize = newSize;
        Printer.out("History set size: "+settings.historySetSize);
    }
    public void SetSleepSessions(int newVal) {
        // Check if the sleep-sessions value is supported.
        // For now, hard-coded.
        newVal = 36;
        settings.sleepSessions = newVal;
        Printer.out("Sleep sessions: "+settings.sleepSessions);
    }

//    int sleepSessions = 12; // 1 minute sleep
  //  int historySetSize = 12; // 1 minute sampling
    //boolean forceAverageBeforeSleep = true; // Then sleep, if true.

//    private EList<TransportOccurrence> transportOccurrences = new EList<>(); // All transport frame IDs
    public int msTotalTimeAnalyzedSinceThreadStart = 0; // o-o

    /// All transport data combined!
    TransportData transportData;

    /// Limit for storing sensing frames. This is also the limit that is applied to converting one sensing frame into a time-frame.
    int secondLimitSensingFrames;
    public boolean sleeping = false; // When false, is sleeping?


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null){ // Wtf even?
            return super.onStartCommand(intent, flags, startId);
        }
        /// Uhhh.. Idk.
        int cmd = intent.getIntExtra(TransportDetectionService.REQUEST_TYPE, -1);
        if (cmd == -1){
            Printer.out("Lacking request type int extra.");
            new Exception().printStackTrace();
            return super.onStartCommand(intent, flags, startId);
        }
//        Printer.out("onStartCommand: "+cmd);
        switch (cmd)
        {
            case START_SERVICE:
                StartDetection();
                Printer.out("Starting up TransportDetectionService");
                break;
            case GET_TOTAL_STATS_FOR_DATA_SECONDS:
                totalStatSecondsRequested = intent.getLongExtra(TransportDetectionService.DATA_SECONDS, 0);
            //    Printer.out("Data for "+totalStatSecondsRequested+" requested.");
                break;
            case GET_LAST_SENSING_FRAMES:
                numLastSensingFramesRequested = intent.getIntExtra(TransportDetectionService.NUM_FRAMES, 0);
  //              Printer.out("Requested "+numLastSensingFramesRequested+" last sensing frames");
                break;
        }

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
        Printer.out("TransportDetectionService onCreate");
        StartDetection();
        instance = this;
    }

    @Override
    public void onDestroy() {
        Printer.out("onDestroy called D:");
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
                    Printer.out("Failed to save");
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
        } catch (FileNotFoundException fnfe){
            Printer.out("File not found, loading saved data failed.");
//            fnfe.printStackTrace();
            return false;
        }
        catch(InvalidClassException ice){
            ice.printStackTrace();
            return false;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
        finally {
            if (objectIn != null) {
                try {
                    objectIn.close();
                } catch (IOException e2) {
                    // do nowt
                    Printer.out("Failed to save");
                    return false;
                }
            }
        };
        return true;
    }

    private void StartDetection() {

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

    // Does what..?
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
        Printer.out(headerText+" nr samples: "+transportOccurrences.size());
        // New array of 0s for each transport.
        EList<TransportOccurrence> totalTransportDurationUsages = GetTotalStatsForData(transportOccurrences);
        for (int i = 0; i < totalTransportDurationUsages.size(); ++i){
            TransportOccurrence to = totalTransportDurationUsages.get(i); // Just print it.
            Printer.out(" "+to.transport.name()+" # "+to.DurationSeconds()+"s, % "+to.ratioUsed);
        }
    }


    EList<TransportOccurrence> GetDataSeconds(long nrOfSecondsToInclude) {
        EList<TransportOccurrence> newArr = new EList<>();
        long nowMs = System.currentTimeMillis();
        long msToInclude = nrOfSecondsToInclude * 1000;
        long threshMs = nowMs - msToInclude;
        EList<TransportOccurrence> transportOccurrences = transportData.GetDataSeconds(nrOfSecondsToInclude);
        Printer.out("transportOccurrences: "+transportOccurrences.size()+" corresponding to: "+TransportOccurrence.TotalTimeMs(transportOccurrences)+"ms");
        for (int i = transportOccurrences.size() - 1; i >= 0; --i){ // Search from the newest occurrence of data and backwards, grab all where start time is after the threshold period.
            TransportOccurrence to = transportOccurrences.get(i);
            long timeDiff = to.startTimeSystemMs - threshMs;
            if (timeDiff < 0) {
                Printer.out("Data " + timeDiff + "ms too old");
                break; // Break loop if too old data.
            }
            newArr.add(to);
        }
        return newArr;
    }

    public boolean ShouldSleep() {
        if (settings.sleepSessions <= 0)
            return false;
        if (forceAverageBeforeSleep){
            if (sensingFramesSinceLastSleep >= settings.historySetSize){
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

    EList<TransportOccurrence> GetTotalStatsForData(EList<TransportOccurrence> transportOccurrences) {
        // New array of 0s for each transport.
        EList<TransportOccurrence> totalTransportDurationUsages = new EList<>();
        int durationTotalMs = 0;
        int detectionMethodUsed = TransportOccurrence.GetMostUsedDetectionMethod(transportOccurrences);
        /*
        if (transportOccurrences.size() > 0){
            detectionMethodUsed = transportOccurrences.get(0).detectionMethodUsed; // Grab the first one in the array?
        }
        */

        /// Create a new counter for each transport.
        for (int i = 0; i < TransportType.values().length; ++i){
            TransportOccurrence occ = new TransportOccurrence(TransportType.values()[i], 0, DurationType.Milliseconds, detectionMethodUsed);
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
            //        Printer.out(" "+classifier.trainingData.classAttribute().value(i)+" # "+to.durationMs+"ms, % "+to.ratioUsed);
        }
        return totalTransportDurationUsages;
    }


    /// Returns an array with one entry for each transport, that holds sums of the duration of each transport, as well as the ratio of each within.
    EList<TransportOccurrence> GetTotalStatsForDataSeconds(long dataSecondsToAnalyze) {
        long dataMillisecondsToAnalyze = dataSecondsToAnalyze * 1000;
        long nowMs = System.currentTimeMillis();
        EList<TransportOccurrence> transportOccurrences = transportData.GetDataSeconds(nowMs - dataMillisecondsToAnalyze);
        return GetTotalStatsForData(transportOccurrences);
    }

    public static String GetTransportString(int value) {
        return dataSamplerThread.GetTransportString(value);
    }

    TransportOccurrence lastAdded = null;
    void AddTransportOccurrence(TransportOccurrence transportOccurrence) {
        lastAdded = transportOccurrence;
        transportData.AddData(transportOccurrence, null); // Add it to the object which will be presisted between sessions and interruptions.
    }

    void OnSensingFrameFinished() {
        ++totalSensingFramesGathered;
        ++sensingFramesSinceLastSleep;
    }

    void OnSleep() {
        sleeping = true; // Flag as sleeping
        sensingFramesSinceLastSleep = 0;
    }

    public int LastAddedTransportOccurrenceDetectionMethod() {
        if (lastAdded == null)
            return TransportOccurrence.NOT_CLASSIFIED_YET;
        return lastAdded.detectionMethodUsed;
    }
}
