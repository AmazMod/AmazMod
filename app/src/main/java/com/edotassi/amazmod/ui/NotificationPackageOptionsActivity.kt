package com.edotassi.amazmod.ui

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import com.edotassi.amazmod.R
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity_Table
import com.edotassi.amazmod.support.SilenceApplicationHelper
import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.sql.language.SQLite
import kotlinx.android.synthetic.main.activity_notification_package_options.*
import org.tinylog.kotlin.Logger

class NotificationPackageOptionsActivity : BaseAppCompatActivity() {

    var app: NotificationPreferencesEntity? = null
    lateinit var packageInfo: PackageInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_package_options)
        try {
            if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        } catch (exception: NullPointerException) {
            Logger.error("FilesExtrasActivity onCreate NullPointerException: $exception")
        }
        val intent = intent
        val packageName = intent.getStringExtra("app")
        if (packageName == null) {
            finish()
            return
        }
        app = loadApp(packageName)
        if (app == null) {
            //Toast.makeText(this, "Package " + packageName + "is not enabled", Toast.LENGTH_SHORT).show();
            finish()
        } else {
            try {
                packageInfo = packageManager.getPackageInfo(packageName, 0)
                activity_notifopts_appinfo_appname.text = packageInfo.applicationInfo.loadLabel(packageManager).toString()
                supportActionBar!!.setTitle(resources.getString(R.string.app_options))
                appinfo_package_name.text = packageInfo.packageName
                appinfo_version.text = packageInfo.versionName
                appinfo_icon.setImageDrawable(packageInfo.applicationInfo.loadIcon(packageManager))
                edittext_filter.setText(app!!.filter)

                // Check if app is muted
                if (app!!.silenceUntil > 0) {
                    silenced_until.setText(SilenceApplicationHelper.getTimeSecondsReadable(app!!.silenceUntil))
                } else {
                    cancel_button.isEnabled = false
                }

                // Whitelist Filter
                if (app!!.isWhitelist) {
                    filter_description.text = resources.getString(R.string.whitelist_notification_options_description)
                } else
                    filter_description.text = resources.getString(R.string.notification_options_description)
                whitelist_switch.isChecked = app!!.isWhitelist
                whitelist_switch.setOnCheckedChangeListener { _, isChecked ->
                    app!!.isWhitelist = isChecked
                    Logger.debug("set filter as whitelist: $isChecked")
                    if (isChecked) {
                        filter_description.text = resources.getString(R.string.whitelist_notification_options_description)
                    } else {
                        filter_description.text = resources.getString(R.string.notification_options_description)
                    }
                }

                // Set filter level
                filter_level.setSelection(app!!.filterLevel)
                filter_level.onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View, pos: Int, id: Long) {
                        app!!.filterLevel = pos
                    }

                    override fun onNothingSelected(arg0: AdapterView<*>?) {
                        // Auto-generated method stub
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {

                //Toast.makeText(this, "Package " + packageName + "not found", Toast.LENGTH_SHORT).show();
                finish()
            }
        }
        cancel_button.setOnClickListener {
            app!!.silenceUntil = 0
            silenced_until.setText("")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        save()
        finish()
        return true
    }

    override fun onBackPressed() {
        save()
        finish()
        super.onBackPressed()
    }

    private fun save() {
        updatePackage(app)
    }

    private fun updatePackage(application: NotificationPreferencesEntity?) {
        var app = application
        val insert = app == null
        if (insert) {
            app = NotificationPreferencesEntity()
        }
        app!!.packageName = packageInfo.packageName
        app.filter = edittext_filter.text.toString()
        app.isWhitelist = whitelist_switch.isChecked
        app.filterLevel = filter_level.selectedItemPosition
        if (insert) {
            Logger.debug("STORING " + packageInfo.packageName + " in AmazmodDB.NotificationPreferences")
            FlowManager
                    .getModelAdapter(NotificationPreferencesEntity::class.java)
                    .insert(app)
        } else {
            Logger.debug("UPDATING " + packageInfo.packageName + " in AmazmodDB.NotificationPreferences")
            FlowManager
                    .getModelAdapter(NotificationPreferencesEntity::class.java)
                    .update(app)
        }
    }

    private fun loadApp(packageName: String): NotificationPreferencesEntity? {
        return SQLite
                .select()
                .from(NotificationPreferencesEntity::class.java)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle()
    }
}