package com.edotassi.amazmod.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;

import com.edotassi.amazmod.AmazModApplication;
import amazmod.com.transport.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.local.IsWatchConnectedLocal;
import com.edotassi.amazmod.notification.NotificationService;
import com.edotassi.amazmod.setup.Setup;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.ui.fragment.BatteryChartFragment;
import com.edotassi.amazmod.ui.fragment.WatchInfoFragment;
import com.michaelflisar.changelog.ChangelogBuilder;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private WatchInfoFragment watchInfoFragment = new WatchInfoFragment();
    private BatteryChartFragment batteryChartFragment = new BatteryChartFragment();


    private List<Card> cards = new ArrayList<Card>() {{
        add(batteryChartFragment);
        add(watchInfoFragment);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.open, R.string.close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        EventBus.getDefault().register(this);

        //isWatchConnectedLocal itc = HermesEventBus.getDefault().getStickyEvent(IsTransportConnectedLocal.class);
        //isWatchConnected = itc == null || itc.getTransportStatus();
        Log.d(Constants.TAG, " MainActivity onCreate isWatchConnected: " + AmazModApplication.isWatchConnected);

        showChangelog(true);

        // Check if it is the first start using shared preference then start presentation if true
        boolean firstStart = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_KEY_FIRST_START, Constants.PREF_DEFAULT_KEY_FIRST_START);

        //Get Locales
        Locale currentLocale = getResources().getConfiguration().locale;

        if (firstStart) {
            //set locale to avoid app refresh after using Settings for the first time
            Log.d(Constants.TAG, " MainActivity firstStart locales: " + AmazModApplication.defaultLocale + " / " + currentLocale);
            Resources res = getResources();
            Configuration conf = res.getConfiguration();
            conf.locale = AmazModApplication.defaultLocale;
            res.updateConfiguration(conf, getResources().getDisplayMetrics());

            //Start Wizard Activity
            Intent intent = new Intent(MainActivity.this, MainIntroActivity.class);
            startActivityForResult(intent, Constants.REQUEST_CODE_INTRO);
        }

        //Change app localization if needed
        final boolean forceEN = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_FORCE_ENGLISH, false);

        Log.d(Constants.TAG, " MainActivity locales: " + AmazModApplication.defaultLocale + " / " + currentLocale);

        if (forceEN && (currentLocale != Locale.US)) {
            Log.d(Constants.TAG, " MaiActivity New locale: US");
            Resources res = getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.locale = Locale.US;
            res.updateConfiguration(conf, dm);
            recreate();
        }

        setupCards();

        // Try to start NotificationService if it is not active
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this);
        if (!packageNames.contains(this.getPackageName())) {
            toggleNotificationService();
        }

        Setup.run(getApplicationContext());
    }

    private void setupCards() {

        if (getSupportFragmentManager().getFragments() != null) {
            for (Fragment f : getSupportFragmentManager().getFragments()) {
                getSupportFragmentManager().beginTransaction().remove(f).commitNow();
            }
        }

        Boolean disableBatteryChart = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_DISABLE_BATTERY_CHART, Constants.PREF_DEFAULT_DISABLE_BATTERY_CHART);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        for (Card card : cards) {
            if (!(disableBatteryChart && (card instanceof BatteryChartFragment))) {
                fragmentTransaction.add(R.id.main_activity_cards, card, card.getName());
            }
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
        Log.d(Constants.TAG, " MainActivity onResume isWatchConnected: " + AmazModApplication.isWatchConnected);
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

            case R.id.nav_files_extras:
                Intent f = new Intent(this, FilesExtrasActivity.class);
                f.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(f);
                return true;

            case R.id.nav_watchface:
                Intent e = new Intent(this, WatchfaceActivity.class);
                e.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(e);
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
            if (AmazModApplication.isWatchConnected != itc.getWatchStatus()) {
                AmazModApplication.isWatchConnected = itc.getWatchStatus();
                watchInfoFragment.onResume();
                //watchInfoFragment.onResume();
            }
        } else {
            AmazModApplication.isWatchConnected = false;
        }
        Log.d(Constants.TAG, " MainActivity getTransportStatus: " + AmazModApplication.isWatchConnected);
    }

    private void showChangelog(boolean managedShowOnStart) {
        new ChangelogBuilder()
                .withUseBulletList(true) // true if you want to show bullets before each changelog row, false otherwise
                .withMinVersionToShow(1)     // provide a number and the log will only show changelog rows for versions equal or higher than this number
                //.withFilter(new ChangelogFilter(ChangelogFilter.Mode.Exact, "somefilterstring", true)) // this will filter out all tags, that do not have the provided filter attribute
                .withManagedShowOnStart(managedShowOnStart)  // library will take care to show activity/dialog only if the changelog has new infos and will only show this new infos
                .withRateButton(true)
                .buildAndShowDialog(this, false);
    }

    private void toggleNotificationService() {
        Log.d(Constants.TAG, "MainActivity toggleNotificationService");
        ComponentName thisComponent = new ComponentName(this, NotificationService.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    }
}
