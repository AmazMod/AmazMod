package com.amazmod.service.sleep.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.amazmod.service.sleep.sleepService;
import com.amazmod.service.sleep.sleepStore;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import amazmod.com.transport.data.SleepData;

public class heartrate implements SensorEventListener {

    private int currentAccuracy = 2;

    public void registerListener(Context context){
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 5 * 60 * 1000 /*5m*/);
    }

    public void unregisterListener(Context context){
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sleepStore.isSuspended())
            return;
        float hr = sensorEvent.values[0];
        if(isAccuracyValid()){
            SleepData sleepData = new SleepData();
            sleepData.setAction(SleepData.actions.ACTION_HRDATA_UPDATE);
            sleepData.setHrdata(new float[]{hr});
            Logger.debug("Sending sleep hr data to phone...");
            sleepService.send(sleepData.toDataBundle(new DataBundle()));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        currentAccuracy = i;
    }

    private boolean isAccuracyValid(){
        //Disabled for testing purposes
        return true; //currentAccuracy >= 1 && currentAccuracy <= 3;
    }
}
