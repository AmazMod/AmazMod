package com.edotassi.amazmod.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.BatteryStatusEntity_Table;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.support.DownloadHelper;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.util.Permissions;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.tinylog.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;

public class BatteryChartFragment extends Card {

    @BindView(R.id.card_battery_last_read)
    TextView lastRead;
    @BindView(R.id.textView2)
    TextView batteryTv;
    @BindView(R.id.imageView2)
    ImageView imageView;
    @BindView(R.id.card_battery)
    CardView cardView;

    @BindView(R.id.battery_chart)
    LineChart chart;

    private Context mContext;
    private static long lastDateChart;
    private static boolean sendNewRequest, requestSent;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        sendNewRequest = false;
        requestSent = false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_battery_chart, container, false);

        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();

        cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                updateChart();
                new MaterialDialog.Builder(activity)

                        .title(activity.getString(R.string.battery_fragment_select_option))
                        .items(new String[]{
                                getResources().getString(R.string.batter_data_request),
                                getResources().getString(R.string.batter_data_export)
                        })
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                switch (position) {
                                    case 0:
                                        SendNewRequest();
                                        break;
                                    case 1:
                                        ExportBatteryStats(activity);
                                        break;
                                }
                            }
                        }).show();
                return true;
            }
        });

        updateChart();
    }

    private void ExportBatteryStats(Activity activity) {
        final int days = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(Constants.PREF_BATTERY_CHART_TIME_INTERVAL, Constants.PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL));


        if (!Permissions.hasPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1 * days);
        long lowX = calendar.getTimeInMillis();

        List<BatteryStatusEntity> batteryReadList = SQLite
                .select()
                .from(BatteryStatusEntity.class)
                .where(BatteryStatusEntity_Table.date.greaterThan(lowX))
                .queryList();

        if (batteryReadList.size() <= 0) {
            // no data
            Toast.makeText(mContext, getResources().getString(R.string.activity_batter_no_data), Toast.LENGTH_SHORT).show();
        } else {
            try {
                String downloadDir = DownloadHelper.getDownloadDir(Constants.MODE_DOWNLOAD);
                String pattern = "yyyy_MM_dd";
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.US);
                String fName = "battery_data_" + simpleDateFormat.format(new Date()) + ".csv";
                String file_csv = downloadDir + File.separator + fName;

                File fOut = new File(file_csv);

                if (fOut.exists()) {
                    fOut.delete();
                }

                // https://stackoverflow.com/questions/15402976/how-to-create-a-csv-file-in-android
                new Thread() {
                    public void run() {
                        try {

                            FileWriter fw = new FileWriter(fOut);
                            BatteryStatusItemToCsv(fw, null);
                            for (int i = 0; i < batteryReadList.size(); i++) {
                                BatteryStatusEntity item = batteryReadList.get(i);
                                BatteryStatusItemToCsv(fw, item);
                            }
                            fw.close();

                            Intent newIntent = new Intent(Intent.ACTION_VIEW);
                            Uri path = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                                    FileProvider.getUriForFile(mContext, Constants.FILE_PROVIDER, fOut)
                                    : Uri.fromFile(fOut);
                            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("csv");

                            newIntent.setDataAndType(path, mimeType);
                            newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                startActivity(newIntent);
                            } catch (ActivityNotFoundException e) {
                                Logger.error(e, "Error opening file");
                                Toast.makeText(mContext, String.format(getString(R.string.battery_error_open_csv), fName), Toast.LENGTH_LONG).show();
                            }

//                            //https://stackoverflow.com/questions/3134683/android-toast-in-a-thread
//                            assert activity != null;
//                            activity.runOnUiThread(() -> Toast.makeText(activity,
//                                    String.format(getString(R.string.battery_export_success), fName),
//                                    Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            Logger.error(e, "Fail thread export data");
                            Toast.makeText(mContext, getString(R.string.batter_error_export), Toast.LENGTH_LONG).show();
                        }
                    }
                }.start();
            } catch (Exception e) {
                Logger.error(e, "BatteryChartFragment onClick export");
            }
        }
    }

    private void SendNewRequest() {
        updateChart();
        Logger.debug("BatteryChartFragment onLongCLick sendNewRequest: " + sendNewRequest);
        if (sendNewRequest) {
            if (!requestSent) {
                requestSent = true;
                Toast.makeText(mContext, mContext.getResources().getString(R.string.battery_chart_request), Toast.LENGTH_SHORT).show();
                Intent i = new Intent("com.edotassi.amazmod.USER_ACTION");
                mContext.getApplicationContext().sendBroadcast(i);
                Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        updateChart();
                    }
                }, 5000);
            } else {
                Toast.makeText(mContext, mContext.getResources().getString(R.string.battery_chart_waiting), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, mContext.getResources().getString(R.string.battery_chart_updated), Toast.LENGTH_SHORT).show();
            requestSent = false;
        }
    }

    /**
     * Dump entity to stream
     *
     * @param fw   stream where write data
     * @param item to serialize
     * @throws IOException
     */
    private void BatteryStatusItemToCsv(FileWriter fw, BatteryStatusEntity item)
            throws IOException {
        String pattern = "yyyy-MM-dd HH:mm:ss";
        String separator = ",";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern, Locale.US);
        if (item == null) {
            fw.append(String.format("Date%1$sLevel%1$sUsb%1$sAc%1$sLastCharge%1$s\n", separator));
            return;
        }

        fw.append(dateFormatter.format(item.getDate()));
        fw.append(separator);

        fw.append(String.format(Locale.US, "%.2f", item.getLevel() * 100));
        fw.append(separator);

        fw.append(String.format(Locale.US, "%b", item.isUsbCharge()));
        fw.append(separator);

        fw.append(String.format(Locale.US, "%b", item.isAcCharge()));
        fw.append(separator);

        fw.append(dateFormatter.format(item.getDateLastCharge()));
        fw.append(separator);

        fw.append("\n");
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.debug("BatteryChartFragment onResume");

        updateChart();
    }

    @Override
    public String getName() {
        return "battery-chart";
    }

    private void updateChart() {
        final List<Entry> yValues = new ArrayList<>();
        final List<Integer> colors = new ArrayList<>();
        final List<Entry> yPredictValues = new ArrayList<>();

        //Cast number of days shown in chart from Preferences
        final int days = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(Constants.PREF_BATTERY_CHART_TIME_INTERVAL, Constants.PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL));

        Calendar calendar = Calendar.getInstance();
        long highX = calendar.getTimeInMillis();

        calendar.add(Calendar.DATE, -1 * days);

        long lowX = calendar.getTimeInMillis();

        List<BatteryStatusEntity> batteryReadList = SQLite
                .select()
                .from(BatteryStatusEntity.class)
                .where(BatteryStatusEntity_Table.date.greaterThan(lowX))
                .queryList();

        if (batteryReadList.size() > 0) {
            BatteryStatusEntity lastEntity = batteryReadList.get(batteryReadList.size() - 1);
            Date lastDate = new Date(lastEntity.getDate());
            if (lastDateChart != lastEntity.getDate() || lastDateChart == 0) {
                lastDateChart = lastEntity.getDate();
                sendNewRequest = false;
            } else {
                sendNewRequest = true;
            }

            long lastChargeDate = lastEntity.getDateLastCharge();
            StringBuilder dateDiff = new StringBuilder();
            String append = Integer.toString(Math.round(lastEntity.getLevel() * 100f)) + "% / ";
            dateDiff.append(append);
            if (lastChargeDate != 0) {
                long diffInMillis = System.currentTimeMillis() - lastChargeDate;
                List<TimeUnit> units = new ArrayList<>(EnumSet.allOf(TimeUnit.class));
                Collections.reverse(units);
                long millisRest = diffInMillis;
                for (TimeUnit unit : units) {
                    long diff = unit.convert(millisRest, TimeUnit.MILLISECONDS);
                    long diffInMillisForUnit = unit.toMillis(diff);
                    millisRest = millisRest - diffInMillisForUnit;
                    if (unit.equals(TimeUnit.DAYS)) {
                        append = diff + "d : ";
                        dateDiff.append(append);
                    } else if (unit.equals(TimeUnit.HOURS)) {
                        append = diff + "h : ";
                        dateDiff.append(append);
                    } else if (unit.equals(TimeUnit.MINUTES)) {
                        append = diff + "m ";
                        dateDiff.append(append);
                        break;
                    }
                }
                dateDiff.append(getResources().getText(R.string.last_charge));
            } else dateDiff.append(getResources().getText(R.string.last_charge_no_info));
            batteryTv.setText(dateDiff.toString());

            Logger.debug("BatteryChart updateChart defaultLocale: " + AmazModApplication.defaultLocale);
            String time = DateFormat.getTimeInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);
            String date = DateFormat.getDateInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);

            Calendar calendarLastDate = Calendar.getInstance();
            calendarLastDate.setTime(lastDate);
            Calendar calendarToday = Calendar.getInstance();
            calendarToday.setTime(new Date());

            String textDate = getResources().getText(R.string.last_read) + ": ";
            textDate += time;
            if (calendarLastDate.get(Calendar.DAY_OF_MONTH) != calendarToday.get(Calendar.DAY_OF_MONTH)) {
                textDate += " " + date;
            }
            lastRead.setText(textDate);
        }

        BatteryStatusEntity prevRead = null;

        int primaryColor = ContextCompat.getColor(mContext, R.color.colorPrimary);
        int chargingColor = ContextCompat.getColor(mContext, R.color.colorCharging);
        int predictionColor = ContextCompat.getColor(mContext, R.color.colorPrediction);

        for (int i = 0; i < batteryReadList.size(); i++) {
            BatteryStatusEntity read = batteryReadList.get(i);
            int level = (int) (read.getLevel() * 100f);
            int prevLevel = prevRead == null ? 100 : ((int) (prevRead.getLevel() * 100f));
            if ((level > 0) && ((prevRead == null) || (level != prevLevel))) {
                Entry entry = new Entry(read.getDate(), level);
                yValues.add(entry);

                int lineColor = level > prevLevel ? chargingColor : primaryColor;
                colors.add(lineColor);
            }

            prevRead = read;
        }

        if (yValues.size() == 0) {
            return;
        }

        // PREDICT BATTERY, calculate values
        // Get last charging point
        int value = 0; // use first data for calculation if never charged
        for (int i = colors.size() - 1; i >= 0; i--) {
            if (colors.get(i) == chargingColor) {
                value = i;
                break;
            }
        }
        boolean charging = false;
        // Still charging?
        if (yValues.size() > 1 && value == colors.size() - 1) {
            charging = true;
            value = 0; // use first data for calculation if never charged
            for (int i = colors.size() - 1; i >= 0; i--) {
                if (colors.get(i) == primaryColor) {
                    value = i;
                    break;
                }
            }
        }
        // Add last battery point as first data point
        yPredictValues.add(yValues.get(yValues.size() - 1));


        // At least 2 data points are needed for the calculation
        // Calculate future 0 or 100% Battery point
        if (yValues.size() > 1) {
            // Charging point
            float x1 = yValues.get(value).getX();
            float y1 = yValues.get(value).getY();
            // Last measure point
            float x2 = yValues.get(yValues.size() - 1).getX();
            float y2 = yValues.get(yValues.size() - 1).getY();

            float target_time;
            float remaininf_now_diff;
            String textDate;
            if (charging) {
                // Future time that battery will be 100%
                target_time = x2 + (x2 - x1) / (y2 - y1) * (100 - y2);

                textDate = lastRead.getText() + ", " + getResources().getText(R.string.full_battery_in) + ": ";
                remaininf_now_diff = (target_time - System.currentTimeMillis()) / (1000 * 60);
                textDate += ((int) remaininf_now_diff / 60) + " " + getResources().getText(R.string.hours) + " " + getResources().getText(R.string.and) + " " + ((int) remaininf_now_diff % 60) + " " + getResources().getText(R.string.minutes);
            } else {
                // Future time that battery will be 0%
                target_time = x2 + (x2 - x1) / (y1 - y2) * y2;

                textDate = lastRead.getText() + ", " + getResources().getText(R.string.remaining_battery) + ": ";
                remaininf_now_diff = (target_time - System.currentTimeMillis()) / (1000 * 60 * 60);
                textDate += ((int) remaininf_now_diff / 24) + " " + getResources().getText(R.string.days) + " " + getResources().getText(R.string.and) + " " + ((int) remaininf_now_diff % 24) + " " + getResources().getText(R.string.hours);
            }

            if (remaininf_now_diff > 0) {
                yPredictValues.add(new Entry(target_time, (charging) ? 100 : 0));

                // Expand graph's range
                highX = (long) target_time;

                // Fix graph range
                //highX = highX + ((long) yValues.get(0).getX() - lowX);
                lowX = (long) yValues.get(0).getX();

                // Add remaining time/full battery time to "Last Read" line
                lastRead.setText(textDate);
            }
        }

        LineDataSet lineDataSet = new LineDataSet(yValues, "Battery");

        lineDataSet.setLineWidth(1.5f);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setDrawValues(false);

        Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.fade_blue_battery);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setFillDrawable(drawable);
        lineDataSet.setColors(colors);
        lineDataSet.setMode(LineDataSet.Mode.LINEAR);
        lineDataSet.setCubicIntensity(0.05f);

        // Prediction line
        LineDataSet linePredictionDataSet = new LineDataSet(yPredictValues, "Estimation");

        linePredictionDataSet.setLineWidth(1.0f);
        linePredictionDataSet.setDrawCircleHole(false);
        linePredictionDataSet.setDrawCircles(false);
        linePredictionDataSet.setDrawValues(false);

        Drawable drawablePrediction = ContextCompat.getDrawable(mContext, (charging) ? R.drawable.fade_green_battery : R.drawable.fade_red_battery);
        linePredictionDataSet.setDrawFilled(true);
        linePredictionDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        linePredictionDataSet.setFillDrawable(drawablePrediction);
        linePredictionDataSet.enableDashedLine(5, 10, 0);
        linePredictionDataSet.setColors((charging) ? chargingColor : predictionColor);
        linePredictionDataSet.setMode(LineDataSet.Mode.LINEAR);
        linePredictionDataSet.setCubicIntensity(0.05f);
        // End of prediction line

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
        xAxis.setTextColor(ThemeHelper.getThemeForegroundColor(Objects.requireNonNull(getContext())));

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
        leftAxis.setTextColor(ThemeHelper.getThemeForegroundColor(getContext()));

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);

        chart.setXAxisRenderer(new CustomXAxisRenderer(chart.getViewPortHandler(), chart.getXAxis(), chart.getTransformer(YAxis.AxisDependency.LEFT)));

        //LineData lineData = new LineData(lineDataSet);
        //chart.setData(lineData);

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(lineDataSet);
        dataSets.add(linePredictionDataSet);

        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);

        chart.invalidate();
    }

    private class CustomXAxisRenderer extends XAxisRenderer {
        private CustomXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
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
}
