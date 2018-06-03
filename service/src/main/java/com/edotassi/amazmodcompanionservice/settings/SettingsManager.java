package com.edotassi.amazmodcompanionservice.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.edotassi.amazmodcompanionservice.Constants;
import com.edotassi.amazmodcompanionservice.events.SyncSettingsEvent;

public class SettingsManager {

    private Context context;

    public SettingsManager(Context context) {
        this.context = context;
    }

    public void sync(SyncSettingsEvent event) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, event.getNotificationScreenTimeout());
        editor.putInt(Constants.PREF_NOTIFICATION_VIBRATION, event.getNotificationVibration());
        editor.putString(Constants.PREF_NOTIFICATION_CUSTOM_REPLIES, event.getNotificationCustomReplies());

        editor.apply();
    }

    public int getInt(String key, int defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
    }
}
