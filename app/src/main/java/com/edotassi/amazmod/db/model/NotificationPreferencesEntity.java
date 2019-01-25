package com.edotassi.amazmod.db.model;

import com.edotassi.amazmod.db.AppDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

@Table(database = AppDatabase.class, name = "NotificationPreferences")
public class NotificationPreferencesEntity {

    @PrimaryKey
    @Column
    private String packageName;

    @Column
    private long silenceUntil;

    @Column
    private String filter;

    @Column
    private boolean whitelist;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getSilenceUntil() {
        return silenceUntil;
    }

    public void setSilenceUntil(long silenceUntil) {
        this.silenceUntil = silenceUntil;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }
}
