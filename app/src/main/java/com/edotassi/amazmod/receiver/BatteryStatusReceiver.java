package com.edotassi.amazmod.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.event.RequestBatteryStatus;
import com.pixplicity.easyprefs.library.Prefs;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class BatteryStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //LoggerScoped.get(BatteryStatusReceiver.class).debug("started");

        if (intent.getAction() == null) {
            //HermesEventBus.getDefault().connectApp(context, Constants.PACKAGE);
            HermesEventBus.getDefault().post(new RequestBatteryStatus());
        }
        else {
            startBatteryReceiver(context);
        }

        Log.d(Constants.TAG, "BatteryStatusReceiver onReceive");
    }

    public static void startBatteryReceiver(Context context) {

        AmazModApplication.syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_BATTERY_BACKGROUND_SYNC_INTERVAL, "60"));
        AmazModApplication.timeLastSync = Prefs.getLong(Constants.PREF_TIME_LAST_SYNC, 0L);

        long delay = ((long) AmazModApplication.syncInterval * 60000L) - SystemClock.elapsedRealtime() - AmazModApplication.timeLastSync;

        Log.i(Constants.TAG, "BatteryStatusReceiver times: " + SystemClock.elapsedRealtime() + " / " + AmazModApplication.timeLastSync);

        if (delay < 0 ) delay = 0;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmBatteryIntent = new Intent(context, BatteryStatusReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmBatteryIntent, 0);

        try {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                    (long) AmazModApplication.syncInterval * 60000L, pendingIntent);
        } catch (NullPointerException e) {
            Log.e(Constants.TAG, "BatteryStatusReceiver setRepeating exception: " + e.toString());
        }
    }
}
