package com.edotassi.amazmod.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.receiver.WatchfaceReceiver;
import com.edotassi.amazmod.util.FilesUtil;
import com.edotassi.amazmod.util.Permissions;
import com.pixplicity.easyprefs.library.Prefs;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.util.MapTimeZoneCache;

import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.mateware.snacky.Snacky;

public class WatchfaceActivity extends BaseAppCompatActivity {

    @BindView(R.id.send_data_switch)
    Switch send_data_swich;
    @BindView(R.id.send_on_battery_change_switch)
    Switch send_on_battery_change_switch;
    @BindView(R.id.send_on_alarm_change_switch)
    Switch send_on_alarm_change_switch;
    @BindView(R.id.send_watchface_data_interval)
    Spinner send_watchface_data_interval;
    @BindView(R.id.send_watchface_data_calendar_events_days)
    Spinner send_watchface_data_calendar_events_days;
    @BindView(R.id.watchface_sync_now_button)
    Button watchface_sync_now_button;
    @BindView(R.id.watchface_last_sync)
    TextView watchface_last_sync;

    @BindView(R.id.watchface_permission_status)
    TextView watchface_permission_status;
    @BindView(R.id.watchface_calendar_radio_group)
    RadioGroup watchface_calendar_radio_group;
    @BindView(R.id.watchface_source_local_radiobutton)
    RadioButton watchface_source_local_radiobutton;
    @BindView(R.id.watchface_ics_calendar_radiobutton)
    RadioButton watchface_ics_calendar_radiobutton;
    @BindView(R.id.watchface_test_ics_button)
    Button watchface_test_ics_button;
    @BindView(R.id.watchface_ics_url_edittext)
    EditText watchface_ics_url_edittext;


    boolean send_data;
    int send_data_interval_index;
    int send_data_calendar_events_days_index;
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.watchface);
        }

        ButterKnife.bind(this);

        mContext = this;

        send_data = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA);
        send_data_interval_index = Prefs.getInt(Constants.PREF_WATCHFACE_SEND_DATA_INTERVAL_INDEX, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX);
        initialInterval = send_data_interval_index;
        send_on_battery_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE);
        send_on_alarm_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE);
        send_data_calendar_events_days_index = Prefs.getInt(Constants.PREF_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX);

        //Restore calendar source data from preferences
        String calendar_source = Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_SOURCE, Constants.PREF_CALENDAR_SOURCE_LOCAL);
        final String url = Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_ICS_URL, "");
        if (!url.isEmpty())
            watchface_ics_url_edittext.setText(url);
        if (Constants.PREF_CALENDAR_SOURCE_LOCAL.equals(calendar_source)) {
            watchface_source_local_radiobutton.setChecked(true);
            changeWidgetsStatus(false);
        } else {
            watchface_ics_calendar_radiobutton.setChecked(true);
            changeWidgetsStatus(true);
        }

        // Send data on/off
        send_data_swich.setChecked(send_data);
        send_data_swich.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //WatchfaceActivity.this.send_data = isChecked;
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_DATA, isChecked);

                WatchfaceActivity.this.send_watchface_data_interval.setEnabled(isChecked);
                WatchfaceActivity.this.send_watchface_data_calendar_events_days.setEnabled(isChecked);
                WatchfaceActivity.this.send_on_battery_change_switch.setEnabled(isChecked);
                WatchfaceActivity.this.send_on_alarm_change_switch.setEnabled(isChecked);
                WatchfaceActivity.this.watchface_sync_now_button.setEnabled(isChecked);
            }
        });

        // Data inteval option
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
                // Auto-generated method stub
            }
        });
        send_watchface_data_interval.setEnabled(send_data);

        // Calendar options
        send_watchface_data_calendar_events_days.setSelection(send_data_calendar_events_days_index);
        send_watchface_data_calendar_events_days.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Prefs.putInt(Constants.PREF_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX, pos);
                Prefs.putString(Constants.PREF_WATCHFACE_CALENDAR_EVENTS_DAYS, getResources().getStringArray(R.array.pref_watchface_calendar_events_days_values)[pos]);

                // Show found local events
                showFoundBuildInCalendarEvents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // Auto-generated method stub
            }
        });
        send_watchface_data_calendar_events_days.setEnabled(send_data);

        // Hide if not a developer
        if( !Prefs.getBoolean(Constants.PREF_ENABLE_DEVELOPER_MODE, false) ){
            send_on_battery_change_switch.setVisibility(View.GONE);
            Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, false);
            send_on_battery_change = false;
        }
        //send_on_alarm_change_switch.setVisibility(View.GONE);

        // battery on change
        send_on_battery_change_switch.setChecked(send_on_battery_change);
        send_on_battery_change_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, isChecked);

            }
        });
        send_on_battery_change_switch.setEnabled(send_data);

        // alarm on change
        send_on_alarm_change_switch.setChecked(send_on_alarm_change);
        send_on_alarm_change_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, isChecked);

            }
        });
        send_on_alarm_change_switch.setEnabled(send_data);

        // Last time read
        watchface_last_sync.setText(lastTimeRead());

        // Sync now button
        final Intent alarmWatchfaceIntent = new Intent(getApplicationContext(), WatchfaceReceiver.class);
        watchface_sync_now_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save new events as last send
                Prefs.putString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, "");

                alarmWatchfaceIntent.putExtra("refresh", true);
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

        // Calendar Source selection
        watchface_calendar_radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //System.out.println(Constants.TAG + "WatchfaceActivity onCheckedChanged: " + checkedId);
                if (checkedId == watchface_source_local_radiobutton.getId()) {
                    Prefs.putString(Constants.PREF_WATCHFACE_CALENDAR_SOURCE, Constants.PREF_CALENDAR_SOURCE_LOCAL);
                    changeWidgetsStatus(false);

                } else {
                    Prefs.putString(Constants.PREF_WATCHFACE_CALENDAR_SOURCE, Constants.PREF_CALENDAR_SOURCE_ICS);
                    changeWidgetsStatus(true);
                }
            }
        });
        // Show found local events
        showFoundBuildInCalendarEvents();

        // Test calendar ICS file
        watchface_test_ics_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkICSFile();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        checkCalendarPermission();
    }

    @Override
    public void onDestroy() {
        if (
                send_data != Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, send_data) ||
                        send_data_interval_index != Prefs.getInt(Constants.PREF_WATCHFACE_SEND_DATA_INTERVAL_INDEX, send_data_interval_index) ||
                        send_on_battery_change != Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, send_on_battery_change) ||
                        send_on_alarm_change != Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, send_on_alarm_change)
                ) {

            WatchfaceReceiver.startWatchfaceReceiver(mContext);
        }

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            System.out.println("D/AmazMod WatchfaceActivity ORIENTATION PORTRAIT");
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            System.out.println("D/AmazMod WatchfaceActivity ORIENTATION LANDSCAPE");
        }
    }

    private String lastTimeRead() {
        Long timeLastWatchfaceDataSend = Prefs.getLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, 0L);
        Date lastDate = new Date(timeLastWatchfaceDataSend);
        String time = DateFormat.getTimeInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);
        String date = DateFormat.getDateInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);

        Calendar calendarLastDate = Calendar.getInstance();
        calendarLastDate.setTime(lastDate);
        Calendar calendarToday = Calendar.getInstance();
        calendarToday.setTime(new Date());

        String textDate = getResources().getText(R.string.activity_watchface_last_time_sent) + ": ";
        textDate += time;
        if (calendarLastDate.get(Calendar.DAY_OF_MONTH) != calendarToday.get(Calendar.DAY_OF_MONTH)) {
            textDate += " " + date;
        }
        return textDate;
    }

    private void checkCalendarPermission() {
        if (Permissions.hasPermission(getApplicationContext(), Manifest.permission.READ_CALENDAR)){
            watchface_permission_status.setText(getResources().getString(R.string.enabled).toUpperCase());
            watchface_permission_status.setTextColor(getResources().getColor(R.color.colorCharging));
        } else {
            watchface_permission_status.setText(getResources().getString(R.string.disabled).toUpperCase());
            watchface_permission_status.setTextColor(getResources().getColor(R.color.colorAccent));
            watchface_permission_status.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
        }
    }

    private void showFoundBuildInCalendarEvents() {
        int events = WatchfaceReceiver.countBuildinCalendarEvents(getApplicationContext());
        String buildInCalendarWithEvents = getResources().getText(R.string.watchface_built_in_calendar) + " ("+ getResources().getString(R.string.watchface_events_found,Integer.toString(events)) +")";
        watchface_source_local_radiobutton.setText( buildInCalendarWithEvents );
    }

    private void showFoundICSCalendarEvents() {
        showFoundICSCalendarEvents(false, null);
    }

    private void showFoundICSCalendarEvents(boolean update, net.fortuna.ical4j.model.Calendar calendar) {
        int events = WatchfaceReceiver.countICSEvents(getApplicationContext(), update, calendar);
        String icsCalendarWithEvents = getResources().getText(R.string.watchface_remote_ics_file) + " ("+ getResources().getString(R.string.watchface_events_found,Integer.toString(events)) +")";
        watchface_ics_calendar_radiobutton.setText( icsCalendarWithEvents );
    }

    private void changeWidgetsStatus(boolean state){
        watchface_test_ics_button.setEnabled(state);
        watchface_ics_url_edittext.setEnabled(state);
    }

    private void checkICSFile() {
        String editText = watchface_ics_url_edittext.getText().toString();
        String testURL = editText.toLowerCase();
        Logger.debug("WatchfaceActivity checkICSFile editText: " + editText);
        MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(mContext)
                .canceledOnTouchOutside(true)
                .positiveText(R.string.ok);
        if (!editText.isEmpty() && (testURL.startsWith("http://") || testURL.startsWith("https://"))
                && (testURL.endsWith("ics"))) {

            String workDir = mContext.getCacheDir().getAbsolutePath();
            try {
                boolean result = new FilesUtil.urlToFile().execute(editText, workDir, "new_calendar.ics").get();
                if (result) {

                    System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());

                    FileInputStream in = new FileInputStream(workDir + File.separator + "new_calendar.ics");
                    CalendarBuilder builder = new CalendarBuilder();
                    net.fortuna.ical4j.model.Calendar calendar = builder.build(in);
                    //List<Property> propertyList = calendar.getProperties();
                    String msg = "";
                    if (calendar.getProperty("PRODID") != null)
                        msg += "PRODID: " + calendar.getProperty("PRODID").getValue() + "\n";
                    if (calendar.getProperty("X-WR-CALDESC") != null)
                        msg += "CALDESC: " + calendar.getProperty("X-WR-CALDESC").getValue() + "\n";
                    if (calendar.getProperty("X-WR-CALNAME") != null)
                        msg += "CALNAME: " + calendar.getProperty("X-WR-CALNAME").getValue() + "\n";
                    if (calendar.getProperty("X-WR-TIMEZONE") != null)
                        msg += "TIMEZONE: " + calendar.getProperty("X-WR-TIMEZONE").getValue();

                    File newFile = new File(workDir + File.separator + "new_calendar.ics");
                    File oldFile = new File(this.getFilesDir() + File.separator + "calendar.ics");

                    if (oldFile.exists())
                        result = oldFile.delete();

                    if (newFile.exists() && result)
                        result = newFile.renameTo(oldFile);
                    else
                        Logger.warn("WatchfaceActivity checkICSFile error moving newFile: " + newFile.getAbsolutePath());

                    if (result) {
                        Prefs.putString(Constants.PREF_WATCHFACE_CALENDAR_ICS_URL, editText);
                        dialogBuilder.title(R.string.success)
                                .content(msg)
                                .show();
                        showFoundICSCalendarEvents(false, calendar);
                    } else {
                        dialogBuilder.title(R.string.error)
                                .content(R.string.activity_files_file_error)
                                .show();
                    }
                } else {
                    dialogBuilder.title(R.string.error)
                            .content(R.string.file_or_connection_error)
                            .show();
                }
            } catch (InterruptedException | ExecutionException | IOException | ParserException e) {
                Logger.error(e.getLocalizedMessage(), e);
            }
        } else {
            dialogBuilder.title(R.string.error)
                    .content(R.string.invalid_url)
                    .show();
        }
    }

}
