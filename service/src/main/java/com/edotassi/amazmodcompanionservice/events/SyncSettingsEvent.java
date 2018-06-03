package com.edotassi.amazmodcompanionservice.events;

import com.huami.watch.transport.DataBundle;

public class SyncSettingsEvent {

    private int notificationVibration;
    private int notificationScreenTimeout;
    private String notificationCustomReplies;

    public SyncSettingsEvent(DataBundle dataBundle) {
        notificationCustomReplies = dataBundle.getString("notificationReplies");
        notificationVibration = dataBundle.getInt("notificationVibration");
        notificationScreenTimeout = dataBundle.getInt("notificationScreenTimeout");
    }

    public int getNotificationVibration() {
        return notificationVibration;
    }

    public void setNotificationVibration(int notificationVibration) {
        this.notificationVibration = notificationVibration;
    }

    public int getNotificationScreenTimeout() {
        return notificationScreenTimeout;
    }

    public void setNotificationScreenTimeout(int notificationScreenTimeout) {
        this.notificationScreenTimeout = notificationScreenTimeout;
    }

    public String getNotificationCustomReplies() {
        return notificationCustomReplies;
    }

    public void setNotificationCustomReplies(String notificationCustomReplies) {
        this.notificationCustomReplies = notificationCustomReplies;
    }
}
