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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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

    // data parameters
    int battery;
    long expire;
    String alarm, calendar_events, weather_data;

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

            // Check if new data
            if (Prefs.getInt(Constants.PREF_WATCHFACE_LAST_BATTERY, 0)!=battery || !Prefs.getString(Constants.PREF_WATCHFACE_LAST_ALARM, "").equals(alarm) || calendar_events!=null) {
                Logger.debug("WatchfaceDataReceiver sending data to phone");

                // If weather data are enabled, run the weather code
                if (Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA))
                    getWeatherData(context);
                // Else, send the data directly
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

        //Log.d(Constants.TAG, "WatchfaceDataReceiver onReceive");
    }
    public void sendnewdata() {
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
            watchfaceData.setWeatherData(weather_data);
        }else{
            watchfaceData.setCalendarEvents(null);
            watchfaceData.setWeatherData(null);
        }

        Watch.get().sendWatchfaceData(watchfaceData);
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

    public void getWeatherData(Context context) {
        Integer units = Prefs.getInt(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_UNITS_INDEX, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA_UNITS_INDEX); // 0:Kelvin, 1: metric, 2: Imperial
        String language = "en"; // TODO add the selected language code here
        boolean show_feels_like = Prefs.getBoolean(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_REAL_FEEL, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA_REAL_FEEL);

        // Get saved API ID
        String appid = Prefs.getString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_API, "-");

        int searchType = 1; // TODO add option in settings (first code the GPS)
        // 0: by location, 1: by City/Country
        String search;

        if (searchType == 0) {
            // Search by location
            double latitude = 51.4655561; // TODO get this from GPS
            double longitude = -0.1726452; // TODO get this from GPS
            search = "lat=" + latitude + "&lon=" + longitude;
        }else{
            // Search by city-country
            String city_country = Prefs.getString(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA_CITY, "-");
            search = "q="+city_country;
        }

        // 5d every 3h forecast URL (OpenWeatherMap)
        String weekUrl ="https://api.openweathermap.org/data/2.5/forecast?"+search+"&appid="+appid+"&lang="+language+ (units==0?"":("&units="+(units==1?"metric":"imperial")));
        // Call current weather data URL (OpenWeatherMap)
        String todayUrl ="https://api.openweathermap.org/data/2.5/weather?"+search+"&appid="+appid+"&lang="+language+ (units==0?"":("&units="+(units==1?"metric":"imperial")));

        // UV API (requires searchType == 0)
        // String uvUrl ="https://api.openweathermap.org/data/2.5/uvi?"+search+"&appid="+appid;

        // Load update times
        Date date = new Date();
        long milliseconds = date.getTime();
        long last_week_weather_update = Prefs.getLong(Constants.PREF_TIME_LAST_WEEK_WEATHER_DATA_SYNC, 0);
        Date week_update_date = new Date(last_week_weather_update);

        // Pick API to call
        String apiUrl;
        if ( date.getDate() != week_update_date.getDate() || milliseconds-last_week_weather_update > 8*60*60*1000 ){// New day or 8h passed
            // Update week forecast
            apiUrl = weekUrl;
        }else{
            // Get current weather
            apiUrl = todayUrl;
        }

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, apiUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Logger.debug("WatchfaceDataReceiver weather data: " + response);

                        // Get last last current weather update time
                        long last_current_weather_update = Prefs.getLong(Constants.PREF_TIME_LAST_CURRENT_WEATHER_DATA_SYNC, 0);

                        // Extract data
                        try {
                            // Extract data from JSON
                            JSONObject weather_data = new JSONObject(response);

                            // Example of weather URL (current)
                            // {"coord":{"lon":-0.17,"lat":51.47},
                            // "weather":[{"id":800,"main":"Clear","description":"clear sky","icon":"01n"}],
                            // "base":"stations",
                            // "main":{"temp":3.71,"feels_like":-3.07,"temp_min":3,"temp_max":4,"pressure":1009,"humidity":87,"temp_min":6.67,"temp_max":9},
                            // "visibility":10000,
                            // "wind":{"speed":5.1,"deg":250},
                            // "clouds":{"all":9},
                            // "dt":1575674895,
                            // "sys":{"type":1,"id":1502,"country":"GB","sunrise":1575618606,"sunset":1575647601},
                            // "timezone":0,"id":6690602,"name":"Battersea","cod":200}

                            // Example of forecast URL
                            // {"cod":"200","message":0,"cnt":40,
                            // "list":[
                            //      {
                            //      "dt":1576357200,
                            //      "main":{"temp":2.34,"feels_like":-2.63,"temp_min":0.6,"temp_max":2.34,"pressure":980,"sea_level":980,"grnd_level":969,"humidity":81,"temp_kf":1.74},
                            //      "weather":[{"id":802,"main":"Clouds","description":"scattered clouds","icon":"03n"}],
                            //      "clouds":{"all":39},
                            //      "wind":{"speed":4.15,"deg":223},
                            //      "sys":{"pod":"n"},
                            //      "dt_txt":"2019-12-14 21:00:00"
                            //      },
                            //      ...
                            // ],
                            // "city":{
                            //      "id":2646670,
                            //      "name":"Somewhere",
                            //      "coord":{"lat":00.000,"lon":-0.000},
                            //      "country":"GB",
                            //      "population":13109,
                            //      "timezone":0,
                            //      "sunrise":1576312722,
                            //      "sunset":1576339022
                            //  }}

                            // Data to sent
                            JSONObject new_weather_info = new JSONObject();
                            // Example:
                            // WeatherInfo
                            // {"isAlert":true, "isNotification":true, "tempFormatted":"28ºC",
                            // "tempUnit":"C", "v":1, "weatherCode":0, "aqi":-1, "aqiLevel":0, "city":"Somewhere",
                            // "forecasts":[{"tempFormatted":"31ºC/21ºC","tempMax":31,"tempMin":21,"weatherCodeFrom":0,"weatherCodeTo":0,"day":1,"weatherFrom":0,"weatherTo":0},{"tempFormatted":"33ºC/23ºC","tempMax":33,"tempMin":23,"weatherCodeFrom":0,"weatherCodeTo":0,"day":2,"weatherFrom":0,"weatherTo":0},{"tempFormatted":"34ºC/24ºC","tempMax":34,"tempMin":24,"weatherCodeFrom":0,"weatherCodeTo":0,"day":3,"weatherFrom":0,"weatherTo":0},{"tempFormatted":"34ºC/23ºC","tempMax":34,"tempMin":23,"weatherCodeFrom":0,"weatherCodeTo":0,"day":4,"weatherFrom":0,"weatherTo":0},{"tempFormatted":"32ºC/22ºC","tempMax":32,"tempMin":22,"weatherCodeFrom":0,"weatherCodeTo":0,"day":5,"weatherFrom":0,"weatherTo":0}],
                            // "pm25":-1, "sd":"50%", //(Humidity)
                            // "temp":28, "time":1531292274457, "uv":"Strong",
                            // "weather":0, "windDirection":"NW", "windStrength":"7.4km/h"}

                            // Standard format (WeatherInfo) is send to watch
                            Double temp, feels_like, temp_min, temp_max, tmp_temp_min, tmp_temp_max, speed;
                            Integer weather_id_from, weather_id_to;
                            String pressure, humidity, tempUnit, city, clouds;
                            int weather_id, visibility, sunrise, sunset, deg;
                            String[] directions = {"N","NE", "E", "SE", "S", "SW", "W", "NW","N/A"};

                            tempUnit = (units==0?"K":(units==1?"C":"F"));
                            new_weather_info.put("tempUnitNo",units);
                            new_weather_info.put("tempUnit",tempUnit);

                            // Get current time
                            Date date = new Date();

                            // Extract forecast data [FORECAST API]
                            if (weather_data.has("list")) {
                                JSONArray list = weather_data.getJSONArray("list");

                                // Create new Huami forecasts array
                                JSONArray forecasts = new JSONArray();

                                // Today
                                int dayofmonth = date.getDate();
                                // initialise data
                                int day = 0;
                                long temp_dt = 0;
                                weather_id_from = weather_id_to = 22;
                                temp_min = 999.0;
                                temp_max = -273.0;

                                // Loop in the pulled data
                                boolean saved = true;
                                for (int i = 0; i < list.length(); i++) {
                                    JSONObject item = list.getJSONObject(i); // each saved instance (every 3h)
                                    int tmp_dayofmonth;
                                    if (item.has("dt")) {
                                        temp_dt = item.getLong("dt")*1000; // pull
                                        //new_weather_info.put("dt",temp_dt); // save
                                        tmp_dayofmonth = new Date(temp_dt).getDate();
                                    }else
                                        tmp_dayofmonth = dayofmonth;

                                    //Logger.debug("WatchfaceDataReceiver JSON weather date: "+ tmp_dayofmonth +" - "+ dayofmonth);

                                    // New day
                                    if ( tmp_dayofmonth != dayofmonth && i != 0 ){
                                        day += 1;
                                        // Save the previous day data
                                        JSONObject new_item = new JSONObject();
                                        if ( temp_min < 999.0 )
                                            new_item.put("tempMin",Math.round(temp_min));
                                        if ( temp_max > -273.0 )
                                            new_item.put("tempMax",Math.round(temp_max));
                                        new_item.put("tempFormatted",Math.round(temp_max)+"º"+tempUnit+"/"+Math.round(temp_min)+"º"+tempUnit);
                                        new_item.put("weatherCodeFrom",weather_id_from);
                                        new_item.put("weatherCodeTo",weather_id_to);
                                        new_item.put("weatherFrom",weather_id_from);
                                        new_item.put("weatherTo",weather_id_to);
                                        new_item.put("day",day);

                                        forecasts.put(new_item);
                                        saved = true;
                                    }

                                    tmp_temp_min = 999.0;
                                    tmp_temp_max = -273.0;
                                    if (item.has("main")) {
                                        // pull
                                        JSONObject main = item.getJSONObject("main");
                                        temp = Double.parseDouble(main.getString("temp"));
                                        feels_like = Double.parseDouble(main.getString("feels_like"));
                                        pressure = main.getInt("pressure")+"hPa";
                                        humidity = main.getInt("humidity")+"%";
                                        tmp_temp_min = Double.parseDouble(main.getString("temp_min"));
                                        tmp_temp_max = Double.parseDouble(main.getString("temp_max"));

                                        // save
                                        if ( i == 0 && temp_dt > last_current_weather_update ) {
                                            new_weather_info.put("tempFormatted", Math.round(temp) + "º" + tempUnit);
                                            new_weather_info.put("temp", (show_feels_like)?Math.round(feels_like):Math.round(temp));
                                            new_weather_info.put("tempMin", Math.round(tmp_temp_min));
                                            new_weather_info.put("tempMax", Math.round(tmp_temp_max));
                                            new_weather_info.put("pressure", pressure);
                                            new_weather_info.put("sd", humidity);

                                            // Update current weather time
                                            Prefs.putLong(Constants.PREF_TIME_LAST_CURRENT_WEATHER_DATA_SYNC, temp_dt);
                                        }
                                    }

                                    weather_id = 22;
                                    if (item.has("weather")) {
                                        // pull
                                        JSONArray weather = item.getJSONArray("weather");
                                        JSONObject weather1 = weather.getJSONObject(0);
                                        weather_id = getHuamiWeatherCode(weather1.getInt("id"));

                                        // save
                                        if ( i == 0 )
                                            new_weather_info.put("weatherCode",weather_id);
                                    }

                                    if (item.has("clouds") && i == 0 && temp_dt > last_current_weather_update ) {
                                        // pull
                                        JSONObject cloudsObj = item.getJSONObject("clouds");
                                        clouds = cloudsObj.getInt("all")+"%";
                                        // save
                                        new_weather_info.put("clouds",clouds);
                                    }

                                    if (item.has("wind") && i == 0 && temp_dt > last_current_weather_update ) {
                                        // pull
                                        JSONObject wind = item.getJSONObject("wind");
                                        speed = Double.parseDouble(wind.getString("speed"))*(units==2?1:3.6); // convert m/s to km/h (x3.6)
                                        deg = wind.getInt("deg");
                                        // save
                                        Integer direction_index = (deg + 45/2) / 45;
                                        new_weather_info.put("windDirection", directions[(direction_index<8)?direction_index:8] );
                                        new_weather_info.put("windDirectionUnit","º");
                                        new_weather_info.put("windDirectionValue",deg+"");
                                        new_weather_info.put("windSpeedUnit", (units==2?"m/h":"km/h") );
                                        new_weather_info.put("windSpeedValue",Math.round(speed));
                                        new_weather_info.put("windStrength",Math.round(speed)+(units==2?"m/h":"km/h"));
                                    }

                                    // Still the same day
                                    if ( tmp_dayofmonth == dayofmonth ){
                                        // Add data to already saved
                                        if ( tmp_temp_min < temp_min )
                                            temp_min = tmp_temp_min;
                                        if ( tmp_temp_max > temp_max )
                                            temp_max = tmp_temp_max;
                                        if ( weather_id < weather_id_from )
                                            weather_id_from = weather_id;
                                        if ( weather_id < 22 && (weather_id_to == 22 || weather_id > weather_id_to) )
                                            weather_id_to = weather_id;
                                        saved = false;
                                    }else{
                                        dayofmonth = tmp_dayofmonth;
                                    }
                                }

                                // Last new day
                                if ( !saved ){
                                    day += 1;
                                    // Save the previous day data
                                    JSONObject new_item = new JSONObject();
                                    if ( temp_min < 999.0 )
                                        new_item.put("tempMin",Math.round(temp_min));
                                    if ( temp_max > -273.0 )
                                        new_item.put("tempMax",Math.round(temp_max));
                                    new_item.put("tempFormatted",Math.round(temp_max)+"º"+tempUnit+"/"+Math.round(temp_min)+"º"+tempUnit);
                                    new_item.put("weatherCodeFrom",weather_id_from);
                                    new_item.put("weatherCodeTo",weather_id_to);
                                    new_item.put("weatherFrom",weather_id_from);
                                    new_item.put("weatherTo",weather_id_to);
                                    new_item.put("day",day);

                                    forecasts.put(new_item);
                                }

                                // save forecast
                                new_weather_info.put("forecasts",forecasts);
                            }
                            // [FORECAST API]
                            if (weather_data.has("city")) {
                                JSONObject sys = weather_data.getJSONObject("city");

                                if (sys.has("name")){
                                    city = sys.getString("name"); // pull
                                    new_weather_info.put("city",city); // save
                                }
                                if (sys.has("sunrise")) {
                                    sunrise = sys.getInt("sunrise"); // pull
                                    new_weather_info.put("sunrise",sunrise); // save
                                }
                                if (sys.has("sunset")) {
                                    sunset = sys.getInt("sunset"); // pull
                                    new_weather_info.put("sunset", sunset);
                                }
                            }

                            // [CURRENT weather API] Only on current weather API and not in the forecast
                            if (weather_data.has("visibility")){
                                // Visibility
                                // pull
                                visibility = weather_data.getInt("visibility");
                                // save
                                new_weather_info.put("visibility",visibility);
                            }
                            // [CURRENT weather API]
                            if (weather_data.has("main")) {
                                // pull
                                JSONObject main = weather_data.getJSONObject("main");
                                temp = Double.parseDouble(main.getString("temp"));
                                feels_like = Double.parseDouble(main.getString("feels_like"));
                                pressure = main.getInt("pressure")+"hPa";
                                humidity = main.getInt("humidity")+"%";
                                tmp_temp_min = Double.parseDouble(main.getString("temp_min"));
                                tmp_temp_max = Double.parseDouble(main.getString("temp_max"));
                                // save
                                new_weather_info.put("tempFormatted", Math.round(temp) + "º" + tempUnit);
                                new_weather_info.put("temp", (show_feels_like)?Math.round(feels_like):Math.round(temp));
                                new_weather_info.put("tempMin", Math.round(tmp_temp_min));
                                new_weather_info.put("tempMax", Math.round(tmp_temp_max));
                                new_weather_info.put("pressure", pressure);
                                new_weather_info.put("sd", humidity);
                            }
                            // [CURRENT weather API]
                            if (weather_data.has("weather")) {
                                // pull
                                JSONArray weather = weather_data.getJSONArray("weather");
                                JSONObject weather1 = weather.getJSONObject(0);
                                weather_id = getHuamiWeatherCode(weather1.getInt("id"));
                                // save
                                new_weather_info.put("weatherCode",weather_id);
                            }
                            // [CURRENT weather API]
                            if (weather_data.has("clouds") ) {
                                // pull
                                JSONObject cloudsObj = weather_data.getJSONObject("clouds");
                                clouds = cloudsObj.getInt("all")+"%";
                                // save
                                new_weather_info.put("clouds",clouds);
                            }
                            // [CURRENT weather API]
                            if (weather_data.has("wind") ) {
                                // pull
                                JSONObject wind = weather_data.getJSONObject("wind");
                                speed = Double.parseDouble(wind.getString("speed"))*(units==2?1:3.6); // convert m/s to km/h (x3.6)
                                deg = wind.getInt("deg");
                                // save
                                int direction_index = (deg + 45/2) / 45;
                                new_weather_info.put("windDirection", directions[(direction_index<8)?direction_index:8] );
                                new_weather_info.put("windDirectionUnit","º");
                                new_weather_info.put("windDirectionValue",deg+"");
                                new_weather_info.put("windSpeedUnit", (units==2?"m/h":"km/h") );
                                new_weather_info.put("windSpeedValue",Math.round(speed));
                                new_weather_info.put("windStrength",Math.round(speed)+(units==2?"m/h":"km/h"));
                            }
                            // [CURRENT weather API]
                            if (weather_data.has("name")){
                                city = weather_data.getString("name"); // pull
                                new_weather_info.put("city",city); // save
                            }
                            // [CURRENT weather API]
                            if (weather_data.has("sys")) {
                                JSONObject sys = weather_data.getJSONObject("sys");

                                if (sys.has("sunrise")) {
                                    sunrise = sys.getInt("sunrise"); // pull
                                    new_weather_info.put("sunrise",sunrise); // save
                                }
                                if (sys.has("sunset")) {
                                    sunset = sys.getInt("sunset"); // pull
                                    new_weather_info.put("sunset", sunset);
                                }
                            }

                            new_weather_info.put("time", date.getTime() ); // save current time in milliseconds

                            // save data to send
                            WatchfaceReceiver.this.weather_data = new_weather_info.toString();
                            Logger.debug("WatchfaceDataReceiver JSON weather data found: "+ WatchfaceReceiver.this.weather_data);

                            // save update time
                            if (weather_data.has("list")) {
                                // [FORECAST API]
                                // Save week weather update time
                                Prefs.putLong(Constants.PREF_TIME_LAST_WEEK_WEATHER_DATA_SYNC, date.getTime());
                            }else{
                                // [CURRENT weather API]
                                // Save current weather update time
                                Prefs.putLong(Constants.PREF_TIME_LAST_CURRENT_WEATHER_DATA_SYNC, date.getTime());
                            }
                        }
                        catch (Exception e) {
                            Logger.error("WatchfaceDataReceiver JSON weather data failed: "+ e.getMessage());
                        }

                        // send data
                        sendnewdata();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Logger.debug("WatchfaceDataReceiver get weather data failed: "+ error.toString());
                // Send data but weather will be null
                sendnewdata();
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private int getHuamiWeatherCode(int weather_id) {
        // Openweathermap icons:
        // https://openweathermap.org/weather-conditions

        // Huami weather icons:
        /*
        "sunny", //0
        "cloudy", //1
        "overcast", //2
        "fog", //3
        "smog", //4
        "shower", //5
        "thunder_shower", //6
        "light_rain", //7
        "moderate_rain", //8
        "heavy_rain", //9
        "rainstorm", //10
        "torrential_rain", //11
        "sleet", //12
        "freezing_rain", //13
        "hail", //14
        "light_snow", //15
        "moderate_snow", //16
        "heavy_snow", //17
        "snowstorm", //18
        "dust", //19
        "blowing_sand", //20
        "sand_storm", //21
        "unknown" //22
        */

        // relate weather provider's weather ID to huami's weather code better than now
        if ( weather_id < 211 ){
            //200	Thunderstorm	thunderstorm with light rain
            //201	Thunderstorm	thunderstorm with rain
            //202	Thunderstorm	thunderstorm with heavy rain
            //210	Thunderstorm	light thunderstorm
            return 10;
        }else if( weather_id < 300 ){
            //211	Thunderstorm	thunderstorm
            //212	Thunderstorm	heavy thunderstorm
            //221	Thunderstorm	ragged thunderstorm
            //230	Thunderstorm	thunderstorm with light drizzle
            //231	Thunderstorm	thunderstorm with drizzle
            //232	Thunderstorm	thunderstorm with heavy drizzle
            return 6;
        }else if( weather_id < 312 ){
            //300	Drizzle	light intensity drizzle
            //301	Drizzle	drizzle
            //302	Drizzle	heavy intensity drizzle
            //310	Drizzle	light intensity drizzle rain
            //311	Drizzle	drizzle rain
            return 5;
        }else if( weather_id < 400 ){
            //312	Drizzle	heavy intensity drizzle rain
            //313	Drizzle	shower rain and drizzle
            //314	Drizzle	heavy shower rain and drizzle
            //321	Drizzle	shower drizzle
            return 6;
        }else if( weather_id < 501 ){
            //500	Rain	light rain
            return 7;
        }else if( weather_id < 502 ){
            //501	Rain	moderate rain
            return 8;
        }else if( weather_id < 504 ){
            //502	Rain	heavy intensity rain
            //503	Rain	very heavy rain
            return 9;
        }else if( weather_id < 512 ){
            //504	Rain	extreme rain
            //511	Rain	freezing rain
            return 11;
        }else if( weather_id < 522 ){
            //520	Rain	light intensity shower rain
            //521	Rain	shower rain
            return 5;
        }else if( weather_id < 532 ){
            //522	Rain	heavy intensity shower rain
            //531	Rain	ragged shower rain
            return 6;
        }else if( weather_id < 601 ){
            //600	Snow	light snow
            return 15;
        }else if( weather_id < 602 ){
            //601	Snow	Snow
            return 16;
        }else if( weather_id < 611 ){
            //602	Snow	Heavy snow
            return 17;
        }else if( weather_id < 612 ){
            //611	Snow	Sleet
            //612	Snow	Light shower sleet
            //613	Snow	Shower sleet
            return 12;
        }else if( weather_id < 617 ){
            //615	Snow	Light rain and snow
            //616	Snow	Rain and snow
            return 13;
        }else if( weather_id < 621 ){
            //620	Snow	Light shower snow
            return 15;
        }else if( weather_id < 622 ){
            //621	Snow	Shower snow
            return 16;
        }else if( weather_id < 623 ){
            //622	Snow	Heavy shower snow
            return 17;
        }else if( weather_id < 730 ){
            //701	Mist	mist
            //711	Smoke	Smoke
            //721	Haze	Haze
            return 4;
        }else if( weather_id < 740 ){
            //731	Dust	sand/ dust whirls
            return 19;
        }else if( weather_id < 750 ){
            //741	Fog	fog
            return 3;
        }else if( weather_id < 760 ){
            //751	Sand	sand
            return 20;
        }else if( weather_id < 762 ){
            //761	Dust	dust
            return 19;
        }else if( weather_id < 780 ){
            //762	Ash	volcanic ash
            //771	Squall	squalls
            return 20;
        }else if( weather_id < 800 ){
            //781	Tornado	tornado
            return 21;
        }else if( weather_id < 802 ){
            //800	Clear	clear sky
            //801	Clouds	few clouds: 11-25%
            return 0;
        }else if( weather_id < 804 ){
            //802	Clouds	scattered clouds: 25-50%
            //803	Clouds	broken clouds: 51-84%
            return 1;
        }else if( weather_id < 900 ){
            //804	Clouds	overcast clouds: 85-100%
            return 2;
        }else{
            return 22;
        }
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
