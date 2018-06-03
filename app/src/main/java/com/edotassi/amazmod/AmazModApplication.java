package com.edotassi.amazmod;

import android.app.Application;
import android.content.Intent;

import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.transport.TransportService;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class AmazModApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.init();

        HermesEventBus.getDefault().init(this);

        startService(new Intent(this, TransportService.class));
    }
}
