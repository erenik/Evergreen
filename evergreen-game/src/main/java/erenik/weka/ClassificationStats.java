/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.weka;

import erenik.util.EList;

/**
 *
 * @author Emil
 */
public class ClassificationStats {
    WClassifier wc = null;
    Settings s = new Settings(); // Settings for generating these stats.
    float accuracy = 0; // Overall accuracy.
    int maxErrorsInSequence = 0; // Maximum errors in a row, before a correct prediction.
    int minErrorsInSequence = 0; // Minimum errors in a row, before a correct prediction returns.
    String testDataSource = "";
    public int idleClassified = 0; // Sample classified as Idle using the naïve approach of checking standard deviation values (non-null or not)
    public EList<Double> valuesHistory = new EList<>();
    // Yeah
    int good = 0;
    public int totalTested = 0;
    /// Time in Ms required to train the classifier using the given test-set.
    public long trainingTimeMs;

    ClassificationStats(WClassifier wc, Settings s){
        this.s.CopyFrom(s);
        this.wc = wc;
    }

    void printAll() {
        String[] strarr = testDataSource.split("\\\\");;
        String lastBit = strarr[strarr.length-1];
        System.out.println(wc.Name()+" on "+lastBit+", settings: "+s.toString()+" acc: "+accuracy+" idle:"+idleClassified);
    }

    public void CalcAccuracy() {
        accuracy = good / (float) totalTested;
    }

    public void printFormat(String format) {
        String[] strarr = testDataSource.split("\\\\");;
        String lastBit = strarr[strarr.length-1];
        String full = format;
        format = format.replace("name", wc.Name()).replace("acc", ""+accuracy);
        System.out.println(format);
    }
}
