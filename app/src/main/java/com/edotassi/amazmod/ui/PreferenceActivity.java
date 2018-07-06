package com.edotassi.amazmod.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.Locale;

/**************************************************************************************
 * Optional PreferenceActivity for using Preferences in app, keeps record of changes
 * and app can be updated for each change if needed
 */

public class PreferenceActivity extends android.preference.PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = "PreferenceActivity";

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_FORCE_ENGLISH)) {

            final boolean forceEN = Prefs.getBoolean(Constants.PREF_FORCE_ENGLISH, false);

            Locale defaultLocale = Locale.getDefault();
            Locale currentLocale = getResources().getConfiguration().locale;
            System.out.println("Initial locales: " + defaultLocale + " / " + currentLocale.toString());

            if (forceEN && (currentLocale != Locale.US)) {
                setLocale(Locale.US);
            } else if (!forceEN && (currentLocale != defaultLocale)){
                setLocale(defaultLocale);
            }
            recreate();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("EXIT", true);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");

        addPreferencesFromResource(R.xml.preferences);

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

    }

    public void setLocale(Locale lang) {
        System.out.println("New locale: " + lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = lang;
        res.updateConfiguration(conf, dm);
//        Intent refresh = new Intent(getApplicationContext(), MainActivity.class);
//        Intent refresh = getIntent();
//        startActivity(refresh);
//        finish();
        recreate();
    }

    private void restartActivity() {
        Log.d(LOG_TAG, "Restart activity " + this);
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

}