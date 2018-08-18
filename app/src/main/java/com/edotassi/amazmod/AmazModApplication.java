package com.edotassi.amazmod;

import android.app.Application;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.receiver.BatteryStatusReceiver;
import com.edotassi.amazmod.transport.TransportService;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.util.Locale;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class AmazModApplication extends Application {

    public static Locale defaultLocale;
    public static boolean isWatchConnected;
    public static int syncInterval;
    public static long timeLastSync;

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.init();
        FlowManager.init(this);

        new Prefs.Builder()
                .setContext(this)
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setUseDefaultSharedPreference(true)
                .build();

        HermesEventBus.getDefault().init(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, TransportService.class));
        } else {
            startService(new Intent(this, TransportService.class));
        }

        BatteryStatusReceiver.startBatteryReceiver(this);

        isWatchConnected = true;
        setupLocale();

        Log.d(Constants.TAG, " AmazModApplication Start syncInterval: " + syncInterval + " / timeLastSync: " + timeLastSync);
    }

    private void setupLocale() {
        defaultLocale = Locale.getDefault();
    }

}
