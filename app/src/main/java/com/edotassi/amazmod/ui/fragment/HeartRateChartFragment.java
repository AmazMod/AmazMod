package com.edotassi.amazmod.ui.fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.ui.card.Card;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;

public class HeartRateChartFragment extends Card {

    @BindView(R.id.card_heartrate_last_read)
    TextView heartrate_lastRead;

    @BindView(R.id.heartrate_chart)
    BarChart heartrateChart;

    //private Context mContext;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //this.mContext = activity.getBaseContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_heartrate_chart, container, false);

        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.d(Constants.TAG, "HeartRateChartFragment onResume");
    }

    @Override
    public String getName() {
        return "heart-rate-chart";
    }

    public void updateChart(String lastHeartRates) {
        // Check if string is correct
        if (!lastHeartRates.contains(","))
            return;

        // Split string to data
        String[] parts = lastHeartRates.split(",");

        // Check if there are actual data
        if(parts.length<2)
            return;

        // Create chart entries
        List<BarEntry> entries = new ArrayList<>();
        // Loop every 2 data (because data are: timestapm, value, timestamp, value...)
        for (int i = 0; i < parts.length - 1; i = i + 2) {
            // code for: time, heart-rate
            // entries.add(new BarEntry(Integer.parseInt(parts[i]), Integer.parseInt(parts[i+1])));
            // code for: i, heart-rate
            entries.add(new BarEntry(i, Integer.parseInt(parts[i+1])));
        }

        BarDataSet set = new BarDataSet(entries, getResources().getString(R.string.heartrate_chart_title));
        set.setColor(Color.RED);

        Description description = new Description();
        description.setText("");
        heartrateChart.setDescription(description);

        heartrateChart.getXAxis().setDrawLabels(false);
        heartrateChart.getAxisRight().setDrawLabels(false);

        BarData data = new BarData(set);
        data.setBarWidth(0.9f); // set custom bar width
        heartrateChart.setData(data);
        heartrateChart.setFitBars(true); // make the x-axis fit exactly all bars
        heartrateChart.getLegend().setEnabled(false); // hide legend
        heartrateChart.invalidate(); // refresh

        // Write the last time data were taken
        Date lastDate = new Date(Long.parseLong(parts[parts.length-2])*1000);
        Log.d(Constants.TAG, "WatchHeartRateFragment lastDate: " + lastDate);
        Calendar calendarLastDate = Calendar.getInstance();
        calendarLastDate.setTime(lastDate);
        Calendar calendarToday = Calendar.getInstance();
        calendarToday.setTime(new Date());
        String textDate = getResources().getText(R.string.last_read) + ": ";
        // add time
        textDate += DateFormat.getTimeInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);
        if (calendarLastDate.get(Calendar.DAY_OF_MONTH) != calendarToday.get(Calendar.DAY_OF_MONTH) ) {
            // add date
            textDate += " " + DateFormat.getDateInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);;
        }
        heartrate_lastRead.setText(textDate);
    }
}
