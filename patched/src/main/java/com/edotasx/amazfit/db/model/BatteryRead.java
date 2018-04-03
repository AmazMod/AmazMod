package com.edotasx.amazfit.db.model;

import com.edotasx.amazfit.db.AppDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

/**
 * Created by edoardotassinari on 02/04/18.
 */

@Table(database = AppDatabase.class)
public class BatteryRead {

    @PrimaryKey
    private long date;

    @Column
    private int level;

    @Column
    private boolean isCharging;

    public void setDate(long date) {
        this.date = date;
    }

    public long getDate() {
        return date;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public void setCharging(boolean isCharging) {
        this.isCharging = isCharging;
    }

    public boolean isCharging() {
        return isCharging;
    }
}
