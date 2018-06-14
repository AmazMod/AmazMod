package com.edotassi.amazmod;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.receiver.BatteryStatusReceiver;
import com.edotassi.amazmod.transport.TransportService;
import com.raizlabs.android.dbflow.config.FlowManager;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class AmazModApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.init();
        FlowManager.init(this);

        HermesEventBus.getDefault().init(this);

        startService(new Intent(this, TransportService.class));

        startBatteryReceiver();
    }

    private void startBatteryReceiver() {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent alarmBatteryIntent = new Intent(this, BatteryStatusReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmBatteryIntent, 0);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
    }
}
