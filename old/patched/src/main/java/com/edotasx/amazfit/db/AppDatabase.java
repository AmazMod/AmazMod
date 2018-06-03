package com.edotasx.amazfit.db;

import com.raizlabs.android.dbflow.annotation.Database;

/**
 * Created by edoardotassinari on 02/04/18.
 */

@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION)
public class AppDatabase {

    public static final String NAME = "AmazModDb";

    public static final int VERSION = 1;

}