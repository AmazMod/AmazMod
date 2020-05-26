package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.databinding.ActivityNotificationPackagesSelectorBinding;
import com.edotassi.amazmod.databinding.RowAppinfoBinding;
import com.edotassi.amazmod.support.AppInfo;
import com.edotassi.amazmod.ui.NotificationPackageOptionsActivity;

import java.util.List;
import java.util.Objects;


public class AppInfoAdapter extends ArrayAdapter<AppInfo> {

    private Bridge appInfoBridge;
    private Context context;
    private RowAppinfoBinding binding;

    public AppInfoAdapter(Bridge appInfoBridge, int resource, @NonNull List<AppInfo> objects) {
        super(appInfoBridge.getContext(), resource, objects);

        this.appInfoBridge = appInfoBridge;
        context = appInfoBridge.getContext();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        binding = RowAppinfoBinding.inflate(LayoutInflater.from(context));
        if (listItem == null) {
            listItem = binding.getRoot();
            //LayoutInflater.from(context).inflate(R.layout.row_appinfo, parent, false);
        }

        final AppInfo currentAppInfo = Objects.requireNonNull(getItem(position));

        ViewHolder viewHolder = new ViewHolder(binding, appInfoBridge, currentAppInfo);

        viewHolder.binding.rowAppInfoAppname.setText(currentAppInfo.getAppName());
        viewHolder.binding.rowAppinfoIcon.setImageDrawable(currentAppInfo.getIcon());
        viewHolder.binding.rowAppinfoVersion.setText(currentAppInfo.getVersionName());
        viewHolder.binding.rowAppInfoPackageName.setText(currentAppInfo.getPackageName());
        viewHolder.binding.rowAppinfoSwitch.setChecked(currentAppInfo.isEnabled());
        viewHolder.binding.rowAppinfoButton.setEnabled(currentAppInfo.isEnabled());

        viewHolder.binding.rowAppinfoButton.setOnClickListener(view -> {
            if (currentAppInfo.isEnabled()) {
                Intent intent = new Intent(context, NotificationPackageOptionsActivity.class);
                intent.putExtra("app", currentAppInfo.getPackageName());
                context.startActivity(intent);
            }
        });
        return listItem;
    }

    static class ViewHolder {

        private RowAppinfoBinding binding;

        private Bridge appInfoBridge;
        private AppInfo appInfo;

        public ViewHolder(RowAppinfoBinding binding, Bridge appInfoBridge, AppInfo appInfo) {
            this.binding = binding;
            this.appInfoBridge = appInfoBridge;
            this.appInfo = appInfo;

            binding.rowAppinfoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != appInfo.isEnabled()) {
                    appInfo.setEnabled(isChecked);
                    appInfoBridge.onAppInfoStatusChange();
                    binding.rowAppinfoButton.setEnabled(isChecked);
                }
            });
        }
    }

    public interface Bridge {
        void onAppInfoStatusChange();

        Context getContext();
    }
}