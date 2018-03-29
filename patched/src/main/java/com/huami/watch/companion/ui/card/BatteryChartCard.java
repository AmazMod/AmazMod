package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.edotasx.amazfit.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;

/**
 * Created by edoardotassinari on 28/03/18.
 */

@DexAdd()
public class BatteryChartCard extends BaseCard {

    public static BatteryChartCard create(Activity activity) {
        return new BatteryChartCard(activity);
    }

    private BatteryChartCard(Activity activity) {
        super(activity);
    }

    @Override
    protected void clickView() {

    }

    @Override
    protected int getLayoutRes() {
        return R.layout.card_battery_chart;
    }

    @Override
    protected void initView() {
        LineChart chart = getView().findViewById(R.id.battery_chart);

        List<Entry> entries = new ArrayList<Entry>();

        for (int i = 0; i < 100; i++) {
            Entry entry = new Entry(i, (float) (Math.random() * 100));
            entries.add(entry);
        }

        LineDataSet lineDataSet = new LineDataSet(entries, "Battery");

        lineDataSet.setLineWidth(4);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawCircles(false);

        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.fade_blue_battery);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillDrawable(drawable);
        lineDataSet.setColor(Color.parseColor("#3F51B5"));

        Description description = new Description();
        description.setText("");
        chart.setDescription(description);

        chart.getXAxis().setEnabled(false);
        chart.getAxisLeft().setDrawLabels(false);
        chart.getAxisRight().setDrawLabels(false);


        chart.getLegend().setEnabled(false);

        LineData lineData = new LineData(lineDataSet);
        chart.setData(lineData);

        chart.invalidate();
    }

    @Override
    public String tag() {
        return "battery-chart";
    }
}
