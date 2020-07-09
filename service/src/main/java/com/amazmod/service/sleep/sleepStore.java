package com.amazmod.service.sleep;

import android.content.Context;

import com.amazmod.service.sleep.sensor.sensorsStore;

import java.util.LinkedList;

public class sleepStore {
    private static sleepStore instance;

    public static sleepStore getInstance() {
        if(instance == null)
            instance = new sleepStore();
        return instance;
    }

    private long batchSize = 12;
    private boolean isSuspended;
    private boolean isTracking;
    private LinkedList<Float> acc_max_data = new LinkedList<>();
    private LinkedList<Float> acc_max_raw_data = new LinkedList<>();

    public boolean isTracking() {
        return isTracking;
    }

    public void setTracking(boolean IsTracking, Context context) {
        isTracking = IsTracking;
        if(IsTracking){
            sensorsStore.getAccelerometer().registerListener(context);
            //sensorsStore.getHrSensor().registerListener(context);
            isSuspended = false;
        } else {
            sensorsStore.getAccelerometer().unregisterListener(context);
            //sensorsStore.getHrSensor().unregisterListener(context);
            batchSize = 12;
        }
    }

    public void addMaxData(float max_data, float max_raw_data){
        acc_max_data.add(max_data);
        acc_max_raw_data.add(max_raw_data);
    }

    public LinkedList<Float> getMaxData(){
        return acc_max_data;
    }

    public LinkedList<Float> getMaxRawData(){
        return acc_max_raw_data;
    }

    public void resetMaxData(){
        acc_max_data = new LinkedList<>();
        acc_max_raw_data = new LinkedList<>();
    }

    public void setBatchSize(long BatchSize){
        batchSize = BatchSize;
    }

    public long getBatchSize(){
        return batchSize;
    }

    public void setSuspended(boolean IsSuspended, Context context){
        isSuspended = IsSuspended;
        if(IsSuspended){
            sensorsStore.getAccelerometer().unregisterListener(context);
            //sensorsStore.getHrSensor().unregisterListener(context);
        } else {
            sensorsStore.getAccelerometer().registerListener(context);
            //sensorsStore.getHrSensor().registerListener(context);
        }
    }

    public boolean isSuspended(){
        return isSuspended;
    }
}
