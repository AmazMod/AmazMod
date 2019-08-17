package com.edotassi.amazmod.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.edotassi.amazmod.BuildConfig;
import com.edotassi.amazmod.db.model.NotficationSentEntity;
import com.edotassi.amazmod.db.model.NotficationSentEntity_Table;
import com.edotassi.amazmod.receiver.BatteryStatusReceiver;
import com.edotassi.amazmod.receiver.WatchfaceReceiver;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.ui.FilesExtrasActivity;
import com.edotassi.amazmod.update.Updater;
import com.google.gson.Gson;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import amazmod.com.transport.Constants;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Setup {

    public static void run(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, TransportService.class));
        } else {
            context.startService(new Intent(context, TransportService.class));
        }

        //Start receivers
        BatteryStatusReceiver.startBatteryReceiver(context);
        WatchfaceReceiver.startWatchfaceReceiver(context);

        //Check for uninstalled apps and command history
        FilesExtrasActivity.checkApps(context);
        FilesExtrasActivity.loadCommandHistoryFromPrefs();

        //Remove old entries from NotificationSent DB
        cleanOldNotificationsSentDb();
    }

    public static void checkServiceUpdate(final Updater updater, final int currentVersion) {
        String updateUrl = Constants.SERVICE_UPDATE_URL;

        Request request = new Request.Builder()
                .url(updateUrl)
                .build();
        Logger.info("checkServiceUpdate: started OTA process");

        OkHttpClient client = new OkHttpClient();
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Logger.debug("checkServiceUpdate: failed to check for updates");
                        updater.updateCheckFailed();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try {
                            String json = response.body().string();
                            Properties data = new Gson().fromJson(json, Properties.class);
                            int betaVersionCode = Integer.valueOf(data.getProperty("betaVersionCode"));
                            int latestVersionValue = Integer.valueOf(data.getProperty("release"));
                            Logger.info("checkServiceUpdate: checking if application VERSION_CODE (" + BuildConfig.VERSION_CODE + ") is greater or equals " + betaVersionCode);
                            if (BuildConfig.VERSION_CODE >= betaVersionCode){
                                latestVersionValue = Integer.valueOf(data.getProperty("beta"));
                                Logger.info("checkServiceUpdate: will use BETA service available " +  latestVersionValue);
                            }else{
                                Logger.info("checkServiceUpdate: will use RELEASE service available " +  latestVersionValue);
                            }
                            Logger.info("checkServiceUpdate: current service = " + currentVersion + " // available service = " + latestVersionValue);
                            if (!(currentVersion >= latestVersionValue)) {
                                Logger.info("checkServiceUpdate: showing warning that new version is available  ( ͡° ͜ʖ ͡°)");
                                updater.updateAvailable(latestVersionValue);
                            }else{
                                Logger.info("checkServiceUpdate: NO new version is available ¯\\_(ツ)_/¯");
                            }
                        } catch (Exception ex) {
                            updater.updateCheckFailed();
                            Logger.error(ex,"checkServiceUpdate: Error checking for update");
                        }
                    }
                });
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
