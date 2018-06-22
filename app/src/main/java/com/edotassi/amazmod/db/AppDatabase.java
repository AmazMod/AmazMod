package com.edotassi.amazmod.db;

import com.raizlabs.android.dbflow.annotation.Database;

@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION)
public class AppDatabase {

    public static final String NAME = "AmazModDb";

    public static final int VERSION = 2;
}
