package erenik.weka.transport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import erenik.evergreen.R;
import erenik.util.EList;
import erenik.weka.WClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.DenseInstance;
import weka.core.Instance;

/**
 * Created by Emil on 2017-03-04.
 */

public class TransportDetectorThread extends Thread implements SensorEventListener
{
    TransportDetectionService callingService;
    SensorManager sensorManager;

    public WClassifier classifier = null,
            accOnlyClassifier = null,
            gyroOnlyClassifier = null;

    private Sensor accSensor, gyroSensor;
    private SensingFrame sensingFrame;
    // List of ... what exactly.
    private EList<SensingFrame> sensingFrames = new EList<>();
    // List of frames which have been successfully Transport-classified. (including reaching sample density >50%)
    private EList<SensingFrame> finishedSensingFrames = new EList<>();

    public int herz = 20; // Samples per second.
    boolean stop = false;
    /// Update time for the sensing frame calculations. 5 seconds?
    int iterationDelayMs = 5000; // ms
//    Handler iterationHandler = new Handler(); // iteration handler

    int msPerIteration = 5000;

    /// All stored accelerometer-sensor points.
//    EList<MagnitudeData> accMagnPoints = new EList<>(),
  //          gyroMagnPoints = new EList<>();


    TransportDetectorThread(TransportDetectionService service){
        callingService = service;
        sensingFrames = new EList<>();
        sensingFrame = new SensingFrame();
    }

    SensingFrame lastSavedSF = null;

    @Override
    public void run() {
        System.out.println("TransportDetectorThread run");

        /// Launch Weka, tran it
        GetTrainingDataInputStream();

        sensingFrame = new SensingFrame();
        callingService.LoadSavedData();

        /// Set up sampling rate.
        EnableSensors();

        if (!LoadClassifiers()) {
            System.out.println("Creating and building classifiers...");
            classifier = callingService.wekaMan.NewRandomForest("RandomForest", GetTrainingDataInputStream(), false);
            accOnlyClassifier = callingService.wekaMan.NewRandomForest("RandomForest Acc only", GetTrainingDataInputStream(), true);
        }
        else {
            System.out.println("Classifiers loaded.");
        }



        /// Set up handler for iterated samplings. // Better just do it in the loop, though..?
        /*
        iterationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Iterate();
            }
        }, iterationDelayMs);
*/
        // Sample until told to stop.
        while (true){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ClassifyInstances();
            System.out.println("Iter Second "+((System.currentTimeMillis() / 1000) % 60));
            /// Check how many occurrences we have calculated now, should we sleep?
            System.out.println("Should sleep? "+callingService.ShouldSleep());
            if (callingService.ShouldSleep()) {
                System.out.println("Sleeping for a bit...");
                // Sleep. Disable callbacks for sensors, etc. for the time being.
                DisableSensors();
                callingService.Save(); // Save
                int toSleepMs = 5000 * callingService.sleepSessions;
                // Emulate what we just detected in between? Or skip it?
                if (lastSavedSF != null) {
                    TransportType tt = TransportType.GetForString(lastSavedSF.transportString);
                    callingService.AddTransportOccurrence(new TransportOccurrence(tt, lastSavedSF.startTimeSystemMs, toSleepMs)); // Create an entry corresponding to the average value (i.e., the last modified result).
                    classifier.valuesHistory = new EList<>();                    // Clear the history set.
                }
                try {
                    //                      System.out.println("Sleeping " + toSleepMs / 1000 + " seconds");
                    callingService.OnSleep();
                    Thread.sleep(toSleepMs);
                    callingService.sleeping = false;
//                        System.out.println("Sleeping done");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Reset arrays of magnitude data from the previous sampling period, if they added more values while sleep was being triggered.
    //            clearData();
                EnableSensors();
                callingService.LoadSavedData(); // Try load to make sure that everything actually works too..
            }
        }
    }

    String classifiersFileName = "classifiers.cls";
    private boolean LoadClassifiers() {
        System.out.println("Loading classifiers");
        ObjectInputStream objectIn = null;
        FileInputStream fileIn = null;
        try {
            fileIn = callingService.getBaseContext().openFileInput(classifiersFileName);
            objectIn = new ObjectInputStream(fileIn);
            classifier = (WClassifier) objectIn.readObject();
            System.out.println("Primary classifier loaded");
            accOnlyClassifier = (WClassifier) objectIn.readObject();
            System.out.println("Acc-only classifier loaded");
            fileIn.getFD().sync();
        } catch (FileNotFoundException fnfe){
            System.out.println("Couldn't open file, failed to load");
//            fnfe.printStackTrace();
            return false;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
        System.out.println("Loaded classifiers");
        didSaveClassifiers = true; // Don't save again if we just loaded them ...
        return true;
    }
    private boolean SaveClassifiers(){
        System.out.println("Saving classifiers");
        ObjectOutputStream objectOut = null;
        FileOutputStream fileOut = null;
        try {
            fileOut = callingService.openFileOutput(classifiersFileName, Activity.MODE_PRIVATE);
            objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(classifier);
            System.out.println("Primary classifier saved.");
            objectOut.writeObject(accOnlyClassifier);
            System.out.println("Acc-only classifier saved.");
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
        System.out.println("Saved classifiers");
        return true;
    }

    private InputStream GetTrainingDataInputStream() {
        InputStream ins = callingService.getResources().openRawResource(
                callingService.getResources().getIdentifier("training_data",
                        "raw", callingService.getPackageName()));
        return ins;
    }

    int microsecondsPerSample = 0;
    private void EnableSensors() {
        float secondsPerSample = 1.f / herz;
        microsecondsPerSample = (int) (secondsPerSample * 1000000);
        if (sensorManager == null)
            sensorManager = (SensorManager) callingService.getSystemService(Context.SENSOR_SERVICE);
        EnableAcc();
        EnableGyro();
    }
    private void EnableAcc(){
        if (accSensor == null)
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accSensor, microsecondsPerSample);
    }
    private void EnableGyro(){
        if (gyroSensor == null) gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyroSensor, microsecondsPerSample);
    }

    private void DisableSensors(){
    //    System.out.println("TransportDetectorThread: Disabling sensors.");
        sensorManager.unregisterListener(this);
    }

    EList<Double> valuesHistory = new EList<>();

    static float averageAccSamples = 0,
        averageGyroSamples = 0;

    static boolean didSaveClassifiers = false;

    private void ClassifyInstances() {
//        System.out.println("Classifier trained: "+callingService.classifier.IsTrained());
        if (!classifier.IsTrained() ||
                !accOnlyClassifier.IsTrained()) // Return if the classifiers are not trained yet.
            return;
        if (!didSaveClassifiers){
            SaveClassifiers();
            didSaveClassifiers = true;
        }

        EList<SensingFrame> toCheckAgain = new EList<>();
        for (int i = 0; i < sensingFrames.size(); ++i) {
            try {
                SensingFrame sfToClassify = sensingFrames.get(i);
                // Skip for up to 5 second(s) after sampling is supposedly finished
                // (if waits occurs, start time is set to current time -5, so up to 5 seconds are there to allow cached/queued samples to arrive).
//                if (sfToClassify.startTimeSystemMs + 6000 < System.currentTimeMillis() ) { // Wait 6 seconds..? Not too arbitrary..?
  //                  toCheckAgain.add(sfToClassify);
    //                continue;
      //          }
                if (Math.abs(sfToClassify.accMagns.size() - averageAccSamples) > averageAccSamples * 0.2f) {
                    System.out.println("Acc samples "+sfToClassify.accMagns.size()+" avg: "+averageAccSamples);
                }
                if (Math.abs(sfToClassify.gyroMagns.size() - averageGyroSamples) > averageGyroSamples * 0.2f) {
                    System.out.println("Gyro samples "+sfToClassify.gyroMagns.size()+" avg: "+averageGyroSamples);
                }
                if (sfToClassify.startTimeSystemMs < System.currentTimeMillis() - 15 * 60 * 1000){
                    // Older than 15 mins ago? Then discard it.
                    System.out.println("Discarding "+(System.currentTimeMillis() - sfToClassify.startTimeSystemMs / 1000)+"s old SF");
                    continue;
                }
                if (sfToClassify.accMagns.size() < 15 || sfToClassify.gyroMagns.size() < 15){
                    System.out.println("Post-poning classification of sensing frame with insufficient samples (<15): "+sfToClassify.accMagns.size()+", "+sfToClassify.gyroMagns.size());
                    sfToClassify.transportString = "Insufficient samples";
                    ++sfToClassify.postPoned;
                    if (sfToClassify.postPoned < 3) // Only consider evaluating it 3 times, since if we are here anyway, then we should have received some samples within 15 seconds..?
                        toCheckAgain.add(sfToClassify);
                    continue;
                }
                System.out.println("Classifying SF");

                averageAccSamples = averageAccSamples * 0.9f + sfToClassify.accMagns.size() * 0.1f;
                averageGyroSamples = averageGyroSamples * 0.9f + sfToClassify.gyroMagns.size() * 0.1f;

                sfToClassify.CalcStats();
                sfToClassify.ClearMagnitudeData(); // Clear magnitude data only after classification - which is done shortly.


                // Predict which transport was chosen.
                Instance inst = new DenseInstance(8);
                inst.setDataset(classifier.trainingData); // Give link to data-set of attributes
                inst.setValue(0, sfToClassify.accMin); // Acc Min
                inst.setValue(1, sfToClassify.accMax); // Acc Max
                inst.setValue(2, sfToClassify.accAvg); // Acc Avg
                inst.setValue(3, sfToClassify.accStdev); // Avg Stdev
                boolean evaluateGyro = sfToClassify.gyroAvg != 0; // Check if gyro data is available, if just 0, then use the acc-only classifier.
                double result;
                if (evaluateGyro) {
                    inst.setValue(4, sfToClassify.gyroMin);
                    inst.setValue(5, sfToClassify.gyroMax);
                    inst.setValue(6, sfToClassify.gyroAvg);
                    inst.setValue(7, sfToClassify.gyroStdev);
                    result = classifier.classifyInstance(inst);
                }
                else
                    result = accOnlyClassifier.classifyInstance(inst);
                double modified = classifier.ModifyResult(result, callingService.historySetSize, valuesHistory);                // Use the smoothing window
                // Save it.
                TransportType tt = TransportType.GetForString(classifier.trainingData.classAttribute().value((int) modified));
                sfToClassify.transportString = tt.name();
                callingService.AddTransportOccurrence(new TransportOccurrence(tt, sfToClassify.startTimeSystemMs, sfToClassify.durationMs));
                System.out.println("Transport predicted (w/o. window): " + classifier.trainingData.classAttribute().value((int) modified) +
                        " (" + classifier.trainingData.classAttribute().value((int) result) + ")" +
                    " at "+sfToClassify.startTimeSystemMs+" "+(System.currentTimeMillis() - sfToClassify.startTimeSystemMs)+"ms ago");
                finishedSensingFrames.add(sfToClassify); // Add the classified one to the array of sensing frames.
                callingService.RecalcAverages();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /// Clear array of sensing frames as it has been evaluated.... wat.
//        finishedSensingFrames.clear();
        sensingFrames = toCheckAgain; // Replace old list with new list of those frames which were not evaluated now.
    }

    /// Query next sampling.
/*        iterationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Iterate();
            }
        }, iterationDelayMs);
        */

//    private void clearData() {
    //    accMagnPoints.clear();
      //  gyroMagnPoints.clear();
       // sensingFrame.accMagns.clear();
       // sensingFrame.gyroMagns.clear();
  //  }


    long lastAccTimestamp = 0;
    long lastGyroTimestamp = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {

        /// Since this method will be called most-often, send the data from here?
        EvaluateRequests();

        if (sensingFrame == null)
            sensingFrame = new SensingFrame();

        /// Calculate the sensing frame.
        long sensorSampleMs = event.timestamp / 1000000; // in Nano-seconds -> 6 zeroes through Micro- to Milli-seconds
        SensingFrame oldFrame = null; //GetFrameForSampleTime(sampleMs);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            oldFrame = GetFrameForAccSensorTime(sensorSampleMs);
            lastAccTimestamp = sensorSampleMs;
        }
        else {
            oldFrame = GetFrameForGyroSensorTime(sensorSampleMs);
            lastGyroTimestamp = sensorSampleMs;
        }
        if (oldFrame != null) {
            AddDataToFrame(oldFrame, event); // If it actually belongs to a pre-existing frame, then add it there.
            return; // n return.
        }

//        System.out.println("sampleMs: "+sampleMs);
        long frameSensorStartTime = event.sensor.getType() == Sensor.TYPE_ACCELEROMETER? sensingFrame.startTimeAccSensorMs : sensingFrame.startTimeGyroSensorMs;
        long timeSpentMs = sensorSampleMs - frameSensorStartTime;

        if (timeSpentMs > sensingFrame.durationMs - 5) { // Finish the frame?
            callingService.msTotalTimeAnalyzedSinceThreadStart += 5000;
            SensingFrame toSaveIntoArrayForClassification = sensingFrame;
            sensingFrame = new SensingFrame();
            sensingFrame.startTimeAccSensorMs = lastAccTimestamp;
            sensingFrame.startTimeGyroSensorMs = lastGyroTimestamp;
            toSaveIntoArrayForClassification.startTimeSystemMs = System.currentTimeMillis() - 5000;
            lastSavedSF = toSaveIntoArrayForClassification;
            System.out.println("Frame "+sensingFrames.size()+" finished, "+toSaveIntoArrayForClassification);
            sensingFrames.add(toSaveIntoArrayForClassification);
            callingService.OnSensingFrameFinished();
        //    finishedSensingFrames.add(finishedOne); // Add to list of all those recently finished...?
        }

        /// Always on?
        float[] values = event.values; // Update TextViews
        String text = "";
        for (int i = 0; i < values.length; ++i) {
            text = text + String.format("%.1f", values[i])+" ";
        }
        AddDataToFrame(sensingFrame, event);

    }

    private void EvaluateRequests() {
        if (callingService.numLastSensingFramesRequested > 0){
            // Broadcast it?
       //     System.out.println("Sending last sensing frames as via sendBroadcast, max frames: "+callingService.numLastSensingFramesRequested);
            Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
            intent.putExtra(TransportDetectionService.REQUEST_TYPE, TransportDetectionService.GET_LAST_SENSING_FRAMES);
            intent.putExtra(TransportDetectionService.NUM_FRAMES, callingService.numLastSensingFramesRequested);
            intent.putExtra(TransportDetectionService.SERIALIZABLE_DATA, GetLastSensingFrames(callingService.numLastSensingFramesRequested));
            callingService.sendBroadcast(intent);
            callingService.numLastSensingFramesRequested = 0;
        }
        if (callingService.totalStatSecondsRequested > 0){
            // Broadcast it?
         //   System.out.println("Sending total stats for given seconds: "+callingService.totalStatSecondsRequested);
            Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
            intent.putExtra(TransportDetectionService.REQUEST_TYPE, TransportDetectionService.GET_TOTAL_STATS_FOR_DATA_SECONDS);
            intent.putExtra(TransportDetectionService.DATA_SECONDS, callingService.totalStatSecondsRequested);
            intent.putExtra(TransportDetectionService.SERIALIZABLE_DATA,  callingService.GetTotalStatsForDataSeconds(callingService.totalStatSecondsRequested));
            callingService.sendBroadcast(intent);
            callingService.totalStatSecondsRequested = 0;
        }
    }

    private EList<SensingFrame> GetLastSensingFrames(int maxNum){
        EList<SensingFrame> sensingFrames = GetSensingFrames(maxNum);
        EList<SensingFrame> sfs = new EList<>();
        if (sensingFrames == null){
            System.out.println("SensingFrames is NULL, TransportDetectionService must have halted.");
            return sfs;
        }
        for (int i = sensingFrames.size() - 1; i >= 0; --i){
            SensingFrame frame = new SensingFrame();
            SensingFrame sf2 = sensingFrames.get(i);
            frame.startTimeSystemMs = sf2.startTimeSystemMs; // Don't need the rest, eh?
            frame.accAvg = sf2.accAvg;
            frame.gyroAvg = sf2.gyroAvg;
            frame.transportString = sf2.transportString;
            sfs.add(frame);
            if (sfs.size() >= maxNum)
                return sfs; // Return early.. lol.
        }
        return sfs;
    }

    static void AddDataToFrame(SensingFrame sf, SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) { // accSensor
            //      System.out.println("AccData");
            SensorData newSensorData = new SensorData(); // Copy over data from Android data to own class type.
            newSensorData.timestamp = event.timestamp; // Time-stamp in nanoseconds.
            System.arraycopy(event.values, 0, newSensorData.values, 0, 3);
            MagnitudeData magnData = new MagnitudeData(newSensorData.VectorLength(), newSensorData.timestamp);
            if (sf.accMagns == null){
                System.out.println("sf accMagns null, sf startTime: "+(System.currentTimeMillis() - sf.startTimeSystemMs)+"ms ago");
                return;
            }
            sf.accMagns.add(magnData);
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            //  System.out.println("GyroData");
            SensorData newSensorData = new SensorData(); // Copy over data from Android data to own class type.
            newSensorData.timestamp = event.timestamp; // Time-stamp in nanoseconds.
            System.arraycopy(event.values, 0, newSensorData.values, 0, 3);
            MagnitudeData magnData = new MagnitudeData(newSensorData.VectorLength(), newSensorData.timestamp);
            //          gyroMagnPoints.add(magnData);
            if (sf.gyroMagns == null){
                System.out.println("sf accMagns null, sf startTime: "+(System.currentTimeMillis() - sf.startTimeSystemMs)+"ms ago");
                return;
            }
            sf.gyroMagns.add(magnData);
        }
    }

    static int oldSamplesSaved = 0;
    private SensingFrame GetFrameForAccSensorTime(long millisecondTimestamp) {
        for (int i = 0; i < sensingFrames.size(); ++i){
            SensingFrame sf = sensingFrames.get(i);
            if (millisecondTimestamp >= sf.startTimeAccSensorMs &&
                    millisecondTimestamp < sf.startTimeAccSensorMs + sf.durationMs) {
                ++oldSamplesSaved;
                return sf;
            }
        }
        if (oldSamplesSaved > 10)
            System.out.println("Previously saved up to "+oldSamplesSaved+" samples into old SensingFrames");
        oldSamplesSaved = 0;
        return null;
    }
    private SensingFrame GetFrameForGyroSensorTime(long millisecondTimestamp) {
        for (int i = 0; i < sensingFrames.size(); ++i){
            SensingFrame sf = sensingFrames.get(i);
            if (millisecondTimestamp >= sf.startTimeGyroSensorMs &&
                    millisecondTimestamp < sf.startTimeGyroSensorMs + sf.durationMs) {
                ++oldSamplesSaved;
                return sf;
            }
        }
        if (oldSamplesSaved > 10)
            System.out.println("Previously saved up to "+oldSamplesSaved+" samples into old SensingFrames");
        oldSamplesSaved = 0;
        return null;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String s;
        switch (accuracy){
            case SensorManager.SENSOR_STATUS_NO_CONTACT: s = "No contact";break;
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH: s = "High";break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW: s = "Low";break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM: s = "Medium";break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE: s = "Unreliable"; break;
                default: s = "Dunno";
        }
        // System.out.println("Accuracy changed: "+accuracy+" s"+s);
    }

    public EList<SensingFrame> GetSensingFrames(int maxNum) {
        EList<SensingFrame> total = new EList<>();
        total.addAll(finishedSensingFrames);
        total.addAll(sensingFrames);
        return total;
    }

    public String GetTransportString(int value) {
        if (classifier == null || classifier.trainingData == null)
            return "Null";
        int numValues = classifier.trainingData.classAttribute().numValues();
        if (value < 0 || value >= numValues)
            return "";
        return classifier.trainingData.classAttribute().value(value);
    }
}
