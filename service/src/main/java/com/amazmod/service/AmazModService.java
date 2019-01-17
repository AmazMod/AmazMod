package com.amazmod.service;


import com.amazmod.service.db.model.BatteryDbEntity;
import com.amazmod.service.db.model.BatteryDbEntity_Table;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class AmazModService extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(Constants.TAG, "AmazModService HermesEventBus init");
        HermesEventBus.getDefault().init(this);

        FlowManager.init(this);
        cleanOldBatteryDb();

        startService(new Intent(this, MainService.class));

    }

    private static void cleanOldBatteryDb() {

        Log.d(Constants.TAG, "AmazModService cleanOldBatteryDb");

        long delta = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 10);
        SQLite
                .delete()
                .from(BatteryDbEntity.class)
                .where(BatteryDbEntity_Table.date.lessThan(delta))
                .query();
    }

}
