package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.db.model.BatteryRead;
import com.edotasx.amazfit.db.model.BatteryRead_Table;
import com.edotasx.amazfit.events.BatteryHistoryUpdatedEvent;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.huami.watch.companion.util.RxBus;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.functions.Consumer;
import lanchon.dexpatcher.annotation.DexAdd;

/**
 * Created by edoardotassinari on 28/03/18.
 */

@DexAdd()
public class BatteryChartCard extends BaseCard {

    private LineChart chart;

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
        chart = getView().findViewById(R.id.battery_chart);

        RxBus.get().toObservable().subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) {
                if (o instanceof BatteryHistoryUpdatedEvent) {
                    updateChart();
                }
            }
        });

        updateChart();
    }

    private void updateChart() {
        final List<Entry> yValues = new ArrayList<Entry>();
        final List<Integer> colors = new ArrayList<>();

        final int days = PreferenceManager.getInt(getContext(), Constants.PREFERENCE_BATTERY_CHART_RANGE, 5);

        Calendar calendar = Calendar.getInstance();
        long highX = calendar.getTimeInMillis();

        calendar.add(Calendar.DATE, -1 * days);

        long lowX = calendar.getTimeInMillis();

        List<BatteryRead> batteryReadList = SQLite
                .select()
                .from(BatteryRead.class)
                .where(BatteryRead_Table.date.greaterThan(lowX))
                .queryList();

        BatteryRead prevRead = null;
        for (int i = 0; i < batteryReadList.size(); i++) {
            BatteryRead read = batteryReadList.get(i);
            if ((read.getLevel() > 0) && ((prevRead == null) || (read.getLevel() != prevRead.getLevel()))) {
                Entry entry = new Entry(read.getDate(), read.getLevel());
                yValues.add(entry);

                colors.add(Color.parseColor(read.isCharging() ? "#00E676" : "#3F51B5"));
            }

            prevRead = read;
        }

        if (yValues.size() == 0) {
            return;
        }

        LineDataSet lineDataSet = new LineDataSet(yValues, "Battery");

        lineDataSet.setLineWidth(1.5f);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);

        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.fade_blue_battery);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setFillDrawable(drawable);
        lineDataSet.setColors(colors);
        lineDataSet.setMode(LineDataSet.Mode.LINEAR);
        lineDataSet.setCubicIntensity(0.05f);

        Description description = new Description();
        description.setText("");
        chart.setDescription(description);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextSize(8);
        xAxis.setAxisMinimum(lowX);
        xAxis.setAxisMaximum(highX);

        final Calendar now = Calendar.getInstance();
        final SimpleDateFormat simpleDateFormatHours = new SimpleDateFormat("HH");
        final SimpleDateFormat simpleDateFormatHoursMinutes = new SimpleDateFormat("HH:mm");
        final SimpleDateFormat simpleDateFormatDateMonth = new SimpleDateFormat("dd/MM");

        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis((long) value);

                Date date = calendar.getTime();

                if ((days > 1) || (now.get(Calendar.DATE) != calendar.get(Calendar.DATE))) {
                    int minutes = calendar.get(Calendar.MINUTE);
                    if (minutes > 30) {
                        calendar.add(Calendar.HOUR, 1);
                    }

                    return simpleDateFormatHours.format(date) + "\n" + simpleDateFormatDateMonth.format(date);
                } else {
                    return simpleDateFormatHoursMinutes.format(calendar.getTime()) + "\n" + simpleDateFormatDateMonth.format(calendar.getTime());
                }

            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawAxisLine(true);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0);
        leftAxis.setAxisMaximum(100);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);

        chart.setXAxisRenderer(new CustomXAxisRenderer(chart.getViewPortHandler(), chart.getXAxis(), chart.getTransformer(YAxis.AxisDependency.LEFT)));

        LineData lineData = new LineData(lineDataSet);
        chart.setData(lineData);

        chart.invalidate();
    }

    @Override
    public String tag() {
        return "battery-chart";
    }

    private class CustomXAxisRenderer extends XAxisRenderer {
        public CustomXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
            super(viewPortHandler, xAxis, trans);
        }

        @Override
        protected void drawLabel(Canvas c, String formattedLabel, float x, float y, MPPointF anchor, float angleDegrees) {
            String line[] = formattedLabel.split("\n");
            if (line.length > 0) {
                Utils.drawXAxisValue(c, line[0], x, y, mAxisLabelPaint, anchor, angleDegrees);

                if (line.length > 1) {
                    Utils.drawXAxisValue(c, line[1], x + mAxisLabelPaint.getTextSize(), y + mAxisLabelPaint.getTextSize(), mAxisLabelPaint, anchor, angleDegrees);
                }
            }
        }
    }
}
