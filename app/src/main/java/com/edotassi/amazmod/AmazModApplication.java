package com.edotassi.amazmod;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;

import com.edotassi.amazmod.setup.Setup;
import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.util.LocaleUtils;
import com.edotassi.amazmod.watch.Watch;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.util.Locale;

public class AmazModApplication extends Application {

    public static Locale defaultLocale;
    public static boolean isWatchConnected;
    //public static int syncInterval;
    public static long timeLastSync;
    public static long timeLastWatchfaceDataSend;

    public static int currentScreenBrightness;
    public static int currentScreenBrightnessMode;

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.init();
        FlowManager.init(this);

        Watch.init(getApplicationContext());


        isWatchConnected = true;
        setupLocale();

        Setup.run(getApplicationContext());
    }

    @Override
    protected void attachBaseContext(Context base) {
        new Prefs.Builder()
                .setContext(base)
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setUseDefaultSharedPreference(true)
                .build();
        super.attachBaseContext(LocaleUtils.onAttach(base));

    }

    private void setupLocale() {
        defaultLocale = LocaleUtils.getLocale();
    }

}
