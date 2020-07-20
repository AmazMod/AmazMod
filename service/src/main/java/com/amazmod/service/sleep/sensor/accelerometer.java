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
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import com.amazmod.service.sleep.alarm.alarmReceiver;
import com.amazmod.service.sleep.sleepConstants;
import com.amazmod.service.sleep.sleepService;
import com.amazmod.service.sleep.sleepStore;
import com.amazmod.service.sleep.sleepUtils;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import amazmod.com.transport.data.SleepData;

import static java.lang.Math.sqrt;
import static java.lang.StrictMath.abs;

public class accelerometer {
    private static final long maxReportLatencyUs = 195_000_000;

    private static float current_max_data;
    private static float current_max_raw_data;
    private static float lastX;
    private static float lastY;
    private static float lastZ;
    private static int latestSaveBatch = 0;

    private SensorManager sm;
    private listener listener;
    private Handler flushHandler;
    private int flushInterval;

    public void registerListener(Context context) {
        Logger.debug("Registering accelerometer listener...");
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        listener = new listener();
        sm.registerListener(listener, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                sleepConstants.SAMPLING_PERIOD_US, (int) maxReportLatencyUs);
        setupHandler();
    }

    public void setupHandler() {
        if (Looper.myLooper() == null) Looper.prepare();
        flushHandler = new Handler(Looper.getMainLooper());
        flushHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!sleepStore.isTracking()) return;
                if(!sleepStore.isSuspended()) flush();
                flushHandler.postDelayed(this, flushInterval);
            }
        }, 10);
    }

    public void unregisterListener() {
        if (sm != null && listener != null) sm.unregisterListener(listener);
        flushHandler.removeCallbacksAndMessages(null);
    }

    public void setBatchSize(long size) {
        if (size > sleepConstants.MAX_BATCH_SIZE) size = sleepConstants.MAX_BATCH_SIZE;
        flushInterval = (int) size * sleepConstants.SECS_PER_MAX_VALUE * 1000;
        flush();
    }

    public void flush() {
        if (sm != null && listener != null) sm.flush(listener);
        Logger.debug("Flushing accelerometer sensor...");
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sleepStore.isSuspended()) return;
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        //SAA processing
        float max = abs(x - lastX) + abs(y - lastY) + abs(z - lastZ);
        if (max > current_max_data) current_max_data = max;
        float max_raw = (float) sqrt((x * x) + (y * y) + (z * z));
        if (max_raw > current_max_raw_data) current_max_raw_data = max_raw;
        lastX = x;
        lastY = y;
        lastZ = z;

        int tsMillis = (int) (sensorEvent.timestamp / 1_000_000L);

        if (latestSaveBatch == 0) latestSaveBatch = tsMillis; //First value
        //If latest time saving batch was >= 10s ago
        if (tsMillis - latestSaveBatch >= sleepConstants.SECS_PER_MAX_VALUE * 1000 /*To millis*/) {
            Logger.debug(new SimpleDateFormat("hh:mm:ss", Locale.US).format(new Date()) + "- Added accelerometer values to batch");
            sleepStore.addMaxData(current_max_data, current_max_raw_data);
            current_max_data = 0;
            current_max_raw_data = 0;
            latestSaveBatch = tsMillis;
            checkAndSendBatch();
        }
    }

    private void checkAndSendBatch() {
        //Send data if batch reached batch size
        if (sleepStore.getMaxData().size() >= sleepStore.getBatchSize()) {
            SleepData sleepData = new SleepData();
            sleepData.setAction(SleepData.actions.ACTION_DATA_UPDATE);
            sleepData.setMax_data(sleepUtils.linkedToArray(sleepStore.getMaxData()));
            sleepData.setMax_raw_data(sleepUtils.linkedToArray(sleepStore.getMaxRawData()));
            sleepStore.resetMaxData();
            Logger.debug("Sending sleep batch to phone...");
            sleepService.send(sleepData.toDataBundle(new DataBundle()));
        }
    }

    private class listener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            accelerometer.this.onSensorChanged(event); //Pass event to the other class
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {} //Ignore
    }
}
