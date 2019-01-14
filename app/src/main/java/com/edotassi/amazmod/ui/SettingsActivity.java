package com.edotassi.amazmod.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import amazmod.com.transport.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.notification.PersistentNotification;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.Locale;

import amazmod.com.transport.data.SettingsData;
import de.mateware.snacky.Snacky;

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_activity_settings_sync) {
            sync(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {

        //Update battery chart properties on preference change
        final boolean disableBatteryChartOnDestroy = Prefs.getBoolean(Constants.PREF_DISABLE_BATTERY_CHART,
                Constants.PREF_DEFAULT_DISABLE_BATTERY_CHART);
        final String batteryChartDaysOnDestroy = Prefs.getString(Constants.PREF_BATTERY_CHART_TIME_INTERVAL,
                Constants.PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL);

        if ((disableBatteryChartOnDestroy != this.disableBatteryChartOnCreate) || (!batteryChartDaysOnDestroy.equals(this.batteryChartDaysOnCreate))) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            finish();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("REFRESH", true);
            startActivity(intent);
        }

        //Change app location configuration and refresh it on preference change
        final boolean forceEN = Prefs.getBoolean(Constants.PREF_FORCE_ENGLISH, false);

        Locale defaultLocale = Locale.getDefault();
        Locale currentLocale = getResources().getConfiguration().locale;
        System.out.println(Constants.TAG + "SettingsActivity locales: " + defaultLocale + " / " + currentLocale.toString());

        if (forceEN && (currentLocale != Locale.US)) {
            setLocale(Locale.US);
        } else if (!forceEN && (currentLocale != defaultLocale)) {
            setLocale(defaultLocale);
        }

        sync(false);

        super.onDestroy();

    }

    private void sync(final boolean sync) {
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
        final boolean phoneConnection = Prefs.getBoolean(Constants.PREF_PHONE_CONNECT_DISCONNECT_ALERT,
                Constants.PREF_DEFAULT_PHONE_CONNECT_DISCONNECT_ALERT);
        final boolean phoneConnectionStandardNotification = Prefs.getBoolean(Constants.PREF_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION,
                Constants.PREF_DEFAULT_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION);
        final boolean disableNotificationsDelay = Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_DISABlE_DELAY,
                Constants.PREF_DEFAULT_NOTIFICATIONS_DISABLE_DELAY);

        final boolean enablePersistentNotificationOnDestroy = Prefs.getBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION,
                Constants.PREF_DEFAULT_ENABLE_PERSISTENT_NOTIFICATION);

        // Update persistent notification due to changes in Settings
        if (!enablePersistentNotificationOnDestroy && this.enablePersistentNotificationOnCreate) {
            PersistentNotification.cancelPersistentNotification(this);
        } else if (enablePersistentNotificationOnDestroy && !this.enablePersistentNotificationOnCreate) {
            PersistentNotification persistentNotification = new PersistentNotification(this, TransportService.model);
            persistentNotification.createPersistentNotification();
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
        settingsData.setPhoneConnectionAlert(phoneConnection);
        settingsData.setPhoneConnectionAlertStandardNotification(phoneConnectionStandardNotification);
        settingsData.setDefaultLocale(Locale.getDefault().toString());
        settingsData.setDisableDelay(disableNotificationsDelay);

        Watch.get().syncSettings(settingsData).continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(@NonNull Task<Void> task) throws Exception {
                final String str;
                if (task.isSuccessful()) {
                    str = getResources().getString(R.string.settings_applied);
                    if (sync) {
                        Snacky.builder()
                                .setActivity(SettingsActivity.this)
                                .setText(str)
                                .setDuration(Snacky.LENGTH_SHORT)
                                .build().show();
                    } else Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
                } else {
                    str = getResources().getString(R.string.settings_cant_be_applied);
                    if (sync) {
                        Snacky.builder()
                                .setActivity(SettingsActivity.this)
                                .setText(str)
                                .setDuration(Snacky.LENGTH_SHORT)
                                .build().show();
                    } else Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
                }
                return null;
            }
        });
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Prefs.putBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION, true);
                Preference persistentNotificationPreference = getPreferenceScreen().findPreference(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION);
                persistentNotificationPreference.setDefaultValue(true);
                persistentNotificationPreference.setEnabled(false);
            }
        }
    }

    //Set locale and set flag used to activity refresh
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
