package com.edotassi.amazmod.event;

import amazmod.com.transport.data.NotificationData;

public class OutcomingNotification {

    private NotificationData notificationData;

    public OutcomingNotification(NotificationData notificationData) {
        this.notificationData = notificationData;
    }

    public NotificationData getNotificationData() {
        return notificationData;
    }
}
