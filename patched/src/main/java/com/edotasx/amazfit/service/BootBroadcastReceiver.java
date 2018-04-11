package com.edotasx.amazfit.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.nightscout.NightscoutReceiver;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.dataflow.model.health.process.Const;

/**
 * Created by edoardotassinari on 06/04/18.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "BootBroadcast receiver started");

        checkNightscout(context);
        checkBatteryStats(context);
    }

    private void checkNightscout(Context context) {
        boolean enable = PreferenceManager.getBoolean(context, Constants.PREFERENCE_NIGHTSCOUT_ENABLED, false);
        int interval = PreferenceManager.getInt(context, Constants.PREFERENCE_NIGHTSCOUT_INTERVAL_SYNC, 30);

        if (enable) {
            Log.d(Constants.TAG, "enabling Nightscout support, interval: " + interval);
        } else {
            Log.d(Constants.TAG, "disabling Nightscout support");
        }

        updateAlarmManager(context, NightscoutReceiver.class, enable, interval);
    }

    //TODO merge in helper class
    private void checkBatteryStats(Context context) {
        boolean enable = !PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_BACKGROUND_SYNC, false) ||
                !PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_BATTERY_CHART, false);
        int interval = PreferenceManager.getInt(context, Constants.PREFERENCE_BATTERY_BACKGROUND_SYNC_INTERVAL, 30);

        if (enable) {
            Log.d(Constants.TAG, "enabling battery stats support, interval: " + interval);
        } else {
            Log.d(Constants.TAG, "disabling battery stats support");
        }

        updateAlarmManager(context, BatteryStatsReceiver.class, enable, interval);
    }

    private void updateAlarmManager(Context context, Class receiver, boolean enable, int interval) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, receiver);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        if (enable) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval * 60 * 1000, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}
