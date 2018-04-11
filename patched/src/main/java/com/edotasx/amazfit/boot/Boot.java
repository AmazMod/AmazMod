package com.edotasx.amazfit.boot;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.View;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.db.AppDatabase;
import com.edotasx.amazfit.nightscout.NightscoutReceiver;
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

        checkNightscout(pContext);
        checkBatteryStats(pContext);

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

    private void checkNightscout(Context context) {
        boolean enable = PreferenceManager.getBoolean(context, Constants.PREFERENCE_NIGHTSCOUT_ENABLED, false);
        int interval = PreferenceManager.getInt(context, Constants.PREFERENCE_NIGHTSCOUT_INTERVAL_SYNC, 30);

        if (enable) {
            Log.d(Constants.TAG, "enabling Nightscout support, interval: " + interval);
        } else {
            Log.d(Constants.TAG, "disabling Nightscout support");
        }

        updateAlarmManager(context, NightscoutReceiver.class, enable, interval, Constants.NIGHTSCOUT_REQUEST_CODE);
    }

    //TODO merge in helper class
    private void checkBatteryStats(Context context) {
        boolean enable = !PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_BACKGROUND_SYNC, false) ||
                !PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_BATTERY_CHART, false);
        int interval = PreferenceManager.getInt(context, Constants.PREFERENCE_BATTERY_BACKGROUND_SYNC_INTERVAL, 30);

        if (enable) {
            Log.d(Constants.TAG, "enabling battery stats support, interval: " + interval);
        } else {
            Log.d(Constants.TAG, "disabling battery stats support");
        }

        updateAlarmManager(context, BatteryStatsReceiver.class, enable, interval, Constants.BATTERY_STATS_REQUEST_CODE);
    }

    private void updateAlarmManager(Context context, Class receiver, boolean enable, int interval, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, receiver);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,requestCode, intent, 0);

        if (enable) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval * 60 * 1000, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }
}