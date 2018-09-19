package com.edotassi.amazmod.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.edotassi.amazmod.AmazModApplication;
import amazmod.com.transport.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.receiver.WatchfaceReceiver;
import com.pixplicity.easyprefs.library.Prefs;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.mateware.snacky.Snacky;

public class WatchfaceActivity extends AppCompatActivity {

    @BindView(R.id.send_data_switch)
    Switch send_data_swich;
    @BindView(R.id.send_on_battery_change_switch)
    Switch send_on_battery_change_switch;
    @BindView(R.id.send_on_alarm_change_switch)
    Switch send_on_alarm_change_switch;
    @BindView(R.id.send_watchface_data_interval)
    Spinner send_watchface_data_interval;
    @BindView(R.id.watchface_sync_now_button)
    Button watchface_sync_now_button;
    @BindView(R.id.watchface_last_sync)
    TextView watchface_last_sync;

    boolean send_data;
    int send_data_interval_index;
    int send_data_interval;
    boolean send_on_battery_change;
    boolean send_on_alarm_change;

    private Context mContext;
    private int initialInterval;

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

        mContext = this;

        send_data = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA);
        send_data_interval_index = Prefs.getInt(Constants.PREF_WATCHFACE_SEND_DATA_INTERVAL_INDEX, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX);
        initialInterval = send_data_interval_index;
        send_on_battery_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE);
        send_on_alarm_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE);

        send_data_swich.setChecked(send_data);
        send_data_swich.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //WatchfaceActivity.this.send_data = isChecked;
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_DATA, isChecked);
                //Toast.makeText(WatchfaceActivity.this, "send data: " + isChecked, Toast.LENGTH_SHORT).show();

                WatchfaceActivity.this.send_watchface_data_interval.setEnabled(isChecked);
                WatchfaceActivity.this.send_on_battery_change_switch.setEnabled(isChecked);
                WatchfaceActivity.this.send_on_alarm_change_switch.setEnabled(isChecked);
                WatchfaceActivity.this.watchface_sync_now_button.setEnabled(isChecked);
            }
        });

        send_watchface_data_interval.setSelection(send_data_interval_index);
        send_watchface_data_interval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                //WatchfaceActivity.this.send_data_interval_index = pos;
                Prefs.putInt(Constants.PREF_WATCHFACE_SEND_DATA_INTERVAL_INDEX, pos);
                Prefs.putString(Constants.PREF_WATCHFACE_BACKGROUND_SYNC_INTERVAL, getResources().getStringArray(R.array.pref_watchface_background_sync_interval_values)[pos]);

                //WatchfaceActivity.this.send_data_interval = Integer.parseInt(getResources().getStringArray(R.array.pref_battery_background_sync_interval_values)[pos]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });
        send_watchface_data_interval.setEnabled(send_data);

        // Hide not ready options
        send_on_battery_change_switch.setVisibility(View.GONE);
        send_on_alarm_change_switch.setVisibility(View.GONE);

        send_on_battery_change_switch.setChecked(send_on_battery_change);
        send_on_battery_change_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //WatchfaceActivity.this.send_on_battery_change = isChecked;
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, isChecked);

            }
        });
        send_on_battery_change_switch.setEnabled(send_data);

        send_on_alarm_change_switch.setChecked(send_on_alarm_change);
        send_on_alarm_change_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //WatchfaceActivity.this.send_on_alarm_change = isChecked;
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, isChecked);
            }
        });
        send_on_alarm_change_switch.setEnabled(send_data);

        // Last time read
        watchface_last_sync.setText(lastTimeRead());

        // Sync now button
        final Intent alarmWatchfaceIntent = new Intent(getApplicationContext(), WatchfaceReceiver.class);
        //alarmWatchfaceIntent.setAction("-");
        watchface_sync_now_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(alarmWatchfaceIntent);

                Snacky.builder()
                        .setActivity(WatchfaceActivity.this)
                        .setText(R.string.activity_watchface_data_send)
                        .setDuration(Snacky.LENGTH_SHORT)
                        .build().show();

                watchface_last_sync.setText(lastTimeRead());
            }
        });
        watchface_sync_now_button.setEnabled(send_data);
    }

    @Override
    public void onDestroy() {
        if(
                send_data!=Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, send_data) ||
                send_data_interval_index!=Prefs.getInt(Constants.PREF_WATCHFACE_SEND_DATA_INTERVAL_INDEX, send_data_interval_index) ||
                send_on_battery_change!=Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, send_on_battery_change) ||
                send_on_alarm_change!=Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, send_on_alarm_change)
        ){
            WatchfaceReceiver.startWatchfaceReceiver(mContext);
        }

        //WatchfaceData watchfaceData = new WatchfaceData();
        //watchfaceData.setBattery(this.send_data);
        //SyncWatchface syncSettings = new SyncWatchface(watchfaceData);
        //EventBus.getDefault().post(syncSettings);
        //Toast.makeText(this, R.string.sync_settings, Toast.LENGTH_SHORT).show();

        super.onDestroy();
    }

    private String lastTimeRead(){
        Long timeLastWatchfaceDataSend = Prefs.getLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, 0L);
        Date lastDate = new Date(timeLastWatchfaceDataSend);
        String time = DateFormat.getTimeInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);
        String date = DateFormat.getDateInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);

        Calendar calendarLastDate = Calendar.getInstance();
        calendarLastDate.setTime(lastDate);
        Calendar calendarToday = Calendar.getInstance();
        calendarToday.setTime(new Date());

        String textDate = getResources().getText(R.string.activity_watchface_data_send) + ": ";
        textDate += time;
        if (calendarLastDate.get(Calendar.DAY_OF_MONTH) != calendarToday.get(Calendar.DAY_OF_MONTH)) {
            textDate += " " + date;
        }
        return textDate;
    }
}
