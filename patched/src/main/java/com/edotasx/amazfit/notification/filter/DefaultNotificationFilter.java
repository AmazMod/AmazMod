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

public class DefaultNotificationFilter implements NotificationFilter {

    private Map<String, Long> mNotificationsSent;

    public DefaultNotificationFilter() {
        mNotificationsSent = new HashMap<>();
    }

    @Override
    public String getPackage() {
        return "";
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public boolean filter(StatusBarNotification statusBarNotification) {
        String lKey = StatusBarNotificationData.getUniqueKey(statusBarNotification);

        Long lTime = mNotificationsSent.get(lKey);
        if (lTime == null) {
            Log.d(Constants.TAG_NOTIFICATION, "accepted by default filter: " + lKey);

            mNotificationsSent.put(lKey, System.currentTimeMillis());
            return false;
        }

        Long lCurrentTime = System.currentTimeMillis();
        if ((lCurrentTime - lTime) > Constants.TIME_BETWEEN_NOTIFICATIONS) {
            Log.d(Constants.TAG_NOTIFICATION, "accepted by default filter: " + lKey);

            mNotificationsSent.put(lKey, lCurrentTime);
            return false;
        }

        Log.d(Constants.TAG_NOTIFICATION, "rejected by default filter: " + statusBarNotification.getPackageName());

        return true;
        /*
        String packageName = statusBarNotification.getPackageName();
        String TAG_NOTIFICATION = statusBarNotification.getTag();

        Log.d(Constants.TAG_NOTIFICATION, "id(" + statusBarNotification.getId() + ")");
        Log.d(Constants.TAG_NOTIFICATION, "packageName(" + packageName + ")");
        Log.d(Constants.TAG_NOTIFICATION, "postTime(" + statusBarNotification.getPostTime() + ")");
        Log.d(Constants.TAG_NOTIFICATION, "key(" + statusBarNotification.getKey() + ")");
        //Log.d(Constants.TAG_NOTIFICATION, "groupKey(" + statusBarNotification.getGroupKey() + ")");
        //Log.d(Constants.TAG_NOTIFICATION, "isOngoing(" + statusBarNotification.isOngoing() + ")");
        Log.d(Constants.TAG_NOTIFICATION, "tag(" + TAG_NOTIFICATION + ")");
        Log.d(Constants.TAG_NOTIFICATION, "bundle: " + dumpBundle(NotificationCompat.getExtras(statusBarNotification.getNotification())));
        Log.d(Constants.TAG_NOTIFICATION, "----");
        Log.d(Constants.TAG_NOTIFICATION, "----");
        */
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
