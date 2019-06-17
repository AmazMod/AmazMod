package com.edotassi.amazmod.ui;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import com.edotassi.amazmod.R;

import org.tinylog.Logger;

/**************************************************************************************
 * Optional PreferenceActivity for using Preferences in app, keeps record of changes
 * and app can be updated for each change if needed
 */

public class PreferenceActivity extends android.preference.PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = "PreferenceActivity";

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.debug(LOG_TAG+" onCreate");

        addPreferencesFromResource(R.xml.preferences);

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();

    }

    @Override
    public void onDestroy() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

}