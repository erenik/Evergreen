package erenik.weka;

import erenik.util.EList;
import weka.core.Instances;

/**
 *
 * @author Emil
 */
public class Settings {
    // Yeah.
    public Instances trainingDataWhole = null; // Whole data to be used for the test.
    public Instances trainingDataFold = null; // Used for the actual tests / folds.
    // If true, will use testDataWhole and partition it, otherwise it will do predictions straight on the test-data.
    public boolean doNFoldCrossValidation = false;
    // For N-fold cross-validation.
    public int folds = 10, fold = 0; // The current fold.
    // Data to test for N-fold cross-validation.
    public Instances testDataWhole = null; // The entire data to be tested and trained into folds.
    public Instances testDataFold = null; // The fold of data to be tested for this specific fold.
    // Options for classifiers?
    String[] options = null; // Set to non-Null if you wanna use it.

    /// If true, assumes Idle if gyroStdev and accStdev are both null.
    boolean useNaiveIdleCheck = false;
    float naiveIdleThreshold = 0.01f;

    /// Size of history set for smoothing results (take away high-frequency noise).
    public int historySetSize = 0;
    /// # of samples slept/skipped and compared with for each sample taken.
    public int sleepSessions = 0;
    /// If true, make use of the history set size capabilities, calculating the average of x samples before sleeping, instead of averaging samples taken after each sleep.
    boolean forceAverageBeforeSleep = false;
    /// How much to alter the test data when predicting - to not use the base data if non-0.
    float randomizationDegree = 0.0f;
    boolean accelerometerOnly = false;
    boolean nullifyGyroDataDuringPrediction = false;
    boolean onlyWhereGyroDataIsPresent = false;
    boolean gyroscopeOnly = false;
    /// For further info.
    public String testDataSource = "";
    // TODO: Add a button/toggle for this in any UI that uses the service?
    public boolean normalizeAcceleration = true; // If true, divides accMin, accMax and accAvg by accAvg before training and testing.
    public EList<ClassConversion> classConversions = new EList<>();

    //    @override
    public String toString(){
        return "hss "+historySetSize+" ss"+sleepSessions+" rd"+randomizationDegree+" aO"+accelerometerOnly+" 0G"+nullifyGyroDataDuringPrediction;
    }


    void CopyFrom(Settings s) {
        this.useNaiveIdleCheck = s.useNaiveIdleCheck;
        this.naiveIdleThreshold = s.naiveIdleThreshold;
        this.folds = s.folds;
        this.historySetSize = s.historySetSize;
        this.sleepSessions = s.sleepSessions;
        this.randomizationDegree = s.randomizationDegree;
        this.forceAverageBeforeSleep = s.forceAverageBeforeSleep;
        this.accelerometerOnly = s.accelerometerOnly;
        this.nullifyGyroDataDuringPrediction = s.nullifyGyroDataDuringPrediction;
        this.normalizeAcceleration = s.normalizeAcceleration;
    }

    Settings Clone(){
        Settings s = new Settings();
        s.CopyFrom(this);
        return s;
    }
}