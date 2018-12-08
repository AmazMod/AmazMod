package com.amazmod.service.support;

import java.util.HashMap;
import java.util.Map;

import amazmod.com.transport.data.NotificationData;

public class NotificationStore {

    private static Map<String, NotificationData> customNotifications;

    public NotificationStore() {
        customNotifications = new HashMap<>();
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

    public static String getKey(String key) {
        return customNotifications.get(key).getKey();
    }

    public static Boolean getHideReplies(String key) {
        return customNotifications.get(key).getHideReplies();
    }

    public static Boolean getForceCustom(String key) {
        return customNotifications.get(key).getForceCustom();
    }

    public static int getTimeoutRelock(String key) {
        return customNotifications.get(key).getTimeoutRelock();
    }

}
