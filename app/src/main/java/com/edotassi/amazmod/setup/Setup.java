package com.edotassi.amazmod.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.crashlytics.android.Crashlytics;
import com.edotassi.amazmod.BuildConfig;
import com.edotassi.amazmod.db.model.NotficationSentEntity;
import com.edotassi.amazmod.db.model.NotficationSentEntity_Table;
import com.edotassi.amazmod.receiver.BatteryStatusReceiver;
import com.edotassi.amazmod.receiver.WatchfaceReceiver;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.ui.FilesExtrasActivity;
import com.edotassi.amazmod.update.Updater;
import com.google.gson.Gson;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.sql.language.SQLite;

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

        BatteryStatusReceiver.startBatteryReceiver(context);
        WatchfaceReceiver.startWatchfaceReceiver(context);

        checkIfAppUninstalledThenRemove(context);

        cleanOldNotificationsSentDb();
    }

    public static void checkServiceUpdate(final Updater updater, final String currentVersion) {

        String updateUrl = Constants.SERVICE_UPDATE_URL;

        Request request = new Request.Builder()
                .url(updateUrl)
                .build();
        System.out.println("AmazMod Setup checkServiceUpdate started");

        OkHttpClient client = new OkHttpClient();
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        updater.updateCheckFailed();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            String json = response.body().string();
                            Properties data = new Gson().fromJson(json, Properties.class);
                            int betaVersionCode = Integer.valueOf(data.getProperty("betaVersionCode"));
                            int latestVersionValue = Integer.valueOf(data.getProperty("release"));
                            if (BuildConfig.VERSION_CODE >= betaVersionCode){
                                latestVersionValue = Integer.valueOf(data.getProperty("beta"));
                            }

                            int currentVersionValue = Integer.valueOf(currentVersion);

                            System.out.println("I/AmazMod Setup versions = " + currentVersionValue + " // " + latestVersionValue);
                            if (!(currentVersionValue >= latestVersionValue)) {
                                updater.updateAvailable(latestVersionValue);
                            }
                        } catch (Exception ex) {
                            updater.updateCheckFailed();
                            Crashlytics.logException(ex);
                        }
                    }
                });
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
