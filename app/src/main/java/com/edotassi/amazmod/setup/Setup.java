package com.edotassi.amazmod.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.edotassi.amazmod.receiver.BatteryStatusReceiver;
import com.edotassi.amazmod.receiver.WatchfaceReceiver;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.ui.FilesExtrasActivity;

public class Setup {

    public static void run(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, TransportService.class));
        } else {
            context.startService(new Intent(context, TransportService.class));
        }

        BatteryStatusReceiver.startBatteryReceiver(context);
        WatchfaceReceiver.startWatchfaceReceiver(context);

        checkIfAppUninstalledThenRemove(context);
    }

    private static void checkIfAppUninstalledThenRemove(Context context) {
        FilesExtrasActivity.checkApps(context);
    }
}
