package com.amazmod.service.events;

public class SilenceApplicationEvent {

    private String packageName;
    private String minutes;

    public SilenceApplicationEvent(String packageName, String minutes) {
        this.packageName = packageName;
        this.minutes = minutes;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getMinutes() {
        return minutes;
    }
}
