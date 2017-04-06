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
    public long trainingTimeMs = 0;
    public long predictionTimeMs = 0;

    ClassificationStats(WClassifier wc, Settings s) {
        this.s.CopyFrom(s);
        this.wc = wc;
    }

    String ToString() {
        String[] strarr = testDataSource.split("\\\\");
        ;
        String lastBit = strarr[strarr.length - 1];
        return (wc.Name() + " on " + lastBit + ", settings: " + s.toString() + " acc: " + accuracy + " idle:" + idleClassified);
    }

    public void CalcAccuracy() {
        accuracy = good / (float) totalTested;
    }

    public static String printFormatHeader(String format) {
        String full = format;
        format = format.replace("name", Format("Name", 12))
                .replace("acc", Format("Acc%", 5))
                .replace("good", Format("Good", 6))
                .replace("total", Format("Total", 6))
                .replace("idle", Format("#Idle", 5))
                .replace("hss", Format("Hss", 3))
                .replace("nit", Format("NIT", 5))
                .replace("folds", Format("Folds", 5))
                .replace("tt", Format("TrainMs", 7))
                .replace("pt", Format("PredMs", 6))
                .replace("normAcc", Format("AccNorm", 5))
                ;
        return format;
    }

    static String Format(String arg, int maxLetters) {
        return String.format("%"+maxLetters+"s",arg);
    }
    public String printFormat(String format) {
        format = format.replace("name", Format(wc.Name(), 12))
                .replace("acc", Format(""+String.format("%.2f", accuracy), 5))
                .replace("good", Format(""+good, 6))
                .replace("total", Format(""+totalTested, 6))
                .replace("idle", Format(""+idleClassified, 5))
                .replace("hss", Format(""+s.historySetSize, 3))
                .replace("nit", Format(""+String.format("%.2f", s.naiveIdleThreshold), 5))
                .replace("folds", Format(""+s.folds, 5))
                .replace("tt", Format(""+trainingTimeMs, 7))
                .replace("pt", Format(""+predictionTimeMs, 6))
                .replace("normAcc", Format(s.normalizeAcceleration? "Yes" : "No", 5))
        ;
        return format;
    }
}
