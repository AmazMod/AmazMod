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
import android.view.View;
import android.widget.Toast;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.edotasx.amazfit.service.BatteryStatsReceiver;
import com.huami.watch.companion.ui.view.ActionbarLayout;

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

        if (key.equals(Constants.PREFERENCE_DISABLE_CRASH_REPORTING)) {
            boolean disabledCrashReporting = sharedPreferences.getBoolean(key, false);
            if (disabledCrashReporting) {
                Toast.makeText(this, R.string.crash_report_disabled_tip, Toast.LENGTH_LONG).show();
            }
        }

        if (key.equals(Constants.PREFERENCE_DISABLE_BACKGROUND_SYNC) ||
                key.equals(Constants.PREFERENCE_DISABLE_BATTERY_CHART)) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, BatteryStatsReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            alarmManager.cancel(pendingIntent);
        }

        if (key.equals(Constants.PREFERENCE_BATTERY_BACKGROUND_SYNC_INTERVAL)) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, BatteryStatsReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

            int interval = PreferenceManager.getInt(this, Constants.PREFERENCE_BATTERY_BACKGROUND_SYNC_INTERVAL, 30);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval * 60 * 1000, pendingIntent);
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
}
