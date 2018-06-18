package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class NotificationPackagesSelectorActivity extends AppCompatActivity {

    @BindView(R.id.activity_notification_packages_selector_list)
    ListView listView;
    @BindView(R.id.activity_notification_packages_selector_progress)
    MaterialProgressBar materialProgressBar;

    private List<AppInfo> appInfoList;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_packages_selector);

        ButterKnife.bind(this);

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
                    AppInfo appInfo = new AppInfo();

                    appInfo.setPackageName(packageInfo.packageName);
                    appInfo.setAppName(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
                    appInfo.setVersionName(packageInfo.versionName);
                    appInfo.setIcon(packageInfo.applicationInfo.loadIcon(getPackageManager()));

                    int packageIndex = Arrays.binarySearch(packagesList, packageInfo.packageName);
                    appInfo.setEnabled(packageIndex >= 0);

                    appInfoList.add(appInfo);
                }

                Collections.sort(appInfoList, new Comparator<AppInfo>() {
                    @Override
                    public int compare(AppInfo o1, AppInfo o2) {
                        return o1.getAppName().compareTo(o2.getAppName());
                    }
                });

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
                                listView.setAdapter(new AppInfoAdapter(NotificationPackagesSelectorActivity.this, R.layout.row_appinfo, appInfoList));

                                materialProgressBar.setVisibility(View.GONE);
                                listView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (appInfoList != null) {
            List<String> enabledPackages = new ArrayList<>();

            Collections.sort(appInfoList, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo o1, AppInfo o2) {
                    return o1.getPackageName().compareTo(o2.getPackageName());
                }
            });

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
