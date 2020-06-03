package com.edotassi.amazmod.ui.fragment;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.databinding.FragmentHeartrateChartBinding;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.ui.card.Card;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.tinylog.Logger;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class HeartRateChartFragment extends Card {

    //private Context mContext;
    private FragmentHeartrateChartBinding binding;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //this.mContext = activity.getBaseContext();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHeartrateChartBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        binding.heartrateChart.setNoDataText(getString(R.string.pref_heartrate_nodata));

    }

    @Override
    public void onResume() {
        super.onResume();
        //Logger.debug("HeartRateChartFragment onResume");
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
        if (parts.length < 2)
            return;

        // Create chart entries
        List<BarEntry> entries = new ArrayList<>();
        // Loop every 2 data (because data are: timestapm, value, timestamp, value...)
        for (int i = 0; i < parts.length - 1; i = i + 2) {
            // code for: time, heart-rate
            // entries.add(new BarEntry(Integer.parseInt(parts[i]), Integer.parseInt(parts[i+1])));
            // code for: i, heart-rate
            entries.add(new BarEntry(i, Integer.parseInt(parts[i + 1])));
        }

        BarDataSet set = new BarDataSet(entries, getResources().getString(R.string.heartrate_chart_title));
        set.setColor(Color.RED);

        Description description = new Description();
        description.setText("");
        binding.heartrateChart.setDescription(description);
        final int themeForegroundColor = ThemeHelper.getThemeForegroundColor(Objects.requireNonNull(getContext()));

        binding.heartrateChart.getXAxis().setDrawLabels(false);
        binding.heartrateChart.getXAxis().setTextColor(themeForegroundColor);
        binding.heartrateChart.getAxisLeft().setTextColor(themeForegroundColor);
        binding.heartrateChart.getAxisRight().setDrawLabels(false);
        BarData data = new BarData(set);
        data.setBarWidth(0.9f); // set custom bar width
        data.setValueTextColor(themeForegroundColor);
        binding.heartrateChart.setData(data);
        binding.heartrateChart.setFitBars(true); // make the x-axis fit exactly all bars
        binding.heartrateChart.getLegend().setEnabled(false); // hide legend
        binding.heartrateChart.invalidate(); // refresh

        // Write the last time data were taken
        Date lastDate = new Date(Long.parseLong(parts[parts.length - 2]) * 1000);
        Logger.debug("WatchHeartRateFragment lastDate: " + lastDate);
        Calendar calendarLastDate = Calendar.getInstance();
        calendarLastDate.setTime(lastDate);
        Calendar calendarToday = Calendar.getInstance();
        calendarToday.setTime(new Date());
        String textDate = getResources().getText(R.string.last_read) + ": ";
        // add time
        textDate += DateFormat.getTimeInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);
        if (calendarLastDate.get(Calendar.DAY_OF_MONTH) != calendarToday.get(Calendar.DAY_OF_MONTH)) {
            // add date
            textDate += " " + DateFormat.getDateInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);
        }
        binding.cardHeartrateLastRead.setText(textDate);
    }
}
