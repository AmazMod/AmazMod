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
import amazmod.com.transport.Constants;
import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.BatteryStatusEntity_Table;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import amazmod.com.transport.data.BatteryData;

public class BatteryStatusReceiver extends BroadcastReceiver {

    private Logger log = Logger.get(BatteryStatusReceiver.class);

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction() == null) {
            if (!Watch.isInitialized()) {
                Watch.init(context);
            }

            Watch.get().getBatteryStatus().continueWith(new Continuation<BatteryStatus, Object>() {
                @Override
                public Object then(@NonNull Task<BatteryStatus> task) throws Exception {
                    if (task.isSuccessful()) {
                        BatteryStatus batteryStatus = task.getResult();
                        updateBattery(batteryStatus);
                    } else {
                        BatteryStatusReceiver.this.log.e(task.getException(), "failed reading battery status");
                    }
                    return null;
                }
            });
        } else {
            startBatteryReceiver(context);
        }

        Log.d(Constants.TAG, "BatteryStatusReceiver onReceive");
    }

    public static void startBatteryReceiver(Context context) {
        int syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_BATTERY_BACKGROUND_SYNC_INTERVAL, "60"));
        AmazModApplication.timeLastSync = Prefs.getLong(Constants.PREF_TIME_LAST_SYNC, 0L);

        long delay = ((long) syncInterval * 60000L) - SystemClock.elapsedRealtime() - AmazModApplication.timeLastSync;

        Log.i(Constants.TAG, "BatteryStatusReceiver times: " + SystemClock.elapsedRealtime() + " / " + AmazModApplication.timeLastSync);

        if (delay < 0) delay = 0;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmBatteryIntent = new Intent(context, BatteryStatusReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmBatteryIntent, 0);

        try {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                    (long) syncInterval * 60000L, pendingIntent);
        } catch (NullPointerException e) {
            Log.e(Constants.TAG, "BatteryStatusReceiver setRepeating exception: " + e.toString());
        }
    }

    private void updateBattery(BatteryStatus batteryStatus) {
        BatteryData batteryData = batteryStatus.getBatteryData();
        BatteryStatusReceiver.this.log.d("batteryStatus: " + batteryData.getLevel());
        BatteryStatusReceiver.this.log.d("charging: " + batteryData.isCharging());
        BatteryStatusReceiver.this.log.d("usb: " + batteryData.isUsbCharge());
        BatteryStatusReceiver.this.log.d("ac: " + batteryData.isAcCharge());
        BatteryStatusReceiver.this.log.d("dateLastCharge: " + batteryData.getDateLastCharge());

        long date = System.currentTimeMillis();

        BatteryStatusEntity batteryStatusEntity = new BatteryStatusEntity();
        batteryStatusEntity.setAcCharge(batteryData.isAcCharge());
        batteryStatusEntity.setCharging(batteryData.isCharging());
        batteryStatusEntity.setDate(date);
        batteryStatusEntity.setLevel(batteryData.getLevel());
        batteryStatusEntity.setDateLastCharge(batteryData.getDateLastCharge());

        //Log.d(Constants.TAG,"TransportService batteryStatus: " + batteryStatus.toString());

        try {
            BatteryStatusEntity storeBatteryStatusEntity = SQLite
                    .select()
                    .from(BatteryStatusEntity.class)
                    .where(BatteryStatusEntity_Table.date.is(date))
                    .querySingle();

            if (storeBatteryStatusEntity == null) {
                FlowManager.getModelAdapter(BatteryStatusEntity.class).insert(batteryStatusEntity);
            }
        } catch (Exception ex) {
            //TODO add crashlitics
            BatteryStatusReceiver.this.log.e(ex, "TransportService batteryStatus exception: " + ex.toString());
        }
        //Save time of last sync
        Prefs.putLong(Constants.PREF_TIME_LAST_SYNC, SystemClock.elapsedRealtime());
    }
}
