package com.edotassi.amazmod.receiver;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.util.FilesUtil;
import com.edotassi.amazmod.watch.Watch;
import com.pixplicity.easyprefs.library.Prefs;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.filter.PeriodRule;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.util.MapTimeZoneCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.WatchfaceData;

import static java.lang.System.currentTimeMillis;

public class WatchfaceReceiver extends BroadcastReceiver {

    private Logger log = Logger.get(WatchfaceReceiver.class);

    private static String default_calendar_days;
    private static boolean refresh;

    static AlarmManager alarmManager;
    static Intent alarmWatchfaceIntent;
    static PendingIntent pendingIntent;
    static WatchfaceReceiver mReceiver = new WatchfaceReceiver();


    @Override
    public void onReceive(final Context context, Intent intent) {

        refresh = intent.getBooleanExtra("refresh", false);

        if (!Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA)) {
            Log.d(Constants.TAG, "WatchfaceDataReceiver onReceive feature is off");
            return;
        }

        if (intent.getAction() == null) {
            if (!Watch.isInitialized()) {
                Watch.init(context);
            }

            // Get data
            int battery = getPhoneBattery(context);
            String alarm = getPhoneAlarm(context);
            String calendar_events = null;

            // Get calendar source data from preferences then the events
            default_calendar_days = context.getResources().getStringArray(R.array.pref_watchface_background_sync_interval_values)[Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX];
            String calendar_source = Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_SOURCE, Constants.PREF_CALENDAR_SOURCE_LOCAL);
            if (Constants.PREF_CALENDAR_SOURCE_LOCAL.equals(calendar_source)) {
                calendar_events = getCalendarEvents(context);
            } else {
                calendar_events = getICSCalendarEvents(context);
            }

            // Check if new data
            if (Prefs.getInt(Constants.PREF_WATCHFACE_LAST_BATTERY, 0)!=battery || !Prefs.getString(Constants.PREF_WATCHFACE_LAST_ALARM, "").equals(alarm) || calendar_events!=null) {
                Log.d(Constants.TAG, "WatchfaceDataReceiver sending data to phone");

                // Save last send values
                Prefs.putInt(Constants.PREF_WATCHFACE_LAST_BATTERY, battery);
                Prefs.putString(Constants.PREF_WATCHFACE_LAST_ALARM, alarm);

                // Put data to bundle
                WatchfaceData watchfaceData = new WatchfaceData();
                watchfaceData.setBattery(battery);
                watchfaceData.setAlarm(alarm);
                watchfaceData.setCalendarEvents(calendar_events);

                Watch.get().sendWatchfaceData(watchfaceData);/*.continueWith(new Continuation<Watchface, Object>() {
                    @Override
                    public Object then(@NonNull Task<Watchface> task) throws Exception {
                        if (task.isSuccessful()) {
                            // Returned data
                            Log.d(Constants.TAG, "WatchfaceDataReceiver data were sent to phone");//Never returns :P
                        } else {
                            WatchfaceReceiver.this.log.e(task.getException(), "failed sending watchface data");
                        }
                        return null;
                    }
                });*/
            }else{
                Log.d(Constants.TAG, "WatchfaceDataReceiver sending data to phone (no new data)");
            }
            //Save update time in milliseconds
            Date date = new Date();
            Long milliseconds = date.getTime();
            Prefs.putLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, milliseconds);
        } else {
            // Other actions
            Log.d(Constants.TAG, "WatchfaceDataReceiver onReceive :"+intent.getAction());
            // If battery/alarm was changed
            if( (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED) && Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE))
                    || (intent.getAction().equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) && Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE) )){
                // Get data
                int battery = getPhoneBattery(context);
                String alarm = getPhoneAlarm(context);

                if (Prefs.getInt(Constants.PREF_WATCHFACE_LAST_BATTERY, 0)!=battery || !Prefs.getString(Constants.PREF_WATCHFACE_LAST_ALARM, "").equals(alarm)) {
                    // New data = update
                    Log.d(Constants.TAG, "WatchfaceDataReceiver sending data to phone (battery/alarm onchange)");

                    // Save last send values
                    Prefs.putInt(Constants.PREF_WATCHFACE_LAST_BATTERY, battery);
                    Prefs.putString(Constants.PREF_WATCHFACE_LAST_ALARM, alarm);

                    // Put data to bundle
                    WatchfaceData watchfaceData = new WatchfaceData();
                    watchfaceData.setBattery(battery);
                    watchfaceData.setAlarm(alarm);
                    watchfaceData.setCalendarEvents(null);

                    Watch.get().sendWatchfaceData(watchfaceData);
                }
                //startWatchfaceReceiver(context);
            }else{
                // Unregister if any receiver
                try {
                    context.unregisterReceiver(WatchfaceReceiver.mReceiver);
                } catch (IllegalArgumentException e) {
                    //e.printStackTrace();
                }
            }
        }

        //Log.d(Constants.TAG, "WatchfaceDataReceiver onReceive");
    }

    public static void startWatchfaceReceiver(Context context) {
        boolean send_data = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA);
        boolean send_on_battery_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE);
        boolean send_on_alarm_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE);

        // Stop if data send is off
        if (!send_data) {
            Log.d(Constants.TAG, "WatchfaceDataReceiver onReceive feature is off");
            return;
        }

        // update as interval
        //if (send_data) {
            int syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_BACKGROUND_SYNC_INTERVAL, context.getResources().getStringArray(R.array.pref_watchface_background_sync_interval_values)[Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX]));

            AmazModApplication.timeLastWatchfaceDataSend = Prefs.getLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, 0L);

            long delay = ((long) syncInterval * 60000L) - (currentTimeMillis() - AmazModApplication.timeLastWatchfaceDataSend);
            if (delay < 0) delay = 0;

            Log.i(Constants.TAG, "WatchfaceDataReceiver times: " + SystemClock.elapsedRealtime() + " / " + AmazModApplication.timeLastWatchfaceDataSend + " = "+delay);

            // Cancel any other intent
            if (alarmManager != null && pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }else {
                alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmWatchfaceIntent = new Intent(context, WatchfaceReceiver.class);
                pendingIntent = PendingIntent.getBroadcast(context, 0, alarmWatchfaceIntent, 0);
            }

            try {
                if (alarmManager != null)
                    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                            (long) syncInterval * 60000L, pendingIntent);
            } catch (NullPointerException e) {
                Log.e(Constants.TAG, "WatchfaceDataReceiver setRepeating exception: " + e.toString());
            }

        /*
        } else {
            try {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent alarmWatchfaceIntent = new Intent(context, WatchfaceReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmWatchfaceIntent, 0);
                if (alarmManager != null)
                    alarmManager.cancel(pendingIntent);
            } catch (NoSuchMethodError e) {
                e.printStackTrace();
            }
        }*/


        // Unregister if any receiver
        try {
            context.unregisterReceiver(WatchfaceReceiver.mReceiver);
        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
        }

        // on battery change
        if (send_on_battery_change) {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            context.registerReceiver(WatchfaceReceiver.mReceiver, ifilter);
        }

        // on alarm change
        if (send_on_alarm_change) {
            IntentFilter ifilter = new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
            context.registerReceiver(WatchfaceReceiver.mReceiver, ifilter);
        }
    }

    public void onDestroy(Context context) {
        // Unregister if any receiver
        try {
            context.unregisterReceiver(WatchfaceReceiver.mReceiver);
        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
        }
    }

    public int getPhoneBattery(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        float batteryPct;
        if (batteryStatus != null) {
            //int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            //boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            //int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            //boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            //boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPct = level / (float) scale;
        } else {
            batteryPct = 0;
        }
        return (int) (batteryPct * 100);
    }

    public String getPhoneAlarm(Context context) {
        String nextAlarm = "--";

        // Proper way to do it (Lollipop +)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                // First check for regular alarm
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    AlarmManager.AlarmClockInfo clockInfo = alarmManager.getNextAlarmClock();
                    if (clockInfo != null) {
                        long nextAlarmTime = clockInfo.getTriggerTime();
                        //Log.d(Constants.TAG, "Next alarm time: " + nextAlarmTime);
                        Date nextAlarmDate = new Date(nextAlarmTime);
                        android.text.format.DateFormat df = new android.text.format.DateFormat();
                        // Format alarm time as e.g. "Fri 06:30"
                        nextAlarm = df.format("EEE HH:mm", nextAlarmDate).toString();
                    }
                }
                // Just to be sure
                if (nextAlarm.isEmpty()) {
                    nextAlarm = "--";
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Alarm already set to --
            }
        } else {
            // Legacy way
            try {
                nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);

                if (nextAlarm.equals("")) {
                    nextAlarm = "--";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Log next alarm
        // Log.d(Constants.TAG, "WatchfaceDataReceiver next alarm: " + nextAlarm);

        return nextAlarm;
    }


    // CALENDAR Instances
    public static final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Events.ACCOUNT_NAME,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_TIMEZONE,
            CalendarContract.Instances.CALENDAR_TIME_ZONE

    };

    private String getCalendarEvents(Context context) {
        // Check if calendar read permission is granted
        if (Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            //Permissions error
            Log.d(Constants.TAG, "WatchfaceDataReceiver calendar events: permissions error");
            return null;
        }

        // Get days to look for events
        int calendar_events_days = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_EVENTS_DAYS, context.getResources().getStringArray(R.array.pref_watchface_calendar_events_days_values)[Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX]));
        if (calendar_events_days == 0) {
            // Disabled
            Log.d(Constants.TAG, "WatchfaceDataReceiver calendar events: disabled");
            Prefs.putString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, "");
            return "{\"events\":[]}";
        }

        // Run query
        Cursor cur = null;
        ContentResolver cr = context.getContentResolver();

        /* Events table does not show all events
        Uri uri = CalendarContract.Events.CONTENT_URI;

        // Date range to pick events
        // Pick timestamp from this day's start (so data are not different each time they are pulled because of expired events)
        Calendar c_start= Calendar.getInstance();
        int year = c_start.get(Calendar.YEAR);
        int month = c_start.get(Calendar.MONTH);
        int day = c_start.get(Calendar.DATE);
        c_start.set(year, month, day, 0, 0, 0);

        Calendar c_end= Calendar.getInstance(); // no it's not redundant
        c_end.set(year, month, day, 0, 0, 0);
        c_end.add(Calendar.DATE, (calendar_events_days+1));

        String selection = "(( " + CalendarContract.Events.DTSTART + " >= " + c_start.getTimeInMillis() + " ) AND ( " + CalendarContract.Events.DTSTART + " <= " + c_end.getTimeInMillis() + " ))";

        // Submit the query and get a Cursor object back.
        try {
            cur = cr.query(uri, EVENT_PROJECTION, selection, null, CalendarContract.Events.DTSTART+" ASC");
        } catch (SecurityException e) {
            //Getting data error
            Log.d(Constants.TAG, "WatchfaceDataReceiver calendar events: get data error");
            return null;
        }
        */

        Calendar c_start = Calendar.getInstance();
        int year = c_start.get(Calendar.YEAR);
        int month = c_start.get(Calendar.MONTH);
        int day = c_start.get(Calendar.DATE);
        c_start.set(year, month, day, 0, 0, 0);

        Calendar c_end = Calendar.getInstance(); // no it's not redundant
        c_end.set(year, month, day, 0, 0, 0);
        c_end.add(Calendar.DATE, (calendar_events_days + 1));

        Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(eventsUriBuilder, c_start.getTimeInMillis());
        ContentUris.appendId(eventsUriBuilder, c_end.getTimeInMillis());
        Uri eventsUri = eventsUriBuilder.build();

        try {
            cur = cr.query(eventsUri, EVENT_PROJECTION, null, null, CalendarContract.Instances.BEGIN + " ASC");
        } catch (SecurityException e) {
            //Getting data error
            Log.d(Constants.TAG, "WatchfaceDataReceiver calendar events: get data error");
            return null;
        }

        // Start formulating JSON
        String jsonEvents = "{\"events\":[";

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            // Get the field values
            String title = cur.getString(0);
            String description = cur.getString(1);
            long start = cur.getLong(2);
            long end = cur.getLong(3);
            String location = cur.getString(4);
            String account = cur.getString(5);
            String all_day = cur.getString(6);
            String tz = cur.getString(7);
            String cal_tz = cur.getString(8);

            String event = "[ \"" + title + "\", \"" + description + "\", \"" + start + "\", \"" + end + "\", \"" + location + "\", \"" + account + "\", \"" + all_day + "\", \"" + tz + "\"] " + cal_tz;
            Log.d(Constants.TAG, "WatchfaceDataReceiver getCalendarEvents jsonEvents: " + event);

            boolean hasOffset;
            try {
                hasOffset = all_day.equals("1") && !cal_tz.equals(tz);
            } catch (NullPointerException ex) {
                hasOffset = false;
            }

            if (hasOffset) {
                Time timeFormat = new Time();
                long offset = TimeZone.getDefault().getOffset(start);
                if (offset < 0)
                    timeFormat.set(start - offset);
                else
                    timeFormat.set(start + offset);
                start = timeFormat.toMillis(true);
                Log.d(Constants.TAG, "WatchfaceDataReceiver getCalendarEvents new start: " + start + " \\ offset: " + offset);
                jsonEvents += "[ \"" + title + "\", \"" + description + "\", \"" + start + "\", \"" + null + "\", \"" + location + "\", \"" + account + "\"],";

            } else
                jsonEvents += "[ \"" + title + "\", \"" + description + "\", \"" + start + "\", \"" + end + "\", \"" + location + "\", \"" + account + "\"],";
        }

        // Remove last "," from JSON
        if (jsonEvents.substring(jsonEvents.length() - 1).equals(",")) {
            jsonEvents = jsonEvents.substring(0, jsonEvents.length() - 1);
        }
        jsonEvents += "]}";

        // Check if there are new data
        if (Prefs.getString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, "").equals(jsonEvents)) {
            // No new data, no update
            Log.d(Constants.TAG, "WatchfaceDataReceiver calendar events: no new data");
            return null;
        }
        // Save new events as last send
        Prefs.putString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, jsonEvents);

        // Count events
        /*
        int events = cur.getCount();
        jsonEvents += "\n\n Counted: "+events;
        */
        cur.close();
        return jsonEvents;
    }

    // Build-in Calendar even counter
    public static int countBuildinCalendarEvents(Context context) {
        // Check if calendar read permission is granted
        if (Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return 0;
        }

        // Get days to look for events
        int calendar_events_days = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_EVENTS_DAYS, context.getResources().getStringArray(R.array.pref_watchface_calendar_events_days_values)[Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX]));
        if (calendar_events_days == 0) {
            return 0;
        }

        // Run query
        Cursor cur = null;
        ContentResolver cr = context.getContentResolver();

        // Start date
        Calendar c_start = Calendar.getInstance();
        int year = c_start.get(Calendar.YEAR);
        int month = c_start.get(Calendar.MONTH);
        int day = c_start.get(Calendar.DATE);
        c_start.set(year, month, day, 0, 0, 0);

        // End date
        Calendar c_end = Calendar.getInstance(); // no it's not redundant
        c_end.set(year, month, day, 0, 0, 0);
        c_end.add(Calendar.DATE, (calendar_events_days + 1));

        Uri.Builder eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(eventsUriBuilder, c_start.getTimeInMillis());
        ContentUris.appendId(eventsUriBuilder, c_end.getTimeInMillis());
        Uri eventsUri = eventsUriBuilder.build();

        try {
            cur = cr.query(eventsUri, EVENT_PROJECTION, null, null, CalendarContract.Instances.BEGIN + " ASC");
        } catch (SecurityException e) {
            return 0;
        }

        // Count events
        int events;
        try{
            events = cur.getCount();
        }catch(NullPointerException e){
            return 0;
        }

        // Close cursor
        cur.close();

        return events;
    }

    private String getICSCalendarEvents(Context context) {

        int calendar_events_days = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_EVENTS_DAYS, default_calendar_days));
        Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents calendar_events_days: " + calendar_events_days);
        String jsonEvents = "{\"events\":[]}";
        String lastEvents = Prefs.getString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, "");
        boolean isEmptyEvents = false;

        if (calendar_events_days == 0) {
            Prefs.putString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, "");
            return jsonEvents;
        }

        if (lastEvents.equals(jsonEvents) || lastEvents.isEmpty())
            isEmptyEvents = true;

        //Check for file update
        String icsURL = Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_ICS_URL, "");
        String workDir = context.getCacheDir().getAbsolutePath();
        net.fortuna.ical4j.model.Calendar calendar = null;
        if (!icsURL.isEmpty()) {
            try {
                boolean result = new FilesUtil.urlToFile().execute(icsURL, workDir, "new_calendar.ics").get();
                if (result) {

                    File newFile = new File(workDir + File.separator + "new_calendar.ics");
                    File oldFile = new File(context.getFilesDir() + File.separator + "calendar.ics");
                    result = true;
                    if (oldFile.exists() && !refresh) {
                        if (!isEmptyEvents && (oldFile.length() == newFile.length())) {
                            Log.i(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents ICS file did not change, returning...");
                            return null;
                        } else {
                            result = oldFile.delete();
                            if (newFile.exists() && result)
                                result = newFile.renameTo(oldFile);
                        }
                    } else
                        result = newFile.renameTo(oldFile);

                    if (!result) {
                        Log.w(Constants.TAG, "WatchfaceActivity checkICSFile error moving newFile: " + newFile.getAbsolutePath());
                        return null;
                    } else {
                        Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents getting ics data");
                        System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
                        FileInputStream in = new FileInputStream(context.getFilesDir() + File.separator + "calendar.ics");
                        CalendarBuilder builder = new CalendarBuilder();
                        calendar = builder.build(in);
                    }

                } else {
                    Log.w(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents error retrieving newFile");
                }
            } catch (InterruptedException | ExecutionException | IOException | ParserException e) {
                Log.e(Constants.TAG, e.getLocalizedMessage(), e);
            }
        } else {
            Log.w(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents icsURL is empty");
            return null;
        }

        // Run query
        if (calendar != null) {
            Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents listing events");

            // create a period for the filter starting now with a duration of calendar_events_days + 1
            java.util.Calendar today = java.util.Calendar.getInstance();
            today.set(java.util.Calendar.HOUR_OF_DAY, 0);
            today.clear(java.util.Calendar.MINUTE);
            today.clear(java.util.Calendar.SECOND);
            Period period = new Period(new DateTime(today.getTime()), new Dur(calendar_events_days + 1, 0, 0, 0));
            Filter filter = new Filter(new PeriodRule(period));

            ComponentList events = (ComponentList) filter.filter(calendar.getComponents(Component.VEVENT));

            ComponentList eventList = new ComponentList();

            for (Object o : events) {
                VEvent event = (VEvent) o;

                if (event.getProperty("SUMMARY") != null)
                    Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event SU: " + event.getProperty("SUMMARY").getValue());
                if (event.getProperty("DESCRIPTION") != null)
                    Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event DC: " + event.getProperty("DESCRIPTION").getValue());
                if (event.getProperty("DTSTART") != null)
                    Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event DS: " + event.getProperty("DTSTART").getValue());
                Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event SD: " + event.getStartDate().getDate().getTime());
                if (event.getProperty("DTEND") != null)
                    Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event DE: " + event.getProperty("DTEND").getValue());
                if (event.getProperty("LOCATION") != null)
                    Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event LO: " + event.getProperty("LOCATION").getValue());
                //Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event: " + event.getProperty("URL").getValue());

                if (event.getProperty("RRULE") != null) {
                    PeriodList list = event.calculateRecurrenceSet(period);

                    for (Object po : list) {
                        try {
                            VEvent vEvent = (VEvent) event.copy();
                            Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents SU: " + vEvent.getSummary().getValue());
                            Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents RRULE: " + (Period) po
                                    + " \\ " + ((Period) po).getStart() + " \\ " + ((Period) po).getStart().getTime());
                            //Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents RRULE: " + (Period) po + " \\ " + ((Period) po).getStart().toString() + " \\ " + ((Period) po).getStart().getTime() + " \\ " + ((Period) po).getEnd().getTime());
                            vEvent.getStartDate().setDate(new DateTime(((Period) po).getStart()));
                            vEvent.getEndDate().setDate(new DateTime(((Period) po).getEnd()));
                            eventList.add(vEvent);
                            Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents SD: " + vEvent.getStartDate().getDate());
                        } catch (Exception e) {
                            Log.e(Constants.TAG, e.getLocalizedMessage(), e);
                        }
                    }

                } else
                    eventList.add(event);
            }

            Collections.sort(eventList, new Comparator<VEvent>() {
                public int compare(VEvent o1, VEvent o2) {
                    if (o1.getStartDate().getDate() == null || o2.getStartDate().getDate() == null)
                        return 0;
                    return o1.getStartDate().getDate().compareTo(o2.getStartDate().getDate());
                }
            });

            // Start formulating JSON
            jsonEvents = "{\"events\":[";

            // Use the cursor to step through the returned records
            for (Object o : eventList) {
                // Get the field values
                VEvent event = (VEvent) o;

                if (event.getSummary() != null)
                    Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event SU: " + event.getSummary().getValue());
                if (event.getDescription() != null)
                    Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event DC: " + event.getDescription().getValue());
                if (event.getStartDate() != null) {
                    Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event DS: " + event.getStartDate().getValue());
                    Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents event SD: " + event.getStartDate().getDate().getTime());
                }

                String title = event.getSummary().getValue();
                String description = event.getDescription().getValue();
                long start = event.getStartDate().getDate().getTime();
                long end = event.getEndDate().getDate().getTime();
                String location = event.getLocation().getValue();
                String account = "ical4j";

                if (isEventAllDay(event)) {
                    Time timeFormat = new Time();
                    long offset = TimeZone.getDefault().getOffset(start);
                    if (offset < 0)
                        timeFormat.set(start - offset);
                    else
                        timeFormat.set(start + offset);
                    start = timeFormat.toMillis(true);
                    jsonEvents += "[ \"" + title + "\", \"" + description + "\", \"" + start + "\", \"" + null + "\", \"" + location + "\", \"" + account + "\"],";
                } else
                    jsonEvents += "[ \"" + title + "\", \"" + description + "\", \"" + start + "\", \"" + end + "\", \"" + location + "\", \"" + account + "\"],";
            }

            // Remove last "," from JSON
            if (jsonEvents.substring(jsonEvents.length() - 1).equals(",")) {
                jsonEvents = jsonEvents.substring(0, jsonEvents.length() - 1);
            }

            jsonEvents += "]}";


            // Check if there are new data
            if (Prefs.getString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, "").equals(jsonEvents)) {
                // No new data, no update
                Log.d(Constants.TAG, "WatchfaceDataReceiver calendar events: no new data");
                return null;
            }
            // Save new events as last send
            Prefs.putString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, jsonEvents);

            return jsonEvents;

        } else {
            Log.w(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents calendar is null");
            return null;
        }
    }

    private static boolean isEventAllDay(VEvent event) {
        return event.getStartDate().toString().contains("VALUE=DATE");
    }
}
