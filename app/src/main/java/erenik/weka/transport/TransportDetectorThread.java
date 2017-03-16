package erenik.weka.transport;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;

import erenik.evergreen.R;
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
    private Sensor accSensor, gyroSensor;
    private SensingFrame sensingFrame;
    ArrayList<SensingFrame> sensingFrames;

    public int herz = 20; // Samples per second.
    boolean stop = false;
    /// Update time for the sensing frame calculations. 5 seconds?
    int iterationDelayMs = 5000; // ms
//    Handler iterationHandler = new Handler(); // iteration handler

    int msPerIteration = 5000;

    /// All stored accelerometer-sensor points.
    ArrayList<MagnitudeData> accMagnPoints = new ArrayList<>(),
            gyroMagnPoints = new ArrayList<>();
    private ArrayList<SensingFrame> finishedSensingFrames = new ArrayList<>();


    TransportDetectorThread(TransportDetectionService service){
        callingService = service;
        sensingFrames = new ArrayList<>();
        sensingFrame = new SensingFrame();
    }

    @Override
    public void run() {
        System.out.println("TransportDetectorThread run");

        /// Launch Weka, tran it
        GetTrainingDataInputStream();

        callingService.classifier = callingService.wekaMan.NewRandomForest("RandomForest", GetTrainingDataInputStream(), false);
        callingService.accOnlyClassifier = callingService.wekaMan.NewRandomForest("RandomForest Acc only", GetTrainingDataInputStream(), true);

        /// Set up sampling rate.
        EnableSensors();


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
            Iterate();
        }
    }

    private InputStream GetTrainingDataInputStream() {
        InputStream ins = callingService.getResources().openRawResource(
                callingService.getResources().getIdentifier("training_data",
                        "raw", callingService.getPackageName()));
        return ins;
    }

    private void EnableSensors() {
        System.out.println("TransportDetectorThread: Enabling sensors.");
        if (sensorManager == null)
            sensorManager = (SensorManager) callingService.getSystemService(Context.SENSOR_SERVICE);
        if (accSensor == null)
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (gyroSensor == null)
            gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        float secondsPerSample = 1.f / herz;
        int microsecondsPerSample = (int) (secondsPerSample * 1000000);
        System.out.println("Using sampling rate of "+herz+"Hz, seconds per sample: "+secondsPerSample+", microseconds per sample: "+microsecondsPerSample);
//        int microSeconds = 10 000 000;
        sensorManager.registerListener(this, accSensor, microsecondsPerSample);
        sensorManager.registerListener(this, gyroSensor, microsecondsPerSample);
    }

    private void DisableSensors(){
        System.out.println("TransportDetectorThread: Disabling sensors.");
        sensorManager.unregisterListener(this);
    }

    /// Called every 5 seconds (5000ms) by default in the main thread loop.
    private void Iterate() {
        /// Calculate the sensing frame.
        long nowMs = System.currentTimeMillis();
        long timeSpent = nowMs - sensingFrame.startTimeMs;
        msPerIteration = (int) timeSpent;
        callingService.msTotalTimeAnalyzedSinceThreadStart += msPerIteration;

//        System.out.println("Time spent: "+timeSpent);
        if (timeSpent > sensingFrame.durationMs - 500) { // Finish the frame?
            /// Calculate sensing frame, if applicable.
            SensingFrame finishedOne = sensingFrame;
            sensingFrame = new SensingFrame();
            finishedOne.CalcStats();
//            System.out.println("Stats: "+finishedOne.toString());
            // Save the finished one into a list or some sort, display it as well?
            finishedOne.ClearMagnitudeData(); // Clear data not needed from now?
            sensingFrames.add(finishedOne);
            finishedSensingFrames.add(finishedOne); // Add to list of all those recently finished.
            ClassifyInstances();
        }
    }

    private void ClassifyInstances() {
        System.out.println("Classifier trained: "+callingService.classifier.IsTrained());
        if (!callingService.classifier.IsTrained() ||
                !callingService.accOnlyClassifier.IsTrained()) // Return if the classifiers are not trained yet.
            return;
        for (int i = 0; i < finishedSensingFrames.size(); ++i) {
            try {
                SensingFrame finishedOne = finishedSensingFrames.get(i);
                // Predict which transport was chosen.
                Instance inst = new DenseInstance(8);
                inst.setDataset(callingService.classifier.trainingData); // Give link to data-set of attributes
                inst.setValue(0, finishedOne.accMin); // Acc Min
                inst.setValue(1, finishedOne.accMax); // Acc Max
                inst.setValue(2, finishedOne.accAvg); // Acc Avg
                inst.setValue(3, finishedOne.accStdev); // Avg Stdev
                boolean evaluateGyro = finishedOne.gyroAvg != 0; // Check if gyro data is available, if just 0, then use the acc-only classifier.
                double result;
                if (evaluateGyro) {
                    inst.setValue(4, finishedOne.gyroMin);
                    inst.setValue(5, finishedOne.gyroMax);
                    inst.setValue(6, finishedOne.gyroAvg);
                    inst.setValue(7, finishedOne.gyroStdev);
                    result = callingService.classifier.classifyInstance(inst);
                }
                else
                    result = callingService.accOnlyClassifier.classifyInstance(inst);
                double modified = callingService.classifier.ModifyResult(result, callingService.historySetSize);                // Use the smoothing window
                // Save it.
                TransportType tt = TransportType.GetForString(callingService.classifier.trainingData.classAttribute().value((int) modified));
                finishedOne.transportString = tt.name();
                callingService.AddTransportOccurrence(new TransportOccurrence(tt, finishedOne.startTimeMs, finishedOne.durationMs));
                System.out.println("Transport predicted (w/o. window): " + callingService.classifier.trainingData.classAttribute().value((int) modified) + " (" + callingService.classifier.trainingData.classAttribute().value((int) result) + ")");
                callingService.RecalcAverages();

                /// Check how many occurrences we have calculated now, should we sleep?
                if (callingService.ShouldSleep()) {
                    // Sleep. Disable callbacks for sensors, etc. for the time being.
                    DisableSensors();
                    int toSleepMs = 5000 * callingService.sleepSessions;
                    callingService.AddTransportOccurrence(new TransportOccurrence(tt, finishedOne.startTimeMs, toSleepMs)); // Create an entry corresponding to the average value (i.e., the last modified result).
                    callingService.classifier.valuesHistory.clear();                    // Clear the history set.
                    try {
                        System.out.println("Sleeping " + toSleepMs / 1000 + " seconds");
                        callingService.sleeping = true; // Flag as sleeping
                        Thread.sleep(toSleepMs);
                        callingService.sleeping = false;
                        System.out.println("Sleeping done");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Reset arrays of magnitude data from the previous sampling period, if they added more values while sleep was being triggered.
                    clearData();
                    EnableSensors();
                }
                // Save progress after each addition?
                callingService.Save();
                callingService.LoadSavedData(); // Try load to make sure that everything actually works too..
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /// Clear array of sensing frames as it has been evaluated.
        finishedSensingFrames.clear();
    }

    /// Query next sampling.
/*        iterationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Iterate();
            }
        }, iterationDelayMs);
        */

    private void clearData() {
        accMagnPoints.clear();
        gyroMagnPoints.clear();
        sensingFrame.accMagns.clear();
        sensingFrame.gyroMagns.clear();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /// Always on?
        float[] values = event.values; // Update TextViews
        String text = "";
        for (int i = 0; i < values.length; ++i) {
            text = text + String.format("%.1f", values[i])+" ";
        }
        if (event.sensor == accSensor) {
      //      System.out.println("AccData");
            SensorData newSensorData = new SensorData(); // Copy over data from Android data to own class type.
            newSensorData.timestamp = event.timestamp; // Time-stamp in nanoseconds.
            System.arraycopy(event.values, 0, newSensorData.values, 0, 3);
            MagnitudeData magnData = new MagnitudeData(newSensorData.VectorLength(), newSensorData.timestamp);
            accMagnPoints.add(magnData);
            sensingFrame.accMagns.add(magnData);
        }
        else if (event.sensor == gyroSensor){
          //  System.out.println("GyroData");
            SensorData newSensorData = new SensorData(); // Copy over data from Android data to own class type.
            newSensorData.timestamp = event.timestamp; // Time-stamp in nanoseconds.
            System.arraycopy(event.values, 0, newSensorData.values, 0, 3);
            MagnitudeData magnData = new MagnitudeData(newSensorData.VectorLength(), newSensorData.timestamp);
            gyroMagnPoints.add(magnData);
            sensingFrame.gyroMagns.add(magnData);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        System.out.println("Accuracy changed");
    }
}
