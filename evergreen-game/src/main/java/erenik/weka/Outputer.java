package erenik.weka;

/**
 * Created by Emil on 2017-04-05.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.output.prediction.AbstractOutput;
import weka.core.Instance;
import weka.core.Instances;

public class Outputer extends AbstractOutput {
    int ok = 0;
    int total = 0;
    Classifier cls;
    float accuracy;
    int verbosity = 0;
    int errorsInARow = 0;
    int sleepSession = 0; // Current sample, iterated. 0 = sample, 1 to N for 0 to N-1 of sleepSessions.

    WekaManager wekaMan = null; // Refer to main class talking with the outputer.
    WClassifier classifier;

    /// The transport identified during last sampling. Used to compare with those skipped while sleeping to detect faults.
    double lastSampleValue = 0;
    Instances TestData() {
        return wekaMan.testData;
    };

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

        // Check if we should classify it at all.
        boolean sleepOK = true;
        if (wekaMan.s.forceAverageBeforeSleep){
            if (wekaMan.valuesHistory.size() < wekaMan.s.historySetSize)
                sleepOK = false;
        }
        if (wekaMan.s.sleepSessions > 0 && sleepOK){
            if (sleepSession < wekaMan.s.sleepSessions){
                ++sleepSession; // Increment
                if (lastSampleValue == instnc.classValue())    // Save this result for comparison when all sleep sessions have been passed.
                    Good();
                else
                    Bad();
                ++total;
                return; // Don't do more here, is sleep session.
            }
            // Reset the counter to 0 if it's sampling time.
            sleepSession = 0;
            if (wekaMan.s.forceAverageBeforeSleep) // If force-using the averaging before sleeping commences, clear the values history after each sleeping period is done.
                wekaMan.valuesHistory.clear();
        }

        double result = clsfr.classifyInstance(instnc);
        result = wekaMan.ModifyResult(result); // If active, modifies result based on history values.
        lastSampleValue = result;
        if (TestData() == null){
            System.out.println("Test data null");
            new Exception().printStackTrace();
            return;
        }
        String resultStr = TestData().classAttribute().value((int) result);
        if (verbosity > 2)
            System.out.println(clsfr.getClass().getSimpleName()+" "+instnc.toString()+" i "+i+" predicted: "+resultStr);
        if (instnc.classValue() == result) // Good result
            Good();
        else // Bad result
            Bad();
        ++total;
    }

    private void Bad() {
        ++errorsInARow;
    }
    @Override
    protected void doPrintClassification(double[] doubles, Instance instnc, int i) throws Exception {
//        System.out.println(doubles.toString()+" "+instnc.toString()+" i "+i);
//                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void doPrintFooter() {
        if (verbosity > 1)
            System.out.println(cls.getClass().getSimpleName()+" OK: "+ok+" out of "+total+" ratio: "+ok/(float)total);
        accuracy = ok / (float) total;
        classifier.accuracy = accuracy; // Save accuracy straight in the classifier for sorting/presenting later.
    }

    private void Good() {
        ++ok;
        if (errorsInARow > 0){
            if (errorsInARow > classifier.maxErrorsInSequence)
                classifier.maxErrorsInSequence = errorsInARow;
            if (errorsInARow < wekaMan.currentClassifier.minErrorsInSequence)
                classifier.minErrorsInSequence = errorsInARow;
            errorsInARow = 0;
        }
    }
};