package com.amazmod.service.support;

import android.graphics.drawable.Drawable;

public class AppInfo {

    private String appName;
    private String packageName;
    private String versionName;
    private String size;
    private Drawable icon;

    public AppInfo(){}

    public AppInfo (String appName, String packageName, String versionName, String size, Drawable icon) {
        this.appName = appName;
        this.packageName = packageName;
        this.versionName = versionName;
        this.size = size;
        this.icon = icon;
    }

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

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

}
