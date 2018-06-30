package com.edotassi.amazmod.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.SyncSettings;
import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.SettingsData;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class SettingsActivity extends AppCompatActivity {

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MyPreferenceFragment())
                .commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            final String replies = sharedPreferences.getString(Constants.PREF_NOTIFICATIONS_REPLIES,
                    Constants.PREF_DEFAULT_NOTIFICATIONS_REPLIES);
            final int vibration = Integer.valueOf(sharedPreferences.getString(Constants.PREF_NOTIFICATIONS_VIBRATION,
                    Constants.PREF_DEFAULT_NOTIFICATIONS_VIBRATION));
            final int screeTimeout = Integer.valueOf(sharedPreferences.getString(Constants.PREF_NOTIFICATIONS_SCREEN_TIMEOUT,
                    Constants.PREF_DEFAULT_NOTIFICATIONS_SCREEN_TIMEOUT));
            final boolean enableCustomUi = sharedPreferences.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI,
                    Constants.PREF_DEFAULT_NOTIFICATIONS_CUSTOM_UI);

            SettingsData settingsData = new SettingsData();
            settingsData.setReplies(replies);
            settingsData.setVibration(vibration);
            settingsData.setScreenTimeout(screeTimeout);
            settingsData.setNotificationsCustomUi(enableCustomUi);

            SyncSettings syncSettings = new SyncSettings(settingsData);

            HermesEventBus.getDefault().post(syncSettings);

            Toast.makeText(getActivity(), R.string.sync_settings, Toast.LENGTH_SHORT).show();
        }
    }
}
