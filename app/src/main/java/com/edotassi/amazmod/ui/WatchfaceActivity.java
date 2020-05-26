package com.edotassi.amazmod.ui;

import android.Manifest;
import android.app.AlertDialog;
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
import com.edotassi.amazmod.adapters.CheckableAdapter;
import com.edotassi.amazmod.databinding.ActivityWatchfaceBinding;
import com.edotassi.amazmod.receiver.WatchfaceReceiver;
import com.edotassi.amazmod.receiver.WatchfaceReceiver.CalendarInfo;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import amazmod.com.transport.Constants;
import de.mateware.snacky.Snacky;

public class WatchfaceActivity extends BaseAppCompatActivity {

    private ActivityWatchfaceBinding binding;

    boolean send_data;
    int send_data_interval_index;
    int send_data_calendar_events_days_index;
    int send_data_interval;
    int watchface_weather_units_index;

    boolean send_on_battery_change;
    boolean send_on_alarm_change;
    boolean send_weather_data;
    boolean weather_real_feel;
    int watchface_weather_location_radio_index;

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
        binding = ActivityWatchfaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.watchface);
        }

        mContext = this;

        send_data = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA);
        send_data_interval_index = Prefs.getInt(Constants.PREF_WATCHFACE_SEND_DATA_INTERVAL_INDEX, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX);
        initialInterval = send_data_interval_index;
        send_on_battery_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE);
        send_on_alarm_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE);
        send_data_calendar_events_days_index = Prefs.getInt(Constants.PREF_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX);
        send_weather_data = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA);
        weather_real_feel = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_REAL_FEEL, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA_REAL_FEEL);
        watchface_weather_location_radio_index = Prefs.getInt(Constants.PREF_WATCHFACE_WEATHER_DATA_LOCATION_RADIO, Constants.PREF_DEFAULT_WATCHFACE_WEATHER_DATA_LOCATION_RADIO);

        // Send data on/off
        binding.sendDataSwitch.setChecked(send_data);
        binding.sendDataSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //WatchfaceActivity.this.send_data = isChecked;
            Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_DATA, isChecked);
            binding.sendWatchfaceDataInterval.setEnabled(isChecked);
            binding.sendWatchfaceDataCalendarEventsDays.setEnabled(isChecked);
            binding.sendOnBatteryChangeSwitch.setEnabled(isChecked);
            binding.sendOnAlarmChangeSwitch.setEnabled(isChecked);
            binding.sendWeatherDataSwitch.setEnabled(isChecked);
            binding.watchfaceSyncNowButton.setEnabled(isChecked);
        });

        // Data inteval option
        binding.sendWatchfaceDataInterval.setSelection(send_data_interval_index);
        binding.sendWatchfaceDataInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        binding.sendWatchfaceDataInterval.setEnabled(send_data);

        // Calendar options
        binding.sendWatchfaceDataCalendarEventsDays.setSelection(send_data_calendar_events_days_index);
        binding.sendWatchfaceDataCalendarEventsDays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        binding.sendWatchfaceDataCalendarEventsDays.setEnabled(send_data);

        // Hide if not a developer
        if( !Prefs.getBoolean(Constants.PREF_ENABLE_DEVELOPER_MODE, false) ){
            binding.sendOnBatteryChangeSwitch.setVisibility(View.GONE);
            Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, false);
            send_on_battery_change = false;
        }
        //send_on_alarm_change_switch.setVisibility(View.GONE);

        // battery on change
        binding.sendOnBatteryChangeSwitch.setChecked(send_on_battery_change);
        binding.sendOnBatteryChangeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, isChecked);

            }
        });
        binding.sendOnBatteryChangeSwitch.setEnabled(send_data);

        // alarm on change
        binding.sendOnAlarmChangeSwitch.setChecked(send_on_alarm_change);
        binding.sendOnAlarmChangeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, isChecked);

            }
        });
        binding.sendOnAlarmChangeSwitch.setEnabled(send_data);

        // weather data
        binding.sendWeatherDataSwitch.setChecked(send_weather_data);
        binding.sendWeatherDataSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA, isChecked);
            }
        });
        binding.sendWeatherDataSwitch.setEnabled(send_data);

        // weather API
        binding.watchfaceWeatherApiInput.setText(Prefs.getString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_API, ""));
        binding.watchfaceWeatherApiInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                Prefs.putString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_API, binding.watchfaceWeatherApiInput.getText().toString());
            }
        });
        // city,country
        binding.watchfaceWeatherCityInput.setText(Prefs.getString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_CITY, ""));
        binding.watchfaceWeatherCityInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Prefs.putString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_CITY, binding.watchfaceWeatherCityInput.getText().toString());
                }
            }
        });
        watchface_weather_units_index = Prefs.getInt(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_UNITS_INDEX, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA_UNITS_INDEX);
        binding.watchfaceWeatherUnits.setSelection(watchface_weather_units_index);
        binding.watchfaceWeatherUnits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Prefs.putInt(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_UNITS_INDEX, pos);

                // Show found local events
                showFoundBuildInCalendarEvents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // Auto-generated method stub
            }
        });
        // real feel
        binding.weatherRealFeelSwitch.setChecked(weather_real_feel);
        binding.weatherRealFeelSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Prefs.putBoolean(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_REAL_FEEL, isChecked);
            }
        });

        // Last time read
        binding.watchfaceLastSync.setText(lastTimeRead());

        // Sync now button
        final Intent alarmWatchfaceIntent = new Intent(getApplicationContext(), WatchfaceReceiver.class);
        binding.watchfaceSyncNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save new events as last send
                Prefs.putString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, "");
                Prefs.putString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_API, binding.watchfaceWeatherApiInput.getText().toString());
                Prefs.putString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_CITY, binding.watchfaceWeatherCityInput.getText().toString());
                alarmWatchfaceIntent.putExtra("refresh", true);
                sendBroadcast(alarmWatchfaceIntent);

                Snacky.builder()
                        .setActivity(WatchfaceActivity.this)
                        .setText(R.string.activity_watchface_data_send)
                        .setDuration(Snacky.LENGTH_SHORT)
                        .build().show();

                binding.watchfaceLastSync.setText(lastTimeRead());

            }
        });
        binding.watchfaceSyncNowButton.setEnabled(send_data);

        //Restore calendar source data from preferences
        String calendar_source = Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_SOURCE, Constants.PREF_CALENDAR_SOURCE_LOCAL);
        final String url = Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_ICS_URL, "");
        if (!url.isEmpty())
            binding.watchfaceIcsUrlEdittext.setText(url);
        if (Constants.PREF_CALENDAR_SOURCE_LOCAL.equals(calendar_source)) {
            binding.watchfaceSourceLocalRadiobutton.setChecked(true);
            changeWidgetsStatus(false);
        } else {
            binding.watchfaceIcsCalendarRadiobutton.setChecked(true);
            changeWidgetsStatus(true);
        }
        // Calendar Source selection
        binding.watchfaceCalendarRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //System.out.println(Constants.TAG + "WatchfaceActivity onCheckedChanged: " + checkedId);
                if (checkedId == binding.watchfaceSourceLocalRadiobutton.getId()) {
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

        // Weather location selection
        if ( watchface_weather_location_radio_index == 0 )
            binding.watchfaceWeatherLocationGpsRadiobutton.setChecked(true);
        else
            binding.watchfaceWeatherLocationManualRadiobutton.setChecked(true);
        binding.watchfaceWeatherLocationRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == binding.watchfaceWeatherLocationGpsRadiobutton.getId())
                    Prefs.putInt(Constants.PREF_WATCHFACE_WEATHER_DATA_LOCATION_RADIO, 0);
                else
                    Prefs.putInt(Constants.PREF_WATCHFACE_WEATHER_DATA_LOCATION_RADIO, 1);
            }
        });

        // Test calendar ICS file
        binding.watchfaceTestIcsButton.setOnClickListener(v -> checkICSFile());

        binding.watchfaceSourceLocalChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseCalendars();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        checkCalendarPermission();
        checkLocationPermission();
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
            binding.watchfacePermissionStatus.setText(getResources().getString(R.string.enabled).toUpperCase());
            binding.watchfacePermissionStatus.setTextColor(getResources().getColor(R.color.colorCharging, getTheme()));

            binding.watchfaceSourceLocalChooseButton.setEnabled(binding.watchfaceSourceLocalRadiobutton.isEnabled());
        } else {
            binding.watchfacePermissionStatus.setText(getResources().getString(R.string.disabled).toUpperCase());
            binding.watchfacePermissionStatus.setTextColor(getResources().getColor(R.color.colorAccent, getTheme()));
            binding.watchfacePermissionStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
            binding.watchfaceSourceLocalChooseButton.setEnabled(false);
        }
    }


    private void checkLocationPermission() {
        if (Permissions.hasPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION)){
            binding.watchfaceGpsPermissionStatus.setText(getResources().getString(R.string.enabled).toUpperCase());
            binding.watchfaceGpsPermissionStatus.setTextColor(getResources().getColor(R.color.colorCharging, getTheme()));
        } else {
            binding.watchfaceGpsPermissionStatus.setText(getResources().getString(R.string.disabled).toUpperCase());
            binding.watchfaceGpsPermissionStatus.setTextColor(getResources().getColor(R.color.colorAccent, getTheme()));
            binding.watchfaceGpsPermissionStatus.setOnClickListener(new View.OnClickListener() {
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
        binding.watchfaceSourceLocalRadiobutton.setText( buildInCalendarWithEvents );
    }

    private void showFoundICSCalendarEvents() {
        showFoundICSCalendarEvents(false, null);
    }

    private void showFoundICSCalendarEvents(boolean update, net.fortuna.ical4j.model.Calendar calendar) {
        int events = WatchfaceReceiver.countICSEvents(getApplicationContext(), update, calendar);
        String icsCalendarWithEvents = getResources().getText(R.string.watchface_remote_ics_file) + " ("+ getResources().getString(R.string.watchface_events_found,Integer.toString(events)) +")";
        binding.watchfaceIcsCalendarRadiobutton.setText( icsCalendarWithEvents );
    }

    private void changeWidgetsStatus(boolean state){
        binding.watchfaceTestIcsButton.setEnabled(state);
        binding.watchfaceIcsUrlEdittext.setEnabled(state);
        binding.watchfaceSourceLocalChooseButton.setEnabled(!state && Permissions.hasPermission(
                getApplicationContext(), Manifest.permission.READ_CALENDAR));
    }

    private void checkICSFile() {
        String editText = binding.watchfaceIcsUrlEdittext.getText().toString();
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

    private void chooseCalendars() {
        Map<String, List<CalendarInfo>> calendarsInfo =
                WatchfaceReceiver.getCalendarsInfo(this);

        List<CheckableAdapter.Item> items = new ArrayList<>();
        final Set<String> selectedCalendarIds = Prefs.getStringSet(
                Constants.PREF_WATCHFACE_CALENDARS_IDS, new HashSet<>());

        // If there was no settings stored yet, select all calendars.
        boolean selectAll = selectedCalendarIds.isEmpty();

        // Transform calendar info to item list for list adapter and initialize listeners.
        for (Map.Entry<String, List<CalendarInfo>> entry : calendarsInfo.entrySet()) {
            items.add(new CheckableAdapter.Item(entry.getKey(),
                    getResources().getColor(R.color.calendar_chooser_account, getTheme())));
            for (final CalendarInfo info : entry.getValue()) {
                if (selectAll) {
                    selectedCalendarIds.add(info.id());
                }

                items.add(new CheckableAdapter.CheckableItem(info.name(), info.color()) {

                    @Override
                    public void setChecked(boolean checked) {
                        if (checked) {
                            selectedCalendarIds.add(info.id());
                        } else {
                            selectedCalendarIds.remove(info.id());
                        }
                    }

                    @Override
                    public boolean isChecked() {
                        return selectedCalendarIds.contains(info.id());
                    }
                });
            }
        }


        new AlertDialog.Builder(this)
                .setAdapter(new CheckableAdapter(items), null)
                .setOnDismissListener(dialog -> {
                        Prefs.putStringSet(Constants.PREF_WATCHFACE_CALENDARS_IDS,
                                selectedCalendarIds);
                        showFoundBuildInCalendarEvents();})
                .create().show();
    }

}
