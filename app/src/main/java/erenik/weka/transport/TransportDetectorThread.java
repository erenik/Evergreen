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
    private ArrayList<SensingFrame> sensingFrames;

    public int herz = 20; // Samples per second.
    boolean stop = false;
    /// Update time for the sensing frame calculations. 5 seconds?
    int iterationDelay = 5000; // ms
    Handler iterationHandler = new Handler(); // iteration handler

    int msPerIteration = 5000;

    /// All stored accelerometer-sensor points.
    ArrayList<MagnitudeData> accMagnPoints = new ArrayList<>(),
            gyroMagnPoints = new ArrayList<>();


    TransportDetectorThread(TransportDetectionService service){
        callingService = service;
        sensingFrames = new ArrayList<>();
        sensingFrame = new SensingFrame();
    }

    @Override
    public void run() {
        System.out.println("TransportDetectorThread run");

        /// Launch Weka, tran it
        InputStream ins = callingService.getResources().openRawResource(
                callingService.getResources().getIdentifier("training_data",
                        "raw", callingService.getPackageName()));
        callingService.classifier = callingService.wekaMan.NewRandomForest(ins);

        /// Fetch sensors
        SensorManager sensorManager = (SensorManager) callingService.getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        /// Set up sampling rate.
        float secondsPerSample = 1.f / herz;
        int microsecondsPerSample = (int) (secondsPerSample * 1000000);
        System.out.println("Using sampling rate of "+herz+"Hz, seconds per sample: "+secondsPerSample+" ms per sample: "+microsecondsPerSample);
//        int microSeconds = 10 000 000;
        sensorManager.registerListener(this, accSensor, microsecondsPerSample);
        sensorManager.registerListener(this, gyroSensor, microsecondsPerSample);

        /// Set up handler for iterated samplings
        iterationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Iterate();
            }
        }, iterationDelay);

        // Sample until told to stop.
        while (true){
            System.out.println("");
        }
    }

    private void Iterate() {
        /// Calculate the sensing frame.
        long now = System.currentTimeMillis();
        long timeSpent = now - sensingFrame.startTimeMs;
        System.out.println("Time spent: "+timeSpent);
        if (timeSpent > sensingFrame.durationMs - 500) { // Finish the frame?
            /// Calculate sensing frame, if applicable.
            SensingFrame finishedOne = sensingFrame;
            sensingFrame = new SensingFrame();
            finishedOne.CalcStats();
            System.out.println("Stats: "+finishedOne.toString());
            // Save the finished one into a list or some sort, display it as well?
            finishedOne.ClearMagnitudeData(); // Clear data not needed from now?
            sensingFrames.add(finishedOne);

            // Predict which transport was chosen.
            Instance inst = new DenseInstance(8);
            inst.setDataset(callingService.classifier.trainingData); // Give link to data-set of attributes
            inst.setValue(0, finishedOne.accMin); // Acc Min
            inst.setValue(1, finishedOne.accMax); // Acc Max
            inst.setValue(2, finishedOne.accAvg); // Acc Avg
            inst.setValue(3, finishedOne.accStdev); // Avg Stdev
            inst.setValue(4, finishedOne.gyroMin);
            inst.setValue(5, finishedOne.gyroMax);
            inst.setValue(6, finishedOne.gyroAvg);
            inst.setValue(7, finishedOne.gyroStdev);
            try {
                double result = callingService.classifier.cls.classifyInstance(inst);
                double modified = callingService.classifier.ModifyResult(result);                // Use the smoothing window
                // Save it.
                callingService.transportOccurrences.add(new TransportOccurrence((int)modified, finishedOne.startTimeMs));
                msPerIteration = (int) timeSpent;
                callingService.msTotalTimeAnalyzedSinceThreadStart += msPerIteration;
                System.out.println("Transport predicted (incl. window): "+callingService.classifier.trainingData.classAttribute().value((int) modified));
                callingService.RecalcAverages();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        /// Query next sampling.
        iterationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Iterate();
            }
        }, iterationDelay);
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
