package com.amazmod.service.sleep.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import com.amazmod.service.sleep.sleepService;
import com.amazmod.service.sleep.sleepStore;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import amazmod.com.transport.data.SleepData;

public class heartrate implements SensorEventListener {

    private int currentAccuracy = 2;
    private int currentValue;
    private float[] currentArray = new float[20];
    private SensorManager sm;
    private Handler handler;

    public void registerListener(Context context){
        Logger.debug("Registering hr sensor...");
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        handler = new Handler();
        register();
    }

    private void register(){
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                , SensorManager.SENSOR_DELAY_FASTEST, 20 * 1000 * 1000);
    }

    public void unregisterListener(Context context){
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.unregisterListener(this);
        handler.removeCallbacksAndMessages(null); //Stop handler
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sleepStore.getInstance().isSuspended())
            return;
        if(isAccuracyValid())
            currentArray[currentValue++] = sensorEvent.values[0];
        if(currentValue == 20){
            SleepData sleepData = new SleepData();
            sleepData.setAction(SleepData.actions.ACTION_HRDATA_UPDATE);
            sleepData.setHrdata(currentArray);
            sleepService.send(sleepData.toDataBundle(new DataBundle()));
            sm.unregisterListener(this);
            handler.postDelayed(this::register, 5 * 60 * 1000 /*Register again in 5m*/);
            Logger.debug("Sending sleep hr data to phone...");
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
