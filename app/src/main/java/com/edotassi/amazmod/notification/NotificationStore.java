package com.edotassi.amazmod.notification;

import android.os.Bundle;

import com.huami.watch.transport.DataBundle;

import java.util.HashMap;
import java.util.Map;

import amazmod.com.transport.data.NotificationData;

public class NotificationStore {

    private static Map<String, DataBundle> standardNotifications;
    private static Map<String, NotificationData> customNotifications;
    private static Map<String, Bundle> notificationsBundle;

    NotificationStore() {
        standardNotifications = new HashMap<>();
        customNotifications = new HashMap<>();
        notificationsBundle= new HashMap<>();
    }

    public static DataBundle getStandardNotification(String key) {
        return standardNotifications.get(key);
    }

    public static int getStandardNotificationCount() {
        return standardNotifications.size();
    }

    public static void addStandardNotification(String key, DataBundle dataBundle) {
        standardNotifications.put(key, dataBundle);
    }

    public static void removeStandardNotification(String key) {
        standardNotifications.remove(key);
    }

    public static NotificationData getCustomNotification(String key) {
        return customNotifications.get(key);
    }

    public static int getCustomNotificationCount() {
        return customNotifications.size();
    }

    public static void addCustomNotification(String key, NotificationData notificationData) {
        customNotifications.put(key, notificationData);
    }

    public static void removeCustomNotification(String key) {
        customNotifications.remove(key);
    }

    public static Bundle getNotificationBundle(String key) {
        return notificationsBundle.get(key);
    }

    public static int getNotificationBundleCount() {
        return notificationsBundle.size();
    }

    public static void addNotificationBundle(String key, Bundle bundle) {
        notificationsBundle.put(key, bundle);
    }

    public static void removeNotificationBundle(String key) {
        notificationsBundle.remove(key);
    }

}
