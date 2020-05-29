package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.LayoutInflaterCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.local.IsWatchConnectedLocal;
import com.edotassi.amazmod.setup.Setup;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.ui.fragment.BatteryChartFragment;
import com.edotassi.amazmod.ui.fragment.HeartRateChartFragment;
import com.edotassi.amazmod.ui.fragment.SilencedApplicationsFragment;
import com.edotassi.amazmod.ui.fragment.WatchInfoFragment;
import com.edotassi.amazmod.ui.fragment.WeatherFragment;
import com.edotassi.amazmod.util.Screen;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.michaelflisar.changelog.ChangelogBuilder;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;
import com.pixplicity.easyprefs.library.Prefs;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import amazmod.com.transport.Constants;
import butterknife.ButterKnife;

public class MainActivity extends BaseAppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private WatchInfoFragment watchInfoFragment = new WatchInfoFragment();
    private BatteryChartFragment batteryChartFragment = new BatteryChartFragment();
    private HeartRateChartFragment heartRateChartFragment = new HeartRateChartFragment();
    private WeatherFragment weatherFragment = new WeatherFragment();
    private SilencedApplicationsFragment silencedApplicationsFragment = new SilencedApplicationsFragment();
    public static boolean systemThemeIsDark = false;

    private List<Card> cards = new ArrayList<Card>() {{
        add(batteryChartFragment);
        add(silencedApplicationsFragment);
        add(heartRateChartFragment);
        add(weatherFragment);
        add(watchInfoFragment);
    }};

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));

        super.onCreate(savedInstanceState);
        isSystemThemeDark();

        if (Screen.isDarkTheme() || systemThemeIsDark) {
            setTheme(R.style.AppThemeDark_NoActionBar);
            setContentView(R.layout.activity_main_dark);
        } else {
            setTheme(R.style.AppTheme_NoActionBar);
            setContentView(R.layout.activity_main);
        }

        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            Logger.error(exception);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.open, R.string.close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Hide if not a developer (old developer mode)
        /*
        if (!Prefs.getBoolean(Constants.PREF_ENABLE_DEVELOPER_MODE, false)) {
            Menu menuNav = navigationView.getMenu();
            MenuItem widgets = menuNav.findItem(R.id.nav_widgets);
            widgets.setVisible(false);
        }
         */
        EventBus.getDefault().register(this);

        Logger.debug("MainActivity onCreate isWatchConnected: " + AmazModApplication.isWatchConnected());

        showChangelog(true);

        // Check if it is the first start using shared preference then start presentation if true
        boolean firstStart = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_KEY_FIRST_START, Constants.PREF_DEFAULT_KEY_FIRST_START);

        // Change locale
        if (firstStart) {
            //Get Locales
            Locale currentLocale = getResources().getConfiguration().locale;

            //set locale to avoid app refresh after using Settings for the first time
            Logger.debug("MainActivity firstStart locales: " + AmazModApplication.defaultLocale + " / " + currentLocale);
            Resources res = getResources();
            Configuration conf = res.getConfiguration();
            conf.locale = AmazModApplication.defaultLocale;
            res.updateConfiguration(conf, getResources().getDisplayMetrics());

            //Start Wizard Activity
            Intent intent = new Intent(MainActivity.this, MainIntroActivity.class);
            startActivityForResult(intent, Constants.REQUEST_CODE_INTRO);
        }

        setupCards();

        Setup.run(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                Snackbar snackbar = Snackbar
                        .make(drawer, R.string.battery_optimization_warning,
                                Constants.SNACKBAR_LONG10);

                String message;
                if ("samsung".equals(Build.MANUFACTURER.toLowerCase()) && Build.VERSION.SDK_INT == Build.VERSION_CODES.M)
                    message = getString(R.string.ok).toUpperCase();
                else
                    message = getString(R.string.remove).toUpperCase();
                snackbar.setAction(message, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            String action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS;
                            if (!"samsung".equals(Build.MANUFACTURER.toLowerCase())) {
                                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
                                intent.setData(Uri.parse("package:" + packageName));
                            }
                            intent.setAction(action);
                            startActivity(intent);
                        } catch (Exception ex) {
                            Logger.error(ex, "MainActivity ignore battery optimization manufacturer: {} exception: {}", Build.MANUFACTURER, ex.getMessage());
                        }
                    }
                });

                View snackbarView = snackbar.getView();
                TextView tv = (TextView) snackbarView.findViewById(R.id.snackbar_text);
                tv.setMaxLines(5);
                snackbar.show();
            }
        }
    }

    private void setupCards() {
        if (getSupportFragmentManager().getFragments() != null)
            for (Fragment f : getSupportFragmentManager().getFragments())
                getSupportFragmentManager().beginTransaction().remove(f).commitNow();

        boolean showBatteryChart = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_BATTERY_CHART, Constants.PREF_DEFAULT_BATTERY_CHART);
        boolean showHeartRateChart = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_HEARTRATE_CHART, Constants.PREF_DEFAULT_HEARTRATE_CHART);
        boolean showWeatherCard = (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_WATCHFACE_SEND_WEATHER_DATA, Constants.PREF_DEFAULT_WATCHFACE_SEND_WEATHER_DATA) &&
                !PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(Constants.PREF_WEATHER_LAST_DATA, "").isEmpty());

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        for (Card card : cards) {
            if ((card instanceof BatteryChartFragment && !showBatteryChart) ||
                    (card instanceof HeartRateChartFragment && !showHeartRateChart) ||
                    (card instanceof WeatherFragment && !showWeatherCard)) {
                continue;
            }
            fragmentTransaction.add(R.id.main_activity_cards, card, card.getName());
        }
        fragmentTransaction.commit();
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
        Logger.debug("MainActivity onResume isWatchConnected: " + AmazModApplication.isWatchConnected());
        showDonateUI();
    }

    private void showDonateUI() {
        long lastDonateAlert = PreferenceManager.getDefaultSharedPreferences(this)
                .getLong(Constants.PREF_LAST_DONATION_ALERT, Constants.PREF_DEFAULT_PREF_LAST_DONATION_ALERT);
        long day = 1000 * 60 * 60 * 24;
        long nextDonateAlert = lastDonateAlert + 7 * day;
        long now = Calendar.getInstance().getTimeInMillis();
        Logger.debug("Donate: Now" + now + " // Next " + nextDonateAlert);
        if (now > nextDonateAlert) {
            new MaterialDialog.Builder(this)
                    .canceledOnTouchOutside(false)
                    .title(R.string.support_us)
                    .content(R.string.support_us_text)
                    .positiveText(R.string.donate)
                    .negativeText(R.string.never)
                    .neutralText(R.string.later)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @SuppressLint("DefaultLocale")
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.putLong(Constants.PREF_LAST_DONATION_ALERT, now);
                            Intent c = new Intent(MainActivity.this, DonationActivity.class);
                            c.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(c);
                        }
                    })
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            Prefs.putLong(Constants.PREF_LAST_DONATION_ALERT, now);
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            //Never = 5 years
                            Prefs.putLong(Constants.PREF_LAST_DONATION_ALERT, now + 365 * 5 * day);
                        }
                    })
                    .show();
        } else {
            Logger.debug("Will not show Donate UI. It is " + now + " and will show only after " + nextDonateAlert);
        }
    }


    @Override
    public void onPause() {
        if (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) != null)
            EventBus.getDefault().removeStickyEvent(IsWatchConnectedLocal.class);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) != null)
            EventBus.getDefault().removeStickyEvent(IsWatchConnectedLocal.class);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //your code
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //your code
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        Intent intent;
        switch (item.getItemId()) {
            case R.id.nav_settings:
                intent = new Intent(this, SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                if (getIntent().getBooleanExtra("REFRESH", true)) {
                    recreate();
                    getIntent().putExtra("REFRESH", false);
                }
                break;

            case R.id.nav_faq:
                intent = new Intent(this, FaqActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            case R.id.nav_about:
                intent = new Intent(this, AboutActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            case R.id.nav_tweaking:
                Intent c = new Intent(this, TweakingActivity.class);
                c.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(c);
                break;

            case R.id.nav_file_explorer:
                intent = new Intent(this, FileExplorerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            case R.id.nav_watchface:
                intent = new Intent(this, WatchfaceActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            case R.id.nav_widgets:
                intent = new Intent(this, WidgetsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            case R.id.nav_stats:
                intent = new Intent(this, StatsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            case R.id.nav_changelog:
                showChangelog(false);
                break;

            case R.id.nav_support_us:
                intent = new Intent(MainActivity.this, DonationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void getTransportStatus(IsWatchConnectedLocal itc) {
        if (itc != null) {
            if (AmazModApplication.isWatchConnected() != itc.getWatchStatus()) {
                AmazModApplication.setWatchConnected(itc.getWatchStatus());
                watchInfoFragment.onResume();
            }
        } else {
            AmazModApplication.setWatchConnected(false);
        }
        Logger.debug("MainActivity getTransportStatus: " + AmazModApplication.isWatchConnected());
    }

    private void showChangelog(boolean managedShowOnStart) {
        final boolean isDarkTheme = Screen.isDarkTheme();
        new ChangelogBuilder()
                .withUseBulletList(true) // true if you want to show bullets before each changelog row, false otherwise
                .withMinVersionToShow(1)     // provide a number and the log will only show changelog rows for versions equal or higher than this number
                //.withFilter(new ChangelogFilter(ChangelogFilter.Mode.Exact, "somefilterstring", true)) // this will filter out all tags, that do not have the provided filter attribute
                .withManagedShowOnStart(managedShowOnStart)  // library will take care to show activity/dialog only if the changelog has new infos and will only show this new infos
                .withRateButton(true)
                .withRateButtonLabel(getString(R.string.rate_app))
                .buildAndShowDialog(this, isDarkTheme);
    }

    private void isSystemThemeDark() {
        switch (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            case android.content.res.Configuration.UI_MODE_NIGHT_YES:
                systemThemeIsDark = true;
                Logger.debug("System night mode is enabled");
                break;
            case android.content.res.Configuration.UI_MODE_NIGHT_NO:
                systemThemeIsDark = false;
                Logger.debug("System night mode is disabled");
                break;
        }
    }

}
