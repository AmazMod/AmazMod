package com.amazmod.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.amazmod.service.MainService;

import org.tinylog.Logger;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Logger.debug("BootReceiver onReceive");

        final String action = intent.getAction();
        if (action != null && !action.isEmpty())
            switch (action) {
                case "android.intent.action.BOOT_COMPLETED":
                case "android.intent.action.QUICKBOOT_POWERON":
                case "android.intent.action.REBOOT":
                case "android.intent.action.USER_PRESENT":
                    Logger.debug("action: " + action);
                    break;
                default:
                    Logger.error("BootReceiver onReceive unknown action!");
                    return;
            }
        else {
            Logger.error("BootReceiver onReceive null action!");
            return;
        }

        Intent serviceIntent = new Intent(context, MainService.class);
        context.startService(serviceIntent);
    }
}
