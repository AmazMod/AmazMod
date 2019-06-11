package com.amazmod.service.support;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import amazmod.com.transport.data.NotificationData;

public class NotificationInfo {

    private String notificationTitle;
    private String notificationText;
    private String notificationTime;
    private Drawable icon;
    private byte[] largeIconData;
    private String key;
    private String id;

    public NotificationInfo(){}

    public NotificationInfo(String notificationTitle, String notificationText, String notificationTime, Drawable icon, byte[] largeIconData, String key, String id) {
        this.notificationTitle = notificationTitle;
        this.notificationTime = notificationTime;
        this.notificationText = notificationText;
        this.icon = icon;
        this.largeIconData = largeIconData;
        this.key = key;
        this.id = id;
    }

    public NotificationInfo(NotificationData notificationData, String key) {
        this.notificationTitle = notificationData.getTitle();
        this.notificationText = notificationData.getText();
        this.notificationTime = notificationData.getTime();
        this.largeIconData = notificationData.getLargeIcon();

        int[] iconData = notificationData.getIcon();
        int iconWidth = notificationData.getIconWidth();
        int iconHeight = notificationData.getIconHeight();
        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

        this.icon = new BitmapDrawable(Resources.getSystem(), bitmap);

        this.key = key;

        this.id = key.substring(key.lastIndexOf("|") + 1);

    }

    public String getNotificationTitle() {
        return this.notificationTitle;
    }

    public String getNotificationTime() {
        return this.notificationTime;
    }

    public String getNotificationText() { return notificationText; }

    public Drawable getIcon() {
        return this.icon;
    }

    public byte[] getLargeIconData() {
        return largeIconData;
    }

    public String getKey() {
        return this.key;
    }

    public String getId() {
        return this.id;
    }

}
