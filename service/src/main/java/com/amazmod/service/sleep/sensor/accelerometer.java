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
    private static float current_max_data;
    private static float current_max_raw_data;
    private static float lastX;
    private static float lastY;
    private static float lastZ;
    private Thread thread;

    public void registerListener(Context context){
        Logger.debug("Registering accelerometer listener...");
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                1000 * 1000 / 10 /*10 values per second*/, 10 * 1000 * 1000 /*10s in us*/);
        thread = new sendDataThread(10 * 1000);
        thread.start();
    }

    public void unregisterListener(Context context){
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.unregisterListener(this);
        thread.interrupt();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sleepStore.getInstance().isSuspended())
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

    private static class sendDataThread extends Thread{

        private int interval;

        private sendDataThread(int interval){
            this.interval = interval;
        }

        public void run(){
            while(sleepStore.getInstance().isTracking() && !Thread.currentThread().isInterrupted()){
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ignored){}
                sleepStore.getInstance().addMaxData(current_max_data, current_max_raw_data);
                current_max_data = 0;
                current_max_raw_data = 0;

                if(sleepStore.getInstance().getMaxData().size() >= sleepStore.getInstance().getBatchSize()){
                    SleepData sleepData = new SleepData();
                    sleepData.setAction(SleepData.actions.ACTION_DATA_UPDATE);
                    sleepData.setMax_data(sleepUtils.linkedToArray(sleepStore.getInstance().getMaxData()));
                    sleepData.setMax_raw_data(sleepUtils.linkedToArray(sleepStore.getInstance().getMaxRawData()));
                    sleepStore.getInstance().resetMaxData();
                    Logger.debug("Sending sleep accelerometer data to phone...");
                    sleepService.send(sleepData.toDataBundle(new DataBundle()));
                }
            }
        }
    }
}
