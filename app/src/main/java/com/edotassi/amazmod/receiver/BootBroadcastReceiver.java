package com.edotassi.amazmod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.edotassi.amazmod.transport.TransportService;

import org.tinylog.Logger;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Logger.debug("boot received");

        if (intent.getAction() != null)
            switch (intent.getAction()) {
                case "android.intent.action.BOOT_COMPLETED":
                case "android.intent.action.QUICKBOOT_POWERON":
                case "android.intent.action.REBOOT":
                case "android.intent.action.USER_PRESENT":
                    Logger.debug("action: " + intent.getAction());
                    break;
                default:
                    Logger.error("unknown action");
                    return;
            }
        else {
            Logger.error("null action");
            return;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, TransportService.class));
        } else {
            context.startService(new Intent(context, TransportService.class));
        }

        // Initiate receivers after boot
        BatteryStatusReceiver.startBatteryReceiver(context);
        WatchfaceReceiver.startWatchfaceReceiver(context);
    }

}
