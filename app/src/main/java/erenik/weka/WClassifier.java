package erenik.weka;

import java.util.Random;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import erenik.util.Tuple;
import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author Emil
 */
public class WClassifier {

    WClassifier(AbstractClassifier cls){
        this.cls = cls;
    }
    public Instances trainingData; // So we can check attributes later.
    public AbstractClassifier cls;
    float accuracy; // of last test test, Weka 10-fold built-in test
    float accuracyNoWindow;
    int minErrorsInSequence = 10000;
    int maxErrorsInSequence = 0;
    int verbosity = 0;

    // Includes all result data for a session, no matter the window size.
    public ArrayList<ClassificationStats> classificationStats = new ArrayList<>();

    String Name() {
        return cls.getClass().getSimpleName();
    }
    ClassificationStats GetStatsForHistoryLength(int historySetSize){
        for (int i = 0; i < classificationStats.size(); ++i){
            ClassificationStats ws = classificationStats.get(i);
            if (ws.historySetSize == historySetSize)
                return ws;
        }
        return null;
    }

    // Trains the classifier without re-ordering the data.
    public boolean Train(Instances inst) {
        try {
            cls.buildClassifier(inst);
            return false;
        } catch (Exception ex) {
            Logger.getLogger(WClassifier.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }
    /// Folds has to be larger than 1.
    public float TestOnDataFolds(Instances inst, int numFolds){
        // Make a copy of the data we can reorder
        Random r = new Random(System.currentTimeMillis());
        ArrayList<ClassificationStats> css = new ArrayList<>();
        // Test on sub-sections of the data?
        int totGood = 0, totTest = 0;
        System.out.println("Num folds: "+numFolds);
        for (int i = 0; i < numFolds; ++i){
            Instances data = null;
            data = inst.testCV(numFolds, i);
            data.randomize(r);
            data.stratify(numFolds); // Sort by class within the fold.
            ClassificationStats cs = TestOnData(data);
            css.add(cs);
            totGood += cs.good;
            totTest += cs.testSize;
        }
        float acc = totGood / (float) totTest;
        System.out.println("Tested acc on randomized/stratified folds of provided data. Accuracy: "+acc);

        for (int i = 1; i < 3; ++i){
            TestWithVariance(inst, numFolds, i * 0.03f);
        }
        totGood = totTest = 0; // Reset test stats.
        acc = totGood / (float) totTest;
        return acc;
    }

    private void TestWithVariance(Instances inst, int numFolds, float variance) {
        // Do tests with variations of base-data.
        int totGood = 0, totTest = 0;
        for (int i = 0; i < numFolds; ++i){
            Instances data = null;
            data = inst.testCV(numFolds, i);
            data.randomize(new Random(1));
            data.stratify(numFolds); // Sort by class within the fold.
            // Adjust values a bit.
            AdjustRandomizeData(data, variance); // Up to 20% variations?
            ClassificationStats cs = TestOnData(data);
            totGood += cs.good;
            totTest += cs.testSize;
        }
        float acc = totGood / (float) totTest;
        System.out.println("Tested acc on randomly adjusted data up to +/-"+Math.round(variance*100) +"%. Accuracy: "+acc);
    }

    /// Tests on target data. If class-values exist, compares with given value.
    ClassificationStats TestOnData(Instances testInstances){
        ClassificationStats cs = new ClassificationStats();
        cs.testSize = testInstances.size();
        int good = 0;
        for (int i = 0; i < testInstances.size(); ++i){
            Instance inst = testInstances.instance(i);
            double pred = 0;
            try {
                pred = cls.classifyInstance(inst);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            pred = ModifyResult(pred); // If active, modifies result based on history values.
            if (inst.classValue() == pred){
                ++good;
            }
            else {

            }
            if (verbosity >= 2) {
                System.out.print("ID: " + inst.value(0));
                System.out.print(", actual: " + testInstances.classAttribute().value((int) inst.classValue()));
                System.out.println(", predicted: " + testInstances.classAttribute().value((int) pred)+" good: "+good);
            }
        }
        cs.good = good;
        cs.accuracy = good / (float) testInstances.numInstances();
        return cs;
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

    Random randomizer = null;
    private void AdjustRandomizeData(Instances data, float plusMinusMinMaxVariation) {
        randomizer = new Random(1);
        randomizer.setSeed(1);
        for (int i = 0; i < data.size(); ++i){
            Instance in = data.get(i);
            AdjustRandomizeInstance(in, plusMinusMinMaxVariation);
        }
    }
    private void AdjustRandomizeInstance(Instance in, float plusMinusMinMaxVariation){
        for (int i = 0; i < in.numAttributes(); ++i){
            Attribute a = in.attribute(i);
            if (in.classIndex() == i){
//                System.out.println("Skipping class index");
                continue;
            }
            double value = in.value(i);
            double valueAdjusted = value * (1.0f + randomizer.nextFloat() * plusMinusMinMaxVariation - plusMinusMinMaxVariation * 0.5f);
//            System.out.println("Attr: "+a+" "+value+" "+a.value(0)+" adj: "+valueAdjusted);
            if (a.toString().contains("acc"))
                in.setValue(i, valueAdjusted);
            if (a.toString().contains("gyro"))
                in.setValue(i, valueAdjusted);
        }
    };


}
