package erenik;

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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
    ArrayList<AbstractClassifier> classifiers = new ArrayList<>();

    Instances trainingData = null, // Always set before usage.
            testData = null;

    AbstractClassifier currentClassifier = null; // Set before usage.

    /// 1 - print some details, 2 - print some more, 3 - print everything?
    int verbosity = 0;
    private boolean do10FoldCrossValidation = false;
    private float accuracy = 0; // The accuracy of the last classifier test.

    String[] options = null; // Set to non-Null if you wanna use it.

    // Populate the list with classifiers.
    public void Init(){
        classifiers.add(new RandomForest());
        classifiers.add(new RandomTree());
        classifiers.add(new BayesNet());
        classifiers.add(new NaiveBayes());
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Very testing.");

        WekaManager wekaMan = new WekaManager();
        wekaMan.Init(); // Populate classifiers for random testing.
        wekaMan.Test();
    }

    /// Tries to fetch .arff data instances from target file.
    Instances GetDataFromFile(String fileName){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new FileReader(fileName));
            Instances data = new Instances(reader);
            reader.close();
            // setting class attribute to the last index - Transports.
            data.setClassIndex(data.numAttributes() - 1);
            System.out.println("Loaded "+data.size()+" data instances");
            return data;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

        // filter
        // meta-classifier which filters and classifies as it reads the data instead of filtering everything separately first.
  //      FilteredClassifier fc = new FilteredClassifier();
//        fc.setFilter(rm);


        NaiveAnd10Fold();
        // TODO: Understand and get these arguments into the code!!!
        // Scheme:       weka.classifiers.trees.RandomForest -P 100 -I 100 -num-slots 1 -K 0 -M 1.0 -V 0.001 -S 1
        // Relation:     sensor-weka.filters.unsupervised.attribute.Remove-R1
        // RandomForest
        // Bagging with 100 iterations and base learner
        // weka.classifiers.trees.RandomTree -K 0 -M 1.0 -V 0.001 -S 1 -do-not-check-capabilities
        // Test various options? What effect they have.
        try {
            // -P 100, maximum number of parents? used for some algorithms?
            // -I 100, max number of iterations? does what?
            // -num-slots maybe the same?
            // -K switches on kernel density estimation for numerical attributes which often improves performance.
            // -M 1.0, use Mutation or # of instances to generate
            // -V 0.001
            // -S ?
            options = Utils.splitOptions("-I 100 -num-slots 1 -K 0 -M 1.0 -S 1");
        } catch (Exception e) {
            e.printStackTrace();
        }
//        NaiveAnd10Fold();
  //      System.out.println();
    //    NaiveAnd10Fold();
      //  System.out.println();
        //NaiveAnd10Fold();
    }

    // Default sets stuff.
    void UpdateSettings(int setting){
        try {
            // http://weka.sourceforge.net/doc.dev/weka/classifiers/trees/RandomForest.html
            if (currentClassifier instanceof RandomForest) {
                options = Utils.splitOptions("");
                // -P Size of each bag, as a percentage of the training set size. (default 100)
//                System.out.println("Updated settings for Random Forest...");
            }
            else if (currentClassifier instanceof RandomTree){
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
        System.out.println("Performing Ten-fold cross-validation tests on chosen classifiers.");
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

    private void TestClassifiers() {
        for (int i = 0; i < classifiers.size(); ++i){
            currentClassifier = classifiers.get(i);
            UpdateSettings(0); // Update settings/options, to match the current classifier.
            // Set options to the classifier, if any?
            if (options != null)
                try {
                    currentClassifier.setOptions(options);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

            if (do10FoldCrossValidation){
                DoTenFoldCrossValidation();
                continue;
            }
            if (!Train())
                continue;
            Predict();
            System.out.println(" accuracy: "+accuracy); // Accuracy of the tests.
        }
    }

    private class Outputer extends AbstractOutput {
        int ok = 0;
        int total = 0;
        Classifier cls;
        Outputer(){
            super();
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
            String resultStr = testData.classAttribute().value((int) result);
      //      System.out.println(clsfr.getClass().getSimpleName()+" "+instnc.toString()+" i "+i+" predicted: "+resultStr);
            if (instnc.classValue() == result)
                ++ok;
            ++total;
//                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void doPrintClassification(double[] doubles, Instance instnc, int i) throws Exception {
    //        System.out.println(doubles.toString()+" "+instnc.toString()+" i "+i);
//                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void doPrintFooter() {
//            System.out.println(m_Buffer.toString());
            System.out.println(cls.getClass().getSimpleName()+" OK: "+ok+" out of "+total+" ratio: "+ok/(float)total);
        }
    }; 
    
    private void DoTenFoldCrossValidation() {
        
        Evaluation eval;
        try {
            eval = new Evaluation(trainingData);
            java.lang.StringBuffer s = new StringBuffer();
      
            Outputer out = new Outputer();
            eval.crossValidateModel(currentClassifier, trainingData, 10, new Random(1), out);            
            
        } catch (Exception ex) {
            Logger.getLogger(WekaManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /*
        
        Instances originalTrainingData = trainingData;
        trainingData = null;
        testData = null;
        // Set up the
        int numFolds = 10;
        int numCorrect = 0, numTotal = 0;
        for (int i = 0; i < numFolds; ++i){
            trainingData = originalTrainingData.trainCV(numFolds, i);             // Set training data.
            testData = originalTrainingData.testCV(numFolds, i);            // Set the test-data.
            if (verbosity > 1)
                System.out.print("training data: "+trainingData.size()+" testData: "+testData.size()+" ");
            numTotal += testData.size();
            if (!Train())
                continue;
            numCorrect += Predict();
            if (verbosity > 0)
                System.out.println("Fold "+i+" Accuracy: "+accuracy); // Accuracy of the tests.
        }
        System.out.println(currentClassifier.getClass().getSimpleName()+" 10-fold accuracy: "+numCorrect / (float) numTotal);
        trainingData = originalTrainingData;
*/
    }


    /// Call before predicting. Make sure currentClassifier and trainingData is set before. Returns false if it fails.
    boolean Train(){
        try {
            currentClassifier.buildClassifier(trainingData);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    /// Predicts the testData that has been provided. Call Train before. Returns the number of correct predictions.
    private int Predict() {
        if (do10FoldCrossValidation == false)
            System.out.print("Predicting using the "+currentClassifier.getClass().getSimpleName());
        // Make predictions
        int good = 0;
        for (int i = 0; i < testData.numInstances(); i++) {
            Instance inst = testData.instance(i);
            double pred = 0;
            try {
                pred = currentClassifier.classifyInstance(inst);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            if (verbosity >= 2) {
                System.out.print("ID: " + inst.value(0));
                System.out.print(", actual: " + testData.classAttribute().value((int) inst.classValue()));
                System.out.println(", predicted: " + testData.classAttribute().value((int) pred));
            }
            if (inst.classValue() == pred){
                ++good;
            }
        }
        accuracy = good / (float) testData.numInstances();
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
