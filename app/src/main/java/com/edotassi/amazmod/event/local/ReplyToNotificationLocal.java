package com.edotassi.amazmod.event.local;

import amazmod.com.transport.data.NotificationReplyData;

public class ReplyToNotificationLocal {

    private NotificationReplyData notificationReplyData;

    public ReplyToNotificationLocal(NotificationReplyData notificationReplyData) {
        this.notificationReplyData = notificationReplyData;
    }

    public NotificationReplyData getNotificationReplyData() {
        return notificationReplyData;
    }
}
