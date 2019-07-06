package com.amazmod.service;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.amazmod.service.db.model.BatteryDbEntity;
import com.amazmod.service.db.model.BatteryDbEntity_Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;

public class AmazModService extends Application {

    private static Application mApp;
    private static String level;

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = this;

        setupLogger();

        //EventBus.getDefault().init(this);
        Logger.info("Tinylog configured debug: {} level: {}", Constants.DEBUG?"TRUE":"FALSE", level.toUpperCase());

        FlowManager.init(this);
        cleanOldBatteryDb();

        startService(new Intent(this, MainService.class));

        Logger.info("AmazModService onCreate");
    }

    private static void cleanOldBatteryDb() {

        Logger.debug("AmazModService cleanOldBatteryDb");

        long delta = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 10);
        SQLite
                .delete()
                .from(BatteryDbEntity.class)
                .where(BatteryDbEntity_Table.date.lessThan(delta))
                .query();
    }

    public static Application getApplication() {
        return mApp;
    }
    public static Context getContext() {
        return getApplication().getApplicationContext();
    }

    private void setupLogger() {
        level = "error";
        if (Constants.DEBUG)
            level = Constants.DEBUG_LEVEL;
        //System.out.println("D/AmazMod AmazModService Tinylog configured debug: " + DEBUG + " level: " + level);
        Configuration.set("writerLogcat", "logcat");
        Configuration.set("writerLogcat.level", level);
        Configuration.set("writerLogcat.tagname", "AmazMod");
        Configuration.set("writerLogcat.format", "{class-name}.{method}(): {message}");
    }
}
