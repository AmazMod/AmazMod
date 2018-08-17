package com.edotassi.amazmod;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.receiver.BatteryStatusReceiver;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.ui.MainActivity;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.util.Locale;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class AmazModApplication extends Application {

    public static Locale defaultLocale;
    public static boolean isWatchConnected;
    public static int syncInterval;
    public static long timeLastSync;

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.init();
        FlowManager.init(this);

        new Prefs.Builder()
                .setContext(this)
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setUseDefaultSharedPreference(true)
                .build();

        HermesEventBus.getDefault().init(this);

        startService(new Intent(this, TransportService.class));

        BatteryStatusReceiver.startBatteryReceiver(this);

        isWatchConnected = true;
        setupLocale();

        // Add persistent notification to keep app running in the background if it is enabled in Settings
        final boolean enableNotification = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION, true);
        final String model = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Constants.PREF_WATCH_MODEL, "");
        TransportService.model = model;
        if (enableNotification) {
            addPersistentNotification(model);
        }

        Log.d(Constants.TAG, " AmazModApplication Start syncInterval: " + syncInterval + " / timeLastSync: " + timeLastSync);
    }

    private void setupLocale() {
        defaultLocale = Locale.getDefault();
    }

    private void addPersistentNotification(String model){

        String msg;
        createNotificationChannel();

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                (int) (long) (System.currentTimeMillis() % 10000L),notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (model != null) {
            if (model.isEmpty()) {
                msg = getResources().getString(R.string.device_not_connected);
            } else
                msg = model + " " + getResources().getString(R.string.device_connected);
        } else msg = getResources().getString(R.string.device_not_connected);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, Constants.TAG)
                .setSmallIcon(R.drawable.outline_watch_black_48)
                .setContentTitle(Constants.TAG)
                .setContentText(msg)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(999989, mBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Constants.TAG, Constants.TAG, NotificationManager.IMPORTANCE_MIN);
            channel.setDescription(getString(R.string.app_name));
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            try {
                notificationManager.createNotificationChannel(channel);
            } catch (NullPointerException e){
                //TODO log to crashlitics
                Log.e(Constants.TAG, "AmazModApplication createNotificationChannel exception: " + e.toString());
            }
        }
    }
}
