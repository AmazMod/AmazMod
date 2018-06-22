package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.NotificationReplyData;

public class NotificationReply {

    private NotificationReplyData notificationReplyData;

    public NotificationReply(DataBundle dataBundle) {
        notificationReplyData = NotificationReplyData.fromDataBundle(dataBundle);
        }

    public NotificationReplyData getNotificationReplyData() {
        return notificationReplyData;
    }
}
