package com.edotassi.amazmod.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.edotassi.amazmod.AmazModApplication;
import amazmod.com.transport.Constants;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.BatteryStatusEntity_Table;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.tinylog.Logger;

import amazmod.com.transport.data.BatteryData;

public class BatteryStatusReceiver extends BroadcastReceiver {

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
                        batteryAlert(batteryStatus, context);
                    } else {
                        Logger.error(task.getException(), "failed reading battery status");
                    }
                    return null;
                }
            });
        } else {
            startBatteryReceiver(context);
        }

        Logger.debug("BatteryStatusReceiver onReceive");
    }

    public static void startBatteryReceiver(Context context) {
        int syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_BATTERY_BACKGROUND_SYNC_INTERVAL, "60"));
        AmazModApplication.timeLastSync = Prefs.getLong(Constants.PREF_TIME_LAST_SYNC, 0L);

        long delay = ((long) syncInterval * 60000L) - SystemClock.elapsedRealtime() - AmazModApplication.timeLastSync;

        Logger.info("BatteryStatusReceiver times: " + SystemClock.elapsedRealtime() + " / " + AmazModApplication.timeLastSync);

        if (delay < 0) delay = 0;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmBatteryIntent = new Intent(context, BatteryStatusReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmBatteryIntent, 0);

        try {
            if (alarmManager != null)
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                    (long) syncInterval * 60000L, pendingIntent);
        } catch (NullPointerException e) {
            Logger.error(e, "BatteryStatusReceiver setRepeating exception: " + e.toString());
        }
    }

    private void updateBattery(BatteryStatus batteryStatus) {
        BatteryData batteryData = batteryStatus.getBatteryData();
        Logger.debug("batteryStatus: " + batteryData.getLevel());
        Logger.debug("charging: " + batteryData.isCharging());
        Logger.debug("usb: " + batteryData.isUsbCharge());
        Logger.debug("ac: " + batteryData.isAcCharge());
        Logger.debug("dateLastCharge: " + batteryData.getDateLastCharge());

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
            Logger.error(ex, "TransportService batteryStatus exception: " + ex.toString());
        }
        // Save time of last sync
        Prefs.putLong(Constants.PREF_TIME_LAST_SYNC, SystemClock.elapsedRealtime());
    }

    private void batteryAlert(BatteryStatus batteryStatus, Context context) {
        // User options/data
        int watchBatteryAlert = Integer.parseInt(Prefs.getString(Constants.PREF_BATTERY_WATCH_ALERT,
                Constants.PREF_DEFAULT_BATTERY_WATCH_ALERT));
        boolean alreadyBatteryNotified = Prefs.getBoolean(Constants.PREF_BATTERY_WATCH_ALREADY_ALERTED,
                false);
        boolean alreadyChargingNotified = Prefs.getBoolean(Constants.PREF_BATTERY_WATCH_CHARGED,
                false);

        BatteryData batteryData = batteryStatus.getBatteryData();
        int battery = Math.round(batteryData.getLevel()*100);
        boolean charging = batteryData.isCharging();

        Logger.debug("Battery check - watch "+battery+"%, charging:"+charging+", limit:"+watchBatteryAlert+"%");

        // Check if low battery
        if( watchBatteryAlert > 0 && watchBatteryAlert > battery && !charging && !alreadyBatteryNotified) {
            Logger.debug("low watch battery...");
            // Send notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.TAG)
                    .setSmallIcon(R.drawable.ic_battery_alert_red_24dp)
                    .setContentTitle(context.getString(R.string.notification_low_battery))
                    .setContentText(context.getString(R.string.notification_low_battery_description,watchBatteryAlert + "%"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notificationManager.notify(0, builder.build());
            Prefs.putBoolean(Constants.PREF_BATTERY_WATCH_ALREADY_ALERTED, true);
        }else{
            // Re-set notification
            Prefs.putBoolean(Constants.PREF_BATTERY_WATCH_ALREADY_ALERTED, false);
        }

        if( battery>99 && charging && !alreadyChargingNotified){
            Logger.debug("watch fully charged...");
            // Fully charged notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.TAG)
                    .setSmallIcon(R.drawable.ic_battery_charging_full_green_24dp)
                    .setContentTitle(context.getString(R.string.notification_watch_charged))
                    .setContentText(context.getString(R.string.notification_watch_charged_description))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            notificationManager.notify(0, builder.build());
            Prefs.putBoolean(Constants.PREF_BATTERY_WATCH_CHARGED, true);
        }else{
            // Re-set notification
            Prefs.putBoolean(Constants.PREF_BATTERY_WATCH_CHARGED, false);
        }
    }
}
