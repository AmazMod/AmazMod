package com.edotassi.amazmod.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.BuildConfig;
import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.MainIntroActivity;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.BatteryStatusEntity_Table;
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
import com.michaelflisar.changelog.ChangelogBuilder;
import com.michaelflisar.changelog.classes.ChangelogFilter;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import amazmod.com.transport.data.WatchStatusData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.card_battery_last_read)
    TextView lastRead;

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

    @BindView(R.id.card_battery)
    CardView batteryCard;
    @BindView(R.id.card_watch)
    CardView watchCard;
    @BindView(R.id.card_watch_progress)
    MaterialProgressBar watchProgress;
    @BindView(R.id.card_watch_detail)
    LinearLayout watchDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.open, R.string.close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        HermesEventBus.getDefault().register(this);

        showChangelog(false, BuildConfig.VERSION_CODE, true);

        // Check if it is the first start using shared preference then start presentation if true
        boolean firstStart = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_KEY_FIRST_START, Constants.PREF_DEFAULT_KEY_FIRST_START);


        if (firstStart) {
            //set locale to avoid app refresh after using Settings for the first time
            Locale defaultLocale = Locale.getDefault();
            Locale currentLocale = getResources().getConfiguration().locale;
            System.out.println("Initial locales: " + defaultLocale + " / " + currentLocale);
            Resources res = getResources();
            Configuration conf = res.getConfiguration();
            conf.locale = defaultLocale;
            res.updateConfiguration(conf, getResources().getDisplayMetrics());

            //Start Wizard Activity
            Intent intent = new Intent(MainActivity.this, MainIntroActivity.class);
            startActivityForResult(intent, Constants.REQUEST_CODE_INTRO);
        }

//        checkNotificationsAccess(); not needed anymore after adding presentation
    }

    // If presentation was run until the end, use shared preference to not start it again
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_INTRO) {
            if (resultCode == RESULT_OK) {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean(Constants.PREF_KEY_FIRST_START, false)
                        .apply();
            } else {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean(Constants.PREF_KEY_FIRST_START, true)
                        .apply();
                //User cancelled the intro so we'll finish this activity too.
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        watchDetail.setVisibility(View.GONE);
        watchProgress.setVisibility(View.VISIBLE);

        Flowable
                .timer(2000, TimeUnit.MILLISECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        HermesEventBus.getDefault().post(new RequestWatchStatus());
                    }
                });

        updateChart();
    }

    @Override
    public void onPause() {
        super.onPause();
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

//        int id = item.getItemId();

        switch (item.getItemId()) {

            case R.id.nav_settings:
                Intent a = new Intent(this, SettingsActivity.class);
                a.setFlags(a.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(a);
                if (getIntent().getBooleanExtra("REFRESH", true)) {
                    recreate();
                    getIntent().putExtra("REFRESH", false);
                }
                return true;

            case R.id.nav_abount:
                Intent b = new Intent(this, AboutActivity.class);
                b.setFlags(b.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(b);
                return true;

            case R.id.nav_tweaking:
                Intent c = new Intent(this, TweakingActivity.class);
                c.setFlags(c.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(c);
                return true;

            case R.id.nav_stats:
                Intent d = new Intent(this, StatsActivity.class);
                d.setFlags(d.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(d);
                return true;

            case R.id.nav_changelog:
                showChangelog(true, 1, false);
                return true;
        }

        return true;
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

        watchProgress.setVisibility(View.GONE);
        watchDetail.setVisibility(View.VISIBLE);
    }

    private void showChangelog(boolean withActivity, int minVersion, boolean managedShowOnStart) {
        ChangelogBuilder builder = new ChangelogBuilder()
                .withUseBulletList(true) // true if you want to show bullets before each changelog row, false otherwise
                .withMinVersionToShow(1)     // provide a number and the log will only show changelog rows for versions equal or higher than this number
                //.withFilter(new ChangelogFilter(ChangelogFilter.Mode.Exact, "somefilterstring", true)) // this will filter out all tags, that do not have the provided filter attribute
                .withManagedShowOnStart(managedShowOnStart)  // library will take care to show activity/dialog only if the changelog has new infos and will only show this new infos
                .withRateButton(true); // enable this to show a "rate app" button in the dialog => clicking it will open the play store; the parent activity or target fragment can also implement IChangelogRateHandler to handle the button click

        if (withActivity) {
            builder.buildAndStartActivity(
                    this, true); // second parameter defines, if the dialog has a dark or light theme
        } else {
            builder.buildAndShowDialog(this, false);
        }
    }

    /**
     * No needed anymore with presentation
     * private void checkNotificationsAccess() {
     * Set<String> packages = NotificationManagerCompat.getEnabledListenerPackages(this);
     * int index = Arrays.binarySearch(packages.toArray(), BuildConfig.APPLICATION_ID);
     * if (index == -1) {
     * new MaterialDialog.Builder(this)
     * .title(R.string.notification_access)
     * .content(R.string.notification_access_not_enabled)
     * .positiveText(R.string.enable)
     * .negativeText(R.string.cancel)
     * .icon(getResources().getDrawable(R.drawable.outline_notifications_black_24))
     * .onPositive(new MaterialDialog.SingleButtonCallback() {
     *
     * @Override public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
     * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
     * startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
     * } else {
     * startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
     * }
     * }
     * })
     * .show();
     * }
     * }
     **/

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

        if (batteryReadList.size() > 0) {
            BatteryStatusEntity lastEntity = batteryReadList.get(batteryReadList.size() - 1);
            Date lastDate = new Date(lastEntity.getDate());
            String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(lastDate);
            String date = DateFormat.getDateInstance(DateFormat.SHORT).format(lastDate);

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
