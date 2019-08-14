package com.amazmod.service.notifications;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.tinylog.Logger;

import java.util.Iterator;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {


    @Override
    public void onCreate() {
        super.onCreate();
        Logger.debug("notification listener create");
    }

    @Override
    public void onDestroy() {
        Logger.debug("notification listener destroy");
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        Logger.debug("notification posted");

        Notification notification = statusBarNotification.getNotification();
        Bundle bundle = notification.extras;

        Set<String> keys = bundle.keySet();
        for (String nextKey : keys) {
            Object data = bundle.get(nextKey);
            if (data != null)
                Logger.debug("notification bundle -> " + nextKey + ": " + data.toString());
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);

        Notification notification = sbn.getNotification();
        Bundle bundle = notification.extras;

        Set<String> keys = bundle.keySet();
        for (String nextKey : keys) {
            Object data = bundle.get(nextKey);
            if (data != null)
                Logger.debug("notification bundle -> " + nextKey + ": " + data.toString());
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
