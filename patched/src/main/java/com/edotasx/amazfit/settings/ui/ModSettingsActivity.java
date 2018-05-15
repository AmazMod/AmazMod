package com.edotasx.amazfit.settings.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.nightscout.NightscoutData;
import com.edotasx.amazfit.nightscout.NightscoutReceiver;
import com.edotasx.amazfit.nightscout.NightscoutService;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.edotasx.amazfit.service.BatteryStatsReceiver;
import com.edotasx.amazfit.transport.TransportService;
import com.huami.watch.transport.DataBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by edoardotassinari on 01/03/18.
 */

public class ModSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private List<String> prefRequireKillApp = new ArrayList<String>() {{
        add(Constants.PREFERENCE_ENABLE_RTL);
        add(Constants.PREFERENCE_DISABLE_BATTERY_CHART);
    }};

    private List<String> prefNeedWatchSync = new ArrayList<String>() {{
        add(Constants.PREFERENCE_AMAZMODSERVICE_VIBRATION);
        add(Constants.PREFERENCE_AMAZMODSERVICE_REPLIES);
        add(Constants.PREFERENCE_AMAZMODSERVICE_ENABLE_REPLIES);
        add(Constants.PREFERENCE_AMAZMODSERVICE_SCREEN_TIMEOUT);
    }};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mod_preferences);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        int index = prefRequireKillApp.indexOf(key);
        if (index > -1) {
            Toast.makeText(this, R.string.pref_require_kill_app, Toast.LENGTH_SHORT).show();
        }

        if (prefNeedWatchSync.indexOf(key) > -1) {
            syncWatchSettings(sharedPreferences);
        }

        if (key.equals(Constants.PREFERENCE_DISABLE_CRASH_REPORTING)) {
            boolean disabledCrashReporting = sharedPreferences.getBoolean(key, false);
            if (disabledCrashReporting) {
                Toast.makeText(this, R.string.crash_report_disabled_tip, Toast.LENGTH_LONG).show();
            }
        }

        if (key.equals(Constants.PREFERENCE_DISABLE_BACKGROUND_SYNC) ||
                key.equals(Constants.PREFERENCE_DISABLE_BATTERY_CHART)) {
            boolean enableBackgroundSync = !sharedPreferences.getBoolean(Constants.PREFERENCE_DISABLE_BACKGROUND_SYNC, false);
            boolean enableChart = !sharedPreferences.getBoolean(Constants.PREFERENCE_DISABLE_BATTERY_CHART, false);

            int interval = Integer.valueOf(sharedPreferences.getString(Constants.PREFERENCE_BATTERY_BACKGROUND_SYNC_INTERVAL, "30"));

            updateAlarmManager(this, BatteryStatsReceiver.class, enableBackgroundSync && enableChart, interval, Constants.BATTERY_STATS_REQUEST_CODE);
        }

        if (key.equals(Constants.PREFERENCE_BATTERY_BACKGROUND_SYNC_INTERVAL)) {
            int interval = PreferenceManager.getInt(this, Constants.PREFERENCE_BATTERY_BACKGROUND_SYNC_INTERVAL, 30);

            updateAlarmManager(this, BatteryStatsReceiver.class, true, interval, Constants.BATTERY_STATS_REQUEST_CODE);
        }

        if (key.equals(Constants.PREFERENCE_NIGHTSCOUT_ENABLED)) {
            boolean enable = sharedPreferences.getBoolean(Constants.PREFERENCE_NIGHTSCOUT_ENABLED, false);
            int interval = Integer.valueOf(sharedPreferences.getString(Constants.PREFERENCE_NIGHTSCOUT_INTERVAL_SYNC, "30"));

            updateAlarmManager(this, NightscoutReceiver.class, enable, interval, Constants.NIGHTSCOUT_REQUEST_CODE);
        }

        if (key.equals(Constants.PREFERENCE_NIGHTSCOUT_INTERVAL_SYNC)) {
            int interval = Integer.valueOf(sharedPreferences.getString(Constants.PREFERENCE_NIGHTSCOUT_INTERVAL_SYNC, "30"));
            updateAlarmManager(this, BatteryStatsReceiver.class, true, interval, Constants.NIGHTSCOUT_REQUEST_CODE);
        }

        if (key.equals(Constants.PREFERENCE_AMAZMODSERVICE_ENABLE)) {
            boolean enable = sharedPreferences.getBoolean(Constants.PREFERENCE_AMAZMODSERVICE_ENABLE, false);

            updateAlarmManager(this, NightscoutService.class, enable, -1, Constants.NIGHTSCOUT_REQUEST_CODE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();

        if ((key != null) && (key.equals(Constants.PREFERENCE_KILL_APP))) {
            System.exit(1);
            //android.os.Process.killProcess(android.os.Process.myPid());
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }

    private void updateAlarmManager(Context context, Class receiver, boolean enable, int interval, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, receiver);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, 0);

        if (enable) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval * 60 * 1000, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void syncWatchSettings(SharedPreferences sharedPreferences) {
        DataBundle dataBundle = new DataBundle();

        String repliesValue = sharedPreferences.getString(
                Constants.PREFERENCE_AMAZMODSERVICE_REPLIES,
                Constants.PREFERENCE_AMAZMODSERVICE_DEFAULT_REPLIES);
        int screenTimeoutValue = Integer.valueOf(sharedPreferences.getString(
                Constants.PREFERENCE_AMAZMODSERVICE_SCREEN_TIMEOUT,
                String.valueOf(Constants.PREFERENCE_AMAZMODSERVICE_DEFAULT_SCREEN_TIMEOUT)));
        int vibrationValue = Integer.valueOf(sharedPreferences.getString(
                Constants.PREFERENCE_AMAZMODSERVICE_VIBRATION,
                String.valueOf(Constants.PREFERENCE_AMAZMODSERVICE_DEFAULT_VIBRATION)));

        dataBundle.putString("notificationReplies", repliesValue);
        dataBundle.putInt("notificationVibration", vibrationValue);
        dataBundle.putInt("notificationScreenTimeout", screenTimeoutValue);

        TransportService
                .sharedInstance(this)
                .send(Constants.SETTINGS_SYNC_ACTION, dataBundle);

        Toast.makeText(this, "Watch settings synced", Toast.LENGTH_SHORT).show();
    }
}
