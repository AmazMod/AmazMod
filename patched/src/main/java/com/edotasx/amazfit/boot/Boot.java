package com.edotasx.amazfit.boot;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.db.AppDatabase;
import com.edotasx.amazfit.notification.NotificationManager;
import com.edotasx.amazfit.permission.PermissionManager;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.edotasx.amazfit.service.BatteryStatsReceiver;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.raizlabs.android.dbflow.config.DatabaseConfig;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

/**
 * Created by edoardotassinari on 11/02/18.
 */

public class Boot {

    private static Boot mInstance;
    private Context mContext;

    private boolean mInitiated;

    public static Boot sharedInstance(Context pContext) {
        if (mInstance == null) {
            mInstance = new Boot(pContext);
        }

        return mInstance;
    }

    private Boot(Context pContext) {
        mContext = pContext;
    }

    public void run() {
        if (mInitiated) {
            return;
        }

        mInitiated = true;

        initiate(mContext);
    }

    private void initiate(Context pContext) {
        PermissionManager.sharedInstance().requestPermissions(pContext);

        initiateDb(pContext);
        initiateBatteryStats(pContext);

        NotificationManager.initialize(pContext);

        new AppUpdater(pContext)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo(Constants.GITHUB_USERNAME, Constants.GITHUB_REPOSITORY)
                .setButtonUpdate(null)
                .setContentOnUpdateAvailable(R.string.content_update_available)
                .start();
    }

    private void initiateDb(Context context) {
        FlowManager.init(
                FlowConfig.builder(context)
                        .addDatabaseConfig(DatabaseConfig.builder(AppDatabase.class).build())
                        .build());
    }

    private void initiateBatteryStats(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BatteryStatsReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        boolean enable = !PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_BACKGROUND_SYNC, false) ||
                !PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_BATTERY_CHART, false);

        if (enable) {
            int interval = PreferenceManager.getInt(context, Constants.PREFERENCE_BATTERY_BACKGROUND_SYNC_INTERVAL, 30);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval * 60 * 1000, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}