package com.edotassi.amazmod.support;

import android.graphics.drawable.Drawable;

public class AppInfo {

    private String appName;
    private String packageName;
    private String versionName;
    private String acivity;
    private Drawable icon;
    private boolean enabled;
    private int position;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // For widgets list
    public int getPosition() {
        return position;
    }
    public void setPosition(int position) {
        this.position = position;
    }
    public void setAcivity(String versionName) {
        this.acivity = acivity;
    }
    public String getAcivity() {
        return acivity;
    }

}
