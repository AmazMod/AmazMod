package com.edotassi.amazmod.receiver;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
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
import android.support.annotation.NonNull;
import android.util.Log;

import com.edotassi.amazmod.AmazModApplication;
import amazmod.com.transport.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.Watchface;
import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.Calendar;
import java.util.Date;

import amazmod.com.transport.data.WatchfaceData;

public class WatchfaceReceiver extends BroadcastReceiver {

    private Logger log = Logger.get(WatchfaceReceiver.class);

    @Override
    public void onReceive(final Context context, Intent intent) {
        if(!Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA)){
            Log.d(Constants.TAG, "WatchfaceDataReceiver onReceive feature is off");
            return;
        }

        if (intent.getAction() == null) {
            if (!Watch.isInitialized()) {
                Watch.init(context);
            }

            Log.d(Constants.TAG, "WatchfaceDataReceiver sending data to phone");
            // Get data
            int battery = getPhoneBattery(context);
            String alarm = getPhoneAlarm(context);
            String calendar_events = getCalendarEvents(context);

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

            //Save update time in milliseconds
            Date date= new Date();
            Long milliseconds = date.getTime();
            Prefs.putLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, milliseconds);
        } else {
            startWatchfaceReceiver(context);
        }

        Log.d(Constants.TAG, "WatchfaceDataReceiver onReceive");
    }

    public static void startWatchfaceReceiver(Context context) {
        boolean send_data = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA);
        boolean send_on_battery_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE);
        boolean send_on_alarm_change = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE);

        // Unregister if any receiver
        /*
        try {
            WatchfaceReceiver unReceiver = new WatchfaceReceiver();
            context.unregisterReceiver(unReceiver);
        } catch (NoSuchMethodError e) {
            e.printStackTrace();
        }
        */
        send_on_battery_change = false;
        send_on_alarm_change = false;

        // update as interval
        if((!send_on_battery_change || !send_on_alarm_change) && send_data) {
            int syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_BACKGROUND_SYNC_INTERVAL, context.getResources().getStringArray(R.array.pref_watchface_background_sync_interval_values)[Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX]));

            AmazModApplication.timeLastWatchfaceDataSend = Prefs.getLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, 0L);

            long delay = ((long) syncInterval * 60000L) - SystemClock.elapsedRealtime() - AmazModApplication.timeLastWatchfaceDataSend;
            if (delay < 0) delay = 0;

            Log.i(Constants.TAG, "WatchfaceDataReceiver times: " + SystemClock.elapsedRealtime() + " / " + AmazModApplication.timeLastWatchfaceDataSend);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent alarmWatchfaceIntent = new Intent(context, WatchfaceReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmWatchfaceIntent, 0);

            try {
                if(alarmManager!=null)
                    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                        (long) syncInterval * 60000L, pendingIntent);
            } catch (NullPointerException e) {
                Log.e(Constants.TAG, "WatchfaceDataReceiver setRepeating exception: " + e.toString());
            }
        }else{
            try {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent alarmWatchfaceIntent = new Intent(context, WatchfaceReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmWatchfaceIntent, 0);
                if(alarmManager!=null)
                    alarmManager.cancel(pendingIntent);
            } catch (NoSuchMethodError e) {
                e.printStackTrace();
            }
        }

        // Stop if data send is off
        if(!send_data){
            Log.d(Constants.TAG, "WatchfaceDataReceiver onReceive feature is off");
            return;
        }

        // on battery change
        if(send_on_battery_change){
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            WatchfaceReceiver mReceiver = new WatchfaceReceiver();
            context.registerReceiver(mReceiver, ifilter);
        }

        // on alarm change
        if(send_on_alarm_change){
            IntentFilter ifilter = new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
            WatchfaceReceiver mReceiver = new WatchfaceReceiver();
            context.registerReceiver(mReceiver, ifilter);
        }
    }

    public int getPhoneBattery(Context context){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        float batteryPct;
        if(batteryStatus!=null) {
            //int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            //boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            //int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            //boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            //boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPct = level / (float) scale;
        }else{
            batteryPct = 0;
        }
        return (int) (batteryPct * 100);
    }

    public String getPhoneAlarm(Context context){
        String nextAlarm = "--";

        // Proper way to do it (Lollipop +)
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            try {
                // First check for regular alarm
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    AlarmManager.AlarmClockInfo clockInfo = alarmManager.getNextAlarmClock();
                    if (clockInfo != null) {
                        long nextAlarmTime = clockInfo.getTriggerTime();
                        Log.d(Constants.TAG, "Next alarm time: " + nextAlarmTime);
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
        }else{
            // Legacy way
            try {
                nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);

                if(nextAlarm.equals("")){
                    nextAlarm = "--";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Log next alarm
        Log.d(Constants.TAG, "WatchfaceDataReceiver next alarm: "+nextAlarm);

        return nextAlarm;
    }

    // CALENDAR EVENTS
    public static final String[] EVENT_PROJECTION = new String[] {
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.ACCOUNT_NAME
    };

    private String getCalendarEvents(Context context){
        // Check if calendar read permission is granted
        if ( Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission( Manifest.permission.READ_CALENDAR ) != PackageManager.PERMISSION_GRANTED ) {
            return "permissions error";
        }

        // Run query
        Cursor cur = null;
        ContentResolver cr = context.getContentResolver();
        Uri uri = CalendarContract.Events.CONTENT_URI;

        // Date range to pick events
        Calendar c_start= Calendar.getInstance();
        Calendar c_end= Calendar.getInstance();
        c_end.add(Calendar.DATE, 30);
        String selection = "(( " + CalendarContract.Events.DTSTART + " >= " + c_start.getTimeInMillis() + " ) AND ( " + CalendarContract.Events.DTSTART + " <= " + c_end.getTimeInMillis() + " ))";

        // Submit the query and get a Cursor object back.
        try {
            cur = cr.query(uri, EVENT_PROJECTION, selection, null, null);
        } catch (SecurityException e) {
            e.printStackTrace();
            return "permissions error";
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

            jsonEvents += "[ \""+title+"\", \""+description+"\", \""+start+"\", \""+end+"\", \""+location+"\", \""+account+"\"],";
        }

        // Remove last , from JSON
        if ( jsonEvents.substring(jsonEvents.length() - 1).equals(",") ) {
            jsonEvents = jsonEvents.substring(0, jsonEvents.length() - 1);
        }
        jsonEvents += "]}";

        // Count events
        int events = cur.getCount();
        jsonEvents += "\n\n Counted: "+events;

        cur.close();
        return jsonEvents;
    }
}
