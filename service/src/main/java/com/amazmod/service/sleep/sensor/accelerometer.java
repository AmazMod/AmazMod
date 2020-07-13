package com.amazmod.service.sleep.sensor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.SystemClock;

import com.amazmod.service.sleep.alarm.alarmReceiver;
import com.amazmod.service.sleep.sleepConstants;
import com.amazmod.service.sleep.sleepService;
import com.amazmod.service.sleep.sleepStore;
import com.amazmod.service.sleep.sleepUtils;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import amazmod.com.transport.data.SleepData;

import static java.lang.Math.sqrt;
import static java.lang.StrictMath.abs;

public class accelerometer {
    private static long maxReportLatencyUs = 3_000_000; //3s

    private SensorManager sm;
    private static float current_max_data;
    private static float current_max_raw_data;
    private static float lastX;
    private static float lastY;
    private static float lastZ;
    private static long latestSaveBatch;
    private static long latestSaveBatchSleeping;

    private listener listener;

    public void registerListener(Context context) {
        Logger.debug("Registering accelerometer listener...");
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        register();
        latestSaveBatch = SystemClock.elapsedRealtimeNanos();
    }

    private void register(){
        listener = new listener();
        sm.registerListener(listener, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                sleepConstants.SAMPLING_PERIOD_US, (int) maxReportLatencyUs);
    }

    public void unregisterListener(){
        if(sm != null)
            sm.unregisterListener(listener);

    }

    public void setBatchSize(long size){
        /*if(size > sleepConstants.MAX_BATCH_SIZE) size = sleepConstants.MAX_BATCH_SIZE; //Set max size to 4 (40s)
        maxReportLatencyUs = (int) (size * 10 * 1000_000_000);
        unregisterListener();
        batchWaker.register(size);
        checkAndSendBatch();
        register();*/
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sleepStore.isSuspended())
            return;
        //Logger.debug("Received accelerometer values with timestamp " + sensorEvent.timestamp + " at time " + SystemClock.elapsedRealtimeNanos());
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

        //If latest time saving batch was >= 10s ago
        if(sensorEvent.timestamp - latestSaveBatch >= (long) sleepConstants.SECS_PER_MAX_VALUE * 1_000_000_000 /*To nanos*/) {
            addData();
            latestSaveBatch = sensorEvent.timestamp;
            checkAndSendBatch();
        } else if(System.currentTimeMillis() - latestSaveBatchSleeping >= 9 * 1000){
            addData();
            latestSaveBatch = sensorEvent.timestamp;
            latestSaveBatchSleeping = System.currentTimeMillis();
        }
    }

    private void addData(){
        sleepStore.addMaxData(current_max_data, current_max_raw_data);
        current_max_data = 0;
        current_max_raw_data = 0;
    }

    private void checkAndSendBatch(){
        //Send data if batch reached batch size
        if(sleepStore.getMaxData().size() >= sleepStore.getBatchSize()){
            SleepData sleepData = new SleepData();
            sleepData.setAction(SleepData.actions.ACTION_DATA_UPDATE);
            sleepData.setMax_data(sleepUtils.linkedToArray(sleepStore.getMaxData()));
            sleepData.setMax_raw_data(sleepUtils.linkedToArray(sleepStore.getMaxRawData()));
            sleepStore.resetMaxData();

            sleepService.send(sleepData.toDataBundle(new DataBundle()));
        }
    }

    private class listener implements SensorEventListener, SensorEventListener2 {
        @Override
        public void onSensorChanged(SensorEvent event) {
            accelerometer.this.onSensorChanged(event); //Pass event to the other class
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {} //Ignore

        @Override
        public void onFlushCompleted(Sensor sensor) {
            Logger.debug("Flush completed for sensor " + sensor.getName());
        }
    }
}
