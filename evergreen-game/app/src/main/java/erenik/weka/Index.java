package erenik.weka;

/** Indices for instances that have been stripped of the timestamp.
 * Created by Emil on 2017-04-05.
 */
public class Index {
    public static final int ACC_MIN = 0,
        ACC_MAX = 1,
        ACC_AVG = 2,
            ACC_STD = 3,
    GYR_MIN = 4,
    GYR_MAX = 5,
    GYR_AVG = 6,
    GYR_STD = 7;

        /*
    inst.setValue(0, sfToClassify.accMin); // Acc Min
    inst.setValue(1, sfToClassify.accMax); // Acc Max
    inst.setValue(2, sfToClassify.accAvg); // Acc Avg
    inst.setValue(3, sfToClassify.accStdev); // Avg Stdev
    boolean evaluateGyro = sfToClassify.gyroAvg != 0; // Check if gyro data is available, if just 0, then use the acc-only classifier.
    double result;
    if (evaluateGyro) {
        inst.setValue(4, sfToClassify.gyroMin);
        inst.setValue(5, sfToClassify.gyroMax);
        inst.setValue(6, sfToClassify.gyroAvg);
        inst.setValue(7, sfToClassify.gyroStdev);
*/

}
