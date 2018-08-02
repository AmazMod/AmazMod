package com.edotassi.amazmod.db.model;

import com.edotassi.amazmod.db.AppDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Index;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

@Table(database = AppDatabase.class, name = "Notification")
public class NotificationEntity {

    @PrimaryKey(autoincrement = true)
    private long id;

    @Index
    @Column
    private long date;

    @Column
    private String packageName;

    @Column
    private byte filterResult;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public byte getFilterResult() {
        return filterResult;
    }

    public void setFilterResult(byte filterResult) {
        this.filterResult = filterResult;
    }
}
