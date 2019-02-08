package com.amazmod.service.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        try {
            return customNotifications.size();
        }catch (NullPointerException e){
            return 0;
        }
    }

    public static void addCustomNotification(String key, NotificationData notificationData) {
        customNotifications.put(key, notificationData);
    }

    public static void removeCustomNotification(String key) {
        customNotifications.remove(key);
    }

    public static String getKey(String key) {
        if (customNotifications.get(key) == null)
            return null;
        else
            return customNotifications.get(key).getKey();
    }

    public static Boolean getHideReplies(String key) {
        if (customNotifications.get(key) == null)
            return true;
        else
            return customNotifications.get(key).getHideReplies();
    }

    public static Boolean getForceCustom(String key) {
        if (customNotifications.get(key) == null)
            return true;
        else
            return customNotifications.get(key).getForceCustom();
    }

    public static int getTimeoutRelock(String key) {
        if (customNotifications.get(key) == null)
            return 0;
        else
            return customNotifications.get(key).getTimeoutRelock();
    }

    public static String getTitle(String key) {
        return customNotifications.get(key).getTitle();
    }

    public static String getTime(String key) {
        return customNotifications.get(key).getTime();
    }

    public static int[] getIcon(String key) {
        return customNotifications.get(key).getIcon();
    }

    public static Set<String> getKeySet() {
        if (customNotifications != null)
            return customNotifications.keySet();
        else
            return null;
    }

    public static void clear() {
        customNotifications.clear();
    }
}
