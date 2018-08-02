package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationEntity;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationLogAdapter extends ArrayAdapter<NotificationEntity> {

    private Context context;

    private Map<Integer, String> causesTranslationsMap;
    private Map<String, Drawable> appsIconsMap;
    private Drawable fallbackAppIcon;

    public NotificationLogAdapter(@NonNull Context context, int resource, @NonNull List<NotificationEntity> objects) {
        super(context, resource, objects);

        this.context = context;

        causesTranslationsMap = new HashMap<>();
        appsIconsMap = new HashMap<>();

        ArrayList<Integer> causes = new ArrayList<Integer>() {{
            add(R.string.notification_block);
            add(R.string.notification_continue);
            add(R.string.notification_group);
            add(R.string.notification_local);
            add(R.string.notification_ongoing);
            add(R.string.notification_return);
            add(R.string.notification_package);
            add(R.string.notification_voice);
        }};

        for (Integer stringKey : causes) {
            causesTranslationsMap.put(stringKey, context.getString(stringKey).toUpperCase());
        }

        fallbackAppIcon = context.getDrawable(R.drawable.ic_launcher_round);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.row_notification_log, parent, false);
        }

        NotificationEntity notificationEntity = getItem(position);

        ViewHolder viewHolder = new ViewHolder(context, notificationEntity, causesTranslationsMap, appsIconsMap, fallbackAppIcon);
        viewHolder.sync(listItem);

        return listItem;
    }

    static class ViewHolder {

        @BindView(R.id.row_notification_log_app_icon)
        ImageView appIconImageView;

        @BindView(R.id.row_notification_log_package)
        TextView packageNameTextView;

        @BindView(R.id.row_notification_log_cause)
        TextView causeTextView;

        @BindView(R.id.row_notification_log_date)
        TextView dateTextView;

        private Context context;
        private NotificationEntity notificationEntity;
        private Map<Integer, String> causesTranslationsMap;
        private Map<String, Drawable> appsIconsMap;
        private Drawable fallbackAppIcon;

        ViewHolder(Context context,
                   NotificationEntity notificationEntity,
                   Map<Integer, String> causesTranslationsMap,
                   Map<String, Drawable> appsIconsMap,
                   Drawable fallbackAppIcon) {
            this.context = context;
            this.notificationEntity = notificationEntity;
            this.causesTranslationsMap = causesTranslationsMap;
            this.appsIconsMap = appsIconsMap;
            this.fallbackAppIcon = fallbackAppIcon;
        }

        void sync(View view) {
            ButterKnife.bind(this, view);

            String causeText = "";
            switch (notificationEntity.getFilterResult()) {
                case (Constants.FILTER_BLOCK): {
                    causeText = causesTranslationsMap.get(R.string.notification_block);
                    break;
                }
                case (Constants.FILTER_CONTINUE): {
                    causeText = causesTranslationsMap.get(R.string.notification_continue);
                    break;
                }
                case (Constants.FILTER_GROUP): {
                    causeText = causesTranslationsMap.get(R.string.notification_group);
                    break;
                }
                case (Constants.FILTER_LOCAL): {
                    causeText = causesTranslationsMap.get(R.string.notification_local);
                    break;
                }
                case (Constants.FILTER_ONGOING): {
                    causeText = causesTranslationsMap.get(R.string.notification_ongoing);
                    break;
                }
                case (Constants.FILTER_RETURN): {
                    causeText = causesTranslationsMap.get(R.string.notification_return);
                    break;
                }
                case (Constants.FILTER_PACKAGE): {
                    causeText = causesTranslationsMap.get(R.string.notification_package);
                    break;
                }
                case (Constants.FILTER_VOICE): {
                    causeText = causesTranslationsMap.get(R.string.notification_voice);
                    break;
                }
            }

            String dateText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(notificationEntity.getDate()));
            dateTextView.setText(dateText);

            String packageName = notificationEntity.getPackageName();
            Drawable appIcon = appsIconsMap.get(packageName);
            if (appIcon == null) {
                try {
                    appIcon = context.getPackageManager().getApplicationIcon(packageName);
                    appsIconsMap.put(packageName, appIcon);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    appIcon = fallbackAppIcon;
                }
            }
            appIconImageView.setImageDrawable(appIcon);

            packageNameTextView.setText(packageName);
            causeTextView.setText(causeText);
        }
    }
}
