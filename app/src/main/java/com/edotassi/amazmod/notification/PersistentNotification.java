package com.edotassi.amazmod.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import amazmod.com.transport.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.ui.MainActivity;

import org.tinylog.Logger;

public class PersistentNotification {

    private Context context;
    private String model;
    private NotificationManagerCompat notificationManager;
    private final static int NOTIFICATION_ID = 999989;

    public PersistentNotification(Context context, String model) {
        this.context = context;
        this.model = model;
        this.notificationManager = NotificationManagerCompat.from(context);
    }

    public int getNotificationId() {
        return NOTIFICATION_ID;
    }

    public Notification createPersistentNotification() {
        String msg;
        createNotificationChannel();

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                (int) (long) (System.currentTimeMillis() % 10000L),notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (model != null) {
            if (model.isEmpty()) {
                msg = context.getResources().getString(R.string.device_not_connected);
            } else
                msg = model + " " + context.getResources().getString(R.string.device_connected);
        } else msg = context.getResources().getString(R.string.device_not_connected);

        Notification notification = new NotificationCompat.Builder(context, Constants.TAG)
                .setSmallIcon(R.drawable.outline_watch_black_48)
                .setContentTitle(Constants.TAG)
                .setContentText(msg)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return notification;
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification);
            return null;
        }
    }

    // Create channel for notification on Oreo+
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Constants.TAG, Constants.TAG, NotificationManager.IMPORTANCE_MIN);
            channel.setDescription(context.getString(R.string.app_name));
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
            else
                Logger.error("PersistentNotification createNotificationChannel null notificationManager!");
        }
    }

    // Update persistent notification if it is enabled in Settings
    public void updatePersistentNotification(boolean isWatchConnected) {

        Logger.debug("PersistentNotification updatePersistentNotification isConnected: " + isWatchConnected);

        final boolean enableNotification = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION, true);

        if (!enableNotification && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        String msg;

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                (int) (long) (System.currentTimeMillis() % 10000L), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (model != null) {
            if (isWatchConnected) {
                msg = model + " " + context.getResources().getString(R.string.device_connected);
            } else
                msg = context.getResources().getString(R.string.device_not_connected);
        } else msg = context.getResources().getString(R.string.device_not_connected);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, Constants.TAG)
                .setSmallIcon(R.drawable.outline_watch_black_48)
                .setContentTitle(Constants.TAG)
                .setContentText(msg)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());

    }

    public static void cancelPersistentNotification(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
