package com.amazmod.service.sleep.sensor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.SystemClock;

import com.amazmod.service.sleep.alarm.alarmReceiver;
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
    private static final int samplingPeriodUs = SensorManager.SENSOR_DELAY_NORMAL;
    private static int maxReportLatencyUs = secondsPerMaxValue * 1000 * 1000; //Initial value, changes

    private SensorManager sm;
    private static float current_max_data;
    private static float current_max_raw_data;
    private static float lastX;
    private static float lastY;
    private static float lastZ;
    private static long latestSaveBatch;

    private Context context;
    private alarm alarm = new alarm();

    public void registerListener(Context context) {
        Logger.debug("Registering accelerometer listener...");
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                samplingPeriodUs, maxReportLatencyUs);
        latestSaveBatch = SystemClock.elapsedRealtimeNanos();
        this.context = context;
        registerAlarm();
    }

    public void unregisterListener(){
        if(sm != null)
            sm.unregisterListener(this);
        alarm.cancelAlarm();
    }

    public void setBatchSize(int size){
        maxReportLatencyUs = size * 10 * 1000 * 1000; //Set latency to batch size in microseconds
        if(sm == null)
            return;
        sm.flush(this);
        unregisterListener();
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                samplingPeriodUs, maxReportLatencyUs);
        registerAlarm();
        checkAndSendBatch(false);
    }

    private void registerAlarm(){
        if(maxReportLatencyUs / 10_000_000 > 2)
            alarm.register(context); //Register alarm if batch size >2
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
        if(sensorEvent.timestamp - latestSaveBatch >= (long) secondsPerMaxValue * 1_000_000_000 /*To ns*/) {
            //Add current data
            sleepStore.addMaxData(current_max_data, current_max_raw_data);
            current_max_data = 0;
            current_max_raw_data = 0;
            latestSaveBatch = sensorEvent.timestamp;

            checkAndSendBatch(true);
        }
    }

    private void checkAndSendBatch(boolean registerAlarm){
        //Send data if batch reached batch size
        if(sleepStore.getMaxData().size() >= sleepStore.getBatchSize()){
            SleepData sleepData = new SleepData();
            sleepData.setAction(SleepData.actions.ACTION_DATA_UPDATE);
            sleepData.setMax_data(sleepUtils.linkedToArray(sleepStore.getMaxData()));
            sleepData.setMax_raw_data(sleepUtils.linkedToArray(sleepStore.getMaxRawData()));
            sleepStore.resetMaxData();

            sleepService.send(sleepData.toDataBundle(new DataBundle()));
            if(registerAlarm)
                registerAlarm(); //Register alarm for next event
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class alarm extends BroadcastReceiver {

        private AlarmManager alarmManager;
        private PendingIntent pendingIntent;

        public void register(Context context){
            Intent alarmIntent = new Intent(context, this.getClass());
            pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if(alarmManager != null)
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + (maxReportLatencyUs / 1000) - 4000,
                        pendingIntent);
        }

        public void cancelAlarm(){
            if(alarmManager != null)
                alarmManager.cancel(pendingIntent);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AmazMod:accelerometerWakeLock");
            wakeLock.acquire(5*1000 /*5 seconds*/);
        }
    }
}
