package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.support.AppInfo;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class AppInfoAdapter extends ArrayAdapter<AppInfo> {

    private Bridge appInfoBridge;
    private Context context;

    public AppInfoAdapter(Bridge appInfoBridge, int resource, @NonNull List<AppInfo> objects) {
        super(appInfoBridge.getContext(), resource, objects);

        this.appInfoBridge = appInfoBridge;
        context = appInfoBridge.getContext();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.row_appinfo, parent, false);
        }

        AppInfo currentAppInfo = getItem(position);

        ViewHolder viewHolder = new ViewHolder(appInfoBridge, currentAppInfo);
        ButterKnife.bind(viewHolder, listItem);

        viewHolder.appInfoAppName.setText(currentAppInfo.getAppName());
        viewHolder.appInfoIcon.setImageDrawable(currentAppInfo.getIcon());
        viewHolder.appInfoVersionName.setText(currentAppInfo.getVersionName());
        viewHolder.appInfoPackageName.setText(currentAppInfo.getPackageName());
        viewHolder.appInfoSwitch.setChecked(currentAppInfo.isEnabled());

        return listItem;
    }

    static class ViewHolder {

        @BindView(R.id.row_appinfo_icon)
        ImageView appInfoIcon;
        @BindView(R.id.row_app_info_appname)
        TextView appInfoAppName;
        @BindView(R.id.row_app_info_package_name)
        TextView appInfoPackageName;
        @BindView(R.id.row_appinfo_version)
        TextView appInfoVersionName;
        @BindView(R.id.row_appinfo_switch)
        Switch appInfoSwitch;

        private Bridge appInfoBridge;
        private AppInfo appInfo;

        public ViewHolder(Bridge appInfoBridge, AppInfo appInfo) {
            this.appInfoBridge = appInfoBridge;
            this.appInfo = appInfo;
        }

        @OnCheckedChanged(R.id.row_appinfo_switch)
        public void onSwitchChanged(Switch switchWidget, boolean checked) {
            appInfo.setEnabled(checked);
            appInfoBridge.onAppInfoStatusChange();
        }
    }

    public interface Bridge {
        void onAppInfoStatusChange();

        Context getContext();
    }
}