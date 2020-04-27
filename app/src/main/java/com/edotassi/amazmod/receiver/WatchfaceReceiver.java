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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.util.FilesUtil;
import com.edotassi.amazmod.util.LocaleUtils;
import com.edotassi.amazmod.util.Weather_API;
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
    private final static int CALENDAR_DATA_INDEX_TITLE = 0;
    private final static int CALENDAR_DATA_INDEX_DESCRIPTION = 1;
    private final static int CALENDAR_DATA_INDEX_START = 2;
    private final static int CALENDAR_DATA_INDEX_END = 3;
    private final static int CALENDAR_DATA_INDEX_LOCATION = 4;
    private final static int CALENDAR_DATA_INDEX_ACCOUNT = 5;
    private final static int CALENDAR_EXTENDED_DATA_INDEX = 6;

    private final static String CALENDAR_DATA_PARAM_EXTENDED_DATA_ALL_DAY = "all_day";

    private static String default_calendar_days;
    private static boolean refresh;

    private static AlarmManager alarmManager;
    private static Intent alarmWatchfaceIntent;
    private static PendingIntent pendingIntent;
    private static WatchfaceReceiver mReceiver = new WatchfaceReceiver();
    private static LocationManager mLocationManager;
    static Double last_known_latitude;
    static Double last_known_longitude;

    // data parameters
    int battery;
    long expire;
    String alarm, calendar_events;
    JSONObject weather_data, weather_uv_data, weather_forecast_data;

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

        // Set data expiration
        Date date = new Date();
        long milliseconds = date.getTime();
        this.expire = milliseconds + 2*Integer.parseInt(context.getResources().getStringArray(R.array.pref_watchface_background_sync_interval_values)[Prefs.getInt(Constants.PREF_WATCHFACE_SEND_DATA_INTERVAL_INDEX, Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX)])*60*1000;

        if (intent.getAction() == null) {
            if (!Watch.isInitialized()) {
                Watch.init(context);
            }

            // Get data
            this.battery = getPhoneBattery(context);
            this.alarm = getPhoneAlarm(context);
            this.calendar_events = null;
            this.weather_data = null;

            // Get calendar source data from preferences then the events
            default_calendar_days = context.getResources().getStringArray(R.array.pref_watchface_background_sync_interval_values)[Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX];
            String calendar_source = Prefs.getString(Constants.PREF_WATCHFACE_CALENDAR_SOURCE, Constants.PREF_CALENDAR_SOURCE_LOCAL);
            calendar_events = (Constants.PREF_CALENDAR_SOURCE_LOCAL.equals(calendar_source))? getCalendarEvents(context) : getICSCalendarEvents(context);

            // Weather
            boolean isWeatherEnabled = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA);

            // Check if new data
            if ( isWeatherEnabled || Prefs.getInt(Constants.PREF_WATCHFACE_LAST_BATTERY, 0) != battery || !Prefs.getString(Constants.PREF_WATCHFACE_LAST_ALARM, "").equals(alarm) || calendar_events != null) {
                Logger.debug("WatchfaceDataReceiver sending data to phone");

                // If weather data are enabled, run the weather code
                if (isWeatherEnabled)
                    getWeatherData(context);
                // Else, send data directly
                else
                    sendnewdata();
            }else{
                Logger.debug("WatchfaceDataReceiver sending data to phone (no new data)");
            }

            // Save update time in milliseconds
            Prefs.putLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, milliseconds);
        } else {
            // Other actions
            Logger.debug("WatchfaceDataReceiver onReceive :"+intent.getAction());
            // If battery/alarm was changed
            if( (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED) && Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_BATTERY_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE))
                    || (intent.getAction().equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) && Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_ALARM_CHANGE, Constants.PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE) )){
                // Get data
                this.battery = getPhoneBattery(context);
                this.alarm = getPhoneAlarm(context);

                if (Prefs.getInt(Constants.PREF_WATCHFACE_LAST_BATTERY, 0)!=battery || !Prefs.getString(Constants.PREF_WATCHFACE_LAST_ALARM, "").equals(alarm)) {
                    // New data = update
                    Logger.debug("WatchfaceDataReceiver sending data to phone (battery/alarm onchange)");

                    // Send data
                    sendnewdata(false);
                }
            }
        }

        //Logger.debug("WatchfaceDataReceiver onReceive");
    }
    public void sendnewdata() {
        this.weather_data = Weather_API.join_data(this.weather_data, this.weather_forecast_data, this.weather_uv_data);
        Logger.debug("WatchfaceDataReceiver JSON weather data found: "+ WatchfaceReceiver.this.weather_data);

        if (this.weather_data != null) {
            // Save weather data for first page use
            Prefs.putString(Constants.PREF_WEATHER_LAST_DATA, this.weather_data.toString());
            Prefs.putLong(Constants.PREF_TIME_LAST_CURRENT_WEATHER_DATA_SYNC, new Date().getTime());
        }
        sendnewdata(true);
    }

    public void sendnewdata(boolean send_calendar_or_weather) {
        // Save last send values
        Prefs.putInt(Constants.PREF_WATCHFACE_LAST_BATTERY, battery);
        Prefs.putString(Constants.PREF_WATCHFACE_LAST_ALARM, alarm);

        // Put data to bundle
        WatchfaceData watchfaceData = new WatchfaceData();
        watchfaceData.setBattery(battery);
        watchfaceData.setAlarm(alarm);
        watchfaceData.setExpire(expire);

        if (send_calendar_or_weather) {
            watchfaceData.setCalendarEvents(calendar_events);
            watchfaceData.setWeatherData( (weather_data==null)?null:weather_data.toString() );
        }else{
            watchfaceData.setCalendarEvents(null);
            watchfaceData.setWeatherData(null);
        }

        Watch.get().sendWatchfaceData(watchfaceData);
        Logger.info("Data has been send to watch");
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

        // Calculate next update time
        int syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_WATCHFACE_BACKGROUND_SYNC_INTERVAL, context.getResources().getStringArray(R.array.pref_watchface_background_sync_interval_values)[Constants.PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX]));
        AmazModApplication.timeLastWatchfaceDataSend = Prefs.getLong(Constants.PREF_TIME_LAST_WATCHFACE_DATA_SYNC, 0L);
        long delay = ((long) syncInterval * 60000L) - (currentTimeMillis() - AmazModApplication.timeLastWatchfaceDataSend);
        if (delay < 0) delay = 0;

        //Logger.info("WatchfaceDataReceiver times: " + SystemClock.elapsedRealtime() + " / " + AmazModApplication.timeLastWatchfaceDataSend + " = "+delay);

        // Cancel any other intent
        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }else {
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmWatchfaceIntent = new Intent(context, WatchfaceReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(context, 0, alarmWatchfaceIntent, 0);
        }

        // Set new intent
        try {
            if (alarmManager != null)
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                        (long) syncInterval * 60000L, pendingIntent);
        } catch (NullPointerException e) {
            Logger.error("WatchfaceDataReceiver setRepeating exception: " + e.toString());
        }

        // Unregister if any receiver
        try {
            context.unregisterReceiver(WatchfaceReceiver.mReceiver);
        } catch (IllegalArgumentException e) {
            //e.printStackTrace();
        }

        // On battery change
        if (send_on_battery_change) {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            context.registerReceiver(WatchfaceReceiver.mReceiver, ifilter);
        }

        // On alarm change
        if (send_on_alarm_change) {
            IntentFilter ifilter = new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
            context.registerReceiver(WatchfaceReceiver.mReceiver, ifilter);
        }

        // Ask for location updates
        if ( Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission( android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Logger.error("WatchfaceDataReceiver location updates not initialized because permissions are not given.");
        }else{
            int LOCATION_REFRESH_DISTANCE = 10 * 1000;//in m, = 10km
            long LOCATION_REFRESH_TIME = ((long) syncInterval * 60000L);// in milliseconds
            //LocationManager.GPS_PROVIDER or LocationManager.NETWORK_PROVIDER
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            assert mLocationManager != null;
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, mLocationListener);
        }
    }

    // GPS - Location tracker
    private static final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            last_known_latitude = location.getLatitude();
            last_known_longitude = location.getLongitude();
            Logger.debug("WatchfaceDataReceiver location updated: "+last_known_latitude+","+last_known_longitude);

            Date date = new Date();
            long milliseconds = date.getTime();

            // Retrieve saved location data [milliseconds, latitude, longitude, watch_status]
            Set<String> saved_data = Prefs.getStringSet(Constants.PREF_LOCATION_GPS_DATA, null);

            // Save new values
            if (saved_data != null) {
                saved_data.add("[time: " + milliseconds + ", lat: " + last_known_latitude + ", lon:" + last_known_longitude + ", watch: " + 1 + "]");
                Prefs.putStringSet(Constants.PREF_LOCATION_GPS_DATA, saved_data);
            }
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onProviderDisabled(String provider) { }
    };

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
        try {
            // First check for regular alarm
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                AlarmManager.AlarmClockInfo clockInfo = alarmManager.getNextAlarmClock();
                if (clockInfo != null) {
                    long nextAlarmTime = clockInfo.getTriggerTime();
                    //Logger.debug("Next alarm time: " + nextAlarmTime);
                    Date nextAlarmDate = new Date(nextAlarmTime);
                    android.text.format.DateFormat df = new android.text.format.DateFormat();
                    // Format alarm time as e.g. "Fri 06:30"
                    nextAlarm = DateFormat.format("EEE HH:mm", nextAlarmDate).toString();
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
        // Log next alarm
        // Logger.debug(Constants.TAG, "WatchfaceDataReceiver next alarm: " + nextAlarm);
        return nextAlarm;
    }


    int pending_requests;

    public void getWeatherData(Context context) {
        // Get current language
        String language = LocaleUtils.getLocaleCode();
        // Load settings
        int units = Prefs.getInt(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_UNITS_INDEX, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA_UNITS_INDEX); // 0:Kelvin, 1: metric, 2: Imperial
        boolean show_feels_like = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_REAL_FEEL, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA_REAL_FEEL);
        int searchType = Prefs.getInt(Constants.PREF_WATCHFACE_WEATHER_DATA_LOCATION_RADIO, Constants.PREF_DEFAULT_WATCHFACE_WEATHER_DATA_LOCATION_RADIO);
        // API ID
        String[] appid = new String[3];
        String user_appid = Prefs.getString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_API, "").trim();
        if (user_appid.length() == 32) {
            appid[0] = appid[1] = appid[2] = user_appid; // User API ID
        }else{
            Prefs.putString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_API, context.getString(R.string.invalid_key));
            appid[0] = "e2cac022c6c3adaf75b05674522d49b1"; // default current weather key
            appid[1] = "6a1745763c358ef66ed8293324ebff58"; // default forecast key
            appid[2] = "2d46a5f377c985e8487657e6351e754b"; // default UV key
        }

        // 0: by location, 1: by City/Country
        String search, searchUV = "", search_pul = "";
        if (searchType == 0) {
            // Search by location
            if ( Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission( android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Logger.error("WatchfaceDataReceiver location updates not initialized because permissions are not given.");
            }else{
                Location getLastLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                if (getLastLocation != null) {
                    Double currentLongitude = getLastLocation.getLongitude();
                    Double currentLatitude = getLastLocation.getLatitude();
                    last_known_latitude = currentLatitude;
                    last_known_longitude = currentLongitude;
                    Logger.debug("WatchfaceDataReceiver last location: " + currentLatitude + "," + currentLongitude);
                }
            }

            if (last_known_latitude != null && last_known_longitude != null){
                search = "lat=" + last_known_latitude + "&lon=" + last_known_longitude;
                searchUV = search;
                search_pul = (last_known_latitude-1)+","+(last_known_longitude-1)+","+(last_known_latitude+1)+","+(last_known_longitude+1);
            }else{
                Logger.error("WatchfaceDataReceiver data not acquired because location is not available.");
                // send data
                sendnewdata();
                return;
            }
        }else{
            // Search by city-country
            String city_country = Prefs.getString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_CITY, "-");
            search = "q="+city_country;

            // Get saved coordinates
            String last_saved_data = Prefs.getString(Constants.PREF_WEATHER_LAST_DATA, "");
            // Extract data
            try {
                // Extract data from JSON
                JSONObject last_data = new JSONObject(last_saved_data);
                if (last_data.has("lon") && last_data.has("lat")){
                    last_known_latitude = Double.parseDouble(last_data.getString("lat"));
                    last_known_longitude = Double.parseDouble(last_data.getString("lon"));
                    searchUV = "lat=" + last_known_latitude + "&lon=" + last_known_longitude;
                    search_pul = (last_known_latitude-1)+","+(last_known_longitude-1)+","+(last_known_latitude+1)+","+(last_known_longitude+1);
                }
            }catch (Exception e) {
                // Logger.error("WatchfaceDataReceiver JSON weather data failed: "+ e.getMessage());
            }
        }

        // 5d every 3h forecast URL (OpenWeatherMap)
        String weekUrl ="https://api.openweathermap.org/data/2.5/forecast?"+search+"&appid="+appid[0]+"&lang="+language+ (units==0?"":("&units="+(units==1?"metric":"imperial")));
        // Call current weather data URL (OpenWeatherMap)
        String todayUrl ="https://api.openweathermap.org/data/2.5/weather?"+search+"&appid="+appid[1]+"&lang="+language+ (units==0?"":("&units="+(units==1?"metric":"imperial")));
        // UV API (requires searchType == 0)
        String uvUrl = "https://api.openweathermap.org/data/2.5/uvi/forecast?appid="+appid[2]+"&";//+"&lat={lat}&lon={lon}&cnt={cnt}
        // Pollution API
        String pollution = "https://api.waqi.info/mapq/bounds/?bounds=";//+lat,lol,lat,lon

        // Load update times
        Date date = new Date();
        long milliseconds = date.getTime();
        long last_weather_update = Prefs.getLong(Constants.PREF_TIME_LAST_CURRENT_WEATHER_DATA_SYNC, 0);

        // Limit default API users to 1 request per hour
        if( !user_appid.equals(appid[0]) && milliseconds-last_weather_update < 60*60*1000) {
            // Send data without weather
            sendnewdata();
            return;
        }

        // APIs to call
        String[] apiUrls = new String[4];
        // Current weather API
        apiUrls[0] = todayUrl;
        // Week forecast API
        apiUrls[1] = weekUrl;
        // UV/Pollution API
        if ( !searchUV.isEmpty() )
            apiUrls[2] = uvUrl + searchUV;
        // Pollution API
        if ( !search_pul.isEmpty() )
            apiUrls[3] = pollution + search_pul;

        this.pending_requests = apiUrls.length;

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        // loop through different urls
        for (String apiUrl : apiUrls) {
            // Move to next if API url is null
            if( apiUrl == null || apiUrl.isEmpty() ) {
                pending_requests = pending_requests - 1;
                continue;
            }

            // Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, apiUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            JSONObject weather_data = Weather_API.weather_server_data(response, units, show_feels_like, last_known_latitude, last_known_longitude);
                            save_weather_response(weather_data);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error.toString().contains("ClientError")) {
                        Toast.makeText(context, context.getString(R.string.weather_city_error), Toast.LENGTH_LONG).show();
                    }
                    if (error.toString().contains("NoConnectionError")) {
                        Toast.makeText(context, context.getString(R.string.weather_connection_error), Toast.LENGTH_LONG).show();
                    }
                    if (error.toString().contains("AuthFailureError")) {
                        Toast.makeText(context, context.getString(R.string.weather_api_error), Toast.LENGTH_LONG).show();
                    }
                    Logger.debug("WatchfaceDataReceiver get weather data request failed: " + error.toString());

                    pending_requests = pending_requests - 1;
                }
            });

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        }
    }

    // save weather data response
    public void save_weather_response(JSONObject weather_data) {
        // Save data
        if( weather_data!=null ) {
            // Save data
            if (weather_data.has("forecasts")) {
                // [FORECAST API]
                this.weather_forecast_data = weather_data;
            }else if(weather_data.has("uvIndex")){
                // [UV API]
                this.weather_uv_data = weather_data;
            }else{
                // [CURRENT weather API]
                this.weather_data = weather_data;
            }
        }

        pending_requests = pending_requests - 1;

        // Send data
        if( pending_requests <= 0 )
            sendnewdata();
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

                events.put(event);
            }
            Logger.debug("WatchfaceDataReceiver getCalendarEvents found " + events.length() + " events");
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
                            //Logger.debug("WatchfaceDataReceiver getICSCalendarEvents RRULE: " + (Period) po + " \\ " + ((Period) po).getStart().toString() + " \\ " + ((Period) po).getStart().getTime() + " \\ " + ((Period) po).getEnd().getTime());
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
