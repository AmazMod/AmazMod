package com.edotassi.amazmod.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.edotassi.amazmod.db.model.NotficationSentEntity;
import com.edotassi.amazmod.db.model.NotficationSentEntity_Table;
import com.edotassi.amazmod.receiver.BatteryStatusReceiver;
import com.edotassi.amazmod.receiver.WatchfaceReceiver;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.ui.FilesExtrasActivity;
import com.raizlabs.android.dbflow.sql.language.SQLite;

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

        cleanOldNotificationsSentDb();
    }

    private static void checkIfAppUninstalledThenRemove(Context context) {
        FilesExtrasActivity.checkApps(context);
    }

    private static void cleanOldNotificationsSentDb() {
        long delta = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 2);
        SQLite
                .delete()
                .from(NotficationSentEntity.class)
                .where(NotficationSentEntity_Table.date.lessThan(delta))
                .query();
    }
}
