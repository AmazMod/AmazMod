package com.edotassi.amazmod.ui

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.edotassi.amazmod.R
import com.edotassi.amazmod.adapters.ApplicationAdapter
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity
import com.edotassi.amazmod.support.AppInfo
import com.edotassi.amazmod.support.SilenceApplicationHelper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_application_select.*
import java.util.*

class ApplicationSelectActivity : BaseAppCompatActivity(), SearchView.OnQueryTextListener {

    private var selectedAll = false
    private var showSystemApps = false
    private lateinit var appList: List<AppInfo>
    private lateinit var adapter: ApplicationAdapter

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setTitle(R.string.installed_apps)
        setContentView(R.layout.activity_application_select)
        application_list.layoutManager = LinearLayoutManager(this)
        listApplications(showSystemApps, null)
    }

    @SuppressLint("CheckResult")
    private fun listApplications(showSystemApps: Boolean, searchQueryText: String?) {
        progress_bar.visibility = View.VISIBLE
        application_list.visibility = View.GONE
        Single.fromCallable<List<AppInfo>> {
            //List installed packages and create a list of appInfo based on them
            val packageInfoList = packageManager.getInstalledPackages(0)
            val appInfoList: MutableList<AppInfo> = ArrayList()
            val packagesMap: Map<String, NotificationPreferencesEntity> = SilenceApplicationHelper.listApps()
            for (packageInfo in packageInfoList) {
                val isSystemApp = packageInfo.applicationInfo.flags and
                        (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM) > 0
                val enabled = packagesMap.containsKey(packageInfo.packageName)
                if (enabled || !isSystemApp || showSystemApps && isSystemApp) {
                    appInfoList.add(createAppInfo(packageInfo, enabled))
                }
            }
            sortAppInfo(appInfoList)
            this@ApplicationSelectActivity.appList = appInfoList
            appInfoList
        }.flatMap { appInfoList: List<AppInfo> -> filterAppInfoBySearchQueryText(appInfoList, searchQueryText) }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { appList: List<AppInfo> ->
                    loadApplicationsToRecyclerView(appList)
                }
    }

    fun sortRecyclerView() {
        sortAppInfo(appList)
        this.adapter.notifyDataSetChanged()
    }

    private fun loadApplicationsToRecyclerView(list: List<AppInfo>) {
        this.adapter = ApplicationAdapter(this, list)
        application_list.adapter = adapter
        application_list.visibility = View.VISIBLE
        progress_bar.visibility = View.GONE
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_notification_packages_selector, menu)

        // Associate searchable configuration with the SearchView
        val searchManager = Objects.requireNonNull(getSystemService(Context.SEARCH_SERVICE) as SearchManager)
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnQueryTextListener(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_activity_notification_packages_selector_show_system) {
            val checked = item.isChecked
            item.isChecked = !checked
            listApplications(!checked, null)
            showSystemApps = !checked
            return true
        }
        if (id == R.id.action_activity_notification_packges_selector_toggle_all) {
            if (appList != null) {
                var count: Long = 0
                for (appInfoCheckForAll in appList) {
                    if (appInfoCheckForAll.isEnabled) count++
                }
                if (appList.size <= count) {
                    selectedAll = true
                }
                for (appInfo in appList) {
                    appInfo.isEnabled = !selectedAll
                }
                selectedAll = !selectedAll
                sortAppInfo(appList)
                loadApplicationsToRecyclerView(appList)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun filterAppInfoBySearchQueryText(
            appInfoList: List<AppInfo>,
            searchQueryText: String?): Single<List<AppInfo>> {
        return if (searchQueryText == null || searchQueryText.isEmpty()) {
            Single.just(appInfoList)
        } else Observable.fromIterable(appInfoList)
                .filter { appInfo: AppInfo -> appInfo.appName.toLowerCase(Locale.getDefault()).contains(searchQueryText.toLowerCase(Locale.getDefault())) }
                .toList()
    }

    private fun sortAppInfo(appInfoList: List<AppInfo>?) {
        Collections.sort(appInfoList) { o1: AppInfo, o2: AppInfo ->
            if (o1.isEnabled && !o2.isEnabled) {
                return@sort -1
            } else if (!o1.isEnabled && o2.isEnabled) {
                return@sort 1
            }
            o1.appName.compareTo(o2.appName, ignoreCase = true)
        }
    }

    private fun createAppInfo(packageInfo: PackageInfo, enabled: Boolean): AppInfo {
        val appInfo = AppInfo()
        appInfo.packageName = packageInfo.packageName
        appInfo.appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
        appInfo.versionName = packageInfo.versionName
        appInfo.icon = packageInfo.applicationInfo.loadIcon(packageManager)
        appInfo.isEnabled = enabled
        return appInfo
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        handleSearchQueryText(query)
        return true
    }

    override fun onQueryTextChange(query: String): Boolean {
        handleSearchQueryText(query)
        return true
    }

    private fun handleSearchQueryText(query: String) {
        listApplications(showSystemApps, query)
    }
}