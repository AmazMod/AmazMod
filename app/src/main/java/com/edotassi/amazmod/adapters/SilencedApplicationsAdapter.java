package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.support.SilenceApplicationHelper;
import com.edotassi.amazmod.ui.fragment.SilencedApplicationsFragment;
import org.tinylog.Logger;
import java.util.List;

public class SilencedApplicationsAdapter extends ArrayAdapter<NotificationPreferencesEntity> {

    private SilencedApplicationsFragment mActivity;
    private Context mContext;

    public SilencedApplicationsAdapter(SilencedApplicationsFragment activity, int resource, @NonNull List<NotificationPreferencesEntity> objects) {
        super(activity.getContext(), resource, objects);
        mContext = activity.getContext();
        mActivity = activity;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View gridItem = convertView;
        if (gridItem == null) {
            gridItem = LayoutInflater.from(mContext).inflate(R.layout.item_silenced_app, parent, false);
        }

        final NotificationPreferencesEntity currentSilencedApplication = getItem(position);
        ViewHolder viewHolder = new ViewHolder(mActivity, gridItem, currentSilencedApplication);

        try {
            PackageManager packageManager = mContext.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(currentSilencedApplication.getPackageName(),0);
            String packageLabel = packageInfo.applicationInfo.loadLabel(packageManager).toString();
            Drawable packageIcon = packageInfo.applicationInfo.loadIcon(packageManager);

            viewHolder.name.setText(packageLabel);
            viewHolder.icon.setImageDrawable(packageIcon);
            viewHolder.silencedUntil.setText(SilenceApplicationHelper.getTimeSecondsReadable(currentSilencedApplication.getSilenceUntil()));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return gridItem;
    }

    static class ViewHolder {
        ImageView close;
        ImageView icon;
        TextView name;
        TextView silencedUntil;
        private SilencedApplicationsFragment mActivity;

        public ViewHolder(SilencedApplicationsFragment activity, View view, NotificationPreferencesEntity silencedApplication) {
            this.mActivity = activity;
            close = view.findViewById(R.id.item_silenced_app_close);
            icon = view.findViewById(R.id.item_silenced_app_icon);
            name = view.findViewById(R.id.item_silenced_app_appname);
            silencedUntil = view.findViewById(R.id.item_silenced_app_silenced_until);

            icon.setOnClickListener(v -> {
                Logger.debug("SilencedApplicationsAdapter onClick: cancel silence of package " + silencedApplication.getPackageName());
                SilenceApplicationHelper.cancelSilence(silencedApplication.getPackageName());
                mActivity.updateSilencedApps();
            });
        }
    }
}