package com.edotassi.amazmod.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.SyncSettings;
import com.edotassi.amazmod.notification.PersistentNotification;
import com.edotassi.amazmod.transport.TransportService;
import com.huami.watch.transport.DataBundle;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.Locale;

import amazmod.com.transport.data.SettingsData;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class SettingsActivity extends AppCompatActivity {

    private boolean disableBatteryChartOnCreate;
    private boolean enablePersistentNotificationOnCreate;
    private String batteryChartDaysOnCreate;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.settings);

        this.disableBatteryChartOnCreate = Prefs.getBoolean(Constants.PREF_DISABLE_BATTERY_CHART,
                Constants.PREF_DEFAULT_DISABLE_BATTERY_CHART);

        this.enablePersistentNotificationOnCreate = Prefs.getBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION,
                Constants.PREF_DEFAULT_ENABLE_PERSISTENT_NOTIFICATION);

        this.batteryChartDaysOnCreate = Prefs.getString(Constants.PREF_BATTERY_CHART_TIME_INTERVAL,
                Constants.PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MyPreferenceFragment())
                .commit();
    }

    @Override
    public void onDestroy() {
        final String replies = Prefs.getString(Constants.PREF_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_NOTIFICATIONS_REPLIES);
        final int vibration = Integer.valueOf(Prefs.getString(Constants.PREF_NOTIFICATIONS_VIBRATION,
                Constants.PREF_DEFAULT_NOTIFICATIONS_VIBRATION));
        final int screeTimeout = Integer.valueOf(Prefs.getString(Constants.PREF_NOTIFICATIONS_SCREEN_TIMEOUT,
                Constants.PREF_DEFAULT_NOTIFICATIONS_SCREEN_TIMEOUT));
        final boolean enableCustomUi = Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI,
                Constants.PREF_DEFAULT_NOTIFICATIONS_CUSTOM_UI);
        final boolean disableNotifications = Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS);
        final boolean disableNotificationReplies = Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);
        final boolean enableInvertedTheme = Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_INVERTED_THEME,
                Constants.PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME);
        final String fontSize = Prefs.getString(Constants.PREF_NOTIFICATIONS_FONT_SIZE,
                Constants.PREF_DEFAULT_NOTIFICATIONS_FONT_SIZE);
        final boolean disableNotificationsScreenOn = Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_SCREENON,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON);

        final boolean disableBatteryChartOnDestroy = Prefs.getBoolean(Constants.PREF_DISABLE_BATTERY_CHART,
                Constants.PREF_DEFAULT_DISABLE_BATTERY_CHART);
        final String batteryChartDaysOnDestroy = Prefs.getString(Constants.PREF_BATTERY_CHART_TIME_INTERVAL,
                Constants.PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL);
        final boolean enablePersistentNotificationOnDestroy = Prefs.getBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION,
                Constants.PREF_DEFAULT_ENABLE_PERSISTENT_NOTIFICATION);

        if ((disableBatteryChartOnDestroy != this.disableBatteryChartOnCreate) || (!batteryChartDaysOnDestroy.equals(this.batteryChartDaysOnCreate))) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            finish();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("REFRESH", true);
            startActivity(intent);
        }

        // Remove persistent notification if it was disabled and was previously enabled
        if (!enablePersistentNotificationOnDestroy && this.enablePersistentNotificationOnCreate) {
            //NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            //notificationManager.cancel(999989);
            PersistentNotification.cancelPersistentNotification(this);
        } else if (enablePersistentNotificationOnDestroy && !this.enablePersistentNotificationOnCreate) {
            PersistentNotification persistentNotification = new PersistentNotification(this, TransportService.model);
            persistentNotification.createPersistentNotification();
        }

        //Change app localtion configuration and refresh it on preferece change
        final boolean forceEN = Prefs.getBoolean(Constants.PREF_FORCE_ENGLISH, false);

        Locale defaultLocale = Locale.getDefault();
        Locale currentLocale = getResources().getConfiguration().locale;
        System.out.println(Constants.TAG + "SettingsActivity locales: " + defaultLocale + " / " + currentLocale.toString());

        if (forceEN && (currentLocale != Locale.US)) {
            setLocale(Locale.US);
        } else if (!forceEN && (currentLocale != defaultLocale)){
            setLocale(defaultLocale);
        }

        SettingsData settingsData = new SettingsData();
        settingsData.setReplies(replies);
        settingsData.setVibration(vibration);
        settingsData.setScreenTimeout(screeTimeout);
        settingsData.setNotificationsCustomUi(enableCustomUi);
        settingsData.setDisableNotifications(disableNotifications);
        settingsData.setDisableNotificationReplies(disableNotificationReplies);
        settingsData.setInvertedTheme(enableInvertedTheme);
        settingsData.setFontSize(fontSize);
        settingsData.setDisableNotificationScreenOn(disableNotificationsScreenOn);

        SyncSettings syncSettings = new SyncSettings(settingsData);

        HermesEventBus.getDefault().post(syncSettings);

        Toast.makeText(this, R.string.sync_settings, Toast.LENGTH_SHORT).show();

        super.onDestroy();
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                addPreferencesFromResource(R.xml.preferences_oreo);
            } else {
                addPreferencesFromResource(R.xml.preferences);
            }

        }
    }

    //set locale and set flag used to activity refresh
    public void setLocale(Locale lang) {
        System.out.println(Constants.TAG + "SettingsActivity New locale: " + lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = lang;
        res.updateConfiguration(conf, dm);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        finish();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("REFRESH", true);
        startActivity(intent);
    }

}
