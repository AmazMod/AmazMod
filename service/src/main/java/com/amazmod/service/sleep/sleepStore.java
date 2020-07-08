package com.amazmod.service.sleep;

import java.util.LinkedList;

public class sleepStore {
    private static long batchSize = 120;
    private static boolean isSuspended;
    private static boolean isTracking;
    private static LinkedList<Float> acc_max_data = new LinkedList<>();
    private static LinkedList<Float> acc_max_raw_data = new LinkedList<>();

    public static boolean isTracking() {
        return isTracking;
    }

    public static void setTracking(boolean isTracking) {
        sleepStore.isTracking = isTracking;
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
        batchSize = BatchSize;
    }

    public static long getBatchSize(){
        return batchSize;
    }

    public static void setSuspended(boolean IsSuspended){
        isSuspended = IsSuspended;
        //TODO Stop/Start sensors here
    }

    public static boolean isSuspended(){
        return isSuspended;
    }
}
