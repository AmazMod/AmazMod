package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.support.AppInfo;
import com.edotassi.amazmod.support.SilenceApplicationHelper;
import com.edotassi.amazmod.ui.NotificationPackageOptionsActivity;

import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;

public class WidgetsAdapter extends ArrayAdapter<AppInfo> {

    private Bridge widgetsBridge;
    private Context context;

    public WidgetsAdapter(Bridge widgetsBridge, int resource, @NonNull List<AppInfo> objects) {
        super(widgetsBridge.getContext(), resource, objects);

        this.widgetsBridge = widgetsBridge;
        context = widgetsBridge.getContext();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.row_drag_widgets, parent, false);
        }

        final AppInfo currentAppInfo = Objects.requireNonNull(getItem(position));

        ViewHolder viewHolder = new ViewHolder(widgetsBridge, currentAppInfo);
        ButterKnife.bind(viewHolder, listItem);

        viewHolder.widgetAppName.setText(currentAppInfo.getAppName());
        viewHolder.widgetPackageName.setText(currentAppInfo.getPackageName());
        viewHolder.widgetSwitch.setChecked(currentAppInfo.isEnabled());

        return listItem;
    }

    static class ViewHolder {

        @BindView(R.id.row_widget_name)
        TextView widgetAppName;
        @BindView(R.id.row_widget_pkg)
        TextView widgetPackageName;
        @BindView(R.id.row_widget_switch)
        Switch widgetSwitch;


        private Bridge widgetBridge;
        private AppInfo widgetInfo;

        public ViewHolder(Bridge appInfoBridge, AppInfo appInfo) {
            this.widgetBridge = appInfoBridge;
            this.widgetInfo = appInfo;
        }

        @OnCheckedChanged(R.id.row_widget_switch)
        void onSwitchChanged(Switch switchWidget, boolean checked) {
            if (checked != widgetInfo.isEnabled()) {
                SilenceApplicationHelper.setPackageEnabled(widgetInfo.getPackageName(), checked);
                widgetInfo.setEnabled(checked);
                widgetBridge.onAppInfoStatusChange();
            }
        }
    }

    public interface Bridge {
        void onAppInfoStatusChange();

        Context getContext();
    }
}