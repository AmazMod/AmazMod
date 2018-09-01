package com.edotassi.amazmod.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.BatteryStatusEntity_Table;
import com.edotassi.amazmod.db.model.WatchfaceDataEntity;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.Watchface;
import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import amazmod.com.transport.data.BatteryData;
import amazmod.com.transport.data.WatchfaceData;

public class WatchfaceReceiver extends BroadcastReceiver {

    private Logger log = Logger.get(WatchfaceReceiver.class);

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction() == null) {
            if (!Watch.isInitialized()) {
                Watch.init(context);
            }

            Watch.get().sendWatchfaceData().continueWith(new Continuation<Watchface, Object>() {
                @Override
                public Object then(@NonNull Task<Watchface> task) throws Exception {
                    if (task.isSuccessful()) {
                        Watchface watchfaceData = task.getResult();
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
}
