package com.edotassi.amazmod;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.support.v4.content.ContextCompat;

import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.receiver.BatteryStatusReceiver;
import com.edotassi.amazmod.transport.TransportService;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.util.Locale;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class AmazModApplication extends Application {

    public static Locale defaultLocale;

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

        startService(new Intent(this, TransportService.class));

        startBatteryReceiver();

        System.out.println(Constants.TAG + " AmazModApplication Start");

        setupLocale();
    }

    private void setupLocale() {
        defaultLocale = Locale.getDefault();
    }

    private void startBatteryReceiver() {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent alarmBatteryIntent = new Intent(this, BatteryStatusReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmBatteryIntent, 0);

        int interval = Integer.valueOf(Prefs.getString(Constants.PREF_BATTERY_BACKGROUND_SYNC_INTERVAL, "60"));

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, interval * 60 * 1000, pendingIntent);
    }
}
