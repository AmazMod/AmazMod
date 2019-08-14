package com.amazmod.service.ui;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.db.model.BatteryDbEntity;
import com.amazmod.service.db.model.BatteryDbEntity_Table;
import com.amazmod.service.springboard.WidgetSettings;
import com.amazmod.service.util.DeviceUtil;
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
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.tinylog.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BatteryGraphActivity extends ListActivity {

    private LineChart chart;
    private TextView batteryValue, graphRange;
    private ListView listView;

    private Context mContext;

    private WidgetSettings widgetSettings;

    private static final long DOUBLE_CLICK_TIME_DELTA = 300; //milliseconds
    private long lastClickTime = 0;
    private static int days;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.debug("BatteryGraphActivity onCreate");

        this.mContext = this;

        setContentView(R.layout.activity_battery_graph);

        View frameLayout = findViewById(R.id.activity_battery_graph_frame_layout);
        View batteryLayout = findViewById(R.id.activity_battery_days_layout);
        listView = findViewById(android.R.id.list);
        chart = findViewById(R.id.activity_battery_graph_chart);
        batteryValue = findViewById(R.id.activity_battery_value);
        graphRange = findViewById(R.id.activity_battery_days);

        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                    onDoubleClick(v);
                } else {
                    onSingleClick(v);
                }
                lastClickTime = clickTime;
            }
        });

        batteryValue.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (DeviceUtil.saveBatteryData(mContext, true)) {
                    Toast.makeText(mContext, "Updated", Toast.LENGTH_SHORT).show();
                    final Handler mHandler = new Handler();
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            updateChart();
                        }
                    }, 1000);
                }
                return true;
            }
        });

        batteryValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimeSinceLastCharge();
            }
        });

        batteryLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                graphRange.setVisibility(View.GONE);
                batteryValue.setVisibility(View.GONE);
                chart.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                showListView();
                return true;
            }
        });

        //Initialize settings
        widgetSettings = new WidgetSettings(Constants.TAG, mContext);

        listView.setVisibility(View.GONE);
        updateChart();

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Logger.debug("BatteryGraphActivity onListItemClick position: " + position);

        if (position >= 21) {
            listView.setVisibility(View.GONE);
            batteryValue.setVisibility(View.VISIBLE);
            chart.setVisibility(View.VISIBLE);
            graphRange.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Logger.debug("BatteryGraphActivity onResume");

        updateChart();

    }

    public void onSingleClick(View v) {
        //
    }

    public void onDoubleClick(View v) {

        Logger.debug("BatteryGraphActivity onDoubleClick days: " + days);

        if (days < 5)
            days++;
        else
            days = 1;

        widgetSettings.set(Constants.PREF_BATTERY_GRAPH_DAYS, days);
        updateChart();
    }

    private void showListView() {

        List<BatteryDbEntity> batteryReadList = SQLite
                .select()
                .from(BatteryDbEntity.class)
                .orderBy(BatteryDbEntity_Table.date.desc())
                .limit(20)
                .queryList();

        List<String> list = new ArrayList<>();
        list.add(String.format("Last 20 Values [%sd]", String.valueOf(days)));
        for (BatteryDbEntity read : batteryReadList) {
            @SuppressLint("SimpleDateFormat") final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM HH:mm");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(read.getDate());
            int level = (int) (read.getLevel() * 100f);
            //Logger.debug("BatteryGraphActivity showListView level: " + level);
            list.add(String.format("%s - %s", simpleDateFormat.format(calendar.getTime()), String.valueOf(level)));
        }
        list.add("[Close List]");

        setListAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list));
    }

    private void updateChart() {

        widgetSettings.reload();
        batteryValue.setText(widgetSettings.get(Constants.PREF_BATT_LEVEL, "N/A%"));
        days = widgetSettings.get(Constants.PREF_BATTERY_GRAPH_DAYS, 5);

        final List<Entry> yValues = new ArrayList<>();
        final List<Integer> colors = new ArrayList<>();
        //final List<Entry> yPredictValues = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        long highX = calendar.getTimeInMillis();

        calendar.add(Calendar.DATE, -1 * days);

        long lowX = calendar.getTimeInMillis();

        List<BatteryDbEntity> batteryReadList = SQLite
                .select()
                .from(BatteryDbEntity.class)
                .where(BatteryDbEntity_Table.date.greaterThan(lowX))
                .queryList();

        BatteryDbEntity prevRead = null;
        int size = batteryReadList.size();

        Logger.debug("BatteryGraphActivity updateChart size: " + size);

        final int primaryColor = ContextCompat.getColor(mContext, R.color.colorGraph);
        final int chargingColor = ContextCompat.getColor(mContext, R.color.colorCharging);
        final int fullyChargedColor = ContextCompat.getColor(mContext, R.color.colorPrimary);
        final int white = ContextCompat.getColor(mContext, R.color.white);
        final int semitransparent = ContextCompat.getColor(mContext, R.color.semitransparent);
        boolean wasNotCharging = true;

        for (int i = 0; i < batteryReadList.size(); i++) {
            BatteryDbEntity read = batteryReadList.get(i);
            int level = (int) (read.getLevel() * 100f);
            int prevLevel = prevRead == null ? 100 : ((int) (prevRead.getLevel() * 100f));
            if (level > 0) {
                Entry entry = new Entry(read.getDate(), level);
                yValues.add(entry);
                int lineColor = level > prevLevel ? chargingColor : primaryColor;

                if (wasNotCharging && (level > prevLevel)) {
                    colors.set(i - 1, chargingColor);
                    wasNotCharging = false;
                } else if ((prevLevel >= level) && (!wasNotCharging))
                    wasNotCharging = true;

                if (level > 99)
                    lineColor = fullyChargedColor;
                else if (level == prevLevel)
                    lineColor = colors.get(i - 1);

                /* Used for debugging
                *
                String color;
                if (lineColor == primaryColor)
                    color = "P";
                else if (lineColor == chargingColor)
                    color = "C";
                else if (lineColor == fullyChargedColor)
                    color = "F";
                else
                    color = "NULL";
                Logger.debug("BatteryGraphActivity updateChart prevLevel: " + prevLevel +
                        " \\ level: " + level + " \\ lineColor : " + color);
                */

                colors.add(lineColor);
            }

            prevRead = read;
        }

        size = yValues.size();

        LineDataSet lineDataSet = new LineDataSet(yValues, "Battery");

        lineDataSet.setLineWidth(1.5f);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);

        //Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.fade_gray_battery);
        //lineDataSet.setDrawFilled(true);
        lineDataSet.setDrawFilled(false);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        //lineDataSet.setFillDrawable(drawable);
        lineDataSet.setColors(colors);
        lineDataSet.setMode(LineDataSet.Mode.LINEAR);
        lineDataSet.setCubicIntensity(0.05f);

        chart.setNoDataText("No Battery data yet…");
        chart.setNoDataTextColor(white);

        graphRange.setText(String.format("[x] = %sd", String.valueOf(days)));

        if (size == 0) {
            return;
        }

        Description description = new Description();
        String textDate;

        if (size < 2) {

            textDate = "Waiting for battery data…";

        } else {

            // PREDICT BATTERY, calculate values
            int value = 0; // use first data for calculation if never charged
            int colorsSize = colors.size();
            boolean charging = false;

            if (colors.get(colorsSize -1) == fullyChargedColor) {

                textDate = "Fully Charged";

            } else {

                // Get last charging point
                for (int i = colorsSize - 1; i >= 0; i--) {
                    if ((colors.get(i) == chargingColor) || (colors.get(i) == fullyChargedColor)) {
                        value = i;
                        break;
                    }
                }

                // Still charging?
                if ((value == colorsSize - 1) && (colors.get(value) == chargingColor)) {
                    charging = true;
                    value = 0; // use first data for calculation if never charged
                    for (int i = colorsSize - 1; i >= 0; i--) {
                        if (colors.get(i) == primaryColor) {
                            value = i;
                            break;
                        }
                    }
                }

                // Add last battery point as first data point
                //yPredictValues.add(yValues.get(yValues.size() - 1));

                // At least 2 data points are needed for the calculation
                // Calculate future 0 or 100% Battery point
                // Charging point
                float x1 = yValues.get(value).getX();
                float y1 = yValues.get(value).getY();
                // Last measure point
                float x2 = yValues.get(size - 1).getX();
                float y2 = yValues.get(size - 1).getY();

                float target_time;
                float remaininf_now_diff;
                if (charging) {
                    // Future time that battery will be 100%
                    target_time = x2 + (x2 - x1) / (y2 - y1) * (100 - y2);

                    textDate = "Charged in: ";
                    remaininf_now_diff = (target_time - System.currentTimeMillis()) / (1000 * 60);
                    textDate += ((int) remaininf_now_diff / 60) + "h:" + ((int) remaininf_now_diff % 60) + "m";

                } else {
                    // Future time that battery will be 0%
                    target_time = x2 + (x2 - x1) / (y1 - y2) * y2;

                    textDate = "Remaining: ";
                    remaininf_now_diff = (target_time - System.currentTimeMillis()) / (1000 * 60 * 60);
                    textDate += ((int) remaininf_now_diff / 24) + "d:" + ((int) remaininf_now_diff % 24) + "h";

                }
            }
        }

        description.setText(textDate);

        description.setTextSize(16);
        description.setTextColor(semitransparent);
        //description.setTextAlign(Paint.Align.LEFT);
        //description.setXOffset(-180f);
        //description.setYOffset(-36f);
        chart.setDescription(description);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setTextSize(12);
        xAxis.setTextColor(white);
        xAxis.setAxisMinimum(lowX);
        xAxis.setAxisMaximum(highX);
        xAxis.setLabelCount(6, true);

        //final Calendar now = Calendar.getInstance();
        //final SimpleDateFormat simpleDateFormatHours = new SimpleDateFormat("HH");
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat simpleDateFormatHoursMinutes = new SimpleDateFormat("HH:mm");
        //final SimpleDateFormat simpleDateFormatDateMonth = new SimpleDateFormat("dd/MM");

        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis((long) value);

                return simpleDateFormatHoursMinutes.format(calendar.getTime());

            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawZeroLine(false);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0);
        leftAxis.setAxisMaximum(100);
        leftAxis.setDrawLabels(false);
        leftAxis.setLabelCount(5, true);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);

        chart.setXAxisRenderer(new CustomXAxisRenderer(chart.getViewPortHandler(), chart.getXAxis(), chart.getTransformer(YAxis.AxisDependency.LEFT)));

        LineData lineData = new LineData(lineDataSet);
        chart.setData(lineData);

        chart.invalidate();
    }

    private class CustomXAxisRenderer extends XAxisRenderer {
        CustomXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
            super(viewPortHandler, xAxis, trans);
        }

        @Override
        protected void drawLabel(Canvas c, String formattedLabel, float x, float y, MPPointF anchor, float angleDegrees) {
            String[] line = formattedLabel.split("\n");
            if (line.length > 0) {
                Utils.drawXAxisValue(c, line[0], x, y, mAxisLabelPaint, anchor, angleDegrees);

                if (line.length > 1) {
                    Utils.drawXAxisValue(c, line[1], x + mAxisLabelPaint.getTextSize(), y + mAxisLabelPaint.getTextSize(), mAxisLabelPaint, anchor, angleDegrees);
                }
            }
        }
    }

    private void showTimeSinceLastCharge() {
        widgetSettings.reload();
        //Get date of last full charge
        long lastChargeDate = widgetSettings.get(Constants.PREF_DATE_LAST_CHARGE, 0L);

        StringBuilder dateDiff = new StringBuilder("");
        if (lastChargeDate != 0L) {
            long diffInMillies = System.currentTimeMillis() - lastChargeDate;
            List<TimeUnit> units = new ArrayList<>(EnumSet.allOf(TimeUnit.class));
            Collections.reverse(units);
            long millisRest = diffInMillies;
            for (TimeUnit unit : units) {
                long diff = unit.convert(millisRest, TimeUnit.MILLISECONDS);
                long diffInMilliesForUnit = unit.toMillis(diff);
                millisRest = millisRest - diffInMilliesForUnit;
                if (unit.equals(TimeUnit.DAYS)) {
                    dateDiff.append(diff).append("d : ");
                } else if (unit.equals(TimeUnit.HOURS)) {
                    dateDiff.append(diff).append("h : ");
                } else if (unit.equals(TimeUnit.MINUTES)) {
                    dateDiff.append(diff).append("min");
                    break;
                }
            }
            dateDiff.append("\n").append(mContext.getResources().getText(R.string.last_charge));
        } else dateDiff.append(mContext.getResources().getText(R.string.last_charge_no_info));

        final String timeSLC = dateDiff.toString();
        Toast.makeText(mContext, timeSLC, Toast.LENGTH_LONG).show();
    }

}
