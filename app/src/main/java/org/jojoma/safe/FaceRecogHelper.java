package org.jojoma.safe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Helps to manage the measured parameters
 */
public class FaceRecogHelper {

    private static final int LATEST_MEASURES_SIZE = 5;
    private static final int EYE_BLINK_THRESHOLD = 4;
    private static final int GENERIC_EYE_ALERT_VALUE = 65;
    private static final int GENERIC_EYE_VALUE = 30;
    private static final int GENERIC_ROLL_VALUE = 0;
    private static final int GENERIC_PITCH_VALUE = 0;
    private static final int GENERIC_YAW_VALUE = 0;

    /**
     *  Calibrated parameters are the references
     */
    private static Float leftEyeMeasureReference;
    private static Float rightEyeMeasureReference;
    private static Float rollMeasureReference;
    private static Float pitchMeasureReference;
    private static Float yawMeasureReference;
    private static Float leftBlinkMeasureReference;
    private static Float rightBlinkMeasureReference;

    /**
     *  Thresholds
     */
    private static Integer leftEyeMeasureThreshold;
    private static Integer rightEyeMeasureThreshold;
    private static Integer rollMeasureThreshold;
    private static Integer pitchMeasureThreshold;
    private static Integer yawMeasureThreshold;
    private static Integer leftBlinkMeasureThreshold;
    private static Integer rightlinkMeasureThreshold;

    /**
     *  Latest measures
     */
    private static Queue<Integer> latestLeftEyeMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);
    private static Queue<Integer> latestRightEyeMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);
    private static Queue<Integer> latestRollMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);
    private static Queue<Integer> latestPitchMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);
    private static Queue<Integer> latestYawMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);

    private static Float getMean(Integer[] arr){
        Float sum = 0F;

        for(Integer i : arr){
            sum += i;
        }
        return sum / LATEST_MEASURES_SIZE;
    }

    /**
     * Sets the blink measure reference
     */
    public static void setAllMeasureReferences(){
        Integer[] arr = latestLeftEyeMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        leftEyeMeasureReference = getMean(arr);

        arr = latestRightEyeMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        rightEyeMeasureReference = getMean(arr);

        arr = latestRollMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        rollMeasureReference = getMean(arr);

        arr = latestPitchMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        pitchMeasureReference = getMean(arr);

        arr = latestYawMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        yawMeasureReference = getMean(arr);

        leftBlinkMeasureReference = 1F;
        rightBlinkMeasureReference = 1F;
    }
    
    /**
     * Adds a measure taken on the left eye
     * @param measure measured parameter
     */
    public static void addLeftEyeMeasure(Integer measure){
        if (latestLeftEyeMeasures.size() == LATEST_MEASURES_SIZE){
            latestLeftEyeMeasures.remove();
        }
        latestLeftEyeMeasures.add(measure);
    }

    /**
     * Adds a measure taken on the right eye
     * @param measure measured parameter
     */
    public static void addRightEyeMeasure(Integer measure){
        if (latestRightEyeMeasures.size() == LATEST_MEASURES_SIZE){
            latestRightEyeMeasures.remove();
        }
        latestRightEyeMeasures.add(measure);
    }

    /**
     * Adds a measure taken on the roll
     * @param measure measured parameter
     */
    public static void addRollMeasure(Integer measure){
        if (latestRollMeasures.size() == LATEST_MEASURES_SIZE){
            latestRollMeasures.remove();
        }
        latestRollMeasures.add(measure);
    }

    /**
     * Adds a measure taken on the pitch
     * @param measure measured parameter
     */
    public static void addPitchMeasure(Integer measure){
        if (latestPitchMeasures.size() == LATEST_MEASURES_SIZE){
            latestPitchMeasures.remove();
        }
        latestPitchMeasures.add(measure);
    }


    /**
     * Adds a measure taken on the yaw
     * @param measure measured parameter
     */
    public static void addYawMeasure(Integer measure){
        if (latestYawMeasures.size() == LATEST_MEASURES_SIZE){
            latestYawMeasures.remove();
        }
        latestYawMeasures.add(measure);
    }

    /**
     * Gets the difference between the mean and the reference on the left eye parameter
     * @return difference between the mean and the reference on the left eye parameter
     */
    public static Float leftEyeThresholdExcess(){
        return getGenericThresholdExcess(latestLeftEyeMeasures, leftEyeMeasureReference);
    }

    /**
     * Gets the difference between the mean and the reference on the right eye parameter
     * @return difference between the mean and the reference on the right eye parameter
     */
    public static Float rightEyeThresholdExcess(){
        return getGenericThresholdExcess(latestRightEyeMeasures, rightEyeMeasureReference);
    }

    /**
     * Gets the difference between the mean and the reference on the left eye blink
     * @return difference between the mean and the reference on the left eye blink
     */
    public static int leftBlinkThresholdExcess(){
        return getBlinkThresholdExcess(latestLeftEyeMeasures);
    }

    /**
     * Gets the difference between the mean and the reference on the right eye blink
     * @return difference between the mean and the reference on the right eye blink
     */
    public static int rightBlinkThresholdExcess() {
        return getBlinkThresholdExcess(latestRightEyeMeasures);
    }

    private static Float getGenericThresholdExcess(Queue<Integer> q, Float ref){
        Integer[] arr = q.toArray(new Integer[LATEST_MEASURES_SIZE]);
        return getMean(arr) - (GENERIC_EYE_ALERT_VALUE + (ref - GENERIC_EYE_VALUE));
    }

    private static int getBlinkThresholdExcess(Queue<Integer> q){
        /* Inefficient way */
        /* TO DO: improve performance saving results each time this func is called */
        int numBlinks = 0;

        Integer j = null;
        for(Integer i : q){
            if (j == null) continue;
            // Detect blink
            if (i >= GENERIC_EYE_ALERT_VALUE && j < GENERIC_EYE_ALERT_VALUE) numBlinks+=1;
            j = i;
        }

        return numBlinks - EYE_BLINK_THRESHOLD;
    }

    /**
     * Gets the criticity of the situation in order to send an alert to the driver
     * @return a list whose first element is a boolean which indicates whether an alert should be popped up
     *          and whose second element is a number which indicates the criticity of the alert in a scale from 0 to 100
     */
    public static List<Object> getAlertLevel(){
        List<Object> res = new ArrayList<Object>();

        Float leftEyeThresholdExcess = getGenericThresholdExcess(latestLeftEyeMeasures, leftEyeMeasureReference);
        Float rightEyeThresholdExcess = getGenericThresholdExcess(latestRightEyeMeasures, rightEyeMeasureReference);
        int leftBlinkThresholdExcess = getBlinkThresholdExcess(latestLeftEyeMeasures);
        int rightBlinkThresholdExcess = getBlinkThresholdExcess(latestRightEyeMeasures);
        int leftBlink = leftBlinkThresholdExcess + EYE_BLINK_THRESHOLD;
        int rightBlink = rightBlinkThresholdExcess + EYE_BLINK_THRESHOLD;
        getGenericThresholdExcess(latestRollMeasures, rollMeasureReference);
        getGenericThresholdExcess(latestPitchMeasures, pitchMeasureReference);
        getGenericThresholdExcess(latestYawMeasures, yawMeasureReference);

        if (leftEyeThresholdExcess > 0
                || rightEyeThresholdExcess > 0
                || leftBlinkThresholdExcess > 0
                || rightBlinkThresholdExcess > 0)
            res.add(0, true);
        else res.add(0, false);

        Integer[] vals = latestLeftEyeMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        Integer[] latestVals = new Integer[4];
        latestVals[3] = vals[LATEST_MEASURES_SIZE - 1];
        latestVals[2] = vals[LATEST_MEASURES_SIZE - 2];
        latestVals[1] = vals[LATEST_MEASURES_SIZE - 3];
        latestVals[0] = vals[LATEST_MEASURES_SIZE - 4];
        double leftEyeMedian = median(latestVals);

        vals = latestRightEyeMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        latestVals[3] = vals[LATEST_MEASURES_SIZE - 1];
        latestVals[2] = vals[LATEST_MEASURES_SIZE - 2];
        latestVals[1] = vals[LATEST_MEASURES_SIZE - 3];
        latestVals[0] = vals[LATEST_MEASURES_SIZE - 4];
        double rightEyeMedian = median(latestVals);

        vals = latestRollMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        latestVals[3] = vals[LATEST_MEASURES_SIZE - 1];
        latestVals[2] = vals[LATEST_MEASURES_SIZE - 2];
        latestVals[1] = vals[LATEST_MEASURES_SIZE - 3];
        latestVals[0] = vals[LATEST_MEASURES_SIZE - 4];
        double rollMedian = median(latestVals);

        vals = latestPitchMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        latestVals[3] = vals[LATEST_MEASURES_SIZE - 1];
        latestVals[2] = vals[LATEST_MEASURES_SIZE - 2];
        latestVals[1] = vals[LATEST_MEASURES_SIZE - 3];
        latestVals[0] = vals[LATEST_MEASURES_SIZE - 4];
        double pitchMedian = median(latestVals);

        vals = latestYawMeasures.toArray(new Integer[LATEST_MEASURES_SIZE]);
        latestVals[3] = vals[LATEST_MEASURES_SIZE - 1];
        latestVals[2] = vals[LATEST_MEASURES_SIZE - 2];
        latestVals[1] = vals[LATEST_MEASURES_SIZE - 3];
        latestVals[0] = vals[LATEST_MEASURES_SIZE - 4];
        double yawMedian = median(latestVals);

        res.add(1, (leftEyeMedian + rightEyeMedian + leftBlink * 16.25 + rightBlink * 16.25) / 1.5);

        return res;
    }

    public static double median(Integer[] arr) {
        Arrays.sort(arr);

        // Calculate median (middle number)
        double median = 0;
        double pos1 = Math.floor((arr.length - 1.0) / 2.0);
        double pos2 = Math.ceil((arr.length - 1.0) / 2.0);
        if (pos1 == pos2 ) {
            median = arr[(int) pos1];
        } else {
            median = (arr[(int) pos1] + arr[(int) pos2]) / 2.0 ;
        }

        return median;
    }
}
