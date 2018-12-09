package com.amazmod.service.support;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import amazmod.com.transport.data.NotificationData;

public class NotificationInfo {

    private String notificationTitle;
    private String notificationTime;
    private Drawable icon;
    private String key;

    public NotificationInfo(){}

    public NotificationInfo(String notificationTitle, String notificationTime, Drawable icon, String key) {
        this.notificationTitle = notificationTitle;
        this.notificationTime = notificationTime;
        this.icon = icon;
        this.key = key;
    }

    public NotificationInfo(NotificationData notificationData, String key) {
        this.notificationTitle = notificationData.getTitle();
        this.notificationTime = notificationData.getTime();

        int[] iconData = notificationData.getIcon();
        int iconWidth = notificationData.getIconWidth();
        int iconHeight = notificationData.getIconHeight();
        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

        this.icon = new BitmapDrawable(Resources.getSystem(), bitmap);

        this.key = key;
    }

    public String getNotificationTitle() {
        return this.notificationTitle;
    }

    public String getNotificationTime() {
        return this.notificationTime;
    }

    public Drawable getIcon() {
        return this.icon;
    }

    public String getKey() {
        return this.key;
    }

}
