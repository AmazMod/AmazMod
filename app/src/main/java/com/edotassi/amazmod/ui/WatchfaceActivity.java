package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.Brightness;
import com.edotassi.amazmod.event.SyncWatchface;
import com.pixplicity.easyprefs.library.Prefs;

import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.WatchfaceData;
import butterknife.BindView;
import butterknife.ButterKnife;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class WatchfaceActivity extends AppCompatActivity {

    @BindView(R.id.altitude_swich)
    Switch altitude_swich;
    @BindView(R.id.phone_battery_swich)
    Switch phone_battery_swich;

    boolean show_altitude = false;
    boolean show_phone_battery = true;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchface);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.watchface);

        ButterKnife.bind(this);

        /*
        altitude_swich.setOnSwitchChangeListener(new Switch.OnS() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                BrightnessData brightnessData = new BrightnessData();
                brightnessData.setLevel(seekBar.getProgress());

                HermesEventBus.getDefault().post(new Brightness(brightnessData));

                Toast.makeText(WatchfaceActivity.this, "Brightness set to " + seekBar.getProgress(), Toast.LENGTH_SHORT).show();
            }
        });*/

        altitude_swich.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WatchfaceActivity.this.show_altitude = isChecked;
                Toast.makeText(WatchfaceActivity.this, "Altitude is " + isChecked, Toast.LENGTH_SHORT).show();
            }
        });

        phone_battery_swich.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WatchfaceActivity.this.show_phone_battery = isChecked;
                Toast.makeText(WatchfaceActivity.this, "Battery is " + isChecked, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        WatchfaceData watchfaceData = new WatchfaceData();
        watchfaceData.setShowAltitude(this.show_altitude);
        watchfaceData.setShowBattery(this.show_phone_battery);

        SyncWatchface syncSettings = new SyncWatchface(watchfaceData);

        HermesEventBus.getDefault().post(syncSettings);

        Toast.makeText(this, R.string.sync_settings, Toast.LENGTH_SHORT).show();

        super.onDestroy();
    }
}
