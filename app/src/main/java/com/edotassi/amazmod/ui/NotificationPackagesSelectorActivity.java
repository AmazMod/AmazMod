package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.AppInfoAdapter;
import com.edotassi.amazmod.support.AppInfo;
import com.google.gson.Gson;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class NotificationPackagesSelectorActivity extends AppCompatActivity implements AppInfoAdapter.Bridge {

    @BindView(R.id.activity_notification_packages_selector_list)
    ListView listView;
    @BindView(R.id.activity_notification_packages_selector_progress)
    MaterialProgressBar materialProgressBar;

    private List<AppInfo> appInfoList;
    private AppInfoAdapter appInfoAdapter;

    private boolean selectedAll;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_packages_selector);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.installed_apps);

        ButterKnife.bind(this);

        appInfoAdapter = new AppInfoAdapter(this, R.layout.row_appinfo, new ArrayList<AppInfo>());
        listView.setAdapter(appInfoAdapter);

        loadApps(false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_notification_packages_selector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_activity_notification_packages_selector_show_system) {
            item.setChecked(!item.isChecked());
            loadApps(item.isChecked());
            return true;
        }

        if (id == R.id.action_activity_notification_packges_selector_toggle_all) {
            for (AppInfo appInfo : appInfoList) {
                appInfo.setEnabled(!selectedAll);
            }
            selectedAll = !selectedAll;

            appInfoAdapter.notifyDataSetChanged();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAppInfoStatusChange() {
        sortAppInfo(appInfoList);
        appInfoAdapter.clear();
        appInfoAdapter.addAll(appInfoList);
        appInfoAdapter.notifyDataSetChanged();

        save();
    }

    @Override
    public Context getContext() {
        return this;
    }

    @SuppressLint("CheckResult")
    private void loadApps(final boolean showSystemApps) {
        materialProgressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        Flowable.fromCallable(new Callable<List<AppInfo>>() {
            @Override
            public List<AppInfo> call() throws Exception {
                List<PackageInfo> packageInfoList = getPackageManager().getInstalledPackages(0);

                List<AppInfo> appInfoList = new ArrayList<>();

                String packagesJson = Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");
                Gson gson = new Gson();

                String[] packagesList = gson.fromJson(packagesJson, String[].class);

                Arrays.sort(packagesList);

                for (PackageInfo packageInfo : packageInfoList) {
                    boolean isSystemApp = (packageInfo.applicationInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0;
                    boolean enabled = Arrays.binarySearch(packagesList, packageInfo.packageName) >= 0;
                    if (enabled || (!isSystemApp || (showSystemApps && isSystemApp))) {
                        // It is a system app
                        AppInfo appInfo = createAppInfo(packageInfo, enabled);
                        appInfoList.add(appInfo);
                    }
                }

                sortAppInfo(appInfoList);

                NotificationPackagesSelectorActivity.this.appInfoList = appInfoList;

                return appInfoList;
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<List<AppInfo>>() {
                    @Override
                    public void accept(final List<AppInfo> appInfoList) throws Exception {
                        NotificationPackagesSelectorActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                appInfoAdapter.clear();
                                appInfoAdapter.addAll(appInfoList);
                                appInfoAdapter.notifyDataSetChanged();

                                materialProgressBar.setVisibility(View.GONE);
                                listView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
    }

    private void sortAppInfo(List<AppInfo> appInfoList) {
        Collections.sort(appInfoList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                if (o1.isEnabled() && !o2.isEnabled()) {
                    return -1;
                } else if (!o1.isEnabled() && o2.isEnabled()) {
                    return 1;
                } else if ((!o1.isEnabled() && !o2.isEnabled()) || (o1.isEnabled() && o2.isEnabled())) {
                    return o1.getAppName().compareTo(o2.getAppName());
                }

                return o1.getAppName().compareTo(o2.getAppName());
            }
        });
    }

    private AppInfo createAppInfo(PackageInfo packageInfo, boolean enabled) {
        AppInfo appInfo = new AppInfo();

        appInfo.setPackageName(packageInfo.packageName);
        appInfo.setAppName(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
        appInfo.setVersionName(packageInfo.versionName);
        appInfo.setIcon(packageInfo.applicationInfo.loadIcon(getPackageManager()));
        appInfo.setEnabled(enabled);

        return appInfo;
    }

    private void save() {
        if (appInfoList != null) {
            List<String> enabledPackages = new ArrayList<>();

            for (AppInfo appInfo : appInfoList) {
                if (appInfo.isEnabled()) {
                    enabledPackages.add(appInfo.getPackageName());
                }
            }

            Gson gson = new Gson();
            String pref = gson.toJson(enabledPackages);

            Prefs.putString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, pref);
        }
    }
}
