package com.edotassi.amazmod.notification.factory;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;

import com.edotassi.amazmod.log.LoggerScoped;

import java.util.Calendar;

import amazmod.com.transport.data.NotificationData;

public class NotificationFactory {

    public static NotificationData fromStatusBarNotification(Context context, StatusBarNotification statusBarNotification) {
        NotificationData notificationData = new NotificationData();

        Notification notification = statusBarNotification.getNotification();

        //Notification time
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(notification.when);
        String notificationTime = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);

        Bundle bundle = NotificationCompat.getExtras(notification);

        String title = bundle.getString(Notification.EXTRA_TITLE);
        String text = bundle.getString(Notification.EXTRA_TEXT);

        CharSequence[] lines = bundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) {
            text = lines[lines.length - 1].toString();
        }

        String notificationPackgae = statusBarNotification.getPackageName();
        try {
            int iconId = bundle.getInt(Notification.EXTRA_SMALL_ICON);
            PackageManager manager = context.getPackageManager();
            Resources resources = manager.getResourcesForApplication(notificationPackgae);
            Bitmap bitmap = BitmapFactory.decodeResource(resources, iconId);

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] intArray = new int[width * height];
            bitmap.getPixels(intArray, 0, width, 0, 0, width, height);

            notificationData.setIcon(intArray);
            notificationData.setIconWidth(width);
            notificationData.setIconHeight(height);
        } catch (Exception e) {
            notificationData.setIcon(new int[]{});
            LoggerScoped.get(NotificationFactory.class).error(e, "Failed to get bipmap from %s", notificationPackgae);
        }

        notificationData.setId(statusBarNotification.getId());
        notificationData.setKey(statusBarNotification.getKey());
        notificationData.setTitle(title);
        notificationData.setText(text);
        notificationData.setTime(notificationTime);

        return notificationData;
    }
}
