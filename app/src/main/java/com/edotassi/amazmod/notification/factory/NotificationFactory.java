package com.edotassi.amazmod.notification.factory;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.support.Logger;

import java.util.Calendar;

import amazmod.com.transport.data.NotificationData;

public class NotificationFactory {

    public static NotificationData fromStatusBarNotification(Context context, StatusBarNotification statusBarNotification) {

        NotificationData notificationData = new NotificationData();
        Notification notification = statusBarNotification.getNotification();
        Bundle bundle = notification.extras;
        String text = "", title = "";

        //Notification time
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(notification.when);
        String notificationTime = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);

        //EXTRA_TITLE and EXTRA_TEXT are usually CharSequence and not regular Strings...
        CharSequence bigTitle = bundle.getCharSequence(Notification.EXTRA_TITLE);
        if (bigTitle != null) {
            title = bigTitle.toString();
        } else try {
            title = bundle.getString(Notification.EXTRA_TITLE);
        } catch (ClassCastException e) {
            System.out.println("AmazMod NotificationFactory exception: " + e.toString() + " title: " + title);
        }

        CharSequence bigText = bundle.getCharSequence(Notification.EXTRA_TEXT);
        if (bigText != null) {
            text = bigText.toString();
        }

        //Use EXTRA_TEXT_LINES instead, if it exists
        CharSequence[] lines = bundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) {
            text = lines[lines.length - 1].toString();
            System.out.println("AmazMod NotificationFactory EXTRA_TEXT_LINES exists");
        }

        //Maybe use android.bigText instead?
        if (bundle.getCharSequence(Notification.EXTRA_BIG_TEXT) != null) {
            try {
                text = bundle.getCharSequence(Notification.EXTRA_BIG_TEXT).toString();
                System.out.println("AmazMod NotificationFactory EXTRA_BIG_TEXT exists");
            } catch (NullPointerException e) {
                System.out.println("AmazMod NotificationFactory exception: " + e.toString() + " text: " + text);
            }
        }

        String notificationPackgae = statusBarNotification.getPackageName();
        try {
            int iconId = bundle.getInt(Notification.EXTRA_SMALL_ICON);
            PackageManager manager = context.getPackageManager();
            Resources resources = manager.getResourcesForApplication(notificationPackgae);

            Drawable drawable = resources.getDrawable(iconId);
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            bitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] intArray = new int[width * height];
            bitmap.getPixels(intArray, 0, width, 0, 0, width, height);

            System.out.println("AmazMod NotificationService mapNotification bitmap dimensions: " + width + " x " + height);

            //This was crashing on Oreo
            //Bitmap bitmap = BitmapFactory.decodeResource(resources, iconId);

            //int width = bitmap.getWidth();
            //int height = bitmap.getHeight();
            //int[] intArray = new int[width * height];
            //bitmap.getPixels(intArray, 0, width, 0, 0, width, height);

            notificationData.setIcon(intArray);
            notificationData.setIconWidth(width);
            notificationData.setIconHeight(height);
        } catch (Exception e) {
            notificationData.setIcon(new int[]{});
            Logger.get(NotificationFactory.class).e(e, "Failed to get bipmap from %s", notificationPackgae);
        }

        notificationData.setId(statusBarNotification.getId());
        notificationData.setKey(statusBarNotification.getKey());
        notificationData.setTitle(title);
        notificationData.setText(text);
        notificationData.setTime(notificationTime);
        notificationData.setForceCustom(false);
        notificationData.setHideReplies(false);
        notificationData.setHideButtons(false);

        return notificationData;
    }
}
