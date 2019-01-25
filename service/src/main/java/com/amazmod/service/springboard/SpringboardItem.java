package com.amazmod.service.springboard;

/**
 * Created by Kieron on 12/02/2018.
 */

public class SpringboardItem {

    //SpringboardItem contains a package name, class name and state. Title is only used in _out (and therefore not needed because we don't modify it), and srl is overwritten so no need to store it
    private String packageName, className;
    private boolean enable;

    //Constructor for the data to update information
    public SpringboardItem(String packageName, String className, boolean enable){
        this.packageName = packageName;
        this.className = className;
        this.enable = enable;
    }

    //Getters for data
    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public boolean isEnable() {
        return enable;
    }

    //Update enabled state
    public void setEnabled(boolean enabled) {
        this.enable = enabled;
    }
}
