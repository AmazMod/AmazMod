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
import android.text.TextUtils;
import android.text.format.Time;

import androidx.annotation.NonNull;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.WatchfaceData;

import static java.lang.System.currentTimeMillis;

public class WatchfaceReceiver extends BroadcastReceiver {
    public final static int CALENDAR_DATA_INDEX_TITLE = 0;
    public final static int CALENDAR_DATA_INDEX_DESCRIPTION = 1;
    public final static int CALENDAR_DATA_INDEX_START = 2;
    public final static int CALENDAR_DATA_INDEX_END = 3;
    public final static int CALENDAR_DATA_INDEX_LOCATION = 4;
    public final static int CALENDAR_DATA_INDEX_ACCOUNT = 5;
    public final static int CALENDAR_EXTENDED_DATA_INDEX = 6;

    public final static String CALENDAR_DATA_PARAM_EXTENDED_DATA_ALL_DAY = "all_day";

    private static String default_calendar_days;
    private static boolean refresh;

    static AlarmManager alarmManager;
    static Intent alarmWatchfaceIntent;
    static PendingIntent pendingIntent;
    static WatchfaceReceiver mReceiver = new WatchfaceReceiver();

    /**
     * Info for single calendar entry in system. This entry can belong to any account - this
     * information is not stored here.
     */
    public static class CalendarInfo {
        public String name() {
            return mName;
        }

        public String id() {
            return mId;
        }

        public int color() {
            return mColor;
        }

        final String mName;
        final String mId;
        final int mColor;

        CalendarInfo(String mName, String mId, int mColor) {
            this.mName = mName;
            this.mId = mId;
            this.mColor = mColor;
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {

        refresh = intent.getBooleanExtra("refresh", false);

        if (!Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA)) {
            Logger.debug("WatchfaceDataReceiver send data is off");
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
                Logger.debug("WatchfaceDataReceiver sending data to phone");

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
                Logger.debug("WatchfaceDataReceiver sending data to phone (no new data)");
            }
            //Save update time in milliseconds
            Date date = new Date();
            Long milliseconds = date.getTime();
            Prefs.putLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, milliseconds);
        } else {
            // Other actions
            Logger.debug("WatchfaceDataReceiver onReceive :"+intent.getAction());
            // If battery/alarm was changed
            if( (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED) && Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE))
                    || (intent.getAction().equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) && Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE) )){
                // Get data
                int battery = getPhoneBattery(context);
                String alarm = getPhoneAlarm(context);

                if (Prefs.getInt(Constants.PREF_WATCHFACE_LAST_BATTERY, 0)!=battery || !Prefs.getString(Constants.PREF_WATCHFACE_LAST_ALARM, "").equals(alarm)) {
                    // New data = update
                    Logger.debug("WatchfaceDataReceiver sending data to phone (battery/alarm onchange)");

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
            Logger.debug("WatchfaceDataReceiver send data is off");
            return;
        }

        // update as interval
        //if (send_data) {
            int syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_BACKGROUND_SYNC_INTERVAL, context.getResources().getStringArray(R.array.pref_watchface_background_sync_interval_values)[Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX]));

            AmazModApplication.timeLastWatchfaceDataSend = Prefs.getLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, 0L);

            long delay = ((long) syncInterval * 60000L) - (currentTimeMillis() - AmazModApplication.timeLastWatchfaceDataSend);
            if (delay < 0) delay = 0;

            //Log.i(Constants.TAG, "WatchfaceDataReceiver times: " + SystemClock.elapsedRealtime() + " / " + AmazModApplication.timeLastWatchfaceDataSend + " = "+delay);

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
                Logger.error("WatchfaceDataReceiver setRepeating exception: " + e.toString());
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
        // Logger.debug(Constants.TAG, "WatchfaceDataReceiver next alarm: " + nextAlarm);

        return nextAlarm;
    }


    // CALENDAR Instances
    private static final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Events.ACCOUNT_NAME,
            CalendarContract.Instances.ALL_DAY
    };

    private static final String[] CALENDAR_PROJECTION = new String[] {
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars._ID};

    private static Cursor getCalendarEventsCursor(Context context) {
        // Get days to look for events
        int calendar_events_days = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_EVENTS_DAYS, context.getResources().getStringArray(R.array.pref_watchface_calendar_events_days_values)[Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX]));
        if (calendar_events_days == 0) {
            return null;
        }

        Set<String> calendars_ids_settings = Prefs.getStringSet(
                Constants.PREF_WATCHFACE_CALENDARS_IDS, null);

        String calendars_ids = calendars_ids_settings != null ?
                TextUtils.join(",", calendars_ids_settings) : null;

        // Run query
        Cursor cur;
        ContentResolver cr = context.getContentResolver();

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
        final String selection = TextUtils.isEmpty(calendars_ids) ? null :
                "(" + CalendarContract.ExtendedProperties.CALENDAR_ID + " IN (" + calendars_ids + "))";

        try {
            cur = cr.query(eventsUri, EVENT_PROJECTION, selection, null, CalendarContract.Instances.BEGIN + " ASC");
        } catch (SecurityException e) {
            //Getting data error
            Logger.debug("WatchfaceDataReceiver calendar events: get data error");
            return null;
        }
        return cur;
    }

    /**
     * Retrieves information about all calendars from all accounts, that exist on device.
     * @param context of process.
     * @return map of infos: key is the name of account and value is list of associated to this
     *                       accounts calendars.
     */
    @NonNull
    public static Map<String, List<CalendarInfo>> getCalendarsInfo(@NonNull final Context context) {
        Map<String, List<CalendarInfo>> info = new HashMap<>();

        if (Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            //Permissions error
            Logger.debug("WatchfaceDataReceiver calendar info: permissions error");
            return info;
        }

        final Cursor calendarCursor = context.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI, CALENDAR_PROJECTION, null, null, null);
        if (calendarCursor == null) {
            return info;
        }

        while (calendarCursor.moveToNext()) {
            final String accountName = calendarCursor.getString(0);
            final String calendarName = calendarCursor.getString(1);
            final int calendarColor = Integer.decode(calendarCursor.getString(2));
            final String calendarId = calendarCursor.getString(3);

            List<CalendarInfo> calendars = info.get(accountName);

            if (calendars == null) {
                calendars = new ArrayList<>();
                info.put(accountName, calendars);
            }
            calendars.add(new CalendarInfo(calendarName, calendarId, calendarColor));
        }
        calendarCursor.close();
        return info;
    }

    private String getCalendarEvents(Context context) {
        // Check if calendar read permission is granted
        if (Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            //Permissions error
            Logger.debug("WatchfaceDataReceiver calendar events: permissions error");
            return null;
        }

        Cursor cur = getCalendarEventsCursor(context);

        if (cur == null) {
            // Disabled
            Logger.debug("WatchfaceDataReceiver calendar events: disabled");
            Prefs.putString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, "");
            return "{\"events\":[]}";
        }

        // Start formulating JSON
        String jsonEvents;
        try {
            JSONObject root = new JSONObject();
            JSONArray events = new JSONArray();
            root.put("events", events);

            // Use the cursor to step through the returned records
            while (cur.moveToNext()) {
                JSONArray event = new JSONArray();

                // Get the field values
                String title = cur.getString(0);
                String description = cur.getString(1);
                long start = cur.getLong(2);
                long end = cur.getLong(3);
                String location = cur.getString(4);
                String account = cur.getString(5);
                String all_day = cur.getString(6);

                event.put(CALENDAR_DATA_INDEX_TITLE, title);
                event.put(CALENDAR_DATA_INDEX_DESCRIPTION, description);
                event.put(CALENDAR_DATA_INDEX_START, start);
                event.put(CALENDAR_DATA_INDEX_END, end);
                event.put(CALENDAR_DATA_INDEX_LOCATION, location);
                event.put(CALENDAR_DATA_INDEX_ACCOUNT, account);

                JSONObject ext_data = new JSONObject();
                ext_data.put(CALENDAR_DATA_PARAM_EXTENDED_DATA_ALL_DAY, all_day);
                event.put(CALENDAR_EXTENDED_DATA_INDEX, ext_data);

                Logger.debug("WatchfaceDataReceiver getCalendarEvents jsonEvents: " + event);

                events.put(event);
            }
            jsonEvents = root.toString();
        } catch (JSONException e) {
            Logger.debug("WatchfaceDataReceiver calendar events: failed to make JSON", e);
            return null;
        }

        // Check if there are new data
        if (Prefs.getString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, "").equals(jsonEvents)) {
            // No new data, no update
            Logger.debug("WatchfaceDataReceiver calendar events: no new data");
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

        // Run query
        Cursor cur = getCalendarEventsCursor(context);

        if (cur == null) {
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
        Logger.debug("WatchfaceDataReceiver getICSCalendarEvents calendar_events_days: " + calendar_events_days);
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
                            Logger.info("WatchfaceDataReceiver getICSCalendarEvents ICS file did not change, returning...");
                            return null;
                        } else {
                            result = oldFile.delete();
                            if (newFile.exists() && result)
                                result = newFile.renameTo(oldFile);
                        }
                    } else
                        result = newFile.renameTo(oldFile);

                    if (!result) {
                        Logger.warn("WatchfaceActivity checkICSFile error moving newFile: " + newFile.getAbsolutePath());
                        return null;
                    } else {
                        Logger.debug("WatchfaceDataReceiver getICSCalendarEvents getting ics data");
                        System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
                        FileInputStream in = new FileInputStream(context.getFilesDir() + File.separator + "calendar.ics");
                        CalendarBuilder builder = new CalendarBuilder();
                        calendar = builder.build(in);
                    }

                } else {
                    Logger.warn("WatchfaceDataReceiver getICSCalendarEvents error retrieving newFile");
                }
            } catch (InterruptedException | ExecutionException | IOException | ParserException e) {
                Logger.error(e.getLocalizedMessage(), e);
            }
        } else {
            Logger.warn("WatchfaceDataReceiver getICSCalendarEvents icsURL is empty");
            return null;
        }

        // Run query
        if (calendar != null) {
            Logger.debug("WatchfaceDataReceiver getICSCalendarEvents listing events");

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
                    Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event SU: " + event.getProperty("SUMMARY").getValue());
                if (event.getProperty("DESCRIPTION") != null)
                    Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event DC: " + event.getProperty("DESCRIPTION").getValue());
                if (event.getProperty("DTSTART") != null)
                    Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event DS: " + event.getProperty("DTSTART").getValue());
                Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event SD: " + event.getStartDate().getDate().getTime());
                if (event.getProperty("DTEND") != null)
                    Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event DE: " + event.getProperty("DTEND").getValue());
                if (event.getProperty("LOCATION") != null)
                    Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event LO: " + event.getProperty("LOCATION").getValue());
                //Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event: " + event.getProperty("URL").getValue());

                if (event.getProperty("RRULE") != null) {
                    PeriodList list = event.calculateRecurrenceSet(period);

                    for (Object po : list) {
                        try {
                            VEvent vEvent = (VEvent) event.copy();
                            Logger.debug("WatchfaceDataReceiver getICSCalendarEvents SU: " + vEvent.getSummary().getValue());
                            Logger.debug("WatchfaceDataReceiver getICSCalendarEvents RRULE: " + (Period) po
                                    + " \\ " + ((Period) po).getStart() + " \\ " + ((Period) po).getStart().getTime());
                            //Log.d(Constants.TAG, "WatchfaceDataReceiver getICSCalendarEvents RRULE: " + (Period) po + " \\ " + ((Period) po).getStart().toString() + " \\ " + ((Period) po).getStart().getTime() + " \\ " + ((Period) po).getEnd().getTime());
                            vEvent.getStartDate().setDate(new DateTime(((Period) po).getStart()));
                            vEvent.getEndDate().setDate(new DateTime(((Period) po).getEnd()));
                            eventList.add(vEvent);
                            Logger.debug("WatchfaceDataReceiver getICSCalendarEvents SD: " + vEvent.getStartDate().getDate());
                        } catch (Exception e) {
                            Logger.error(e.getLocalizedMessage(), e);
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
                    Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event SU: " + event.getSummary().getValue());
                if (event.getDescription() != null)
                    Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event DC: " + event.getDescription().getValue());
                if (event.getStartDate() != null) {
                    Logger.debug("WatchfaceDataReceiver getICSCalendarEvents event DS: " + event.getStartDate().getValue());
                    Logger.debug( "WatchfaceDataReceiver getICSCalendarEvents event SD: " + event.getStartDate().getDate().getTime());
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
                Logger.debug("WatchfaceDataReceiver calendar events: no new data");
                return null;
            }
            // Save new events as last send
            Prefs.putString(Constants.PREF_WATCHFACE_LAST_CALENDAR_EVENTS, jsonEvents);

            return jsonEvents;

        } else {
            Logger.warn("WatchfaceDataReceiver getICSCalendarEvents calendar is null");
            return null;
        }
    }

    public static int countICSEvents(Context context) {
        return countICSEvents(context, false, null);
    }

    public static int countICSEvents(Context context, boolean update, net.fortuna.ical4j.model.Calendar calendar) {
        int calendar_events_days = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_EVENTS_DAYS, default_calendar_days));
        String icsURL = Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_ICS_URL, "");

        if (calendar_events_days == 0 || icsURL.isEmpty()) return 0;

        if(calendar == null) {
            // Check for old file
            String workDir = context.getCacheDir().getAbsolutePath();
            File oldFile = new File(context.getFilesDir() + File.separator + "calendar.ics");
            if (!oldFile.exists()) update = true;

            // Check for file update
            if (update) {
                try {
                    boolean result = new FilesUtil.urlToFile().execute(icsURL, workDir, "new_calendar.ics").get();
                    if (result) {
                        File newFile = new File(workDir + File.separator + "new_calendar.ics");

                        if (oldFile.exists() && newFile.exists())
                            result = oldFile.delete();
                        if (newFile.exists() && result)
                            result = newFile.renameTo(oldFile);

                        if (result)
                            Logger.debug("WatchfaceDataReceiver countICSEvents ics file successfully updated");
                        else
                            Logger.debug("WatchfaceDataReceiver countICSEvents ics file was not updated");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Logger.error(e, e.getLocalizedMessage());
                }
            }

            // Get calendar events from file
            try {
                System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
                FileInputStream in = new FileInputStream(context.getFilesDir() + File.separator + "calendar.ics");
                CalendarBuilder builder = new CalendarBuilder();
                calendar = builder.build(in);
            } catch (IOException | ParserException e) {
                Logger.error(e.getLocalizedMessage(), e);
            }

            // Run query
            if (calendar == null)
                return 0;
        }
        Logger.debug("WatchfaceDataReceiver countICSEvents listing events");

        // create a period for the filter starting now with a duration of calendar_events_days + 1
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.clear(java.util.Calendar.MINUTE);
        today.clear(java.util.Calendar.SECOND);
        Period period = new Period(new DateTime(today.getTime()), new Dur(calendar_events_days + 1, 0, 0, 0));
        Filter filter = new Filter(new PeriodRule(period));

        ComponentList events = (ComponentList) filter.filter(calendar.getComponents(Component.VEVENT));

        int eventsCounter = 0;
        for (Object o : events) {
            VEvent event = (VEvent) o;

            if (event.getProperty("RRULE") != null) {
                PeriodList list = event.calculateRecurrenceSet(period);
                for (Object po : list)
                    eventsCounter++;
            } else
                eventsCounter++;
        }

        return eventsCounter;
    }

    private static boolean isEventAllDay(VEvent event) {
        return event.getStartDate().toString().contains("VALUE=DATE");
    }
}
