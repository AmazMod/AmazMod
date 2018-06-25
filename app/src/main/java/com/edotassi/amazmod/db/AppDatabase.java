package com.edotassi.amazmod.db;

import com.edotassi.amazmod.db.model.NotificationEntity;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;

@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION)
public class AppDatabase {

    public static final String NAME = "AmazModDb";

    public static final int VERSION = 3;

    @Migration(version = 3, database = AppDatabase.class)
    public static class AddDateToNotificationEntity extends AlterTableMigration<NotificationEntity> {

        public AddDateToNotificationEntity(Class<NotificationEntity> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "date");
        }
    }
}
