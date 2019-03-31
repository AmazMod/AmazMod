package com.amazmod.service.support;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.amazmod.service.AmazModService;
import com.amazmod.service.Constants;
import com.amazmod.service.MainService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import amazmod.com.transport.data.NotificationData;

public class NotificationStore {

    public static Map<String, NotificationData> customNotifications = new HashMap<>();

    //public NotificationStore() {
        //customNotifications = new HashMap<>();
    //}

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

    public static void setNotificationCount(Context context) {
        setNotificationCount(context,getCustomNotificationCount());
    }

    public static void setNotificationCount(Context context, int count) {
        //Stores notificationCount in JSON Object
        String data = Settings.System.getString(context.getContentResolver(), Constants.CUSTOM_WATCHFACE_DATA);
        try {
            JSONObject json_data = new JSONObject(data);
            json_data.put("notifications", count);
            Settings.System.putString(context.getContentResolver(), Constants.CUSTOM_WATCHFACE_DATA, json_data.toString());
        } catch (JSONException e) {
            String notification_json = "{\"notifications\":\"" + count+"\"}";
            Log.d(Constants.TAG, "NotificationStore setNotificationCount: JSONException/invalid JSON: " + e.toString() + " - JSON defined to: " + notification_json);
            Settings.System.putString(context.getContentResolver(), Constants.CUSTOM_WATCHFACE_DATA, notification_json);
        }
    }

}
