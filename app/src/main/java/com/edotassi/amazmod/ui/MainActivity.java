package com.edotassi.amazmod.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.BatteryStatusEntity_Table;
import com.edotassi.amazmod.event.RequestBatteryStatus;
import com.edotassi.amazmod.event.RequestWatchStatus;
import com.edotassi.amazmod.event.WatchStatus;
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
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import amazmod.com.transport.data.WatchStatusData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.card_amazmodservice)
    TextView amazModService;
    @BindView(R.id.card_product_device)
    TextView productDevice;
    @BindView(R.id.card_product_manufacter)
    TextView productManufacter;
    @BindView(R.id.card_product_model)
    TextView productModel;
    @BindView(R.id.card_product_name)
    TextView productName;
    @BindView(R.id.card_revision)
    TextView revision;
    @BindView(R.id.card_serialno)
    TextView serialNo;
    @BindView(R.id.card_build_date)
    TextView buildDate;
    @BindView(R.id.card_build_description)
    TextView buildDescription;
    @BindView(R.id.card_display_id)
    TextView displayId;
    @BindView(R.id.card_huami_model)
    TextView huamiModel;
    @BindView(R.id.card_huami_number)
    TextView huamiNumber;
    @BindView(R.id.card_build_fingerprint)
    TextView fingerprint;
    @BindView(R.id.battery_chart)
    LineChart chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.open, R.string.close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        HermesEventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        HermesEventBus.getDefault().post(new RequestWatchStatus());

        updateChart();
    }

    @Override
    public void onPause() {
        super.onPause();

        resetWatchStatus();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        //int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @OnClick(R.id.refresh_battery)
    public void onBatteryRefreshClick() {
        boolean isReg = HermesEventBus.getDefault().isRegistered(new RequestBatteryStatus());
        HermesEventBus.getDefault().post(new RequestBatteryStatus());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWatchStatus(WatchStatus watchStatus) {
        WatchStatusData watchStatusData = watchStatus.getWatchStatusData();

        amazModService.setText(watchStatusData.getAmazModServiceVersion());
        productDevice.setText(watchStatusData.getRoProductDevice());
        productManufacter.setText(watchStatusData.getRoProductManufacter());
        productModel.setText(watchStatusData.getRoProductModel());
        productName.setText(watchStatusData.getRoProductName());
        revision.setText(watchStatusData.getRoRevision());
        serialNo.setText(watchStatusData.getRoSerialno());
        buildDate.setText(watchStatusData.getRoBuildDate());
        buildDescription.setText(watchStatusData.getRoBuildDescription());
        displayId.setText(watchStatusData.getRoBuildDisplayId());
        huamiModel.setText(watchStatusData.getRoBuildHuamiModel());
        huamiNumber.setText(watchStatusData.getRoBuildHuamiNumber());
        fingerprint.setText(watchStatusData.getRoBuildFingerprint());
    }

    private void resetWatchStatus() {
        amazModService.setText("-");
        productDevice.setText("-");
        productManufacter.setText("-");
        productModel.setText("-");
        productName.setText("-");
        revision.setText("-");
        serialNo.setText("-");
        buildDate.setText("-");
        buildDescription.setText("-");
        displayId.setText("-");
        huamiModel.setText("-");
        huamiNumber.setText("-");
        fingerprint.setText("-");
    }

    private void updateChart() {
        final List<Entry> yValues = new ArrayList<Entry>();
        final List<Integer> colors = new ArrayList<>();


        //TODO use Preferences for days
        final int days = 3;

        Calendar calendar = Calendar.getInstance();
        long highX = calendar.getTimeInMillis();

        calendar.add(Calendar.DATE, -1 * days);

        long lowX = calendar.getTimeInMillis();

        List<BatteryStatusEntity> batteryReadList = SQLite
                .select()
                .from(BatteryStatusEntity.class)
                .where(BatteryStatusEntity_Table.date.greaterThan(lowX))
                .queryList();

        BatteryStatusEntity prevRead = null;
        for (int i = 0; i < batteryReadList.size(); i++) {
            BatteryStatusEntity read = batteryReadList.get(i);
            int level = (int) (read.getLevel() * 100f);
            int prevLevel = prevRead == null ? 0 : ((int) (prevRead.getLevel() * 100f));
            if ((level > 0) && ((prevRead == null) || (level != prevLevel))) {
                Entry entry = new Entry(read.getDate(), level);
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

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_blue_battery);
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
