/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka;

import evergreen.util.EList;
import evergreen.util.Printer;

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
    // Array of values - mainly for confusion matrix.
    // First array the true value, second value the prediction.
    public int[][] confusionMatrix = null;
    public int matrixSize = 0;
    public String[] classValues = null;
    public int[] trueValuesPerClass = null;

    ClassificationStats(WClassifier wc, Settings s) {
        this.s.CopyFrom(s);
        this.wc = wc;

        if (s.testDataWhole != null) {
            classValues = new String[s.testDataWhole.classAttribute().numValues()];
            trueValuesPerClass = new int[classValues.length];
            for (int i = 0; i < s.testDataWhole.classAttribute().numValues(); ++i) {
                classValues[i] = s.testDataWhole.classAttribute().value(i);
                trueValuesPerClass[i] = 0;
            }
        }
        else {
            Printer.out("testDataWhole null, initializing ClassificationStats without class values saved");
        }
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

    public String confusionMatrixAsString(){
        String string = "\nPredicted values in each column\n";
//        Printer.out(" settings"+s+" fold: "+s.trainingDataFold);
        // Predicted value on top.
        char a = 'a';
        int cs = 5; // Cell-size.
        for (int predictedValue = 0; predictedValue < matrixSize; ++predictedValue){ // So, horizontally predicted values for the given true value. so true value on the right - rows
            if (trueValuesPerClass[predictedValue] == 0)
                continue;
            string += Format((char)(a+predictedValue)+" ", cs);
        }
        string += " True values in each row. ";
        for (int trueValue = 0; trueValue < matrixSize; ++trueValue){
            if (trueValuesPerClass[trueValue] == 0)
                continue;
            string += "\n";
            for (int predictedValue = 0; predictedValue < matrixSize; ++predictedValue){ // So, horizontally predicted values for the given true value. so true value on the right - rows
                if (trueValuesPerClass[predictedValue] == 0)
                    continue;
                string += Format((confusionMatrix[trueValue][predictedValue])+" ", cs);
            }
            string += " "+(char)(a+trueValue)+" - "
                    +" TP "+  Format(String.format("%.1f", 100 * confusionMatrix[trueValue][trueValue] / (float) trueValuesPerClass[trueValue]), 4)
                            +" "+classValues[trueValue];
//            string += " "+(a+trueValue)+" "+s.trainingDataFold.classAttribute().value(trueValue);
        }
        return string;
    }
}
