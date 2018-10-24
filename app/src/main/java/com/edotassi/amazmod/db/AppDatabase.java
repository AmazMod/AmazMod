package com.edotassi.amazmod.db;

import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;

@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION)
public class AppDatabase {

    static final String NAME = "AmazModDb";
    static final int VERSION = 8;

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

    @Migration(version = 4, database = AppDatabase.class)
    public static class AddDateLastCharge extends AlterTableMigration<BatteryStatusEntity> {

        public AddDateLastCharge(Class<BatteryStatusEntity> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "dateLastCharge");
        }
    }

    @Migration(version = 5, database = AppDatabase.class)
    public static class AddFilterResult extends AlterTableMigration<NotificationEntity> {

        public AddFilterResult(Class<NotificationEntity> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.INTEGER, "filterResult");
        }
    }
}
