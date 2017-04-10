package erenik.weka;

import java.io.IOException;
import java.io.Serializable;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import erenik.util.EList;
import erenik.util.Printer;
import erenik.util.Tuple;
import erenik.weka.transport.TransportType;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SystemInfo;

/**
 *
 * @author Emil
 */
public class WClassifier implements Serializable {
    private static final long serialVersionUID = 1L;

    // A custom identifier for this classifier.
    public String name;
    /// Index/value of the "Idle" label, as it is treated specially by the classifier.
    private int idleIndex = -1;
    // The stats currently being used while predicting/evaluating.
    private ClassificationStats currentStats = null;
    /// Settings currently used for the last training or testing.
    private Settings currentSettings = null;

    WClassifier(AbstractClassifier cls){
        this.cls = cls;
        this.name = cls.getClass().getSimpleName();
    }
    WClassifier(AbstractClassifier cls, String name){
        this.cls = cls;
        this.name = name;
    }
    /// Oh yeah.
    public void SetSettings(Settings s){
        currentSettings = s;
    }

    static int version = 0; // Initial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeObject(name);
        Printer.out("Saving abstract classifier");
        out.writeObject(cls);
        Printer.out("Saving training data - cleared");
        currentSettings.trainingDataWhole.clear(); // Clear it?
        out.writeObject(currentSettings.trainingDataWhole);
        Printer.out("Done");
        out.writeBoolean(isTrained);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        name = (String) in.readObject();
        cls = (AbstractClassifier) in.readObject();
        currentSettings = new Settings();
        currentSettings.trainingDataWhole = (Instances) in.readObject();
        isTrained = in.readBoolean();

        classificationStats = new EList<>();
    }


    private AbstractClassifier cls;
    float Accuracy() { return currentStats.accuracy; }; // of last test test, Weka 10-fold built-in test
//    float accuracyNoWindow;
    int minErrorsInSequence = 10000;
    int maxErrorsInSequence = 0;
    int verbosity = 0;

    // Includes all result data for a session, no matter the window size.
    private EList<ClassificationStats> classificationStats = new EList<>();

    String Name() {
        return cls.getClass().getSimpleName();
    }
    ClassificationStats GetStatsForHistoryLength(int historySetSize){
        for (int i = 0; i < classificationStats.size(); ++i){
            ClassificationStats ws = classificationStats.get(i);
            if (ws.s.historySetSize == historySetSize)
                return ws;
        }
        return null;
    }

    // Trains the classifier without re-ordering the data.
    private boolean isTrained = false;
    private ClassifierTrainer trainerThread = null;

    public boolean IsTrained(){
        if (trainerThread != null){
            isTrained = trainerThread.isDone;
        }
        return isTrained;
    };

    /// Trains it now.
    public boolean TrainSync(){
        return BuildClassifier();
    }
    /// Trains it asynchronously in a separate thread.
    public void TrainAsync(Instances inst) {
        currentSettings.trainingDataFold = inst;
        ClassifierTrainer ct = new ClassifierTrainer(this, inst);
        ct.start();
    }

    public double ClassifyInstance(Instance inst) throws Exception {
        if (currentSettings.normalizeAcceleration)
            NormalizeAccValues(inst);

        if (isTrained) {
            if (currentSettings.useNaiveIdleCheck && IsIdle(inst)) {
//                Printer.out("Is idle, classifying as such "+currentStats.idleClassified);
                ++currentStats.idleClassified;
                return Idle();
            }
            return cls.classifyInstance(inst);
        }
        Printer.out("\nTrying to classify instance on untrained classifier.");
        new Exception().printStackTrace();
        System.exit(1);
        return -1;
    }
    /// Returns the value signifying Idle, as specified within the Instances used to train this classifier.
    private double Idle() {
        if (idleIndex != -1)
            return idleIndex;
        for (int i = 0; i < currentSettings.trainingDataFold.numAttributes(); ++i){
            String nameAttr = currentSettings.trainingDataFold.classAttribute().value(i);
            currentSettings.trainingDataFold.classAttribute();
         //   Printer.out("Name: "+nameAttr);
            if (nameAttr.equals("Idle")) {
                idleIndex = i;
                return idleIndex;
            }
        }
        Printer.out("Couldn't find index of Idle, returning -1");
        new Exception().printStackTrace();
        return -1;
    }

    long totalAccStdevNull = 0;
    private boolean IsIdle(Instance inst) {
        double accStdev = inst.value(Index.ACC_STD),
            gyroStdev = inst.value(Index.GYR_STD);
//        Printer.out("accStdev: "+accStdev+" gyroStdev: "+gyroStdev);
    //    if (accStdev == 0) {
  //          ++totalAccStdevNull;
//            if (totalAccStdevNull % 10 == 0)
                //Printer.out("totalAccstdevnull: "+totalAccStdevNull);
      //  }
        if (accStdev < currentSettings.naiveIdleThreshold && gyroStdev < currentSettings.naiveIdleThreshold){
            return true;
        }
        return false;
    }

    public Classifier GetClassifier() {
        return cls;
    }

    public void ClearStats() {
        classificationStats = new EList<>();
        currentStats = null;
    }

    public ClassificationStats GetStats(int historySetSize, int sleepSessions) {
        for (int i = 0; i < classificationStats.size(); ++i){
            ClassificationStats ws = classificationStats.get(i);
            if (ws.s.historySetSize == historySetSize &&
                    ws.s.sleepSessions == sleepSessions)
                return ws;
        }
        return null;
    }

    public String AllStatsAsString(String format) {
        String tot = "";
        for (int i = 0; i < classificationStats.size(); ++i){
            ClassificationStats cs = classificationStats.get(i);
            if (format == null || format.equals(""))
                tot += "\n"+cs.ToString();
            else
                tot += "\n"+cs.printFormat(format);
        }
        return tot;
    }

    void PrintHistorySleep() {
        Printer.out("History set sizes and sleep samples and their affects on accuracy.");
        // Grab sleep sessions tested?
        EList<Integer> sleepSessionsTested = GetSleepSessionsTested();
        // Print them one row at a time?
        Printer.out("Sleep sessions");
        System.out.print("Acc   ");
        for (int i = 0; i < sleepSessionsTested.size(); ++i){
            System.out.printf("  %1$4s", sleepSessionsTested.get(i));
        }
        EList<Integer> historyLengthsTested = GetHistoryLengthsTested();

        // First row, print sleep sessions?
        for (int h = 0; h < historyLengthsTested.size(); ++h){
            int historyLength = historyLengthsTested.get(h);
            System.out.printf("\n%1$4s", "hss");
            System.out.printf("%2d", historyLength);
            for (int i = 0; i < sleepSessionsTested.size(); ++i){
                int ss = sleepSessionsTested.get(i);
//                System.out.print(" Sleep sessions "+ss);
                ClassificationStats cs = GetStats(historyLength, ss);
                if (cs == null)
                    continue;
                System.out.printf(" %.3f", cs.accuracy);
            }
        }
        Printer.out();
    }

    private EList<Integer> GetSleepSessionsTested() {
        EList<Integer> sessions = new EList<>();
        for (int i = 0; i < classificationStats.size(); ++i){
            ClassificationStats cs =  classificationStats.get(i);
            if (sessions.contains(cs.s.sleepSessions))
                continue;
            sessions.add(cs.s.sleepSessions);
        }
        return sessions;
    }
    private EList<Integer> GetHistoryLengthsTested() {
        EList<Integer> lengths = new EList<>();
        for (int i = 0; i < classificationStats.size(); ++i){
            ClassificationStats cs =  classificationStats.get(i);
            if (lengths.contains(cs.s.historySetSize))
                continue;
            lengths.add(cs.s.historySetSize);
        }
        return lengths;
    }

    public void setOptions(String[] options) throws Exception {
        cls.setOptions(options);
    }

    private boolean BuildClassifier() {
        if (currentSettings.trainingDataFold == null){
            Printer.out("trainingDataFold null, assign it before building.");
            new Exception().printStackTrace();
            return false;
        }
        try {
            if (currentSettings.normalizeAcceleration){
                Printer.out("Normalizing acceleration values");
                NormalizeAccValues(currentSettings.trainingDataFold);
            }
            if (currentSettings.classConversions.size() > 0)
                PerformClassConversions(currentSettings.trainingDataFold);
            long startTimeMs = System.currentTimeMillis();
            cls.buildClassifier(currentSettings.trainingDataFold);
            currentStats.trainingTimeMs = System.currentTimeMillis() - startTimeMs;
            if (currentStats.trainingTimeMs > 200) // Print only if the classifier actually is slow?
                Printer.out(" Classifier trained, took "+currentStats.trainingTimeMs+"ms");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        isTrained = true;
        return true;
    }

    private void PerformClassConversions(Instances instances) {
        int conP = 0;
        for (int i = 0; i < currentSettings.classConversions.size(); ++i){
            ClassConversion cc = currentSettings.classConversions.get(i);
            double classValue = GetClassValue(cc.className, instances);
            double newClassValue = GetClassValue(cc.newClassName, instances);
            if (newClassValue == -1) {
                // Then add it?
                instances.classAttribute().addStringValue(cc.newClassName);
                newClassValue = GetClassValue(cc.newClassName, instances);
            }

            Printer.out("cv" +classValue+" ncv: "+newClassValue);
            for (int j = 0; j < instances.size(); ++j){
                Instance inst = instances.get(j);
                double val = inst.value(8); // 0-3 acc, 4-7 gyr, 8 - class
                if (val == classValue) {
                    inst.setValue(8, newClassValue);
                    ++conP;
                }
            }
        }

        Printer.out("\nConversions performed: "+conP);
    }

    private double GetClassValue(String className, Instances instances) {
        for (int i = 0; i < instances.classAttribute().numValues(); ++i){
            if (className.equals(instances.classAttribute().value(i)))
                return i;
        }
        return -1;
    }

    /// Normalizes min, max anv Avg by dividing by avg (-> 1.0)
    private void NormalizeAccValues(Instances instances) {
        int indexMin = GetIndexOf(instances, "accMin");
        if (indexMin == -1)
            System.exit(5);
        for (int i = 0; i < instances.size(); ++i){
            Instance inst = instances.get(i);
            double accMin = inst.value(0); // Acc min
            double accMax = inst.value(1); // Acc Max.
            double accAvg = inst.value(2); // Acc Avg.
           // Printer.out("To normalized: "+accMin+" "+accMax+" "+accAvg);
            accMin /= accAvg;
            accMax /= accAvg;
            accAvg /= accAvg;
           // Printer.out("Normalized: "+accMin+" "+accMax+" "+accAvg);
            inst.setValue(0, accMin);
            inst.setValue(1, accMax);
            inst.setValue(2, accAvg);
        }
    }
    /// Normalizes min, max anv Avg by dividing by avg (-> 1.0)
    private void NormalizeAccValues(Instance inst) {
        double accMin = inst.value(0); // Acc min
        double accMax = inst.value(1); // Acc Max.
        double accAvg = inst.value(2); // Acc Avg.
        // Printer.out("To normalized: "+accMin+" "+accMax+" "+accAvg);
        accMin /= accAvg;
        accMax /= accAvg;
        accAvg /= accAvg;
        // Printer.out("Normalized: "+accMin+" "+accMax+" "+accAvg);
        inst.setValue(0, accMin);
        inst.setValue(1, accMax);
        inst.setValue(2, accAvg);
    }


    private int GetIndexOf(Instances instances, String attrName) {
        Attribute classAttribute = instances.classAttribute();
        Printer.out("num attributes: "+instances.numAttributes());
        for (int i = 0; i < instances.numAttributes(); ++i){
            Attribute attr = instances.attribute(i);
            Printer.out("attrStr: "+attr+" name: "+attr.name());
            String name = attr.name();
            if (name.equals(attrName))
                return i;
        }
        return -1;
    }

    /// Creates a new set of classification statistics, and sets it as current for this classifier. Adds any previously current stats to the array of stored stats.
    public ClassificationStats NewClassificationStats(Settings s) {
        currentStats = new ClassificationStats(this, s);
        classificationStats.add(currentStats);
        return currentStats;
    }

    public EList<ClassificationStats> GetClassificationStats() {
        return classificationStats;
    }

    public void Test(Settings s) {
        SetSettings(s);
        // Tests this classifier.
        if (s.doNFoldCrossValidation)
            DoOwnNFoldCrossValidation();
        else
            DoOwnTest();
    }

//                cs.accuracy = currentClassifier.accuracy;
  //              cs.minErrorsInSequence = currentClassifier.minErrorsInSequence;
    //            cs.maxErrorsInSequence = currentClassifier.maxErrorsInSequence;
      //              currentClassifier.accuracyNoWindow = currentClassifier.accuracy;

    private void DoOwnTest(){
        if (currentSettings.testDataWhole == null) {
            Printer.out("test data null, aborting");
            return;
        }
        currentSettings.testDataFold = currentSettings.testDataWhole; // Assume whole has been set but fold has not.
        currentSettings.trainingDataFold = currentSettings.testDataWhole;
        currentSettings.trainingDataFold.randomize(new Random(1)); // Randomize training data.
        PrepareForTest();
        TrainAndPredict();
        AfterTest();
    }
    private void DoOwnNFoldCrossValidation() {
        Instances data = new Instances(currentSettings.trainingDataWhole); // N-fold validation -> use same base data for testing as training.
        data.randomize(new Random(1)); // But first randomize it.
        if (data.classAttribute().isNominal()) { // And stratify it according to folds.
//            Printer.out("Stratifying");
            data.stratify(currentSettings.folds);
        }
        else
            Printer.out("Not stratifying");
        currentSettings.testDataWhole = data;

        PrepareForTest();
        int numCorrect = 0, numTotal = 0;
        for (int i = 0; i < currentSettings.folds; ++i){
            currentSettings.fold = i;
            // Set up the folds now.
            Instances testData = data.testCV(currentSettings.folds, i);
            testData.randomize(new Random(1)); // Random
            testData.stratify(currentSettings.folds); // Stratify
            currentSettings.testDataFold = testData;
            currentSettings.trainingDataFold = data.trainCV(currentSettings.folds, i);
            currentSettings.trainingDataFold.randomize(new Random(1));
            TrainAndPredict();
            currentStats.CalcAccuracy();
            if (currentStats.trainingTimeMs > 200) // Display this only if slow?
                Printer.out("Fold "+currentSettings.fold+" Accuracy: "+Accuracy()+" good: "+currentStats.good+" of: "+currentStats.totalTested); // Accuracy of the tests.
        }
        AfterTest();
  //      Printer.out();
//        Printer.out(Name()+" 10-fold accuracy: "+numCorrect / (float) numTotal);
    }
/*
From Evaluation.java
=================================================================
// Make a copy of the data we can reorder
    data = new Instances(data);
    data.randomize(random);
    if (data.classAttribute().isNominal()) {
      data.stratify(numFolds);
    }

    // We assume that the first element is a
    // weka.classifiers.evaluation.output.prediction.AbstractOutput object
    AbstractOutput classificationOutput = null;
    if (forPredictionsPrinting.length > 0) {
      // print the header first
      classificationOutput = (AbstractOutput) forPredictionsPrinting[0];
      classificationOutput.setHeader(data);
      classificationOutput.printHeader();
    }

    // Do the folds
    for (int i = 0; i < numFolds; i++) {
      Instances train = data.trainCV(numFolds, i, random);
      setPriors(train);
      Classifier copiedClassifier = AbstractClassifier.makeCopy(classifier);
      copiedClassifier.buildClassifier(train);
      Instances test = data.testCV(numFolds, i);
      evaluateModel(copiedClassifier, test, forPredictionsPrinting);
    }
    m_NumFolds = numFolds;

    if (classificationOutput != null) {
      classificationOutput.printFooter();
    }

 */

/*
    private float DoNFoldCrossValidation() {
        currentStats.valuesHistory.clear(); // Clear the history set.
        PrintSettings();

        if (s.historySetSize == 1 || s.historySetSize == 2) // Bad argument.
            return 0;

        // Randomizes input of trainging data, then performs cross-validation.
        Outputer out = new Outputer(this);
        out.classifier = currentClassifier;
        Evaluation eval;
        float accuracy = 0;
        try {
            eval = new Evaluation(trainingData);
            java.lang.StringBuffer s = new StringBuffer();
            eval.crossValidateModel(currentClassifier.cls, trainingData, 10, new Random(1), out);
            accuracy = out.accuracy;
            Printer.out(eval.toClassDetailsString()+"\n"+eval.toSummaryString());
        } catch (Exception ex) {
            Logger.getLogger(WekaManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (verbosity > 0)
            Printer.out(currentClassifier.Name()+" accuracy: "+out.accuracy);
        return accuracy;
    }
*/
    private void PrintSettings() {
        Printer.out("Performing 10-fold cross with settings: ");
        Printer.out("  Classifier: "+Name());
        Printer.out("  History set size (hss): "+currentSettings.historySetSize);
        Printer.out("  Sleep sessions per sample: "+currentSettings.sleepSessions);
        Printer.out("  Force averaging before sleep/clear history set after each sleep: "+currentSettings.forceAverageBeforeSleep);
    }

    private void TrainAndPredict() {
        if (!TrainSync()) {
            new Exception().printStackTrace();
            return;
        }
        Predict();
    }
    /// Creates fresh statistics object, Sets various options based on the given settings.
    private void PrepareForTest() {
        NewClassificationStats(currentSettings);
        // Set options to the classifier, if any?
        if (currentSettings.options != null)
            try {
                setOptions(currentSettings.options);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        if (currentSettings.doNFoldCrossValidation) {
            System.out.print("Performing N-fold cross validation on classifier: " + Name() + ", folds: " + currentSettings.folds);
        }
        else
            Printer.out("Performing test on classifier: "+Name());
        if (verbosity > 1)
            System.out.print("training data: "+currentSettings.trainingDataFold.size()+" testData: "+currentSettings.testDataFold.size()+" ");

    }
    private void AfterTest() {
        currentStats.CalcAccuracy();
        currentStats.s = currentSettings.Clone();
        System.out.print(" accuracy "+Accuracy());
    }


    private void Predict() {
        Settings s = currentSettings;
        Instances testData = s.testDataFold;
        PerformClassConversions(testData); // Convert classes as requested before analysis.
        // Randomize and stratify the test-data.
        if (currentSettings.randomizationDegree > 0) // Adjust it.
            AdjustRandomizeData(testData, currentSettings.randomizationDegree);

        if (testData == null){
            Printer.out("Prediction has no test data");
            new Exception("ey").printStackTrace();
            System.exit(4);
        }
        if (s.nullifyGyroDataDuringPrediction)
            WekaManager.NullifyGyroData(testData); // Nullify it.
        // Make predictions
        long predictionStartMs = System.currentTimeMillis();
        for (int i = 0; i < testData.numInstances(); i++) {
            Instance inst = testData.instance(i);
            double pred = 0;
            try {
                pred = ClassifyInstance(inst);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            ++currentStats.totalTested;
            pred = ModifyResult(pred, s.historySetSize, currentStats.valuesHistory); // If active, modifies result based on history values.
            if (inst.classValue() == pred){
                ++currentStats.good;
            }
            if (currentStats.confusionMatrix == null) {
                int matrixSize = currentStats.matrixSize = currentSettings.trainingDataFold.classAttribute().numValues();
                currentStats.confusionMatrix = new int[matrixSize][matrixSize];
            }
            ++currentStats.confusionMatrix[(int) inst.classValue()][(int) pred]; // First array the true value, second value the prediction.
            ++currentStats.trueValuesPerClass[(int) inst.classValue()]; // Increment.

            if (verbosity >= 2) {
                System.out.print("ID: " + inst.value(0));
                System.out.print(", actual: " + testData.classAttribute().value((int) inst.classValue()));
                Printer.out(", predicted: " + testData.classAttribute().value((int) pred)+" good: "+currentStats.good);
            }
        }
        currentStats.predictionTimeMs = System.currentTimeMillis() - predictionStartMs;
    }

    public void ResetValuesHistory() {
        currentStats.valuesHistory = new EList<>();
    }

    public Instances TrainingData() {
        return currentSettings.trainingDataFold;
    }

    private class ClassifierTrainer extends Thread {
        WClassifier wc;
        Instances inst;
        ClassifierTrainer(WClassifier classifier, Instances inst){
            this.wc = classifier;
            this.inst = inst;
        }
        boolean isDone = false;
        @Override
        public void run() {
            Printer.out("Started training classifier: "+wc.name);
            try {
                wc.BuildClassifier();
            } catch (Exception ex) {
                Logger.getLogger(WClassifier.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            isDone = true;
            wc.isTrained = true;
            Printer.out("Classifier trained: "+wc.name);
            /// Test it briefly?
//            float acc = wc.TestOnDataFolds(inst, 10, 3);
  //          Printer.out("Tested accuracy on rand/strat/fold training data: "+acc);
        }
    };


//    public EList<Double> valuesHistory = new EList();

/*    /// Folds has to be larger than 1.
    public float TestOnDataFolds(Instances inst, int numFolds, int historySetSize){
        // Make a copy of the data we can reorder
        Random r = new Random(System.currentTimeMillis());
        EList<ClassificationStats> css = new EList<>();
        // Test on sub-sections of the data?
        int totGood = 0, totTest = 0;
        Printer.out("Num folds: "+numFolds);
        for (int i = 0; i < numFolds; ++i){
            Instances data = null;
            data = inst.testCV(numFolds, i);
            data.randomize(r);
            data.stratify(numFolds); // Sort by class within the fold.
            ClassificationStats cs = TestOnData(data, historySetSize);
            css.add(cs);
            totGood += cs.good;
            totTest += cs.testSize;
        }
        float acc = totGood / (float) totTest;
        Printer.out("Tested acc on randomized/stratified folds of provided data. Accuracy: "+acc);

        for (int i = 1; i < 3; ++i){
            TestWithVariance(inst, numFolds, i * 0.03f, historySetSize);
        }
        acc = totGood / (float) totTest;
        totGood = totTest = 0; // Reset test stats.
        return acc;
    }
*/
    /*
    private void TestWithVariance(Instances inst, int numFolds, float variance, int historySetSize) {
        // Do tests with variations of base-data.
        int totGood = 0, totTest = 0;
        for (int i = 0; i < numFolds; ++i){
            Instances data = null;
            data = inst.testCV(numFolds, i);
            data.randomize(new Random(1));
            data.stratify(numFolds); // Sort by class within the fold.
            // Adjust values a bit.
            AdjustRandomizeData(data, variance); // Up to 20% variations?
            ClassificationStats cs = TestOnData(data, historySetSize);
            totGood += cs.good;
            totTest += cs.testSize;
        }
        float acc = totGood / (float) totTest;
        Printer.out("Tested acc on randomly adjusted data up to +/-"+Math.round(variance*100) +"%. Accuracy: "+acc);
    }

    /// Tests on target data. If class-values exist, compares with given value.
    ClassificationStats TestOnData(Instances testInstances, int historySetSize){
        Settings s = new Settings(); // Default/no-settings?
        ClassificationStats cs = new ClassificationStats(this, s);
        cs.testSize = testInstances.size();
        int good = 0;
        for (int i = 0; i < testInstances.size(); ++i){
            Instance inst = testInstances.instance(i);
            double pred = 0;
            try {
                pred = ClassifyInstance(inst);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            pred = ModifyResult(pred, s.historySetSize, valuesHistory); // If active, modifies result based on history values.
            if (inst.classValue() == pred){
                ++good;
            }
            else {

            }
            if (verbosity >= 2) {
                System.out.print("ID: " + inst.value(0));
                System.out.print(", actual: " + testInstances.classAttribute().value((int) inst.classValue()));
                Printer.out(", predicted: " + testInstances.classAttribute().value((int) pred)+" good: "+good);
            }
        }
        cs.good = good;
        cs.accuracy = good / (float) testInstances.numInstances();
        return cs;
    }
*/
    public double ModifyResult(double value, int historySetSize, EList<Double> valuesHistory){
        if (historySetSize < 2) // Just return if history window is not active, i.e., less than 2 in size. 1 would be the same as giving the most recent value.
            return value;
        valuesHistory.add(value); // Add it.
        // Remove old values if the history exceeds a certain length.
        while(valuesHistory.size() > historySetSize){
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
        if (valueCount.size() == 0) {
            Printer.out("Some issue when counting results in WClassifier.ModifyResult, returning value without modification.");
            return value;
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
    protected void AdjustRandomizeData(Instances data, float plusMinusMinMaxVariation) {
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
//                Printer.out("Skipping class index");
                continue;
            }
            double value = in.value(i);
            double valueAdjusted = value * (1.0f + randomizer.nextFloat() * plusMinusMinMaxVariation - plusMinusMinMaxVariation * 0.5f);
//            Printer.out("Attr: "+a+" "+value+" "+a.value(0)+" adj: "+valueAdjusted);
            if (a.toString().contains("acc"))
                in.setValue(i, valueAdjusted);
            if (a.toString().contains("gyro"))
                in.setValue(i, valueAdjusted);
        }
    };


}
