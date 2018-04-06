package com.edotasx.amazfit.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.preference.PreferenceManager;

/**
 * Created by edoardotassinari on 06/04/18.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "BootBroadcast receiver started");

        initiateBatteryStats(context);
    }

    //TODO merge in helper class
    private void initiateBatteryStats(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BatteryStatsReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        boolean enable = !PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_BACKGROUND_SYNC, false) ||
                !PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_BATTERY_CHART, false);

        if (enable) {
            int interval = PreferenceManager.getInt(context, Constants.PREFERENCE_BATTERY_BACKGROUND_SYNC_INTERVAL, 30);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval * 60 * 1000, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}
