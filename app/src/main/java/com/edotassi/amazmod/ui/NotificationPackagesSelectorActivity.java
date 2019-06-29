package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.AppInfoAdapter;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.support.AppInfo;
import com.edotassi.amazmod.support.SilenceApplicationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class NotificationPackagesSelectorActivity extends BaseAppCompatActivity
        implements AppInfoAdapter.Bridge, SearchView.OnQueryTextListener {

    @BindView(R.id.activity_notification_packages_selector_list)
    ListView listView;
    @BindView(R.id.activity_notification_packages_selector_progress)
    MaterialProgressBar materialProgressBar;

    private List<AppInfo> appInfoList;
    private AppInfoAdapter appInfoAdapter;

    private boolean selectedAll;
    private boolean showSystemApps;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_packages_selector);

        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.installed_apps);

        ButterKnife.bind(this);

        appInfoAdapter = new AppInfoAdapter(
                this, R.layout.row_appinfo, new ArrayList<>());
        listView.setAdapter(appInfoAdapter);

        loadApps(showSystemApps, null);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_notification_packages_selector, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                Objects.requireNonNull((SearchManager) getSystemService(Context.SEARCH_SERVICE));
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_activity_notification_packages_selector_show_system) {
            boolean checked = item.isChecked();
            item.setChecked(!checked);
            loadApps(!checked, null);
            showSystemApps = !checked;
            return true;
        }

        if (id == R.id.action_activity_notification_packges_selector_toggle_all) {
            if (appInfoList != null) {
                long count = 0;
                for (AppInfo appInfoCheckForAll : appInfoList) {
                    if (appInfoCheckForAll.isEnabled()) count++;
                }
                if (appInfoList.size() <= count) {
                    selectedAll = true;
                }
                for (AppInfo appInfo : appInfoList) {
                    appInfo.setEnabled(!selectedAll);
                }
                selectedAll = !selectedAll;
                sortAppInfo(appInfoList);
                appInfoAdapter.clear();
                appInfoAdapter.addAll(appInfoList);
                appInfoAdapter.notifyDataSetChanged();
            }
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
    }

    @Override
    public Context getContext() {
        return this;
    }

    @SuppressLint("CheckResult")
    private void loadApps(final boolean showSystemApps, @Nullable String searchQueryText) {
        materialProgressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        Single.fromCallable(() -> {
            //List installed packages and create a list of appInfo based on them
            List<PackageInfo> packageInfoList = getPackageManager().getInstalledPackages(0);
            List<AppInfo> appInfoList = new ArrayList<>();
            Map<String, NotificationPreferencesEntity> packagesMap = SilenceApplicationHelper.listApps();
            for (PackageInfo packageInfo : packageInfoList) {
                boolean isSystemApp = (packageInfo.applicationInfo.flags &
                        (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0;
                boolean enabled = packagesMap.containsKey(packageInfo.packageName);
                if (enabled || !isSystemApp || (showSystemApps && isSystemApp)) {
                    appInfoList.add(createAppInfo(packageInfo, enabled));
                }
            }
            sortAppInfo(appInfoList);
            NotificationPackagesSelectorActivity.this.appInfoList = appInfoList;
            return appInfoList;
        }).flatMap(appInfoList -> filterAppInfoBySearchQueryText(appInfoList, searchQueryText))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(appInfoList -> {
                    //fill List with appInfoList
                    appInfoAdapter.clear();
                    appInfoAdapter.addAll(appInfoList);
                    appInfoAdapter.notifyDataSetChanged();

                    materialProgressBar.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                });
    }

    private Single<List<AppInfo>> filterAppInfoBySearchQueryText(
            @NonNull List<AppInfo> appInfoList,
            @Nullable String searchQueryText) {
        if (searchQueryText == null || searchQueryText.isEmpty()) {
            return Single.just(appInfoList);
        }

        return Observable.fromIterable(appInfoList)
                .filter(appInfo ->
                        appInfo.getAppName().toLowerCase().contains(searchQueryText.toLowerCase()))
                .toList();
    }

    private void sortAppInfo(List<AppInfo> appInfoList) {
        Collections.sort(appInfoList, (o1, o2) -> {
            if (o1.isEnabled() && !o2.isEnabled()) {
                return -1;
            } else if (!o1.isEnabled() && o2.isEnabled()) {
                return 1;
            } else if ((!o1.isEnabled() && !o2.isEnabled()) || (o1.isEnabled() && o2.isEnabled())) {
                return o1.getAppName().compareTo(o2.getAppName());
            }
            return o1.getAppName().compareTo(o2.getAppName());
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

    @Override
    public boolean onQueryTextSubmit(String query) {
        handleSearchQueryText(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        handleSearchQueryText(query);
        return true;
    }

    private void handleSearchQueryText(@Nullable String query) {
        loadApps(showSystemApps, query);
    }
}
