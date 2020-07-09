package com.amazmod.service.sleep.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import com.amazmod.service.sleep.sleepService;
import com.amazmod.service.sleep.sleepStore;
import com.amazmod.service.sleep.sleepUtils;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import amazmod.com.transport.data.SleepData;

import static java.lang.Math.sqrt;
import static java.lang.StrictMath.abs;

public class accelerometer implements SensorEventListener {
    private float current_max_data;
    private float current_max_raw_data;
    private float lastX;
    private float lastY;
    private float lastZ;
    private Handler handler;

    public void registerListener(Context context){
        Logger.debug("Registering accelerometer listener...");
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                1000 * 1000 / 10 /*10 values per second*/, 10 * 1000 * 1000 /*10s in us*/);
        handler = new Handler(context.getMainLooper());
        handler.postDelayed(new Runnable(){
            public void run(){
                if(!sleepStore.isTracking())
                    return; //If not tracking anymore stop loop to avoid it keep running
                handler.postDelayed(this, 10 * 1000 /*10s per float, as saa requests*/);
                if(sleepStore.isSuspended())
                    return;
                sleepStore.addMaxData(current_max_data, current_max_raw_data);
                current_max_data = 0;
                current_max_raw_data = 0;

                if(sleepStore.getMaxData().size() >= sleepStore.getBatchSize() / 10){
                    SleepData sleepData = new SleepData();
                    sleepData.setAction(SleepData.actions.ACTION_DATA_UPDATE);
                    sleepData.setMax_data(sleepUtils.linkedToArray(sleepStore.getMaxData()));
                    sleepData.setMax_raw_data(sleepUtils.linkedToArray(sleepStore.getMaxRawData()));
                    sleepStore.resetMaxData();
                    Logger.debug("Sending sleep accelerometer data to phone...");
                    sleepService.send(sleepData.toDataBundle(new DataBundle()));
                }
            }
        }, 10 * 1000 + 10 /*Leave some ms for batching to save first data*/);
    }

    public void unregisterListener(Context context){
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.unregisterListener(this);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sleepStore.isSuspended())
            return;
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        float max = abs(x - lastX) + abs(y - lastY) + abs(z - lastZ);
        if (max > current_max_data)
            current_max_data = max;

        float max_raw = (float) sqrt((x * x) + (y * y) + (z * z));
        if (max_raw > current_max_raw_data)
            current_max_raw_data = max_raw;

        lastX = x;
        lastY = y;
        lastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
