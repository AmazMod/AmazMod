package com.amazmod.service;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.amazmod.service.db.model.BatteryDbEntity;
import com.amazmod.service.db.model.BatteryDbEntity_Table;
import com.amazmod.service.settings.SettingsManager;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;

import java.io.File;
import java.util.Locale;

public class AmazModService extends Application {

    private static Application mApp;
    private static String level;

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = this;

        setupLogger();

        setupLanguage();

        //EventBus.getDefault().init(this);
        Logger.info("Tinylog configured debug: {} level: {}",
                (BuildConfig.VERSION_NAME.toLowerCase().contains("dev"))?"TRUE":"FALSE", level.toUpperCase());

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
        //if (BuildConfig.VERSION_NAME.toLowerCase().contains("dev"))
            level = "trace";
        //System.out.println("D/AmazMod AmazModService Tinylog configured debug: " + DEBUG + " level: " + level);
        Configuration.set("writerLogcat", "logcat");
        Configuration.set("writerLogcat.level", level);
        Configuration.set("writerLogcat.tagname", "AmazMod");
        Configuration.set("writerLogcat.format", "{class-name}.{method}(): {message}");
    }

    private void setupLanguage() {
        // Load settings
        SettingsManager settingsManager = new SettingsManager(getContext());
        // Get phone app language
        String language = settingsManager.getString(Constants.PREF_DEFAULT_LOCALE, null);
        Logger.debug("Amazmod locale app language: "+language);

        if (language == null)
                return;
        if (language.contains("iw")) {
            if (!new File("/system/fonts/NotoSansHebrew-Regular.ttf").exists()) {
                language = "en_EN";
                Logger.debug("Amazmod locale: Hebrew font is missing, setting english language");
            } else
                Logger.debug("Amazmod locale: Hebrew font exist, setting Hebrew language");
        }
        if (language.contains("ar")) {
            if (!new File("/system/fonts/NotoSansArabic-Regular.ttf").exists()) {
                language = "en_EN";
                Logger.debug("Amazmod locale: Arabic font is missing, setting english language");
            } else
                Logger.debug("Amazmod locale: Arabic font exist, setting Hebrew language");
        }
        Resources res = getContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = getLocaleByLanguageCode(language);
        res.updateConfiguration(conf, dm);

        Logger.debug("Amazmod locale set:"+getLocaleByLanguageCode(language));
    }

    private static Locale getLocaleByLanguageCode(String languageCode) {
        String[] languageCodes = languageCode.split("_");
        if (languageCodes.length > 1) {
            return new Locale(languageCodes[0], languageCodes[1]);
        } else {
            return new Locale(languageCode, languageCode.toUpperCase());
        }
    }
}
