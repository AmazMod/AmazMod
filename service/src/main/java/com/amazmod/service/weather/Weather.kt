package com.amazmod.service.weather

import android.content.Context
import android.provider.Settings
import com.amazmod.service.util.SystemProperties.isStratos3
import org.json.JSONException
import org.json.JSONObject
import org.tinylog.Logger
import java.util.*

object Weather {
    var MAIN_URI = "WeatherInfo"
    var SECONDARY_URI = "WeatherCheckedSummary"
    var MCU_URI = "McuWeatherInfo"
    var DATA_HAVE_EXPIRED = "DATA_HAVE_EXPIRED"
    @JvmField
    var DATA_HAVE_NOT_UPDATE = "DATA_HAVE_NOT_UPDATE"

    fun updateWeatherData(context: Context, new_weather_data: String, expire: Long?): String {
        return updateWeatherData(context, new_weather_data, false, expire)
    }

    // Update system weather data with new data
    @JvmOverloads
    @JvmStatic
    fun updateWeatherData(context: Context, new_weather_data: String, clear_previous_values: Boolean = false, expire: Long? = null): String {
        try {
            // Check if correct form of JSON
            val json_data = JSONObject(new_weather_data)

            // Check if watch is Stratos 3
            var mcu = isStratos3()

            // Get system saved data
            var systemJsonData = JSONObject()
            var systemJsonDataShort = JSONObject()
            var systemJsonDataMCU = JSONObject() // Stratos 3 format
            if (!clear_previous_values) {
                try {
                    var data = Settings.System.getString(context.contentResolver, MAIN_URI)
                    systemJsonData = JSONObject(if (data == null || data == "") "{}" else data)
                    // Example:
                    // {"isAlert":true,"isNotification":true,"tempFormatted":"6┬║C","tempUnit":"C","v":1,"weatherCode":3,"aqi":-1,"aqiLevel":0,"city":"Holywood",
                    // "forecasts":[
                    //      {"tempFormatted":"6ºC/4ºC","tempMax":6,"tempMin":4,"weatherCodeFrom":5,"weatherCodeTo":5,"day":1,"weatherFrom":0,"weatherTo":0},
                    //      ...
                    //      {"tempFormatted":"5ºC/1ºC","tempMax":5,"tempMin":1,"weatherCodeFrom":0,"weatherCodeTo":5,"day":7,"weatherFrom":0,"weatherTo":0}],
                    // "pm25":-1,"sd":"87%","temp":6,"time":1575843188054,"uv":"Weakest","uvIndex":"1","weather":5,
                    // "windDirection":"SW","windDirectionUnit":"°","windDirectionValue":"225","windSpeedUnit":"km/h","windSpeedValue":"32","windStrength":"32km/h"}

                    data = Settings.System.getString(context.contentResolver, SECONDARY_URI)
                    systemJsonDataShort = JSONObject(if (data == null || data == "") "{}" else data)
                    // Example: {"tempUnit":"1","temp":"6","weatherCodeFrom":3}

                    if(mcu) {
                        data = Settings.System.getString(context.contentResolver, MCU_URI) // Stratos 3
                        systemJsonDataMCU = JSONObject(if (data == null || data == "") "{}" else data)
                        // Example:
                        // {"AQIStr":"","AQIValue":-1,
                        // "currentAir":0,"currentTemp":13, "tempUnit":0,
                        // "currentUVI":1,
                        // "data":[
                        //      {"airConFrom":3,"airConTo":34,"highestTemp":16,"lowestTemp":7,"weatherStr":""},
                        //      {"airConFrom":34,"airConTo":34,"highestTemp":9,"lowestTemp":4,"weatherStr":""},
                        //      {"airConFrom":34,"airConTo":34,"highestTemp":11,"lowestTemp":9,"weatherStr":""},
                        //      {"airConFrom":3,"airConTo":34,"highestTemp":14,"lowestTemp":9,"weatherStr":""},
                        //      {"airConFrom":34,"airConTo":3,"highestTemp":14,"lowestTemp":6,"weatherStr":""},
                        //      {"airConFrom":3,"airConTo":34,"highestTemp":9,"lowestTemp":4,"weatherStr":""},
                        //      {"airConFrom":3,"airConTo":1,"highestTemp":13,"lowestTemp":7,"weatherStr":""}
                        // ],
                        // "flag":48,"humidityStr":"62%","location":"Somewhere","sunriseHour":2,"sunriseMin":11,"sunsetHour":4,"sunsetMin":23,"timestamp":9999999999,"windStr":"SO"}
                    }

                } catch (e: JSONException) {
                    Logger.debug("[Weather API] Getting system weather data error (code continues assuming empty values): {}", e)
                }
            }

            // Check if new data have expired
            if (expire != null) {
                if (systemJsonData.has("time")) {
                    val systemDataTimestamp = json_data.getLong("time")
                    if (systemDataTimestamp > expire) return DATA_HAVE_EXPIRED
                }
            }

            // WeatherInfo & WeatherCheckedSummary
            if (json_data.has("tempUnit")) {
                val tempUnit = json_data.getString("tempUnit")
                systemJsonData.put("tempUnit", tempUnit)
                systemJsonDataShort.put("tempUnit", temperatureStringToUnit(tempUnit))
                if(mcu)
                    systemJsonDataMCU.put("tempUnit", temperatureStringToUnit(tempUnit))
            }
            /*if (json_data.has("tempUnitNo")){
                system_json_data_short.put("tempUnit", json_data.getString("tempUnitNo"));
            }*/
            if (json_data.has("tempFormatted")) {
                systemJsonData.put("tempFormatted", json_data.getString("tempFormatted"))
            }
            if (json_data.has("temp")) {
                systemJsonData.put("temp", json_data.getString("temp"))
                systemJsonDataShort.put("temp", json_data.getString("temp"))
                if(mcu)
                    systemJsonDataMCU.put("currentTemp", json_data.getString("temp"))
            }
            if (json_data.has("weatherCode")) {
                systemJsonData.put("weatherCode", json_data.getInt("weatherCode"))
                systemJsonDataShort.put("weatherCodeFrom", json_data.getInt("weatherCode"))
                if(mcu)
                    systemJsonDataMCU.put("currentAir", json_data.getInt("weatherCode"))
            }
            if (json_data.has("forecasts")) {
                systemJsonData.put("forecasts", json_data.getJSONArray("forecasts"))
                if (mcu)
                    systemJsonDataMCU.put("data", json_data.getJSONArray("forecasts")) // These data are wrong but are fixed with string replace before save
            }
            if (json_data.has("sd")) { // Humidity
                systemJsonData.put("sd", json_data.getString("sd"))
                if(mcu)
                    systemJsonDataMCU.put("humidityStr", json_data.getString("sd"))
            }
            if (json_data.has("windDirection")) {
                systemJsonData.put("windDirection", json_data.getString("windDirection"))
            }
            // Direction angle symbol is different after transfer, so we save the proper one (it doesn't matter anyway)
            systemJsonData.put("windDirectionUnit", "°")

            if (json_data.has("windDirectionValue")) {
                systemJsonData.put("windDirectionValue", json_data.getString("windDirectionValue"))
            }
            if (json_data.has("windSpeedUnit")) {
                systemJsonData.put("windSpeedUnit", json_data.getString("windSpeedUnit"))
            }
            if (json_data.has("windSpeedValue")) {
                systemJsonData.put("windSpeedValue", json_data.getString("windSpeedValue"))
            }
            if (json_data.has("windStrength")) {
                systemJsonData.put("windStrength", json_data.getString("windStrength"))
            }
            if (json_data.has("city")) {
                systemJsonData.put("city", json_data.getString("city"))
                if(mcu)
                    systemJsonDataMCU.put("location", json_data.getString("city"))
            }
            if (json_data.has("time")) {
                systemJsonData.put("time", json_data.getLong("time"))
                if(mcu)
                    systemJsonDataMCU.put("timestamp", json_data.getLong("time"))
            }

            // TODO Currently not updated
            // aqi":-1, "aqiLevel":0, "pm25":-1,
            // MCU: "AQIStr":"","AQIValue":-1,

            // New UV values in weather
            var uvIndex = -1
            if (json_data.has("uvIndex")) {
                uvIndex = json_data.getInt("uvIndex")
                systemJsonData.put("uvIndex", uvIndex)
                if(mcu)
                    systemJsonDataMCU.put("currentUVI", uvIndex)
            }
            if (json_data.has("uv")) {
                systemJsonData.put("uv", json_data.getString("uv"))
            } else if (uvIndex > -1) {
                systemJsonData.put("uv", uvIndexToString(uvIndex))
            }

            // New custom values in weather (these values don't exist by default in most watches)
            if (json_data.has("tempMin")) {
                systemJsonData.put("tempMin", json_data.getString("tempMin"))
            }
            if (json_data.has("tempMax")) {
                systemJsonData.put("tempMax", json_data.getString("tempMax"))
            }
            if (json_data.has("pressure")) {
                systemJsonData.put("pressure", json_data.getString("pressure"))
            }
            if (json_data.has("visibility")) {
                systemJsonData.put("visibility", json_data.getInt("visibility"))
            }
            if (json_data.has("clouds")) {
                systemJsonData.put("clouds", json_data.getString("clouds"))
            }
            if (json_data.has("sunrise")) {
                systemJsonData.put("sunrise", json_data.getInt("sunrise"))
                if(mcu){
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = json_data.getInt("sunrise")*1000L
                    systemJsonDataMCU.put("sunriseHour", cal.get(Calendar.HOUR))
                    systemJsonDataMCU.put("sunriseMin", cal.get(Calendar.MINUTE))
                }
            }
            if (json_data.has("sunset")) {
                systemJsonData.put("sunset", json_data.getInt("sunset"))
                if(mcu){
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = json_data.getInt("sunset")*1000L
                    systemJsonDataMCU.put("sunsetHour", cal.get(Calendar.HOUR))
                    systemJsonDataMCU.put("sunsetMin", cal.get(Calendar.MINUTE))
                }
            }
            if (json_data.has("actual_temp")) {
                systemJsonData.put("actual_temp", json_data.getInt("actual_temp"))
            }
            if (json_data.has("real_feel")) {
                systemJsonData.put("real_feel", json_data.getString("real_feel"))
            }
            if (json_data.has("country")) {
                systemJsonData.put("country", json_data.getString("country")) // country code
            }
            if (json_data.has("lat")) {
                systemJsonData.put("lat", json_data.getString("lat")) // latitude
            }
            if (json_data.has("lon")) {
                systemJsonData.put("lon", json_data.getString("lon")) // longitude
            }

            // Update data
            val data = systemJsonData.toString().replace("\\\\/".toRegex(), "/")
            Settings.System.putString(context.contentResolver, MAIN_URI, data)
            Settings.System.putString(context.contentResolver, SECONDARY_URI, systemJsonDataShort.toString())

            if (mcu) {
                val dataMCU = systemJsonDataMCU.toString()
                        .replace("weatherCodeFrom".toRegex(), "airConFrom")
                        .replace("weatherCodeTo".toRegex(), "airConTo")
                        .replace("tempMax".toRegex(), "highestTemp")
                        .replace("tempMin".toRegex(), "lowestTemp")
                Settings.System.putString(context.contentResolver, MCU_URI, dataMCU)
            }

            Logger.debug("[Weather API] Updated system weather data to: {}", data)
            //Logger.debug("Updating weather summary data: " + system_json_data_short.toString());

            // Return updated data
            return data
        } catch (e: JSONException) {
            // Default
            Logger.error("[Weather API] Updating system weather data error: {}", e)
            // Data haven't been updated
            return DATA_HAVE_NOT_UPDATE
        }
    }

    private fun uvIndexToString(uvIndex: Int): String {
        // TODO Translation
        return when {
            uvIndex <= 2 -> "Weakest"
            uvIndex <= 4 -> "Weak"
            uvIndex <= 6 -> "Moderate"
            uvIndex <= 9 -> "Strong"
            else -> "Very strong"
        }
    }

    private fun temperatureStringToUnit(tempUnit: String): Int {
        // Find temperature units number (0: C, 1: F, 2: K)
        return when (tempUnit) {
            "K" -> 2 // Kelvin, This is not supported by AmazfitWeather app
            "F" -> 1 // Fahrenheit
            else -> 0 // Celsius is the default value
        }
    }

    @JvmStatic
    public fun getWeatherMonitorParameter(): String {
        return if (isStratos3())
            MCU_URI
        else
            MAIN_URI
    }
}