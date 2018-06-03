package com.edotassi.amazmod.event;

import com.edotassi.amazmod.transport.payload.NotificationData;

public class OutcomingNotification {

    private NotificationData notificationData;

    public OutcomingNotification(NotificationData notificationData) {
        this.notificationData = notificationData;
    }

    public NotificationData getNotificationData() {
        return notificationData;
    }
}
