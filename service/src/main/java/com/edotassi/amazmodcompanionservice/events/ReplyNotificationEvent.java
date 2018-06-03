package com.edotassi.amazmodcompanionservice.events;

public class ReplyNotificationEvent {

    private String key;
    private String message;

    public ReplyNotificationEvent(String key, String message) {
        this.key = key;
        this.message = message;
    }

    public String getKey() {
        return key;
    }

    public String getMessage() {
        return message;
    }
}
