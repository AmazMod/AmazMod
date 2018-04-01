package com.edotasx.amazfit.notification.filter;

import android.app.Notification;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.huami.watch.notification.data.StatusBarNotificationData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public class TimeNotificationFilter implements NotificationFilter {

    private Map<Long, String> notificationTimeGone;

    public TimeNotificationFilter() {
        notificationTimeGone = new HashMap<>();
    }

    @Override
    public String getPackage() {
        return "";
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public boolean filter(StatusBarNotification statusBarNotification) {
        String lKey = StatusBarNotificationData.getUniqueKey(statusBarNotification);
        Long time = statusBarNotification.getNotification().when;

        Log.d("TimeFilter", statusBarNotification.getPackageName() + " -> " + time);

        if (notificationTimeGone.containsKey(time)) {
            return true;
        } else {
            notificationTimeGone.put(time, lKey);
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private String buildKey(StatusBarNotification statusBarNotification) {
        Bundle bundle = NotificationCompat.getExtras(statusBarNotification.getNotification());

        String lPackageName = statusBarNotification.getPackageName();
        String text = bundle.getString(Notification.EXTRA_TEXT);

        return lPackageName + ":" + text;
    }

    private String dumpBundle(Bundle bundle) {
        String ris = "";

        Iterator<String> iterator = bundle.keySet().iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            String value = bundle.getString(next);

            ris += next + "(" + value + "), ";
        }

        return ris;
    }
}
