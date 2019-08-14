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

import org.tinylog.Logger;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SilencedApplicationsAdapter extends ArrayAdapter<NotificationPreferencesEntity> {

    private Bridge silencedApplicationsBridge;
    private Context context;

    public SilencedApplicationsAdapter(Bridge silencedApplicationsBridge, int resource, @NonNull List<NotificationPreferencesEntity> objects) {
        super(silencedApplicationsBridge.getContext(), resource, objects);

        this.silencedApplicationsBridge = silencedApplicationsBridge;
        context = silencedApplicationsBridge.getContext();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View gridItem = convertView;
        if (gridItem == null) {
            gridItem = LayoutInflater.from(context).inflate(R.layout.item_silenced_app, parent, false);
        }

        final NotificationPreferencesEntity currentSilencedApplication = getItem(position);

        ViewHolder viewHolder = new ViewHolder(silencedApplicationsBridge, currentSilencedApplication, context);
        ButterKnife.bind(viewHolder, gridItem);

        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(currentSilencedApplication.getPackageName(),0);
            String packageLabel = packageInfo.applicationInfo.loadLabel(packageManager).toString();
            Drawable packageIcon = packageInfo.applicationInfo.loadIcon(packageManager);

            viewHolder.silencedApplicationNameView.setText(packageLabel);
            viewHolder.silencedApplicationIconView.setImageDrawable(packageIcon);
            viewHolder.silencedApplicationSilencedUntilView.setText(SilenceApplicationHelper.getTimeSecondsReadable(currentSilencedApplication.getSilenceUntil()));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return gridItem;
    }

    static class ViewHolder {

        @BindView(R.id.item_silenced_app_close)
        ImageView silencedApplicationCloseView;
        @BindView(R.id.item_silenced_app_icon)
        ImageView silencedApplicationIconView;
        @BindView(R.id.item_silenced_app_appname)
        TextView silencedApplicationNameView;
        @BindView(R.id.item_silenced_app_silenced_until)
        TextView silencedApplicationSilencedUntilView;


        private Bridge silencedApplicationBridge;
        private NotificationPreferencesEntity silencedApplication;
        private Context context;

        public ViewHolder(Bridge silencedApplicationBridge, NotificationPreferencesEntity silencedApplication, Context context) {
            this.silencedApplicationBridge = silencedApplicationBridge;
            this.silencedApplication = silencedApplication;
            this.context = context;
        }

        @OnClick(R.id.item_silenced_app_icon)
        public void onIconClick() {
            Logger.debug("SilencedApplicationsAdapter onClick: cancel silence of package " + silencedApplication.getPackageName());
            SilenceApplicationHelper.cancelSilence(silencedApplication.getPackageName());
            silencedApplicationBridge.onSilencedApplicationStatusChange();
        }
    }

    public interface Bridge {
        void onSilencedApplicationStatusChange();
        Context getContext();
    }
}