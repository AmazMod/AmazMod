package com.edotassi.amazmod.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.edotassi.amazmod.R
import com.edotassi.amazmod.support.AppInfo
import com.edotassi.amazmod.ui.ApplicationSelectActivity
import com.edotassi.amazmod.ui.NotificationPackageOptionsActivity


class ApplicationAdapter(private val mActivity: ApplicationSelectActivity, private val appList: List<AppInfo>) : RecyclerView.Adapter<ApplicationAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(mActivity).inflate(R.layout.row_appinfo, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appList[position]
        holder.setApp(app)
    }

    override fun getItemCount(): Int {
        return appList.size
    }

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var app : AppInfo

        var appInfoButton: ImageView = view.findViewById(R.id.row_appinfo_button)
        var appInfoIcon: ImageView  = view.findViewById(R.id.row_appinfo_icon)
        var appInfoAppName: TextView = view.findViewById(R.id.row_app_info_appname)
        var appInfoPackageName: TextView  = view.findViewById(R.id.row_app_info_package_name)
        var appInfoVersionName: TextView = view.findViewById(R.id.row_appinfo_version)
        var appInfoSwitch: Switch  = view.findViewById(R.id.row_appinfo_switch)

        init {
            appInfoButton.setOnClickListener {
                if (app.isEnabled) {
                    val intent = Intent(mActivity, NotificationPackageOptionsActivity::class.java)
                    intent.putExtra("app", app.packageName)
                    mActivity.startActivity(intent)
                }
            }

            appInfoSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != app.isEnabled) {
                    app.isEnabled = isChecked
                    appInfoButton.isEnabled = isChecked
                    mActivity.sortRecyclerView()
                }
            }
        }


        fun setApp(app:AppInfo) {
            this.app = app
            appInfoAppName.text = app.appName;
            appInfoIcon.setImageDrawable(app.icon)
            appInfoVersionName.text = app.versionName
            appInfoPackageName.text = app.packageName
            appInfoSwitch.isChecked = app.isEnabled
            appInfoButton.isEnabled = app.isEnabled

        }
    }
}