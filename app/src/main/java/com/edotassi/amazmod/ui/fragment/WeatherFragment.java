package com.edotassi.amazmod.ui.fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.util.FilesUtil;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.pixplicity.easyprefs.library.Prefs;

import org.json.JSONObject;
import org.tinylog.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;

public class WeatherFragment extends Card {

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

    //private Context mContext;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //this.mContext = activity.getBaseContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather_card, container, false);

        ButterKnife.bind(this, view);

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
    int[] weatherIcons = new int[]{
            R.drawable.sunny, //0
            R.drawable.cloudy, //1
            R.drawable.cloudy,//overcast, //2
            R.drawable.fog, //..
            R.drawable.fog,//smog,
            R.drawable.shower,
            R.drawable.thunder_shower,
            R.drawable.shower,//light_rain,
            R.drawable.shower,//moderate_rain,
            R.drawable.heavy_rain,
            R.drawable.heavy_rain,//rainstorm,
            R.drawable.heavy_rain,//torrential_rain,
            R.drawable.sleet,
            R.drawable.freezing_rain,
            R.drawable.freezing_rain,//hail,
            R.drawable.light_snow,
            R.drawable.light_snow,//moderate_snow,
            R.drawable.heavy_snow,
            R.drawable.heavy_snow,//snowstorm,
            R.drawable.dust,
            R.drawable.dust,//blowing_sand, //..
            R.drawable.sand_storm, //21
            R.drawable.unknown //22
    };

    public void updateCard() {

        String last_saved_data = Prefs.getString(Constants.PREF_WEATHER_LAST_DATA, "");

        // Extract data
        try {
            // Extract data from JSON
            JSONObject last_data = new JSONObject(last_saved_data);

            if (last_data.has("tempUnit"))
                temperature_sign.setText("°"+last_data.getString("tempUnit"));
            if (last_data.has("temperature"))
                temperature.setText(last_data.getString("temperature"));
            if (last_data.has("real_feel") && last_data.has("tempUnit"))
                real_feel.setText(last_data.getString("real_feel")+"°"+last_data.getString("tempUnit"));
            if (last_data.has("humidity"))
                humidity.setText(last_data.getString("humidity"));
            if (last_data.has("pressure"))
                pressure.setText(last_data.getString("pressure"));
            if (last_data.has("code"))
                weather_image.setImageResource( weatherIcons[last_data.getInt("code")] );
            if (last_data.has("description"))
                status.setText(FilesUtil.capitalise(last_data.getString("description")));
            if (last_data.has("clouds"))
                cloud.setText(last_data.getString("clouds"));
            if (last_data.has("wind"))
                wind.setText(last_data.getString("wind"));
            if (last_data.has("city") && last_data.has("country"))
                location.setText(last_data.getString("city") +", "+ last_data.getString("country"));
            if (last_data.has("lon") && last_data.has("lat"))
                coordinates.setText("("+last_data.getString("lat") +", "+ last_data.getString("lon")+")");
        }catch (Exception e) {
            //Logger.error("WatchfaceDataReceiver JSON weather data failed: "+ e.getMessage());
        }

        // Write the last time data were taken
        Date lastDate = new Date(Prefs.getLong(Constants.PREF_TIME_LAST_CURRENT_WEATHER_DATA_SYNC, 0L));
        SimpleDateFormat format = new SimpleDateFormat("EEEE h:mm a", Locale.getDefault());
        last_read.setText( format.format(lastDate) );
    }
}
