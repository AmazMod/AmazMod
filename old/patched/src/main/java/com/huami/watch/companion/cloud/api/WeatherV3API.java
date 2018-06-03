package com.huami.watch.companion.cloud.api;

import android.content.Context;
import android.util.Log;

import com.huami.watch.companion.cloud.bean.CloudWeatherForecast;
import com.huami.watch.companion.cloud.bean.CloudWeatherRealtime;

import java.util.Date;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexReplace;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 21/02/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class WeatherV3API {

    @DexWrap
    public static CloudWeatherRealtime getWeatherRealtime(Context var0, String var1_3) {
        return getWeatherRealtime(var0, var1_3);
        /*
            CloudWeatherRealtime cloudWeatherRealtime = new CloudWeatherRealtime();
            CloudWeatherRealtime.Wind wind = new CloudWeatherRealtime.Wind();

            CloudWeatherRealtime.UnitValue windDirection = new CloudWeatherRealtime.UnitValue();
            windDirection.setValue("10");
            windDirection.setUnit("m/s");
            wind.setDirection(windDirection);
            wind.setSpeed(windDirection);

            cloudWeatherRealtime.setWind(wind);

            cloudWeatherRealtime.setHumidity(windDirection);
            cloudWeatherRealtime.setTemperature(windDirection);
            cloudWeatherRealtime.setWeather("16");
            cloudWeatherRealtime.setUvIndex("10");
            cloudWeatherRealtime.setWind(new CloudWeatherRealtime.Wind());

            return cloudWeatherRealtime;
            */
    }

    @DexWrap
    public static CloudWeatherForecast getWeatherForecast(Context var0, String var1_3, int var2_4) {
        return getWeatherForecast(var0, var1_3, var2_4);
        /*
        Log.d("Weather", "var1_3: " + var1_3 + ", var2_4: " + var2_4);

        CloudWeatherForecast cloudWeatherForecast = new CloudWeatherForecast();
        CloudWeatherForecast.Temperature temperature = new CloudWeatherForecast.Temperature();
        CloudWeatherForecast.Weather weather = new CloudWeatherForecast.Weather();

        CloudWeatherForecast.FromTo[] fromToList = new CloudWeatherForecast.FromTo[5];
        for(int i = 0; i < 5; i++) {
            CloudWeatherForecast.FromTo fromTo = new CloudWeatherForecast.FromTo();
            fromTo.setTo("16");
            fromTo.setFrom("10");

            fromToList[i] = fromTo;
        }


        temperature.setValue(fromToList);
        temperature.setStatus(20);
        temperature.setUnit("℃");

        weather.setValue(fromToList);
        weather.setStatus(50);


        cloudWeatherForecast.setWeather(weather);
        cloudWeatherForecast.setTemperature(temperature);

        return cloudWeatherForecast;
        */
    }
}
