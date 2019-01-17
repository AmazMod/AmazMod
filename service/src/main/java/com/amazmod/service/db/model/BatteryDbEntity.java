package com.amazmod.service.db.model;

import com.amazmod.service.db.AppDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

@Table(database = AppDatabase.class, name = "BatteryStatus")
public class BatteryDbEntity {
    @PrimaryKey
    private long date;

    @Column
    private float level;

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

}
