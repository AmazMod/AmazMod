package com.edotassi.amazmod.event;

import com.huami.watch.notification.data.StatusBarNotificationData;

import amazmod.com.transport.data.NotificationData;

public class OutcomingNotification {

    private NotificationData notificationData;
    private StatusBarNotificationData statusBarNotificationData;

    public OutcomingNotification(NotificationData notificationData) {
        this.notificationData = notificationData;
    }

    public OutcomingNotification(NotificationData notificationData, StatusBarNotificationData statusBarNotificationData) {
        this.notificationData = notificationData;
        this.statusBarNotificationData = statusBarNotificationData;
    }

    public NotificationData getNotificationData() {
        return notificationData;
    }

    public StatusBarNotificationData getStatusBarNotificationData() {
        return statusBarNotificationData;
    }
}
