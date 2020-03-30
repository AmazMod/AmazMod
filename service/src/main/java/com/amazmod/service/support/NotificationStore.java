package com.amazmod.service.support;

import android.content.Context;
import android.provider.Settings;

import androidx.collection.ArrayMap;

import com.amazmod.service.Constants;
import com.amazmod.service.util.DeviceUtil;
import com.huami.watch.notification.data.NotificationKeyData;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.util.Set;

import amazmod.com.transport.data.NotificationData;

public class NotificationStore {

    private static ArrayMap<String, NotificationData> customNotifications = new ArrayMap<>();
    public static ArrayMap<String, String> keyMap = new ArrayMap<>();

    public NotificationStore() {
        customNotifications = new ArrayMap<>();
        keyMap = new ArrayMap<>();
    }

    public static NotificationData getCustomNotification(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return null;
        else
            return notificationData;
    }

    public static int getCustomNotificationCount() {
        return customNotifications.size();
    }

    public static void addCustomNotification(String key, NotificationData notificationData) {
        customNotifications.put(key, notificationData);
        keyMap.put(key, notificationData.getKey());
    }

    public static void removeCustomNotification(String key) {
        sendRequestDeleteNotification(key);
        customNotifications.remove(key);
        keyMap.remove((key));
    }

    public static String getKey(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return null;
        else
            return notificationData.getKey();
    }

    public static Boolean getHideReplies(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return true;
        else
            return notificationData.getHideReplies();
    }

    public static Boolean getForceCustom(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return true;
        else
            return notificationData.getForceCustom();
    }

    public static int getTimeoutRelock(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return 0;
        else
            return notificationData.getTimeoutRelock();
    }

    public static String getTitle(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return null;
        else
            return notificationData.getTitle();
    }

    public static String getTime(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return null;
        else
            return notificationData.getTime();
    }

    public static int[] getIcon(String key) {
        NotificationData notificationData = customNotifications.get(key);
        if (notificationData == null)
            return null;
        else
            return notificationData.getIcon();
    }

    public static Set<String> getKeySet() {
        if (customNotifications != null)
            return customNotifications.keySet();
        else
            return null;
    }

    public static void clear() {
        if (!customNotifications.isEmpty()) {
            for (String key : customNotifications.keySet())
                sendRequestDeleteNotification(key);
            customNotifications.clear();
            keyMap.clear();
        }
    }

    public static void setNotificationCount(Context context) {
        setNotificationCount(context, getCustomNotificationCount());
    }

    public static void setNotificationCount(Context context, int count) {
        DeviceUtil.notificationCounterSet(context, count);
    }

    private static boolean isEmpty() {
        return getCustomNotificationCount() == 0;
    }

    private static void sendRequestDeleteNotification(String key) {

        Logger.debug("NotificationStore sendRequestDeleteNotification key: {} ", key);

        NotificationData notificationData = customNotifications.get(key);

        if (notificationData != null) {
            String pkg = key.split("\\|")[1];
            Logger.debug("NotificationStore sendRequestDeleteNotification pkg: {} ", pkg);

            //NotificationKeyData from(String pkg, int id, String tag, String key, String targetPkg)
            NotificationKeyData notificationKeyData = NotificationKeyData.from(pkg, notificationData.getId(),
                    null, notificationData.getKey(), null);
            EventBus.getDefault().post(notificationKeyData);
        }
    }

}
