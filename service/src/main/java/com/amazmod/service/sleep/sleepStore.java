package com.amazmod.service.sleep;

import android.content.Context;

import com.amazmod.service.sleep.sensor.sensorsStore;

import java.util.LinkedList;

public class sleepStore {
    private static long batchSize = 1;
    private static boolean isSuspended;
    private static boolean isTracking;
    private static LinkedList<Float> acc_max_data = new LinkedList<>();
    private static LinkedList<Float> acc_max_raw_data = new LinkedList<>();

    public static boolean isTracking() {
        return isTracking;
    }

    public static void setTracking(boolean IsTracking, Context context) {
        if(IsTracking){
            if(!isTracking) //Don't start sensors again if it was listening
                sleepUtils.setSensorsState(true, context);
            isSuspended = false;
        } else {
            sleepUtils.setSensorsState(false, context);
            batchSize = 1;
        }
        isTracking = IsTracking;
    }

    public static void addMaxData(float max_data, float max_raw_data){
        acc_max_data.add(max_data);
        acc_max_raw_data.add(max_raw_data);
    }

    public static LinkedList<Float> getMaxData(){
        return acc_max_data;
    }

    public static LinkedList<Float> getMaxRawData(){
        return acc_max_raw_data;
    }

    public static void resetMaxData(){
        acc_max_data = new LinkedList<>();
        acc_max_raw_data = new LinkedList<>();
    }

    public static void setBatchSize(long BatchSize){
        long oldBatchSize = batchSize;
        batchSize = BatchSize;
        if(oldBatchSize != batchSize)
            sensorsStore.getAccelerometer().setBatchSize(BatchSize);
    }

    public static long getBatchSize(){
        return batchSize;
    }

    public static void setSuspended(boolean IsSuspended, Context context){
        isSuspended = IsSuspended;
        if(IsSuspended)
            sleepUtils.setSensorsState(false, context);
        else
            sleepUtils.setSensorsState(true, context);
    }

    public static boolean isSuspended(){
        return isSuspended;
    }
}
