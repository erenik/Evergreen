/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package erenik.weka;

/**
 *
 * @author Emil
 */
public class ClassificationStats {
    int historySetSize = 0;
    float accuracy = 0; // Overall accuracy.
    int maxErrorsInSequence = 0; // Maximum errors in a row, before a correct prediction.
    int minErrorsInSequence = 0; // Minimum errors in a row, before a correct prediction returns.
    int good = 0;
    int testSize = 0;
}
