package com.edotassi.amazmod.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.event.Watchface;
import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.pixplicity.easyprefs.library.Prefs;

import amazmod.com.transport.data.WatchfaceData;

public class WatchfaceReceiver extends BroadcastReceiver {

    private Logger log = Logger.get(WatchfaceReceiver.class);

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction() == null) {
            if (!Watch.isInitialized()) {
                Watch.init(context);
            }

            // Get data
            int battery = getPhoneBattery(context);
            String alarm = getPhoneAlarm(context);

            // Put data to bundle
            WatchfaceData watchfaceData = new WatchfaceData();
            watchfaceData.setBattery(battery);
            watchfaceData.setAlarm(alarm);

            Watch.get().sendWatchfaceData(watchfaceData).continueWith(new Continuation<Watchface, Object>() {
                @Override
                public Object then(@NonNull Task<Watchface> task) throws Exception {
                    if (task.isSuccessful()) {
                        // Returned data
                        //Watchface watchfaceData = task.getResult();
                        //updateWatchfaceData(watchfaceData);
                    } else {
                        WatchfaceReceiver.this.log.e(task.getException(), "failed sending watchface data");
                    }
                    return null;
                }
            });
        } else {
            startWatchfaceReceiver(context);
        }

        Log.d(Constants.TAG, "WatchfaceDataReceiver onReceive");
    }

    public static void startWatchfaceReceiver(Context context) {
        int syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_BACKGROUND_SYNC_INTERVAL, "15"));

        AmazModApplication.timeLastWatchfaceDataSend = Prefs.getLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, 0L);

        long delay = ((long) syncInterval * 60000L) - SystemClock.elapsedRealtime() - AmazModApplication.timeLastWatchfaceDataSend;

        Log.i(Constants.TAG, "WatchfaceDataReceiver times: " + SystemClock.elapsedRealtime() + " / " + AmazModApplication.timeLastWatchfaceDataSend);

        if (delay < 0) delay = 0;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmWatchfaceIntent = new Intent(context, WatchfaceReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmWatchfaceIntent, 0);

        try {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                    (long) syncInterval * 60000L, pendingIntent);
        } catch (NullPointerException e) {
            Log.e(Constants.TAG, "WatchfaceDataReceiver setRepeating exception: " + e.toString());
        }
    }

    public int getPhoneBattery(Context context){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }

    public String getPhoneAlarm(Context context){
        String nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        return nextAlarm;
    }
}
