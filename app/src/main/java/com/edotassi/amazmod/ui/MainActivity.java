package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.core.view.LayoutInflaterCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.local.IsWatchConnectedLocal;
import com.edotassi.amazmod.setup.Setup;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.ui.fragment.BatteryChartFragment;
import com.edotassi.amazmod.ui.fragment.HeartRateChartFragment;
import com.edotassi.amazmod.ui.fragment.SilencedApplicationsFragment;
import com.edotassi.amazmod.ui.fragment.WatchInfoFragment;
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
    private SilencedApplicationsFragment silencedApplicationsFragment = new SilencedApplicationsFragment();


    private List<Card> cards = new ArrayList<Card>() {{
        add(batteryChartFragment);
        add(silencedApplicationsFragment);
        add(heartRateChartFragment);
        add(watchInfoFragment);
    }};

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));

        super.onCreate(savedInstanceState);

        if (Screen.isDarkTheme()) {
            setTheme(R.style.AppThemeDark_NoActionBar);
            setContentView(R.layout.activity_main_dark);
        } else
            setContentView(R.layout.activity_main);

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

        // Hide if not a developer
        if (!Prefs.getBoolean(Constants.PREF_ENABLE_DEVELOPER_MODE, false)) {
            Menu menuNav = navigationView.getMenu();
            MenuItem widgets = menuNav.findItem(R.id.nav_widgets);
            widgets.setVisible(false);
        }


        EventBus.getDefault().register(this);

        Logger.debug("MainActivity onCreate isWatchConnected: " + AmazModApplication.isWatchConnected());

        showChangelog(true);

        // Check if it is the first start using shared preference then start presentation if true
        boolean firstStart = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_KEY_FIRST_START, Constants.PREF_DEFAULT_KEY_FIRST_START);

        //Get Locales
        Locale currentLocale = getResources().getConfiguration().locale;

        if (firstStart) {
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
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
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
                                /* intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS); *** This will cause ban from PlayStore! ***
                                intent.setData(Uri.parse("package:" + packageName));                       *** and it doesn't work with some Samsung phones ***/
                                    intent.setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                    startActivity(intent);
                                } catch (Exception ex) {
                                    Logger.error(ex, "MainActivity ignore battery optimization manufacturer: {} exception: {}", Build.MANUFACTURER, ex.getMessage());
                                }
                            }
                        });

                View snackbarView = snackbar.getView();
                TextView tv= (TextView) snackbarView.findViewById(R.id.snackbar_text);
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

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        for (Card card : cards) {
            if ((card instanceof BatteryChartFragment && !showBatteryChart) ||
                    (card instanceof HeartRateChartFragment && !showHeartRateChart)) {
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        switch (item.getItemId()) {
            case R.id.nav_settings:
                Intent a = new Intent(this, SettingsActivity.class);
                a.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(a);
                if (getIntent().getBooleanExtra("REFRESH", true)) {
                    recreate();
                    getIntent().putExtra("REFRESH", false);
                }
                return true;

            case R.id.nav_faq:
                Intent faqIntent = new Intent(this, FaqActivity.class);
                faqIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(faqIntent);
                return true;

            case R.id.nav_abount:
                Intent b = new Intent(this, AboutActivity.class);
                b.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(b);
                return true;

            case R.id.nav_tweaking:
                Intent c = new Intent(this, TweakingActivity.class);
                c.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(c);
                return true;

            case R.id.nav_file_explorer:
                Intent fileExplorerIntent = new Intent(this, FileExplorerActivity.class);
                fileExplorerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(fileExplorerIntent);
                return true;

            case R.id.nav_watchface:
                Intent e = new Intent(this, WatchfaceActivity.class);
                e.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(e);
                return true;

            case R.id.nav_widgets:
                Intent f = new Intent(this, WidgetsActivity.class);
                f.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(f);
                return true;

            case R.id.nav_stats:
                Intent d = new Intent(this, StatsActivity.class);
                d.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(d);
                return true;

            case R.id.nav_changelog:
                showChangelog(false);
                return true;
        }

        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void getTransportStatus(IsWatchConnectedLocal itc) {
        if (itc != null) {
            if (AmazModApplication.isWatchConnected() != itc.getWatchStatus()) {
                AmazModApplication.setWatchConnected(itc.getWatchStatus());
                watchInfoFragment.onResume();
                //watchInfoFragment.onResume();
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
                .buildAndShowDialog(this, isDarkTheme);
    }

}
