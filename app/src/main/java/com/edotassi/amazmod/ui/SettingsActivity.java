package com.edotassi.amazmod.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.notification.PersistentNotification;
import com.edotassi.amazmod.support.SilenceApplicationHelper;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.util.LocaleUtils;
import com.edotassi.amazmod.util.Screen;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import java.util.Locale;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.data.SettingsData;
import de.mateware.snacky.Snacky;

public class SettingsActivity extends BaseAppCompatActivity {

    private static final String STATE_CURRENT_LOCALE_LANGUAGE = "STATE_CURRENT_LOCALE_LANGUAGE";

    private boolean currentBatteryChart;
    private boolean currentHeartRateChart;
    private boolean enablePersistentNotificationOnCreate;
    private boolean enableInternetCompaionOnCreate;
    private String currentBatteryChartDays;
    private String currentLocaleLanguage;
    private String currentLogLevel;
    private boolean currentLogToFile;
    private boolean currentDarkTheme;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Screen.isDarkTheme() || MainActivity.systemThemeIsDark) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppTheme);
        }

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
            Logger.error("SettingsActivity onCreate NullPointerException: " + exception.getMessage());
        }

        currentBatteryChart = Prefs.getBoolean(Constants.PREF_BATTERY_CHART, Constants.PREF_DEFAULT_BATTERY_CHART);
        currentHeartRateChart = Prefs.getBoolean(Constants.PREF_HEARTRATE_CHART, Constants.PREF_DEFAULT_HEARTRATE_CHART);

        currentBatteryChartDays = Prefs.getString(Constants.PREF_BATTERY_CHART_TIME_INTERVAL,
                Constants.PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL);

        enablePersistentNotificationOnCreate = Prefs.getBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION,
                Constants.PREF_DEFAULT_ENABLE_PERSISTENT_NOTIFICATION);

        enableInternetCompaionOnCreate = Prefs.getBoolean(Constants.PREF_ENABLE_INTERNET_COMPANION, false);

        currentLogToFile = Prefs.getBoolean(Constants.PREF_LOG_TO_FILE, Constants.PREF_LOG_TO_FILE_DEFAULT);
        currentLogLevel = Prefs.getString(Constants.PREF_LOG_TO_FILE_LEVEL, Constants.PREF_LOG_TO_FILE_LEVEL_DEFAULT);
        currentDarkTheme = Prefs.getBoolean(Constants.PREF_AMAZMOD_DARK_THEME, Constants.PREF_AMAZMOD_DARK_THEME_DEFAULT);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MyPreferenceFragment())
                .commit();

        if (savedInstanceState != null) {
            currentLocaleLanguage = savedInstanceState.getString(STATE_CURRENT_LOCALE_LANGUAGE);
        } else {
            currentLocaleLanguage = LocaleUtils.getLanguage();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        applyLocale();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_CURRENT_LOCALE_LANGUAGE, currentLocaleLanguage);
        super.onSaveInstanceState(outState);
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
        if (restartNeeded()) {
            restartApplication(getApplicationContext());
        } else if (reloadNeeded()) {
            reloadMainActivity();
        }
        sync(false);
        super.onDestroy();
    }

    private void reloadMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("REFRESH", true);
        startActivity(intent);
        finish();
    }

    public static void restartApplication(Context c) {
        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    } else {
                        Logger.error("Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Logger.error("Was not able to restart application, PM null");
                }
            } else {
                Logger.error("Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Logger.error("Was not able to restart application");
        }
    }

    /**
     * Checks if important Preferences were changed and return true if a restart of FULL APP is necessary
     *
     * @return boolean
     */
    private boolean restartNeeded() {
        return currentLogToFile != Prefs.getBoolean(Constants.PREF_LOG_TO_FILE, Constants.PREF_LOG_TO_FILE_DEFAULT)
                || !currentLogLevel.equals(Prefs.getString(Constants.PREF_LOG_TO_FILE_LEVEL, Constants.PREF_LOG_TO_FILE_LEVEL_DEFAULT))
                || currentDarkTheme != Prefs.getBoolean(Constants.PREF_AMAZMOD_DARK_THEME, Constants.PREF_AMAZMOD_DARK_THEME_DEFAULT);
    }

    /**
     * Checks if important Preferences were changed and return true if a restart of MainActivity is necessary
     *
     * @return boolean
     */
    private boolean reloadNeeded() {
        return currentBatteryChart != Prefs.getBoolean(Constants.PREF_BATTERY_CHART, Constants.PREF_DEFAULT_BATTERY_CHART)
                || !currentBatteryChartDays.equals(Prefs.getString(Constants.PREF_BATTERY_CHART_TIME_INTERVAL, Constants.PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL))
                || currentHeartRateChart != Prefs.getBoolean(Constants.PREF_HEARTRATE_CHART, Constants.PREF_DEFAULT_HEARTRATE_CHART);

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
        final boolean enableSoundCustomUI = Prefs.getBoolean(Constants.PREF_NOTIFICATION_ENABLE_SOUND,
                Constants.PREF_DEFAULT_NOTIFICATION_SOUND);
        final boolean disableNotifications = !Prefs.getBoolean(Constants.PREF_ENABLE_NOTIFICATIONS,
                Constants.PREF_DEFAULT_ENABLE_NOTIFICATIONS);
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
        final boolean disableNotificationsDelay = !Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_DELAY,
                Constants.PREF_DEFAULT_NOTIFICATIONS_ENABLE_DELAY);
        final boolean amazModKeepWidget = Prefs.getBoolean(Constants.PREF_AMAZMOD_KEEP_WIDGET,
                Constants.PREF_DEFAULT_AMAZMOD_KEEP_WIDGET);
        final boolean overlayLauncher = Prefs.getBoolean(Constants.PREF_AMAZMOD_OVERLAY_LAUNCHER,
                Constants.PREF_DEFAULT_AMAZMOD_OVERLAY_LAUNCHER);
        final boolean hourlyChime = Prefs.getBoolean(Constants.PREF_AMAZMOD_HOURLY_CHIME,
                Constants.PREF_DEFAULT_AMAZMOD_HOURLY_CHIME);
        final boolean heartrateData = Prefs.getBoolean(Constants.PREF_HEARTRATE_CHART,
                Constants.PREF_DEFAULT_HEARTRATE_CHART);
        final int watchBatteryAlert = Integer.parseInt(Prefs.getString(Constants.PREF_BATTERY_WATCH_ALERT,
                Constants.PREF_DEFAULT_BATTERY_WATCH_ALERT));
        final int phoneBatteryAlert = Integer.parseInt(Prefs.getString(Constants.PREF_BATTERY_PHONE_ALERT,
                Constants.PREF_DEFAULT_BATTERY_PHONE_ALERT));
        final int logLines = Integer.parseInt(Prefs.getString(Constants.PREF_LOG_LINES_SHOWN,
                Constants.PREF_LOG_LINES_SHOWN_DEFAULT));

        final boolean enablePersistentNotificationOnDestroy = Prefs.getBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION,
                Constants.PREF_DEFAULT_ENABLE_PERSISTENT_NOTIFICATION);

        final boolean enableInternetCompanionOnDestroy = Prefs.getBoolean(Constants.PREF_ENABLE_INTERNET_COMPANION, false);

        // Update persistent notification due to changes in Settings
        if (!enablePersistentNotificationOnDestroy && this.enablePersistentNotificationOnCreate) {
            PersistentNotification.cancelPersistentNotification(this);
            this.enablePersistentNotificationOnCreate = false;
        } else if (enablePersistentNotificationOnDestroy && !this.enablePersistentNotificationOnCreate) {
            final PersistentNotification persistentNotification = new PersistentNotification(this, TransportService.model);
            persistentNotification.createPersistentNotification();
            this.enablePersistentNotificationOnCreate = true;
        }

        // Update Internet Companion due to changes in Settings
        if (!enableInternetCompanionOnDestroy && this.enableInternetCompaionOnCreate) {
            TransportService.stopInternetCompanion();
            this.enableInternetCompaionOnCreate = false;
        } else if (enableInternetCompanionOnDestroy && !this.enableInternetCompaionOnCreate) {
            TransportService.startInternetCompanion(getApplicationContext());
            this.enableInternetCompaionOnCreate = true;
        }

        // Maps Notification
        String packageName = "com.google.android.apps.maps";
        if (Prefs.getBoolean(Constants.PREF_ENABLE_MAPS_NOTIFICATION, Constants.PREF_ENABLE_MAPS_NOTIFICATION_DEFAULT)) {
            SilenceApplicationHelper.enablePackage(packageName);
            Logger.debug("Enable Maps notification");
        } else {
            SilenceApplicationHelper.disablePackage(packageName);
            Logger.debug("Disable Maps notification");
        }

        SettingsData settingsData = new SettingsData();
        settingsData.setReplies(replies);
        settingsData.setVibration(vibration);
        settingsData.setScreenTimeout(screeTimeout);
        settingsData.setNotificationsCustomUi(enableCustomUi);
        settingsData.setNotificationSound(enableSoundCustomUI);
        settingsData.setDisableNotifications(disableNotifications);
        settingsData.setDisableNotificationReplies(disableNotificationReplies);
        settingsData.setInvertedTheme(enableInvertedTheme);
        settingsData.setFontSize(fontSize);
        settingsData.setDisableNotificationScreenOn(disableNotificationsScreenOn);
        settingsData.setPhoneConnectionAlert(phoneConnection);
        settingsData.setPhoneConnectionAlertStandardNotification(phoneConnectionStandardNotification);
        settingsData.setDefaultLocale(Locale.getDefault().toString());
        settingsData.setDisableDelay(disableNotificationsDelay);
        settingsData.setAmazModKeepWidget(amazModKeepWidget);
        settingsData.setOverlayLauncher(overlayLauncher);
        settingsData.setHourlyChime(hourlyChime);
        settingsData.setHeartrateData(heartrateData);
        settingsData.setBatteryWatchAlert(watchBatteryAlert);
        settingsData.setBatteryPhoneAlert(phoneBatteryAlert);
        settingsData.setLogLines(logLines);

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

            // Check if Maps is installed
           Package maps = Package.getPackage("com.google.android.apps.maps");
           Preference mapsSetting = getPreferenceScreen().findPreference("preference.enable.maps.notification");
            if(null != maps){
                mapsSetting.setEnabled(true);
                Logger.debug("Google Maps is installed");
            } else {
                mapsSetting.setEnabled(false);
                Prefs.putBoolean(Constants.PREF_ENABLE_MAPS_NOTIFICATION, false);
                mapsSetting.setDefaultValue(true);
                Logger.debug("Google Maps isn't installed");
            }

            // Enable Notification Sound if Verge Only
            Preference vergeNotificationSoundSetting = getPreferenceScreen().findPreference("preference.notification.enable.sound");
            if (Screen.isVerge()) {
                Prefs.putBoolean(Constants.PREF_NOTIFICATION_ENABLE_SOUND, false);
                Preference vergeNotificationSound = getPreferenceScreen().findPreference(Constants.PREF_NOTIFICATION_ENABLE_SOUND);
                vergeNotificationSound.setDefaultValue(false);
                vergeNotificationSound.setEnabled(true);
            } else {
                vergeNotificationSoundSetting.setEnabled(false);
                vergeNotificationSoundSetting.setShouldDisableView(true);
                PreferenceCategory customUI = (PreferenceCategory) findPreference( "preference.customUI");
                customUI.removePreference(vergeNotificationSoundSetting);
            }

            // Persistent Notification Settings
            Preference persistentNotificationDeviceSettingsPreference = getPreferenceScreen().findPreference("preference.persistent.notification.device.settings");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Prefs.putBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION, true);
                Preference persistentNotificationPreference =
                        getPreferenceScreen().findPreference(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION);
                persistentNotificationPreference.setDefaultValue(true);
                persistentNotificationPreference.setEnabled(false);
                // getPreferenceScreen().removePreference(persistentNotificationPreference);

                // Link to notification channel system settings
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, Constants.PERSISTENT_NOTIFICATION_CHANNEL);
                persistentNotificationDeviceSettingsPreference.setIntent(intent);
            } else {
                // Remove link to notification channel system settings
                persistentNotificationDeviceSettingsPreference.setEnabled(false);
                persistentNotificationDeviceSettingsPreference.setShouldDisableView(true);
                PreferenceCategory categoryOthers = (PreferenceCategory) findPreference("preference.others");
                categoryOthers.removePreference(persistentNotificationDeviceSettingsPreference);
            }

            // Disable phone battery alert option, if watchface battery data are off
            if (!Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA)) {
                getPreferenceScreen().findPreference("preference.battery.phone.alert").setEnabled(false);
            }

            Preference darkThemeDefault = getPreferenceScreen().findPreference(Constants.PREF_AMAZMOD_DARK_THEME);
            darkThemeDefault.setDefaultValue(Constants.PREF_AMAZMOD_DARK_THEME_DEFAULT);
        }
    }

    //Set locale and set flag used to activity refresh
    public void applyLocale() {
        if (currentLocaleLanguage.equals(LocaleUtils.getLanguage())) {
            return;
        }
        reloadMainActivity();
    }
}
