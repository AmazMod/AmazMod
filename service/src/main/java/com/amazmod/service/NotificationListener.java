package com.amazmod.service;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.Iterator;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "notification listener create");
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.TAG, "notification listener destroy");
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        Log.d(Constants.TAG, "notification posted");

        Notification notification = statusBarNotification.getNotification();
        Bundle bundle = notification.extras;

        Set<String> keys = bundle.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String nextKey = iterator.next();
            Log.d(Constants.TAG, "notification bundle -> " + nextKey + ": " + bundle.get(nextKey).toString());
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);

        Notification notification = sbn.getNotification();
        Bundle bundle = notification.extras;

        Set<String> keys = bundle.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String nextKey = iterator.next();
            Log.d(Constants.TAG, "notification bundle -> " + nextKey + ": " + bundle.get(nextKey).toString());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationRemoved(sbn, rankingMap);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        super.onNotificationRemoved(sbn, rankingMap, reason);
    }
}
