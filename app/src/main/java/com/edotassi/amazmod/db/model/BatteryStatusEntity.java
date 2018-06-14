package com.edotassi.amazmod.db.model;

import com.edotassi.amazmod.db.AppDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

@Table(database = AppDatabase.class, name = "BatteryStatus")
public class BatteryStatusEntity {
    @PrimaryKey
    private long date;

    @Column
    private float level;

    @Column
    private boolean charging;

    @Column
    private boolean usbCharge;

    @Column
    private boolean acCharge;

    public void setDate(long date) {
        this.date = date;
    }

    public long getDate() {
        return date;
    }

    public void setLevel(float level) {
        this.level = level;
    }

    public float getLevel() {
        return level;
    }

    public boolean isUsbCharge() {
        return usbCharge;
    }

    public void setUsbCharge(boolean usbCharge) {
        this.usbCharge = usbCharge;
    }

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public boolean isAcCharge() {
        return acCharge;
    }

    public void setAcCharge(boolean acCharge) {
        this.acCharge = acCharge;
    }
}
