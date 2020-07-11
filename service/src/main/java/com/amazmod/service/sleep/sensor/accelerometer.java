package com.amazmod.service.sleep.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

import com.amazmod.service.sleep.sleepService;
import com.amazmod.service.sleep.sleepStore;
import com.amazmod.service.sleep.sleepUtils;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import amazmod.com.transport.data.SleepData;

import static java.lang.Math.sqrt;
import static java.lang.StrictMath.abs;

public class accelerometer implements SensorEventListener {
    private static final int secondsPerMaxValue = 10;
    private static final int samplingPeriodUs = 200_000_000; //200ms
    private static int maxReportLatencyUs = secondsPerMaxValue * 1000_000;

    private SensorManager sm;
    private static float current_max_data;
    private static float current_max_raw_data;
    private static float lastX;
    private static float lastY;
    private static float lastZ;
    private static long latestSaveBatch;

    public void registerListener(Context context) {
        Logger.debug("Registering accelerometer listener...");
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                samplingPeriodUs, maxReportLatencyUs);
        latestSaveBatch = SystemClock.elapsedRealtimeNanos();
    }

    public void unregisterListener(){
        if(sm != null)
            sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sleepStore.isSuspended())
            return;
        Logger.debug("Received accelerometer values"); //Just for testing
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
        if(sensorEvent.timestamp - latestSaveBatch >= (long) secondsPerMaxValue * 1_000_000_000 /*To nanos*/) {
            //Add current data
            sleepStore.addMaxData(current_max_data, current_max_raw_data);
            current_max_data = 0;
            current_max_raw_data = 0;
            latestSaveBatch = sensorEvent.timestamp;

            checkAndSendBatch();
        }
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}
