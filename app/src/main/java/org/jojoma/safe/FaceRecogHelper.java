package org.jojoma.safe;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Helps to manage the measured parameters
 */
public class FaceRecogHelper {

    public static final int LATEST_MEASURES_SIZE = 100;

    /**
     *  Calibrated parameters are the references
     */
    private static Float eyeMeasureReference;
    private static Float headIncMeasureReference;
    private static Float blinkMeasureReference;

    /**
     *  Thresholds
     */
    private static Integer eyeMeasureThreshold;
    private static Integer headIncMeasureThreshold;
    private static Integer blinkMeasureThreshold;

    /**
     *  Latest measures
     */
    private static Queue<Integer> latestEyeMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);
    private static Queue<Integer> latestHeadIncMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);
    private static Queue<Integer> latestBlinkMeasures = new ArrayBlockingQueue<Integer>(LATEST_MEASURES_SIZE);

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
        Integer[] arr = (Integer[]) latestEyeMeasures.toArray();
        eyeMeasureReference = getMean(arr);

        arr = (Integer[]) latestHeadIncMeasures.toArray();
        headIncMeasureReference = getMean(arr);

        arr = (Integer[]) latestBlinkMeasures.toArray();
        blinkMeasureReference = getMean(arr);
    }
    
    /**
     * Adds a measure taken on the eye
     * @param measure measured parameter
     */
    public static void addEyeMeasure(Integer measure){
        if (latestEyeMeasures.size() == LATEST_MEASURES_SIZE){
            latestEyeMeasures.remove();
            latestEyeMeasures.add(measure);
        }
    }

    /**
     * Adds a measure taken on the head inclination
     * @param measure measured parameter
     */
    public static void addHeadIncMeasure(Integer measure){
        if (latestHeadIncMeasures.size() == LATEST_MEASURES_SIZE){
            latestHeadIncMeasures.remove();
            latestHeadIncMeasures.add(measure);
        }
    }

    /**
     * Adds a measure taken on the blink
     * @param measure measured parameter
     */
    public static void addBlinkMeasure(Integer measure){
        if (latestBlinkMeasures.size() == LATEST_MEASURES_SIZE){
            latestBlinkMeasures.remove();
            latestBlinkMeasures.add(measure);
        }
    }

    /**
     * Gets the difference between the mean and the reference on the eye parameter
     * @return difference between the mean and the reference on the eye parameter
     */
    public static Float eyeThresholdExcess(){
        Integer[] arr = (Integer[]) latestEyeMeasures.toArray();
        return getMean(arr) - eyeMeasureReference;
    }

    /**
     * Gets the difference between the mean and the reference on the head inclination parameter
     * @return difference between the mean and the reference on the head inclination parameter
     */
    public static Float headIncThresholdExcess(){
        Integer[] arr = (Integer[]) latestHeadIncMeasures.toArray();
        return getMean(arr) - headIncMeasureReference;
    }

    /**
     * Gets the difference between the mean and the reference on the blink parameter
     * @return difference between the mean and the reference on the blink parameter
     */
    public static Float blinkThresholdExcess(){
        Integer[] arr = (Integer[]) latestBlinkMeasures.toArray();
        return getMean(arr) - blinkMeasureReference;
    }
}
