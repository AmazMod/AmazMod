package com.edotassi.amazmod.notification.factory;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.edotassi.amazmod.AmazModApplication;
import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.NotificationData;

public class NotificationFactory {

    public static NotificationData fromStatusBarNotification(Context context, StatusBarNotification statusBarNotification) {

        NotificationData notificationData = new NotificationData();
        Notification notification = statusBarNotification.getNotification();
        Bundle bundle = notification.extras;
        String text = "", title = "";

        Logger.trace("notification key: {}", statusBarNotification.getKey());

        //Notification time
        //Calendar c = Calendar.getInstance();
        //c.setTimeInMillis(notification.when);
        //String notificationTime = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
        String notificationTime = DateFormat.getTimeInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(notification.when);

        //EXTRA_TITLE and EXTRA_TEXT are usually CharSequence and not regular Strings...
        CharSequence bigTitle = bundle.getCharSequence(Notification.EXTRA_TITLE);
        if (bigTitle != null) {
            title = bigTitle.toString();
        } else try {
            title = bundle.getString(Notification.EXTRA_TITLE);
        } catch (ClassCastException e) {
            Logger.debug(e,"NotificationFactory exception: " + e.toString() + " title: " + title);
        }

        CharSequence bigText = bundle.getCharSequence(Notification.EXTRA_TEXT);
        if (bigText != null) {
            text = bigText.toString();
        }

        //Use EXTRA_TEXT_LINES instead, if it exists
        CharSequence[] lines = bundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if ((lines != null) && (lines.length > 0)) {
            text += "\n*Extra lines:\n" + lines[Math.min(lines.length - 1, 0)].toString();
            Logger.debug("NotificationFactory EXTRA_TEXT_LINES exists");
        }

        //Maybe use android.bigText instead?
        if (bundle.getCharSequence(Notification.EXTRA_BIG_TEXT) != null) {
            try {
                text = bundle.getCharSequence(Notification.EXTRA_BIG_TEXT).toString();
                Logger.debug("NotificationFactory EXTRA_BIG_TEXT exists");
            } catch (NullPointerException e) {
                Logger.debug(e,"NotificationFactory exception: " + e.toString() + " text: " + text);
            }
        }

        String notificationPackage = statusBarNotification.getPackageName();
        try {
            int iconId = bundle.getInt(Notification.EXTRA_SMALL_ICON);
            PackageManager manager = context.getPackageManager();
            Resources resources = manager.getResourcesForApplication(notificationPackage);

            Drawable drawable = resources.getDrawable(notification.icon);
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            if (bitmap.getWidth() > 48) {
                bitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
            }
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] intArray = new int[width * height];
            bitmap.getPixels(intArray, 0, width, 0, 0, width, height);

            //System.out.println("AmazMod NotificationService mapNotification bitmap dimensions: " + width + " x " + height);

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
            Logger.error("Failed to get bitmap from {}", notificationPackage);
        }

        extractImagesFromNotification(bundle, statusBarNotification, notificationData);

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

    public static void extractImagesFromNotification(Bundle bundle, StatusBarNotification statusBarNotification, NotificationData notificationData) {

        if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_LARGE_ICON, Constants.PREF_NOTIFICATIONS_LARGE_ICON_DEFAULT)) {
            extractLargeIcon(bundle, statusBarNotification, notificationData);
        }

        if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_IMAGES, Constants.PREF_NOTIFICATIONS_IMAGES_DEFAULT)) {
            extractPicture(bundle, statusBarNotification, notificationData);
        }
    }

    private static void extractLargeIcon(Bundle bundle, StatusBarNotification statusBarNotification, NotificationData notificationData) {
        Logger.trace("notification key: {}", notificationData.getKey());
        try {
            Bitmap largeIcon = (Bitmap) bundle.get(Notification.EXTRA_LARGE_ICON);
            if (largeIcon != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                largeIcon.compress(Bitmap.CompressFormat.PNG, 80, stream);
                byte[] byteArray = stream.toByteArray();

                notificationData.setLargeIcon(byteArray);
                notificationData.setLargeIconWidth(largeIcon.getWidth());
                notificationData.setLargeIconHeight(largeIcon.getHeight());
            } else
                Logger.warn("notification key: {} has null largeIcon!", notificationData.getKey());
        } catch (Exception exception) {
            Logger.error(exception, exception.getMessage());
        }
    }

    private static void extractPicture(Bundle bundle, StatusBarNotification statusBarNotification, NotificationData notificationData) {
        Logger.trace("notification key: {}", notificationData.getKey());
        try {
            Bitmap originalBitmap = (Bitmap) bundle.get(Notification.EXTRA_PICTURE);
            Bitmap largeIconBig = (Bitmap) bundle.get(Notification.EXTRA_LARGE_ICON_BIG);
            if (originalBitmap != null) {
                Bitmap scaledBitmap = scaleBitmap(originalBitmap);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
                byte[] byteArray = stream.toByteArray();

                notificationData.setPicture(byteArray);
                notificationData.setPictureWidth(scaledBitmap.getWidth());
                notificationData.setPictureHeight(scaledBitmap.getHeight());

            } else if (largeIconBig != null) {
                Bitmap scaledBitmap = scaleBitmap(largeIconBig);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
                byte[] byteArray = stream.toByteArray();

                notificationData.setPicture(byteArray);
                notificationData.setPictureWidth(scaledBitmap.getWidth());
                notificationData.setPictureHeight(scaledBitmap.getHeight());

            } else
                Logger.warn("notification key: {} has null picture!", notificationData.getKey());

        } catch (Exception exception) {
            Logger.error(exception, exception.getMessage());
        }
    }

    private static Bitmap scaleBitmap(Bitmap bitmap) {
        if (bitmap.getWidth() <= 320) {
            return bitmap;
        }

        float horizontalScaleFactor = bitmap.getWidth() / 320f;
        float destHeight = bitmap.getHeight() / horizontalScaleFactor;

        return Bitmap.createScaledBitmap(bitmap, 320, (int) destHeight, false);
    }

}
