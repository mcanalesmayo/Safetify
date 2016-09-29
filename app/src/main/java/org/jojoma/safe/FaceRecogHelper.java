package org.jojoma.safe;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Helps to manage the measured parameters
 */
public class FaceRecogHelper {

    private static final int LATEST_MEASURES_SIZE = 50;
    private static final int EYE_BLINK_THRESHOLD = 5;
    private static final int IS_EYE_BLINK = 65;

    /**
     *  Calibrated parameters are the references
     */
    private static Float leftEyeMeasureReference;
    private static Float rightEyeMeasureReference;
    private static Float headIncMeasureReference;
    private static Float leftBlinkMeasureReference;
    private static Float rightBlinkMeasureReference;

    /**
     *  Thresholds
     */
    private static Integer leftEyeMeasureThreshold;
    private static Integer rightEyeMeasureThreshold;
    private static Integer headIncMeasureThreshold;
    private static Integer leftBlinkMeasureThreshold;
    private static Integer rightlinkMeasureThreshold;

    /**
     *  Latest measures
     */
    private static Queue<Integer> latestLeftEyeMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);
    private static Queue<Integer> latestRightEyeMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);
    private static Queue<Integer> latestHeadIncMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);
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
        Integer[] arr = (Integer[]) latestLeftEyeMeasures.toArray();
        leftEyeMeasureReference = getMean(arr);

        arr = (Integer[]) latestRightEyeMeasures.toArray();
        rightEyeMeasureReference = getMean(arr);

        arr = (Integer[]) latestHeadIncMeasures.toArray();
        headIncMeasureReference = getMean(arr);
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
     * Adds a measure taken on the head inclination
     * @param measure measured parameter
     */
    public static void addHeadIncMeasure(Integer measure){
        if (latestHeadIncMeasures.size() == LATEST_MEASURES_SIZE){
            latestHeadIncMeasures.remove();
        }
        latestHeadIncMeasures.add(measure);
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
     * Gets the difference between the mean and the reference on the head inclination parameter
     * @return difference between the mean and the reference on the head inclination parameter
     */
    public static Float headIncThresholdExcess(){
        return getGenericThresholdExcess(latestHeadIncMeasures, headIncMeasureReference);
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
        Integer[] arr = (Integer[]) q.toArray();
        return getMean(arr) - ref;
    }

    private static int getBlinkThresholdExcess(Queue<Integer> q){
        /* Inefficient way */
        /* TO DO: improve performance saving results each time this func is called */
        int numBlinks = 0;

        Integer j = null;
        for(Integer i : q){
            if (j == null) continue;
            // Detectamos parpadeo
            if (i >= IS_EYE_BLINK && j < IS_EYE_BLINK) numBlinks+=1;
            j = i;
        }

        return numBlinks - EYE_BLINK_THRESHOLD;
    }
}
