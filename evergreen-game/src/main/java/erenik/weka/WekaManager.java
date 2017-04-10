package erenik.weka;

import erenik.util.FileUtil;
import erenik.util.Printer;
import erenik.util.Tuple;
import weka.attributeSelection.StartSetHandler;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import erenik.util.EList;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.Evaluation;
import java.util.Random;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.output.prediction.AbstractOutput;

/**
 * Created by Emil on 2017-03-01.
 */

public class WekaManager {
    EList<WClassifier> classifiers = new EList<>();

//    Instances trainingData = null, // Always set before usage.
  //          testData = null;

    EList<Integer> testedWindowSizes = new EList<>();
    public Settings s = new Settings(); // Settings to use for tests/analysis.

    public WClassifier NewRandomForest(String name, String fromArffData){
        InputStream is = new ByteArrayInputStream(fromArffData.getBytes());        // convert String into InputStream
        return NewRandomForest(name, is, false);
    }
    /// If accOnly is specified, gyro-entries will be omitted.
    public WClassifier NewRandomForest(String name, InputStream fromArffDataInputStream, boolean accOnly){
        BufferedReader br = new BufferedReader(new InputStreamReader(fromArffDataInputStream));	// read it with BufferedReader
        Instances inst = GetInstancesFromReader(br, accOnly);
        RandomForest rf = new RandomForest();
        WClassifier wc = new WClassifier(rf, name);
        s.trainingDataWhole = inst;
        wc.SetSettings(s);
        classifiers.add(wc);
        wc.TrainAsync(inst); // Train using the instances. They are randomized and stratified first?
        return wc;
    }

    public WClassifier New(AbstractClassifier classifier, InputStream fromArffDataInputStream, boolean accOnly) {
        BufferedReader br = new BufferedReader(new InputStreamReader(fromArffDataInputStream));	// read it with BufferedReader
        Instances inst = GetInstancesFromReader(br, accOnly);
        WClassifier wc = new WClassifier(classifier, classifier.getClass().getSimpleName());
        s.trainingDataWhole = inst;
        wc.SetSettings(s);
        classifiers.add(wc);
        wc.TrainAsync(inst); // Train using the instances. They are randomized and stratified first?
        return wc;
    }


    private WClassifier GetBestClassifier() {
        WClassifier best = classifiers.get(0);
        for (int i = 1; i < classifiers.size(); ++i){
            WClassifier b = classifiers.get(i);
            if (b.Accuracy() > best.Accuracy())
                best = b;
        }
        return best;
    }

    private void PrintResultsWindowTests(boolean printMinMaxErrors) {
        /// Print header.
        System.out.printf("\t\t %1$13s\n", "History set sizes, sleep samples: "+s.sleepSessions);
        System.out.printf("%1$13s", "Classifier");
//            System.out.printf("\t%1$4s", 0);
        for (int i = 0; i < classifiers.size(); ++i){
            WClassifier wc = classifiers.get(i);
            EList<ClassificationStats> cs = wc.GetClassificationStats();
            if (i == 0){ // First row, print header. remaining part.
                for (int j = 0; j < cs.size(); ++j){
                    System.out.printf("  Acc%1$2s", cs.get(j).s.historySetSize);
                    if (printMinMaxErrors){
                        System.out.printf(" %1$4s", "MinE");
                        System.out.printf(" %1$4s", "MaxE");
                    }
                }
                System.out.print(String.format("\n%200s", "").replace(' ', '-'));
            }
            System.out.printf("\n%1$13s", wc.Name());
//            System.out.printf("\t%1.3f", wc.accuracyNoWindow);      // Print acc without a window first.
            for (int j = 0; j < cs.size(); ++j){
                // Print all stats for this run.
                ClassificationStats ws = cs.get(j);
                System.out.printf("  %1.3f", ws.accuracy);
                if (printMinMaxErrors){
                    System.out.printf(" %4d", ws.minErrorsInSequence);
                    System.out.printf(" %4d", ws.maxErrorsInSequence);
                }
            }
        }
        Printer.out();
    }

    private void TestClassifiersHistory(int startWindowSize, int stopWindowSize) {
        WClassifier best = null;
        float bestAcc = 0;
        int bestHistoryLength = 0;
        for (int i = startWindowSize; i <= stopWindowSize; ++i){
            TestClassifiers10FoldHistory(i);
            best = GetBestClassifier();
            Printer.out("Best classifier: "+best.Name()+" acc: "+best.Accuracy());
            if (best.Accuracy() > bestAcc){
                bestAcc = best.Accuracy();
                bestHistoryLength = s.historySetSize;
            }
            PrintResultsWindowTests(true);
        }
        if (best == null)
            Printer.out("No best classifier could be determined (error)");
        else
            Printer.out("Best accuracy "+bestAcc+" for history "+bestHistoryLength+" and classifier "+best.Name());
    }

    private void PrintResultsSimple() {
        System.out.printf("%1$13s\tAccuracy", "Classifier");
        System.out.print(String.format("\n%150s", "").replace(' ', '-'));
        for (int i = 0; i < classifiers.size(); ++i){
            WClassifier wc = classifiers.get(i);
            System.out.printf("\n%1$13s ", wc.Name());
            System.out.printf("\t%1.3f", wc.Accuracy());
        }
        Printer.out();
    }

    WClassifier currentClassifier = null; // Set before usage.

    /// 1 - print some details, 2 - print some more, 3 - print everything?
    static int verbosity = 0;
    //  private float accuracy = 0; // The accuracy of the last classifier test.

    // Populate the list with classifiers.
    public void Init(){
        EList<AbstractClassifier> alac = new EList();
        alac.add(new RandomForest());
        alac.add(new RandomTree());
        alac.add(new BayesNet());
        alac.add(new NaiveBayes());
        for (int i = 0; i < alac.size(); ++i)
            classifiers.add(new WClassifier(alac.get(i), "Test"));
    }

    public static void main(String[] args) throws IOException {
        WClassifierTest test = new WClassifierTest();
        test.Test();
    }

    /// Tries to fetch .arff data instances from target file.
    Instances GetInstancesFromReader(Reader reader, boolean accOnly){
        try {
            Instances data = new Instances(reader);
            reader.close();
            // Remove the index 0 with time.
//            for (int i = 0; i < data.numAttributes(); ++i){
  //              Printer.out("Attribute "+i+" : "+data.attribute(i).toString());
    //        }
            if (data.attribute(0).toString().contains("Time")){
                data = RemoveColumn(1, data); // Remove the start-time?
                Printer.out("num Attrs: "+data.numAttributes());
            }
            /*
            if (accOnly){
                for (int i = 0; i < data.numAttributes(); ++i){
                    String name = data.attribute(i).toString();
                    if (name.contains("gyro")){
                        data = RemoveColumn(i, data);
                        --i;
                        Printer.out("Removing column: "+name);
                    }
                }
            }*/
            data.setClassIndex(data.numAttributes() - 1);            // setting class attribute to the last index - Transports.
            Printer.out("Loaded "+data.size()+" data instances");
            return data;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    /// Tries to fetch .arff data instances from target file.
    Instances GetDataFromFile(String fileName){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new FileReader(fileName));
            return GetInstancesFromReader(reader, false);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WekaManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    // Default sets stuff.
    void UpdateSettings(int setting){
//        try {
            // http://weka.sourceforge.net/doc.dev/weka/classifiers/trees/RandomForest.html
//            if (currentClassifier.cls instanceof RandomForest) {
  //              options = Utils.splitOptions("");
                // -P Size of each bag, as a percentage of the training set size. (default 100)
//                Printer.out("Updated settings for Random Forest...");}
// //else if (currentClassifier.cls instanceof RandomTree){options = Utils.splitOptions("");} else options = Utils.splitOptions("");
//                options = Utils.splitOptions("-I 100 -num-slots 1 -K 0 -M 1.0 -S 1");
     //   } catch (Exception e){e.printStackTrace();}
    }

/*
    void NaiveAnd10Fold(){
        Printer.out("Performing Naive test of same training data as testing data.");
        TestClassifiers();
        PrintResultsSimple();
        Printer.out("\nPerforming Ten-fold cross-validation tests on chosen classifiers.");
        s.doNFoldCrossValidation = true;
        TestClassifiers();
        PrintResultsSimple();
        s.doNFoldCrossValidation = false;
    }
*/
    void TestClassifiers10FoldTestsOnly(){
        Printer.out("\nPerforming Ten-fold cross-validation tests on chosen classifiers.");
        s.doNFoldCrossValidation = true;
        TestClassifiers();
        s.doNFoldCrossValidation = false;
    }


    /// Column starting enumeration from 1, not 0.
    protected Instances RemoveColumn(int i, Instances data) {
        Remove remove = new Remove();                         // new instance of filter
        try {
            String[] options = new String[2];
            options[0] = "-R";
            options[1] = i+""; // Integer to string for the index.
            remove.setOptions(options);                           // set options
            remove.setInputFormat(data);                          // inform filter about dataset **AFTER** setting options
//            Printer.out("Num attrs: "+data.numAttributes());
            Instances newData = Filter.useFilter(data, remove);   // apply filter
            Printer.out("Removed a column, new attrs: "+newData.numAttributes());
            if (newData.numAttributes() == 9 ||
                    newData.numAttributes() == 5)
                return newData;
            Printer.out("Bad number of columns, or what do you say?");
            new Exception().printStackTrace();;
            System.exit(14);
            return newData; // Just copy it over after it's done.
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Tests all classifiers for the previously set training data, window size, etc.
        Performs 10-cross validation if the do10FoldCrossValidation-boolean has been set to true.
     */
    private void TestClassifiers() {
        for (int i = 0; i < classifiers.size(); ++i){
            currentClassifier = classifiers.get(i);
            UpdateSettings(0); // Update settings/options, to match the current classifier.
            currentClassifier.Test(s);
        }
    }

    EList<Double> valuesHistory = new EList();

    /*
    public double ModifyResult(double value){
        if (s.historySetSize < 2) // Just return if history window is not active, i.e., less than 2 in size. 1 would be the same as giving the most recent value.
            return value;
        valuesHistory.add(value); // Add it.
        // Remove old values if the history exceeds a certain length.
        if (valuesHistory.size() > historyLength){
            valuesHistory.remove(0); // Remove index 0 - the oldest value.
        }
        /// Count them.
        EList<Tuple<Double, Integer>> valueCount = new EList();
        for (int i = 0; i < valuesHistory.size(); ++i){
            double valueInHistory = valuesHistory.get(i);
            boolean foundIt = false;
            for (int j = 0; j < valueCount.size(); ++j){
                Tuple<Double,Integer> t = valueCount.get(j);
                if (t.x == valueInHistory){
                    ++t.y;
                    foundIt = true;
                    break; // Break the inner loop.
                }
            }
            if (foundIt)
                continue;
            // Didn't find it?
            valueCount.add(new Tuple<Double, Integer>(valueInHistory, 1)); // Initial count to 1.
        }
        Tuple<Double, Integer> highestCount = valueCount.get(0);
        for (int i = 1; i < valueCount.size(); ++i){
            Tuple<Double, Integer> t = valueCount.get(i);
            if (t.y > highestCount.y)
                highestCount = t;
        }
        return highestCount.x; // Return value of the value with highest count as counted in the history of recent values.
    }
    */

    public void TestClassifiers10FoldHistory(int historySize) {
        s.historySetSize = historySize;
        testedWindowSizes.add(historySize);
        Printer.out("Testing again, with history window active, size: "+s.historySetSize);
        TestClassifiers10FoldTestsOnly();
    }

    public void ClearStatsAndSettings() {
        ResetSettings();
        ClearStats();
//        testData = trainingData; // Same test-data as training-data by default?
    }
    private void ResetSettings() {
        s = new Settings();
    }
    public void ClearStats() {
        for (int i = 0; i < classifiers.size(); ++i){
            classifiers.get(i).ClearStats();
        }
    }

    /// Classifier selection.
    public EList<WClassifier> UseRandomForest() {
        classifiers.clear();
        classifiers.add(new WClassifier(new RandomForest()));
        return classifiers;
    }
    public void UseRandomTree(){
        classifiers.clear();
        classifiers.add(new WClassifier(new RandomTree()));
    }
    public void UseBayesNet(){
        classifiers.clear();
        classifiers.add(new WClassifier(new BayesNet()));
    }
    public void UseNaiveBayes(){
        classifiers.clear();
        classifiers.add(new WClassifier(new NaiveBayes()));
    }
    public EList<WClassifier> UseTestClassifiersQuick() {
        classifiers.clear();
        EList<AbstractClassifier> alac = new EList();
        alac.add(new NaiveBayes());
        alac.add(new BayesNet());
        alac.add(new RandomTree());
//        alac.add(new RandomForest());
        for (int i = 0; i < alac.size(); ++i)
            classifiers.add(new WClassifier(alac.get(i)));
        return classifiers.clone();
    }
    public EList<WClassifier> UseTestClassifiers() {
        classifiers.clear();
        EList<AbstractClassifier> alac = new EList();
        alac.add(new NaiveBayes());
        alac.add(new BayesNet());
        alac.add(new RandomTree());
        alac.add(new RandomForest());
        for (int i = 0; i < alac.size(); ++i)
            classifiers.add(new WClassifier(alac.get(i)));
        return classifiers;
    }

    /// Returns accuracy.
    void DoOwnNFoldCrossValidation(){
        Printer.out("Training data : "+s.trainingDataWhole.size());
        s.doNFoldCrossValidation = true;
        for (int c = 0; c < classifiers.size(); ++c){
            currentClassifier = classifiers.get(c);
            currentClassifier.Test(s);
        }
    }

    public static void NullifyGyroData(Instances testData) {
        if (verbosity > 0)
            Printer.out("Nullifying gyro data on test data.");
        String before = "", after;
        for (int i = 0; i < testData.numInstances(); ++i){
            Instance inst = testData.get(i);
            if (i % 500 == 0)
                before = InstanceAsString(inst);
            // Assume it is attribute 4, 5, 6 and 7, which it should be...
            inst.setValue(4, 0);
            inst.setValue(5, 0);
            inst.setValue(6, 0);
            inst.setValue(7, 0);
            if (i % 500 == 0 && verbosity > 1)
                Printer.out(before+" -> "+InstanceAsString(inst));

        }
    }

    public static String InstanceAsString(Instance inst) {
        String s = "";
        for (int i = 0; i < inst.numAttributes(); ++i){
            Attribute a = inst.attribute(i);
            s += a.name()+" "+inst.value(i)+", ";
        }
        return s;
    }

    /// Tests the current classifiers against the set provided on the given path. Performs default filtration of the set.
    public void TestAgainstSet(String setPath) {
        String[] args = setPath.split("\\\\");
        System.out.print("Testing against file: "+args[args.length - 1]+" settings: "+s);
        s.testDataSource = setPath;
        s.doNFoldCrossValidation = false;
        Instances toTest = GetDataFromFile(setPath);
        s.testDataWhole = toTest;
        for (int i = 0; i < classifiers.size(); ++i){
            WClassifier wc = classifiers.get(i);
            wc.Test(s);
        }
        Printer.out();
    }

    void SetRandomizationDegree(float randomizationDegree) {
        s.randomizationDegree = randomizationDegree;
    }

    public void UseClassifiers(EList<WClassifier> testClassifiers) {
        classifiers = testClassifiers;
    }

    public void UseClassifier(WClassifier wClassifier) {
        classifiers.clear();
        classifiers.add(wClassifier);
    }

    String fileToAppendTo = "";
    public void AppendToFile(String file, String text) {
        fileToAppendTo = file;
        FileUtil.AppendWithTimeStampToFile("HH:mm:ss", ".", fileToAppendTo, text);
    }
    public void AppendToFile(String text) {
        if (fileToAppendTo.length() > 0) {
            AppendToFile(fileToAppendTo, text);
            Printer.out(text);
        }
    }

    public void ClearSettings() {
        ResetSettings();
    }



    /*
    private float DoTenFoldCrossValidation() {
        // Randomizes input of trainging data, then performs cross-validation.
        Outputer out = new Outputer(this);
        Evaluation eval;
        float accuracy = 0;
        try {
            eval = new Evaluation(trainingData);
            java.lang.StringBuffer s = new StringBuffer();
            eval.crossValidateModel(currentClassifier.GetClassifier(), trainingData, 10, new Random(1), out);
            accuracy = out.accuracy;
        } catch (Exception ex) {
            Logger.getLogger(WekaManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (verbosity > 0)
            Printer.out(currentClassifier.Name()+" accuracy: "+out.accuracy);
        return accuracy;
    }*/

    /// Call before predicting. Make sure currentClassifier and trainingData is set before. Returns false if it fails.
    /*
    boolean Train(){
        try {
            currentClassifier.BuildClassifier(trainingData);
//            Printer.out("Classifier trained");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Printer.out("Failed to tran Classifier");
        return false;
    }*/

}
