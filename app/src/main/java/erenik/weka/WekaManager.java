package erenik.weka;

import erenik.util.Tuple;
import weka.attributeSelection.StartSetHandler;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
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
import java.util.ArrayList;
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
    ArrayList<WClassifier> classifiers = new ArrayList<>();

    Instances trainingData = null, // Always set before usage.
            testData = null;

    ArrayList<Integer> testedWindowSizes = new ArrayList<>();

    public WClassifier NewRandomForest(String fromArffData){
        InputStream is = new ByteArrayInputStream(fromArffData.getBytes());        // convert String into InputStream
        return NewRandomForest(is);
    }
    public WClassifier NewRandomForest(InputStream fromArffDataInputStream){
        BufferedReader br = new BufferedReader(new InputStreamReader(fromArffDataInputStream));	// read it with BufferedReader
        Instances inst = GetInstancesFromReader(br);
        RandomForest rf = new RandomForest();
        WClassifier wc = new WClassifier(rf);
        wc.trainingData = inst;
        classifiers.add(wc);
        wc.Train(inst); // Train using the instances. They are randomized and stratified first?
        trainingData = inst;
        /// Test it briefly?
        float acc = wc.TestOnDataFolds(inst, 10);
        System.out.println("Tested accuracy on rand/strat/fold training data: "+acc);
        return wc;
    }


    private WClassifier GetBestClassifier() {
        WClassifier best = classifiers.get(0);
        for (int i = 1; i < classifiers.size(); ++i){
            WClassifier b = classifiers.get(i);
            if (b.accuracy > best.accuracy)
                best = b;
        }
        return best;
    }

    private void PrintResultsWindowTests(boolean printMinMaxErrors) {
        /// Print header.
        System.out.printf("\t\t %1$13s\n", "Window sizes");
        System.out.printf("%1$13s", "Classifier");
//            System.out.printf("\t%1$4s", 0);
        for (int i = 0; i < classifiers.size(); ++i){
            WClassifier wc = classifiers.get(i);
            if (i == 0){ // First row, print header. remaining part.
                for (int j = 0; j < wc.classificationStats.size(); ++j){
                    System.out.printf("  Acc%1$2s", wc.classificationStats.get(j).historySetSize);
                    if (printMinMaxErrors){
                        System.out.printf(" %1$4s", "MinE");
                        System.out.printf(" %1$4s", "MaxE");
                    }
                }
                System.out.print(String.format("\n%200s", "").replace(' ', '-'));
            }
            System.out.printf("\n%1$13s", wc.Name());
//            System.out.printf("\t%1.3f", wc.accuracyNoWindow);      // Print acc without a window first.
            for (int j = 0; j < wc.classificationStats.size(); ++j){
                // Print all stats for this run.
                ClassificationStats ws = wc.classificationStats.get(j);
                System.out.printf("  %1.3f", ws.accuracy);
                if (printMinMaxErrors){
                    System.out.printf(" %4d", ws.minErrorsInSequence);
                    System.out.printf(" %4d", ws.maxErrorsInSequence);
                }
            }
        }
        System.out.println();
    }

    private void TestClassifiersHistory(int startWindowSize, int stopWindowSize) {
        WClassifier best = null;
        float bestAcc = 0;
        int bestHistoryLength = 0;
        for (int i = startWindowSize; i <= stopWindowSize; ++i){
            TestClassifiers10FoldHistory(i);
            best = GetBestClassifier();
            System.out.println("Best classifier: "+best.Name()+" acc: "+best.accuracy);
            if (best.accuracy > bestAcc){
                bestAcc = best.accuracy;
                bestHistoryLength = historyLength;
            }
            PrintResultsWindowTests(true);
        }
        if (best == null)
            System.out.println("No best classifier could be determined (error)");
        else
            System.out.println("Best accuracy "+bestAcc+" for history "+bestHistoryLength+" and classifier "+best.Name());
    }

    private void PrintResultsSimple() {
        System.out.printf("%1$13s\tAccuracy", "Classifier");
        System.out.print(String.format("\n%150s", "").replace(' ', '-'));
        for (int i = 0; i < classifiers.size(); ++i){
            WClassifier wc = classifiers.get(i);
            System.out.printf("\n%1$13s ", wc.Name());
            System.out.printf("\t%1.3f", wc.accuracy);
        }
        System.out.println();
    }

    WClassifier currentClassifier = null; // Set before usage.

    /// 1 - print some details, 2 - print some more, 3 - print everything?
    int verbosity = 0;
    private boolean do10FoldCrossValidation = false;
    //  private float accuracy = 0; // The accuracy of the last classifier test.

    String[] options = null; // Set to non-Null if you wanna use it.

    // Populate the list with classifiers.
    public void Init(){
        ArrayList<AbstractClassifier> alac = new ArrayList();
        alac.add(new RandomForest());
        alac.add(new RandomTree());
        alac.add(new BayesNet());
        alac.add(new NaiveBayes());
        for (int i = 0; i < alac.size(); ++i)
            classifiers.add(new WClassifier(alac.get(i)));
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Very testing.");

        WekaManager wekaMan = new WekaManager();
        wekaMan.Init(); // Populate classifiers for random testing.
        wekaMan.Test();
    }

    /// Tries to fetch .arff data instances from target file.
    Instances GetInstancesFromReader(Reader reader){
        try {
            Instances data = new Instances(reader);
            reader.close();
            // Remove the index 0 with time.
            for (int i = 0; i < data.numAttributes(); ++i){
                System.out.println("Attribute "+i+" : "+data.attribute(i).toString());
            }
            if (data.attribute(0).toString().contains("Time")){
                data = RemoveColumn(1, data); // Remove the start-time?
            }
            data.setClassIndex(data.numAttributes() - 1);            // setting class attribute to the last index - Transports.
            System.out.println("Loaded "+data.size()+" data instances");
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
            return GetInstancesFromReader(reader);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WekaManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    private void LoadBedogniData() {
        Instances data = GetDataFromFile("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Bedogni_sorted_merged.arff");
        trainingData = data;
    }
    private void LoadMyData(){
        Instances data = GetDataFromFile("D:\\Dropbox\\PERCCOM mine\\Thesis\\Samples\\Hedemalm_alltogether.arff");
        trainingData = RemoveColumn(1, data);
    }

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
    }

    // Default sets stuff.
    void UpdateSettings(int setting){
        try {
            // http://weka.sourceforge.net/doc.dev/weka/classifiers/trees/RandomForest.html
            if (currentClassifier.cls instanceof RandomForest) {
                options = Utils.splitOptions("");
                // -P Size of each bag, as a percentage of the training set size. (default 100)
//                System.out.println("Updated settings for Random Forest...");
            }
            else if (currentClassifier.cls instanceof RandomTree){
                options = Utils.splitOptions("");
            }
            else
                options = Utils.splitOptions("");
//                options = Utils.splitOptions("-I 100 -num-slots 1 -K 0 -M 1.0 -S 1");
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    void NaiveAnd10Fold(){
        System.out.println("Performing Naive test of same training data as testing data.");
        TestClassifiers();
        PrintResultsSimple();
        System.out.println("\nPerforming Ten-fold cross-validation tests on chosen classifiers.");
        do10FoldCrossValidation = true;
        TestClassifiers();
        PrintResultsSimple();
        do10FoldCrossValidation = false;
    }

    void TestClassifiers10FoldTestsOnly(){
        System.out.println("\nPerforming Ten-fold cross-validation tests on chosen classifiers.");
        do10FoldCrossValidation = true;
        TestClassifiers();
        do10FoldCrossValidation = false;
    }


    /// Column starting enumeration from 1, not 0.
    private Instances RemoveColumn(int i, Instances data) {
        Remove remove = new Remove();                         // new instance of filter
        try {
            String[] options = new String[2];
            options[0] = "-R";
            options[1] = i+""; // Integer to string for the index.
            remove.setOptions(options);                           // set options
            remove.setInputFormat(data);                          // inform filter about dataset **AFTER** setting options
            Instances newData = Filter.useFilter(data, remove);   // apply filter
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
            // Set options to the classifier, if any?
            if (options != null)
                try {
                    currentClassifier.cls.setOptions(options);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

            if (do10FoldCrossValidation){
                DoTenFoldCrossValidation();
                ClassificationStats cs = currentClassifier.GetStatsForHistoryLength(historyLength);
                if (cs == null){
                    // Create one.
                    cs = new ClassificationStats();
                    cs.historySetSize = historyLength;
                    currentClassifier.classificationStats.add(cs);
                }
                cs.accuracy = currentClassifier.accuracy;
                cs.minErrorsInSequence = currentClassifier.minErrorsInSequence;
                cs.maxErrorsInSequence = currentClassifier.maxErrorsInSequence;
                if (verbosity > 0)
                    System.out.println("Adding accuracy: "+currentClassifier.accuracy+" minErr: "+currentClassifier.minErrorsInSequence+" maxErr: "+currentClassifier.maxErrorsInSequence);
                else {
                    currentClassifier.accuracyNoWindow = currentClassifier.accuracy;
                }
                continue;
            }
            if (!Train())
                continue;
            Predict();
            if (verbosity > 0)
                System.out.println(" accuracy: "+currentClassifier.accuracy); // Accuracy of the tests.
        }
    }

    ArrayList<Double> valuesHistory = new ArrayList();
    int historyLength = 0;

    public double ModifyResult(double value){
        if (historyLength < 2) // Just return if history window is not active, i.e., less than 2 in size. 1 would be the same as giving the most recent value.
            return value;
        valuesHistory.add(value); // Add it.
        // Remove old values if the history exceeds a certain length.
        if (valuesHistory.size() > historyLength){
            valuesHistory.remove(0); // Remove index 0 - the oldest value.
        }
        /// Count them.
        ArrayList<Tuple<Double, Integer>> valueCount = new ArrayList();
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

    private void TestClassifiers10FoldHistory(int historySize) {
        historyLength = historySize;
        testedWindowSizes.add(historySize);
        System.out.println("Testing again, with history window active, size: "+historyLength);
        TestClassifiers10FoldTestsOnly();
    }

    private class Outputer extends AbstractOutput {
        int ok = 0;
        int total = 0;
        Classifier cls;
        float accuracy;
        int verbosity = 0;

        int errorsInARow = 0;

        WekaManager wekaMan = null; // Refer to main class talking with the outputer.

        Outputer(WekaManager wekaMan){
            super();
            this.wekaMan = wekaMan;
            m_Buffer = new StringBuffer();
        }
        @Override
        public String globalInfo() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getDisplay() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void doPrintHeader() {
            //        System.out.println("m_Buffer: "+m_Buffer.toString());
        }
        @Override
        protected void doPrintClassification(Classifier clsfr, Instance instnc, int i) throws Exception {
            cls = clsfr;
            double result = clsfr.classifyInstance(instnc);
            result = wekaMan.ModifyResult(result); // If active, modifies result based on history values.
            String resultStr = testData.classAttribute().value((int) result);
            if (verbosity > 2)
                System.out.println(clsfr.getClass().getSimpleName()+" "+instnc.toString()+" i "+i+" predicted: "+resultStr);
            if (instnc.classValue() == result){ // Good result
                ++ok;
                if (errorsInARow > 0){
                    if (errorsInARow > wekaMan.currentClassifier.maxErrorsInSequence)
                        wekaMan.currentClassifier.maxErrorsInSequence = errorsInARow;
                    if (errorsInARow < wekaMan.currentClassifier.minErrorsInSequence)
                        wekaMan.currentClassifier.minErrorsInSequence = errorsInARow;
                    errorsInARow = 0;
                }
            }
            else { // Bad result
                ++errorsInARow;
            }
            ++total;
//                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        //@Override
        protected void doPrintClassification(double[] doubles, Instance instnc, int i) throws Exception {
            //        System.out.println(doubles.toString()+" "+instnc.toString()+" i "+i);
//                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void doPrintFooter() {
            if (verbosity > 1)
                System.out.println(cls.getClass().getSimpleName()+" OK: "+ok+" out of "+total+" ratio: "+ok/(float)total);
            accuracy = ok / (float) total;
            currentClassifier.accuracy = accuracy; // Save accuracy straight in the classifier for sorting/presenting later.
        }
    };

    private float DoTenFoldCrossValidation() {
        // Randomizes input of trainging data, then performs cross-validation.
        Outputer out = new Outputer(this);
        Evaluation eval;
        float accuracy = 0;
        try {
            eval = new Evaluation(trainingData);
            java.lang.StringBuffer s = new StringBuffer();
            eval.crossValidateModel(currentClassifier.cls, trainingData, 10, new Random(1), out);
            accuracy = out.accuracy;
        } catch (Exception ex) {
            Logger.getLogger(WekaManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Skip it.
        if (false){
            accuracy = DoOwn10FoldCrossValidation(); // Now to verify myself!
        }
        if (verbosity > 0)
            System.out.println(currentClassifier.Name()+" accuracy: "+out.accuracy);
        return accuracy;
    }

    /// Returns accuracy.
    float DoOwn10FoldCrossValidation(){
        Instances oldData = trainingData;
        Instances dataToTest = new Instances(trainingData);
        dataToTest.randomize(new Random(1));
        trainingData = null;
        testData = null;
        // Set up the
        int numFolds = 10;
        int numCorrect = 0, numTotal = 0;
        for (int i = 0; i < numFolds; ++i){
            trainingData = dataToTest.trainCV(numFolds, i);             // Set training data.
            testData = dataToTest.testCV(numFolds, i);            // Set the test-data.
            if (verbosity > 1)
                System.out.print("training data: "+trainingData.size()+" testData: "+testData.size()+" ");
            numTotal += testData.size();
            if (!Train())
                continue;
            numCorrect += Predict();
            if (verbosity > 0)
                System.out.println("Fold "+i+" Accuracy: "+currentClassifier.accuracy); // Accuracy of the tests.
        }
        if (verbosity > 0)
            System.out.println(currentClassifier.Name()+" 10-fold accuracy: "+numCorrect / (float) numTotal);
        // Paste over old data again.
        trainingData = oldData;
        return numCorrect / (float) numTotal;
    }


    /// Call before predicting. Make sure currentClassifier and trainingData is set before. Returns false if it fails.
    boolean Train(){
        try {
            currentClassifier.cls.buildClassifier(trainingData);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    /// Predicts the testData that has been provided. Call Train before. Returns the number of correct predictions.
    private int Predict() {
        if (do10FoldCrossValidation == false && verbosity > 0)
            System.out.print("Predicting using the "+currentClassifier.Name());
        // Make predictions
        int good = 0;
        for (int i = 0; i < testData.numInstances(); i++) {
            Instance inst = testData.instance(i);
            double pred = 0;
            try {
                pred = currentClassifier.cls.classifyInstance(inst);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            pred = ModifyResult(pred); // If active, modifies result based on history values.
            if (inst.classValue() == pred){
                ++good;
            }
            if (verbosity >= 2) {
                System.out.print("ID: " + inst.value(0));
                System.out.print(", actual: " + testData.classAttribute().value((int) inst.classValue()));
                System.out.println(", predicted: " + testData.classAttribute().value((int) pred)+" good: "+good);
            }
        }
        currentClassifier.accuracy = good / (float) testData.numInstances();
        return good;
    }
}

// Incremental loading, conserves memory perhaps!
//    // load data
//    ArffLoader loader = new ArffLoader();
//loader.setFile(new File("/some/where/data.arff"));
//        Instances structure = loader.getStructure();
//        structure.setClassIndex(structure.numAttributes() - 1);
//
//        // train NaiveBayes
//        NaiveBayesUpdateable nb = new NaiveBayesUpdateable();
//        nb.buildClassifier(structure);
//        Instance current;
//        while ((current = loader.getNextInstance(structure)) != null)
//        nb.updateClassifier(current);
