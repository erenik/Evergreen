package erenik.weka;

import java.io.IOException;

import erenik.util.EList;
import erenik.util.Printer;
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

        //      Printer.out();
        //    NaiveAnd10Fold();
        //  Printer.out();
        //NaiveAnd10Fold();
    }*/


    WekaManager wekaMan = null;

    public WClassifierTest() {
        wekaMan = new WekaManager();
    }

    EList<WClassifier> testClassifiers = new EList<>();
    Instances trainingData = null;

    String file = "WClassifierTest_output.txt";
    void AppendToFile(String text){
        wekaMan.AppendToFile(file, text);
    }

    int minHistoryToTest = 0;
    int maxHistoryToTest = 10;
    int historyStep = 1;
    public void DoAllTests(){
        testClassifiers = wekaMan.UseTestClassifiersQuick();
        //testClassifiers = wekaMan.UseTestClassifiers();
       // testClassifiers = wekaMan.UseRandomForest();
//        maxSleepHistoryToTest = 10;
        minHistoryToTest = 0;
        maxHistoryToTest = 51;
        historyStep = 10;

        AppendToFile("WClassifierTest - all tests");

        wekaMan.s.folds = 2;
  //      wekaMan.s.useNaiveIdleCheck = true;
        wekaMan.s.naiveIdleThreshold = 0.01f;
        wekaMan.s.normalizeAcceleration = false;

        MergePlane();
//        MergeWheelRail();
  //      MergePublicTransport();

//        wekaMan.s.useNaiveIdleCheck = false;
  //      wekaMan.s.naiveIdleThreshold = 0;

        testAlgorithmsHistorySetSizeOwn();
        wekaMan.s.normalizeAcceleration = true;
        testAlgorithmsHistorySetSizeOwn();

//        testAlgorithmsIdleCheckVsWithout();
  //      testAlgorithmsHistorySetSizeOwn();

  //      testAlgorithms();
        /*
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
        */
    }

    private void MergePlane() {
        MergeConvert("PlaneT", "Bus"); // Like a slow bus or train?
        MergeConvert("PlaneC", "Plane");
        MergeConvert("PlaneL", "Plane");
    }

    private void MergePublicTransport() {
        MergeConvert("Bus", "PublicTransport");
        MergeConvert("Train", "PublicTransport");
        MergeConvert("Tram", "PublicTransport");
        MergeConvert("Subway", "PublicTransport");
    }

    private void MergeWheelRail() {
        MergeConvert("Car", "WheelTransport");
        MergeConvert("Bus", "WheelTransport");
        MergeConvert("Train", "RailTransport");
        MergeConvert("Tram", "RailTransport");
        MergeConvert("Subway", "RailTransport");
    }

    private void MergeConvert(String className, String newClassName) {
        wekaMan.s.classConversions.add(new ClassConversion(className, newClassName));
    }

    void LoadBedogniData() {
        Instances data = wekaMan.GetDataFromFile("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Bedogni_sorted_merged.arff");
        wekaMan.s.trainingDataWhole = data;
        trainingData = wekaMan.s.trainingDataWhole;
    }
    void LoadMyData(){
        wekaMan.s.trainingDataWhole = wekaMan.GetDataFromFile("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Hedemalm_alltogether.arff");
        trainingData = wekaMan.s.trainingDataWhole;
        // Remove time? - already removed.
//        wekaMan.s.trainingDataWhole = wekaMan.RemoveColumn(1, data);
//        Printer.out("num Attrs: "+wekaMan.s.trainingDataWhole.numAttributes());
    }

    private void testAlgorithmsIdleCheckVsWithout() {
        Begin("TEST - Naive Idle check - with and without");
        ClearStatsAndSettings();
        LoadMyData();
        UseTestClassifiers();
        wekaMan.s.useNaiveIdleCheck = false;
        wekaMan.DoOwnNFoldCrossValidation();
        Printer.out("Without idle check");
        String format = "name acc good idle total nit";
        PrintAllClassificationResults(format);

        ClearStats();
        wekaMan.s.useNaiveIdleCheck = true;
        wekaMan.DoOwnNFoldCrossValidation();
        Printer.out("With idle check");
        PrintAllClassificationResults(format);

        ClearStats();
        for (int i = 0; i < 7; ++i){
            wekaMan.s.useNaiveIdleCheck = true;
            wekaMan.s.naiveIdleThreshold = 0.25f * (i+1);
            wekaMan.DoOwnNFoldCrossValidation();
            Printer.out("With idle thresh "+wekaMan.s.naiveIdleThreshold);
        }
        PrintAllClassificationResults(format);
    }

    private void ClearStats() {
        for (int i = 0; i < testClassifiers.size(); ++i){
            testClassifiers.get(i).ClearStats();
        }
    }

    private void ClearStatsAndSettings() {
        wekaMan.ClearStatsAndSettings();
        ClearStats();
    }

    private void UseTestClassifiers() {
        wekaMan.UseClassifiers(testClassifiers);
    }

    public void Test(){
        DoAllTests();
    }

    public void Begin(String s){
        AppendToFile(s);
        if (wekaMan.s.doNFoldCrossValidation)
            AppendToFile("Using "+wekaMan.s.folds+" folds for N-fold cross-validation.");
//        Printer.out("\n"+s);
    }


    public void testAlgorithms(){
        Begin("TEST - algorithms on own data.");
        wekaMan.ClearStatsAndSettings();
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwnNFoldCrossValidation();
        PrintAllClassificationResults("name acc good idle total");
    }

    public void testAlgorithmsHistorySetSizeOwn(){
        Begin("TEST - History set size");
        ClearStats();
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwnNFoldCrossValidation();
        Printer.out("Without idle check");
        LoadMyData();
        testAlgorithmsHistorySetSize();
        PrintAllClassificationResults("name acc hss good total idle nit folds tt pt normAcc");
    }
    public void testAlgorithmsHistorySetSizeBedogni(){
        LoadBedogniData();
        testAlgorithmsHistorySetSize();
    }
    /// Only vary the history set size,
    public void testAlgorithmsHistorySetSize(){
        Begin("TEST - algorithms on own data, history set size and various classifiers: "+testClassifiers.size());
        // Testing SleepHistory with RandomForest.
        for (int j = minHistoryToTest; j <= maxHistoryToTest; j += historyStep) {
            switch (j) {
                case 1:
                case 2:
                    continue;
            }
            wekaMan.TestClassifiers10FoldHistory(j);
        }
    }

    private void SetTrainingData() {
        wekaMan.s.trainingDataWhole = trainingData;
    }

    void TestClassifiersSleepHistory(int maxSleepSamples, int historySetSizeMin, int historySetSizeMax) {
        Printer.out("Testing classifiers Sleep samples vs. History Set size on accuracy.");
        for (int i = 0; i < maxSleepSamples; ++i){
            wekaMan.s.sleepSessions = i;
            for (int j = historySetSizeMin; j <= historySetSizeMax; ++j){
                wekaMan.TestClassifiers10FoldHistory(j);
               // PrintClassifiersHistorySleep();
            }
        }
    }

    private void PrintClassifiersHistorySleep() {
        EList<WClassifier> classifiers = testClassifiers;
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
        wekaMan.DoOwnNFoldCrossValidation();
        PrintAllClassificationResults();
    }

    public void testAlgorithmsAccOnly(){
        Begin("TEST - algorithms on own data, Accelerometer-data only (both training/testing)");
        wekaMan.ClearStatsAndSettings();
        wekaMan.s.accelerometerOnly = true;
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwnNFoldCrossValidation();
        PrintAllClassificationResults();
    }
    public void testAlgorithmsGyroOnly() {
        Begin("TEST - algorithms on own data, Gyroscope-data only (both training/testing)");
        wekaMan.ClearStatsAndSettings();
        wekaMan.s.gyroscopeOnly = true;
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwnNFoldCrossValidation();
        PrintAllClassificationResults();
    }


    public void testAlgorithmsNullifyGyroData(){
        Begin("TEST - algorithms on own data, nullifying gyro attributes on test data to simulate devices lacking Gyroscope");
        wekaMan.ClearStatsAndSettings();
        wekaMan.s.nullifyGyroDataDuringPrediction = true;
        LoadMyData();
        UseTestClassifiers();
        wekaMan.DoOwnNFoldCrossValidation();
        PrintAllClassificationResults();
    }

    public void testOwnDataVsBedogni(){
        Begin("TEST - Own data vs Bedogni");
        wekaMan.ClearStatsAndSettings();
        LoadMyData();
        UseTestClassifiers();
        wekaMan.TestAgainstSet("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Bedogni_all.arff");
        PrintAllClassificationResults();

        wekaMan.ClearStatsAndSettings();
        LoadBedogniData();
        UseTestClassifiers();
        wekaMan.TestAgainstSet("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Hedemalm_alltogether.arff");
        PrintAllClassificationResults();
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
        PrintAllClassificationResults();
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
                Printer.out("value: "+value+" modified: "+modified+", "+instances.classAttribute().value((int)modified));
            }
        }catch (Exception e){
            Printer.out("Exception e: "+e.getMessage());
            fail();
        }
    }
    */

    void PrintAllClassificationResults() {
        PrintAllClassificationResults("");
    }
    void PrintAllClassificationResults(String format) {
        /// Fetch all stats.
        EList<ClassificationStats> stats = new EList<>();
        for (int i = 0; i < testClassifiers.size(); ++i)
            stats.addAll(testClassifiers.get(i).GetClassificationStats());

        /// Sort them? By accuracy?
        for (int i = 0; i < stats.size(); ++i){
            for (int j = i + 1; j < stats.size(); ++j){
                ClassificationStats cs = stats.get(i);
                ClassificationStats cs2 = stats.get(j);
                if (cs2.accuracy > cs.accuracy) {
//                    Printer.out("Cs2: "+cs2.good+" better than "+cs.good);
                    stats.swap(i, j);
                }
            }
        }

        String bestConfusionMatrix = "";
        String s = "Print all classification results";
        s += "\n"+ClassificationStats.printFormatHeader(format);
        s += "\n==========================================================================";
        for (int i = 0; i < stats.size(); ++i){
            ClassificationStats cs = stats.get(i);
            s += "\n"+cs.printFormat(format);
            if (i == 0)
                bestConfusionMatrix += ""+cs.confusionMatrixAsString();
//            WClassifier wc = testClassifiers.get(i);
  //          s += "\n"+wc.AllStatsAsString(format);
        }
        AppendToFile(s+"\nConfusion matrix for best result: "+
                "\n=========================================================================="+
                bestConfusionMatrix);
    }

}
