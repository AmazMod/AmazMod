package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.SyncWatchface;

import org.greenrobot.eventbus.EventBus;

import amazmod.com.transport.data.WatchfaceData;
import butterknife.BindView;
import butterknife.ButterKnife;

public class WatchfaceActivity extends AppCompatActivity {

    @BindView(R.id.send_data_switch)
    Switch send_data_swich;
    @BindView(R.id.send_on_battery_change_switch)
    Switch send_on_battery_change_switch;
    @BindView(R.id.send_on_alarm_change_switch)
    Switch send_on_alarm_change_switch;
    @BindView(R.id.send_watchface_data_interval)
    Spinner send_watchface_data_interval;

    boolean send_data = true;
    int send_data_interval_index = 0;
    int send_data_interval;
    boolean send_on_battery_change = false;
    boolean send_on_alarm_change = false;

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

        send_data_swich.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WatchfaceActivity.this.send_data = isChecked;
                Toast.makeText(WatchfaceActivity.this, "send data: " + isChecked, Toast.LENGTH_SHORT).show();
            }
        });

        send_watchface_data_interval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                WatchfaceActivity.this.send_data_interval = Integer.parseInt(getResources().getStringArray(R.array.pref_battery_background_sync_interval_values)[pos]);
                Toast.makeText(parent.getContext(),
                        "OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString(),
                        Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });

        send_on_battery_change_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WatchfaceActivity.this.send_on_battery_change = isChecked;
                Toast.makeText(WatchfaceActivity.this, "send battery on change: " + isChecked, Toast.LENGTH_SHORT).show();
            }
        });

        send_on_alarm_change_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                WatchfaceActivity.this.send_on_alarm_change = isChecked;
            }
        });
    }

    @Override
    public void onDestroy() {
        //WatchfaceData watchfaceData = new WatchfaceData();
        //watchfaceData.setBattery(this.send_data);
        //SyncWatchface syncSettings = new SyncWatchface(watchfaceData);
        //EventBus.getDefault().post(syncSettings);
        //Toast.makeText(this, R.string.sync_settings, Toast.LENGTH_SHORT).show();

        super.onDestroy();
    }
}
