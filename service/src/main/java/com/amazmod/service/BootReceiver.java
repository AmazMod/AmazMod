package com.amazmod.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.tinylog.Logger;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.debug("BOOT_COMPLETED");

        Intent serviceIntent = new Intent(context, MainService.class);
        context.startService(serviceIntent);
    }
}
