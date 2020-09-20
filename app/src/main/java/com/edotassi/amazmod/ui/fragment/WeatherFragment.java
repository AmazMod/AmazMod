package com.edotassi.amazmod.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.databinding.FragmentWeatherCardBinding;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.util.FilesUtil;
import com.pixplicity.easyprefs.library.Prefs;

import org.json.JSONObject;
import org.tinylog.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import amazmod.com.transport.Constants;

public class WeatherFragment extends Card {
/*
    @BindView(R.id.card_weather_location)
    TextView location;
    @BindView(R.id.card_weather_coordinates)
    TextView coordinates;
    @BindView(R.id.card_weather_last_read)
    TextView last_read;
    @BindView(R.id.card_weather_status)
    TextView status;
    @BindView(R.id.card_weather_temperature)
    TextView temperature;
    @BindView(R.id.card_weather_temperature_sign)
    TextView temperature_sign;
    @BindView(R.id.card_weather_humidity_value)
    TextView humidity;
    @BindView(R.id.card_weather_pressure_value)
    TextView pressure;
    @BindView(R.id.card_weather_wind_value)
    TextView wind;
    @BindView(R.id.card_weather_cloud_value)
    TextView cloud;
    @BindView(R.id.card_weather_real_feel_value)
    TextView real_feel;
    @BindView(R.id.card_weather_image)
    ImageView weather_image;
    @BindView(R.id.card_weather_big_image)
    ImageView weather_big_image;
    @BindView(R.id.card_weather)
    CardView card_weather;
*/
    //private Context mContext;
    private FragmentWeatherCardBinding binding;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWeatherCardBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateCard();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateCard();
    }

    @Override
    public String getName() {
        return "weather-card";
    }

    // Weather icons
    private int[] weatherIcons = new int[]{
            R.drawable.sunny, //0
            R.drawable.cloudy, //1
            R.drawable.overcast, //2
            R.drawable.fog, //..
            R.drawable.fog,//smog,
            R.drawable.shower,
            R.drawable.thunder_shower,
            R.drawable.light_rain,
            R.drawable.moderate_rain,
            R.drawable.heavy_rain,
            R.drawable.heavy_rain,//rainstorm,
            R.drawable.heavy_rain,//torrential_rain,
            R.drawable.sleet,
            R.drawable.freezing_rain,
            R.drawable.freezing_rain,//hail,
            R.drawable.light_snow,
            R.drawable.moderate_snow,
            R.drawable.heavy_snow,
            R.drawable.heavy_snow,//snowstorm,
            R.drawable.dust,
            R.drawable.blowing_sand, //..
            R.drawable.sand_storm, //21
            R.drawable.unknown //22
    };

    private void updateCard() {
        String last_saved_data = Prefs.getString(Constants.PREF_WEATHER_LAST_DATA, "");
        //Logger.debug("[Weather Card] JSON weather data: {}", last_saved_data);

        if( last_saved_data == null || last_saved_data.isEmpty() ) {
            binding.cardWeather.setVisibility(View.GONE);
            return;
        }

        // Extract data
        try {
            // Extract data from JSON
            JSONObject last_data = new JSONObject(last_saved_data);

            if (last_data.has("tempUnit"))
                binding.cardWeatherTemperatureSign.setText("°"+last_data.getString("tempUnit"));
            if (last_data.has("actual_temp"))
                binding.cardWeatherTemperature.setText(last_data.getString("actual_temp"));
            if (last_data.has("real_feel") && last_data.has("tempUnit"))
                binding.cardWeatherRealFeelValue.setText(last_data.getString("real_feel")+"°"+last_data.getString("tempUnit"));
            if (last_data.has("sd")) // humidity
                binding.cardWeatherHumidityValue.setText(last_data.getString("sd"));
            if (last_data.has("pressure"))
                binding.cardWeatherPressureValue.setText(last_data.getString("pressure"));
            if (last_data.has("weatherCode")){
                binding.cardWeatherImage.setImageResource( weatherIcons[last_data.getInt("weatherCode")] );
                binding.cardWeatherBigImage.setImageResource( weatherIcons[last_data.getInt("weatherCode")] );
            }
            if (last_data.has("weatherDescription"))
                binding.cardWeatherStatus.setText(FilesUtil.capitalise(last_data.getString("weatherDescription")));
            if (last_data.has("clouds"))
                binding.cardWeatherCloudValue.setText(last_data.getString("clouds"));
            if (last_data.has("windStrength"))
                binding.cardWeatherWindValue.setText(last_data.getString("windStrength"));
            if (last_data.has("city") && last_data.has("country"))
                binding.cardWeatherLocation.setText(last_data.getString("city") +", "+ last_data.getString("country"));
            if (last_data.has("lon") && last_data.has("lat"))
                binding.cardWeatherCoordinates.setText("("+last_data.getString("lat") +", "+ last_data.getString("lon")+")");
        }catch (Exception e) {
            Logger.error("[Weather Card] JSON weather data failed: {}", e.getMessage());
            binding.cardWeather.setVisibility(View.GONE);
            return;
        }

        // Write the last time data were taken
        Date lastDate = new Date(Prefs.getLong(Constants.PREF_TIME_LAST_CURRENT_WEATHER_DATA_SYNC, 0L));
        SimpleDateFormat format = new SimpleDateFormat("EEEE H:mm", Locale.getDefault());
        binding.cardWeatherLastRead.setText( format.format(lastDate) );
    }
}
