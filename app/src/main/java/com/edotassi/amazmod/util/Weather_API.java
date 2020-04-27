package com.edotassi.amazmod.util;

import com.edotassi.amazmod.receiver.WatchfaceReceiver;
import com.pixplicity.easyprefs.library.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.util.Date;

import amazmod.com.transport.Constants;

public class Weather_API {

    public static JSONObject weather_server_data(String response, int units, boolean show_feels_like, Double latitude, Double longitude) {
        Logger.debug("WatchfaceDataReceiver weather data: " + response);

        // If response is an array, wrap it inside an object (UV & Pollution cases)
        if ( response.trim().substring(0, 1).equals("[") )
            response = "{\"uv-pollution\":"+response+"}";

        // Get last last current weather update time
        long last_current_weather_update = Prefs.getLong(Constants.PREF_TIME_LAST_CURRENT_WEATHER_DATA_SYNC, 0);

        // JSON data
        JSONObject new_weather_info = new JSONObject();

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
            // "timezone":0,"id":111111,"name":"Battersea","cod":200}

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
            //      "id":1111,
            //      "name":"Somewhere",
            //      "coord":{"lat":00.000,"lon":-0.000},
            //      "country":"GB",
            //      "population":10000,
            //      "timezone":0,
            //      "sunrise":1500000000,
            //      "sunset":1000000000
            //  }}

            // Example of UV URL
            // [
            //      {
            //      "lat":00.00,
            //      "lon":-5.84,
            //      "date_iso":"2020-02-18T12:00:00Z",
            //      "date":1582027200,
            //      "value":0.84
            //      },
            //      ...
            // ]

            // Example of Pollution URL
            // [
            //      {
            //      "lat":00.00,
            //      "lon":-0.00,
            //      "city":"Somewhere","idx":41111,"stamp":1588010400,
            //      "pol":"pm25","x":"3160","aqi":"29",
            //      "tz":"+01:00","utime":"2020-04-27 19:00:00","img":"_C_W2ckrNSUssLlFwTs0rKUoFAA"
            //      },
            //      ...
            // ]

            // Data to sent
            new_weather_info = new JSONObject();
            // Example:
            // WeatherInfo
            // {"isAlert":true, "isNotification":true, "tempFormatted":"28ºC",
            // "tempUnit":"C", "v":1, "weatherCode":0, "aqi":-1, "aqiLevel":0, "city":"Somewhere",
            // "forecasts":[{"tempFormatted":"31ºC/21ºC","tempMax":31,"tempMin":21,"weatherCodeFrom":0,"weatherCodeTo":0,"day":1,"weatherFrom":0,"weatherTo":0},{"tempFormatted":"33ºC/23ºC","tempMax":33,"tempMin":23,"weatherCodeFrom":0,"weatherCodeTo":0,"day":2,"weatherFrom":0,"weatherTo":0},{"tempFormatted":"34ºC/24ºC","tempMax":34,"tempMin":24,"weatherCodeFrom":0,"weatherCodeTo":0,"day":3,"weatherFrom":0,"weatherTo":0},{"tempFormatted":"34ºC/23ºC","tempMax":34,"tempMin":23,"weatherCodeFrom":0,"weatherCodeTo":0,"day":4,"weatherFrom":0,"weatherTo":0},{"tempFormatted":"32ºC/22ºC","tempMax":32,"tempMin":22,"weatherCodeFrom":0,"weatherCodeTo":0,"day":5,"weatherFrom":0,"weatherTo":0}],
            // "pm25":-1, "sd":"50%", //(Humidity)
            // "temp":28, "time":1531292274457, "uv":"Strong",
            // "weather":0, "windDirection":"NW", "windStrength":"7.4km/h"}

            // Standard format (WeatherInfo) is send to watch
            double temp, feels_like, temp_min, temp_max, tmp_temp_min, tmp_temp_max, speed;
            int weather_id_from, weather_id_to;
            String pressure, humidity, tempUnit, city, clouds, country, lon, lat, description, weather_from, weather_to;
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
                weather_id_from = 22;
                weather_id_to = -1;
                temp_min = 999.0;
                temp_max = -273.0;
                weather_from = weather_to = description = "N/A";

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
                        new_item.put("weatherFrom",weather_from);
                        new_item.put("weatherTo",weather_to);
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
                            new_weather_info.put("tempFormatted", Math.round(temp) + "°" + tempUnit);
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
                        weather_id = Weather_API.getHuamiWeatherCode(weather1.getInt("id"));
                        description = weather1.getString("description");

                        // save
                        if ( i == 0 )
                            new_weather_info.put("weatherCode", weather_id);
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
                        if (wind.has("deg"))
                            deg = wind.getInt("deg");
                        else
                            deg = 0;
                        // save
                        int direction_index = (deg + 45/2) / 45;
                        new_weather_info.put("windDirection", directions[Math.min(direction_index, 8)] );
                        new_weather_info.put("windDirectionUnit","°");
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
                        if ( weather_id_from == 22 || weather_id > weather_id_from ) {
                            weather_id_from = weather_id;
                            weather_from = description;
                        }
                        if ( weather_id_to == -1 || weather_id < weather_id_to ) {
                            weather_id_to = weather_id;
                            weather_to = description;
                        }
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
                    new_item.put("weatherFrom",weather_from);
                    new_item.put("weatherTo",weather_to);
                    new_item.put("day",day);

                    forecasts.put(new_item);
                }

                // save forecast
                new_weather_info.put("forecasts", forecasts);
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
                new_weather_info.put("pressure", pressure); // hPa
                new_weather_info.put("sd", humidity);
                new_weather_info.put("actual_temp", Math.round(temp)); // New value
                new_weather_info.put("real_feel", Math.round(feels_like)); // New value
            }
            // [CURRENT weather API]
            if (weather_data.has("weather")) {
                // pull
                JSONArray weather = weather_data.getJSONArray("weather");
                JSONObject weather1 = weather.getJSONObject(0);
                weather_id = Weather_API.getHuamiWeatherCode(weather1.getInt("id"));

                description = "N/A";
                if (weather1.has("description"))
                    description = weather1.getString("description");

                // save
                new_weather_info.put("weatherCode",weather_id);
                new_weather_info.put("weatherDescription", description); // New value
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

                if (wind.has("deg"))
                    deg = wind.getInt("deg");
                else
                    deg = 0;

                // save
                int direction_index = (deg + 45/2) / 45;
                new_weather_info.put("windDirection", directions[Math.min(direction_index, 8)] );
                new_weather_info.put("windDirectionUnit","º");
                new_weather_info.put("windDirectionValue",deg+"");
                new_weather_info.put("windSpeedUnit", (units==2?"m/h":"km/h") );
                new_weather_info.put("windSpeedValue",Math.round(speed));
                new_weather_info.put("windStrength",Math.round(speed)+(units==2?"m/h":"km/h"));
            }
            // [CURRENT weather API]
            if (weather_data.has("name")){
                new_weather_info.put("city", weather_data.getString("name"));
            }
            // [CURRENT weather API]
            if (weather_data.has("sys")) {
                JSONObject sys = weather_data.getJSONObject("sys");

                if (sys.has("sunrise")) {
                    new_weather_info.put("sunrise", sys.getInt("sunrise"));
                }
                if (sys.has("sunset")) {
                    new_weather_info.put("sunset", sys.getInt("sunset"));
                }
                if (sys.has("country")) {
                    new_weather_info.put("country", sys.getString("country")); // New value
                }
            }
            // [CURRENT weather API]
            if (weather_data.has("coord")) {
                JSONObject coord = weather_data.getJSONObject("coord");

                if (coord.has("lon")) {
                    new_weather_info.put("lon", coord.getString("lon")); // New value
                }
                if (coord.has("lat")) {
                    new_weather_info.put("lat", coord.getString("lat")); // New value
                }
            }

            // [UV/Pollution weather API]
            if (weather_data.has("uv-pollution")) {
                Double distance = -1.0;
                JSONArray json = weather_data.getJSONArray("uv-pollution");
                for(int i=0;i<json.length();i++){
                    JSONObject item = json.getJSONObject(i);

                    // UV
                    if (item.has("date")) {
                        if(item.has("value") && ( item.getInt("date")*1000 < date.getTime() )){
                            double uvIndex = Double.parseDouble(item.getString("value"));
                            new_weather_info.put("uvIndex", Math.round(uvIndex));
                        }
                    }

                    // Pollution
                    if (item.has("aqi") && item.has("pol") && latitude!=null && longitude!=null) {
                        if (item.getString("pol").equals("pm25") && item.has("lat") && item.has("lon")) {
                            // Calculate station distance
                            Double tempDistance = getDistanceFromLatLonInKm(latitude,longitude,item.getDouble("lat"),item.getDouble("lon"));

                            // Save the closest station
                            if( distance < 0 || tempDistance < distance ) {
                                distance = tempDistance;

                                int aqi = Integer.parseInt(item.getString("pm25"));
                                new_weather_info.put("pm25", aqi);
                            }
                        }
                    }
                }
            }

            new_weather_info.put("time", date.getTime() ); // save current time in milliseconds
        }
        catch (Exception e) {
            Logger.error("WatchfaceDataReceiver JSON weather data failed: "+ e.getMessage());
        }

        return new_weather_info;
    }

    // Merge 2nd and 3rd object in first
    public static JSONObject join_data(JSONObject current, JSONObject forecast, JSONObject uv) {
        // Add forecast to current weather data
        if (forecast!=null) {
            try {
                if(forecast.has("forecasts"))
                    current.put("forecasts", forecast.getJSONArray("forecasts"));

                if(forecast.has("city"))
                    current.put("city", forecast.getString("city"));

                if(forecast.has("sunrise"))
                    current.put("sunrise", forecast.getInt("sunrise"));

                if(forecast.has("sunset"))
                    current.put("sunset", forecast.getInt("sunset"));
            }catch (Exception e) {
                Logger.error("WatchfaceDataReceiver JSON weather forecast data save to current data failed: "+ e.getMessage());
            }
        }

        // Add uv data to current weather data
        if(uv!=null){
            try {
                if(uv.has("uvIndex"))
                    current.put("uvIndex", uv.getInt("uvIndex"));
            }catch (Exception e) {
                Logger.error("WatchfaceDataReceiver JSON weather uv data save to current data failed: "+ e.getMessage());
            }
        }

        return current;
    }

    private static Double getDistanceFromLatLonInKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        // Haversine formula
        Double R = 6371.0; // Radius of the earth in km
        Double dLat = deg2rad(lat2-lat1);  // Convert degrees to radian
        Double dLon = deg2rad(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private static Double deg2rad(Double deg) {
        return deg * (Math.PI/180);
    }

    private static int getHuamiWeatherCode(int weather_id) {
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
}
