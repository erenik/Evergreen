package erenik.weka;

import java.io.IOException;

import erenik.util.EList;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

/**
 * Created by Emil on 2017-04-05.
 */

public class WClassifierTest {

    /*
    public void Test() throws IOException {
//        LoadBedogniData();
        LoadMyData();
        testData = trainingData; // Same test-data as training-data?

        NaiveAnd10Fold();
        try {
            options = Utils.splitOptions("-I 100 -num-slots 1 -K 0 -M 1.0 -S 1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Test with different history-window sizes.
        float bestAcc = 0;
        int bestHistoryLength = 0;
        WClassifier best = null;
        TestClassifiersHistory(3, 10);

        //      System.out.println();
        //    NaiveAnd10Fold();
        //  System.out.println();
        //NaiveAnd10Fold();
    }*/


    WekaManager wekaMan = null;

    public WClassifierTest() {
        wekaMan = new WekaManager();
    }

    EList<WClassifier> testClassifiers = new EList<>();
    Instances trainingData = null;

    int maxSleepHistoryToTest = 50;

    public void DoAllTests(){
        testClassifiers = wekaMan.UseTestClassifiersQuick();
        maxSleepHistoryToTest = 10;

        testAlgorithmsIdleCheckVsWithout();
        testAlgorithms();
        testAlgorithmsHistorySetSizeOwn();
        testAlgorithmsHistorySetSizeBedogni();
//        testAlgorithmsHistorySetSize();
        testAlgorithmsOnlyWhereGyroDataIsPresent();
        testAlgorithmsNullifyGyroData();
        testAlgorithmsGyroOnly();
        testAlgorithmsAccOnly();
        testOwnDataVsBedogni();
        testOwnPrediction();
        //testRandomForest();
        testOwnPrediction(3);
    }

    void LoadBedogniData() {
        Instances data = wekaMan.GetDataFromFile("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Bedogni_sorted_merged.arff");
        wekaMan.s.trainingDataWhole = data;
        trainingData = wekaMan.s.trainingDataWhole;
    }
    void LoadMyData(){
        Instances data = wekaMan.GetDataFromFile("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Hedemalm_alltogether_PlanesMerged.arff");
        wekaMan.s.trainingDataWhole = wekaMan.RemoveColumn(1, data);
        trainingData = wekaMan.s.trainingDataWhole;
    }

    private void testAlgorithmsIdleCheckVsWithout() {
        Begin("TEST - Naïve Idle check - with and without");
        wekaMan.ClearStatsAndSettings();
        LoadMyData();
        UseTestClassifiers();
        wekaMan.s.useNaiveIdleCheck = false;
        wekaMan.DoOwn10FoldCrossValidation();
        System.out.println("Without idle check");
        wekaMan.PrintAllClassificationResults("name\t\tacc");

        wekaMan.ClearStats();
        wekaMan.s.useNaiveIdleCheck = true;
        wekaMan.DoOwn10FoldCrossValidation();
        System.out.println("With idle check");
        wekaMan.PrintAllClassificationResults("name\t\tacc");
    }

    private void UseTestClassifiers() {
        wekaMan.UseClassifiers(testClassifiers);
    }

    public void Test(){
        DoAllTests();
    }

    public void Begin(String s){
        System.out.println("\n"+s);
    }

    public void testAlgorithms(){
        Begin("TEST - algorithms on own data.");
        wekaMan.ClearStatsAndSettings();
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwn10FoldCrossValidation();
        wekaMan.PrintAllClassificationResults("name\t\tacc");
    }

    public void testAlgorithmsHistorySetSizeOwn(){
        LoadMyData();
        testAlgorithmsHistorySetSize();
    }
    public void testAlgorithmsHistorySetSizeBedogni(){
        LoadBedogniData();
        testAlgorithmsHistorySetSize();
    }

    public void testAlgorithmsHistorySetSize(){
        Begin("TEST - algorithms on own data, history set size and various classifiers.");
        // Testing SleepHistory with RandomForest.
        for (int i = 0; i < testClassifiers.size(); ++i){
            wekaMan.ClearStatsAndSettings();
            SetTrainingData();
            wekaMan.UseClassifier(testClassifiers.get(i));
            wekaMan.s.forceAverageBeforeSleep = true;
            TestClassifiersSleepHistory(1, 0, maxSleepHistoryToTest);
        }
    }

    private void SetTrainingData() {
        wekaMan.s.trainingDataWhole = trainingData;
    }

    void TestClassifiersSleepHistory(int maxSleepSamples, int historySetSizeMin, int historySetSizeMax) {
        System.out.println("Testing classifiers Sleep samples vs. History Set size on accuracy.");
        for (int i = 0; i < maxSleepSamples; ++i){
            wekaMan.s.sleepSessions = i;
            for (int j = historySetSizeMin; j <= historySetSizeMax; ++j){
                wekaMan.TestClassifiers10FoldHistory(j);
                PrintClassifiersHistorySleep();
            }
        }
    }

    private void PrintClassifiersHistorySleep() {
        EList<WClassifier> classifiers = wekaMan.classifiers;
        for (int j = 0; j < classifiers.size(); ++j){
            classifiers.get(j).PrintHistorySleep();
        }
    }

    public void testAlgorithmsOnlyWhereGyroDataIsPresent(){
        Begin("TEST - algorithms on own data, testing and traning only with those data samples which include Gyroscope data");
        wekaMan.ClearStatsAndSettings();
        wekaMan.s.onlyWhereGyroDataIsPresent = true;
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwn10FoldCrossValidation();
        wekaMan.PrintAllClassificationResults();
    }

    public void testAlgorithmsAccOnly(){
        Begin("TEST - algorithms on own data, Accelerometer-data only (both training/testing)");
        wekaMan.ClearStatsAndSettings();
        wekaMan.s.accelerometerOnly = true;
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwn10FoldCrossValidation();
        wekaMan.PrintAllClassificationResults();
    }
    public void testAlgorithmsGyroOnly() {
        Begin("TEST - algorithms on own data, Gyroscope-data only (both training/testing)");
        wekaMan.ClearStatsAndSettings();
        wekaMan.s.gyroscopeOnly = true;
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwn10FoldCrossValidation();
        wekaMan.PrintAllClassificationResults();
    }


    public void testAlgorithmsNullifyGyroData(){
        Begin("TEST - algorithms on own data, nullifying gyro attributes on test data to simulate devices lacking Gyroscope");
        wekaMan.ClearStatsAndSettings();
        wekaMan.s.nullifyGyroDataDuringPrediction = true;
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwn10FoldCrossValidation();
        wekaMan.PrintAllClassificationResults();
    }

    public void testOwnDataVsBedogni(){
        Begin("TEST - Own data vs Bedogni");
        wekaMan.ClearStatsAndSettings();
        LoadMyData();
        UseTestClassifiers();
        wekaMan.TestAgainstSet("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Bedogni_all.arff");
        wekaMan.PrintAllClassificationResults();

        wekaMan.ClearStatsAndSettings();
        LoadBedogniData();
        UseTestClassifiers();
        wekaMan.TestAgainstSet("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Hedemalm_alltogether.arff");
        wekaMan.PrintAllClassificationResults();
    }

    public void testOwnPrediction(){
        testOwnPrediction(0);
    }
    /// 0 and up.
    public void testOwnPrediction(int maxVariation){
        Begin("TEST - Own prediction on various files");

        LoadMyData();
        wekaMan.ClearStatsAndSettings();
        wekaMan.UseRandomForest();

        int maxIndex = maxVariation + 1;
        if (maxVariation <= 1)
            maxIndex = 1;
//        wekaMan.Set
        for (int i = 0; i < maxIndex; ++i){
            wekaMan.SetRandomizationDegree(0.01f * i); // 0, 5%, 10%, etc.
            // wekaMan.TestAgainstSet("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Bedogni_all.arff");
            wekaMan.TestAgainstSet("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Hedemalm_alltogether.arff");
            wekaMan.TestAgainstSet("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Filepersample\\Iris Eating and going to korta vägen building and talking_noTime.arff");
            wekaMan.TestAgainstSet("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Filepersample\\Iris Vaasa to Umea boat.arff");
            wekaMan.TestAgainstSet("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Filepersample\\ReinholdB_Plane 0317.arff");
        }
        wekaMan.PrintAllClassificationResults();
    }

    /*
    public void testRandomForest() {
        /// Try own test before standard cross-validation?
        BufferedReader reader = null;
        String fileName = "D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Hedemalm_alltogether.arff";
        try {
            FileInputStream fis = new FileInputStream(fileName);
            WClassifier wc = wekaMan.NewRandomForest(fis);
            Instances instances = wc.trainingData;
            Random r = new Random(1);
            wc.historyLength = 5;
            for (int i = 0; i < 5; ++i){
                // Make some custom instances and analyze them?
                Instance inst = new DenseInstance(8);
                inst.setDataset(instances); // Give link to data-set of attributes
                float d = r.nextFloat() * 3;
                inst.setValue(0, 9.0 - d);
                inst.setValue(1, 9.0 + d);
                inst.setValue(2, 9.0);
                inst.setValue(3, d);
                d = r.nextFloat() * 1;
                inst.setValue(4, d);
                float d2 = r.nextFloat() * 3;
                inst.setValue(5, d+d2);
                inst.setValue(6, (d+d2 + d) / 2);
                inst.setValue(7, d2 - d);
                double value = wc.cls.classifyInstance(inst);
                double modified = wc.ModifyResult(value);
                System.out.println("value: "+value+" modified: "+modified+", "+instances.classAttribute().value((int)modified));
            }
        }catch (Exception e){
            System.out.println("Exception e: "+e.getMessage());
            fail();
        }
    }
    */

}
