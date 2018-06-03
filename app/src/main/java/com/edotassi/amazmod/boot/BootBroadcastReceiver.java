package com.edotassi.amazmod.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.transport.TransportService;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.debug("boot completed");

        Intent serviceIntent = new Intent(context, TransportService.class);
        context.startService(serviceIntent);
    }

}
