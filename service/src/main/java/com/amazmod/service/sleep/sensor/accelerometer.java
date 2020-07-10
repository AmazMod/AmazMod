package com.amazmod.service.sleep.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.amazmod.service.sleep.sleepService;
import com.amazmod.service.sleep.sleepStore;
import com.amazmod.service.sleep.sleepUtils;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import amazmod.com.transport.data.SleepData;

import static java.lang.Math.sqrt;
import static java.lang.StrictMath.abs;

public class accelerometer implements SensorEventListener {
    private static final int samplingPeriodUs = SensorManager.SENSOR_DELAY_NORMAL;
    private static int maxReportLatencyUs = 10 * 1000 * 1000; //Initial will be 10s

    private SensorManager sm;
    private static float current_max_data;
    private static float current_max_raw_data;
    private static float lastX;
    private static float lastY;
    private static float lastZ;

    private Thread thread = new sendDataThread();

    public void registerListener(Context context){
        Logger.debug("Registering accelerometer listener...");
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                samplingPeriodUs, maxReportLatencyUs);
        thread.start();
    }

    public void unregisterListener(boolean killThread){
        if(sm != null)
            sm.unregisterListener(this);
        if(killThread)
            thread.interrupt();
    }

    public void setBatchSize(int size){
        maxReportLatencyUs = size * 10 * 1000 * 1000; //Set latency to batch size in microseconds
        if(sm == null)
            return;
        sm.flush(this);
        unregisterListener(false);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                samplingPeriodUs, maxReportLatencyUs);
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

    private class sendDataThread extends Thread {
        public void run(){
            while(!Thread.currentThread().isInterrupted() && sleepStore.isTracking()){
                try {
                    Thread.sleep(maxReportLatencyUs / 1000);
                } catch (InterruptedException e) {
                    break;
                }
                //Add current data
                sleepStore.addMaxData(current_max_data, current_max_raw_data);
                current_max_data = 0;
                current_max_raw_data = 0;
                //Check if batch is full to send data
                if(sleepStore.getMaxData().size() >= sleepStore.getBatchSize()){
                    SleepData sleepData = new SleepData();
                    sleepData.setAction(SleepData.actions.ACTION_DATA_UPDATE);
                    sleepData.setMax_data(sleepUtils.linkedToArray(sleepStore.getMaxData()));
                    sleepData.setMax_raw_data(sleepUtils.linkedToArray(sleepStore.getMaxRawData()));
                    sleepStore.resetMaxData();

                    sleepService.send(sleepData.toDataBundle(new DataBundle()));
                }
            }
        }
    }
}
