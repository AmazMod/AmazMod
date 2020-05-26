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
        holder.appInfoAppName.text = app.appName
        holder.appInfoIcon.setImageDrawable(app.icon)
        holder.appInfoVersionName.text = app.versionName
        holder.appInfoPackageName.text = app.packageName
        holder.appInfoSwitch.isChecked = app.isEnabled
        holder.appInfoButton.isEnabled = app.isEnabled

        holder.appInfoButton.setOnClickListener {
            if (app.isEnabled) {
                val intent = Intent(mActivity, NotificationPackageOptionsActivity::class.java)
                intent.putExtra("app", app.packageName)
                mActivity.startActivity(intent)
            }
        }

        holder.appInfoSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked != app.isEnabled) {
                app.isEnabled = isChecked
                notifyItemChanged(position)
                holder.appInfoButton.isEnabled = isChecked
            }
        }
    }

    override fun getItemCount(): Int {
        return appList.size
    }

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var appInfoButton: ImageView = view.findViewById(R.id.row_appinfo_button)
        var appInfoIcon: ImageView  = view.findViewById(R.id.row_appinfo_icon)
        var appInfoAppName: TextView = view.findViewById(R.id.row_app_info_appname)
        var appInfoPackageName: TextView  = view.findViewById(R.id.row_app_info_package_name)
        var appInfoVersionName: TextView = view.findViewById(R.id.row_appinfo_version)
        var appInfoSwitch: Switch  = view.findViewById(R.id.row_appinfo_switch)
    }
}