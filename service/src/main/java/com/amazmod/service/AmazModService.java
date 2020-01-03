package com.amazmod.service;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

import com.amazmod.service.db.model.BatteryDbEntity;
import com.amazmod.service.db.model.BatteryDbEntity_Table;
import com.amazmod.service.util.LocaleUtils;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;

import java.util.Locale;

public class AmazModService extends Application {

    public static Locale defaultLocale;
    private static Application mApp;
    private static String level;

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = this;

        setupLogger();

        setupLocale();

/*      //THIS ONE IS WORK ALONE WITHOUT NEED   PROTECTED VOID ATTACHBASECONTEXT, PRIVATE VOID SETUPLOCALE(), AND THE LOCATEUTILS BUT CAN USE ONLY ONE LANG SET ON "CONF.LOCATE" LITE

        Resources res = getContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = Locale.ITALY;
        //conf.setLayoutDirection(locale);
        res.updateConfiguration(conf, dm);
*/
        //EventBus.getDefault().init(this);
        Logger.info("Tinylog configured debug: {} level: {}",
                (BuildConfig.VERSION_NAME.toLowerCase().contains("dev"))?"TRUE":"FALSE", level.toUpperCase());

        FlowManager.init(this);
        cleanOldBatteryDb();

        startService(new Intent(this, MainService.class));

        Logger.info("AmazModService onCreate");
    }

    @Override
    protected void attachBaseContext(Context context) {
        new Prefs.Builder()
                .setContext(context)
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setUseDefaultSharedPreference(true)
                .build();
        super.attachBaseContext(LocaleUtils.onAttach(context));
        Logger.debug("AmazModApplication attachBaseContext");
    }

    private void setupLocale() {
        defaultLocale = LocaleUtils.getLocale();
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
        if (BuildConfig.VERSION_NAME.toLowerCase().contains("dev"))
            level = "trace";
        //System.out.println("D/AmazMod AmazModService Tinylog configured debug: " + DEBUG + " level: " + level);
        Configuration.set("writerLogcat", "logcat");
        Configuration.set("writerLogcat.level", level);
        Configuration.set("writerLogcat.tagname", "AmazMod");
        Configuration.set("writerLogcat.format", "{class-name}.{method}(): {message}");
    }
}
