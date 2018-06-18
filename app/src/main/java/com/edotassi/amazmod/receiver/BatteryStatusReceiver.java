package com.edotassi.amazmod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.RequestBatteryStatus;
import com.edotassi.amazmod.event.RequestWatchStatus;
import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.log.LoggerScoped;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class BatteryStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LoggerScoped.get(BatteryStatusReceiver.class).debug("started");

        HermesEventBus.getDefault().connectApp(context, Constants.PACKAGE);
        HermesEventBus.getDefault().post(new RequestBatteryStatus());
    }
}
