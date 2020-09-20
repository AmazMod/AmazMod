package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.databinding.RowNotificationLogBinding;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.ui.NotificationsLogActivity;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import amazmod.com.transport.Constants;

public class NotificationLogAdapter extends ArrayAdapter<NotificationEntity> {

    private NotificationsLogActivity mActivity;
    private RowNotificationLogBinding binding;
    private Map<Integer, String> causesTranslationsMap;
    private Map<String, Drawable> appsIconsMap;
    private Drawable fallbackAppIcon;

    public NotificationLogAdapter(@NonNull NotificationsLogActivity activity, int resource, @NonNull List<NotificationEntity> objects) {
        super(activity, resource, objects);
        this.mActivity = activity;
        causesTranslationsMap = new ArrayMap<>();
        appsIconsMap = new ArrayMap<>();

        ArrayList<Integer> causes = new ArrayList<Integer>() {{
            add(R.string.notification_block);
            add(R.string.notification_continue);
            add(R.string.notification_group);
            add(R.string.notification_local);
            add(R.string.notification_ongoing);
            add(R.string.notification_return);
            add(R.string.notification_disabled);
            add(R.string.notification_screenon);
            add(R.string.notification_screenlocked);
            add(R.string.notification_package);
            add(R.string.notification_voice);
            add(R.string.notification_maps);
            add(R.string.notification_ungroup);
            add(R.string.notification_localok);
            add(R.string.notification_silenced);
            add(R.string.notification_text_filter);
        }};
        for (Integer stringKey : causes) {
            causesTranslationsMap.put(stringKey, mActivity.getString(stringKey).toUpperCase());
        }
        fallbackAppIcon = mActivity.getDrawable(R.drawable.ic_launcher_foreground);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        binding = RowNotificationLogBinding.inflate(mActivity.getLayoutInflater());
        View listItem = convertView;
        if (listItem == null) {
            listItem = binding.getRoot();
        }
        NotificationEntity notificationEntity = getItem(position);
        ViewHolder viewHolder = new ViewHolder(mActivity, binding, notificationEntity, causesTranslationsMap, appsIconsMap, fallbackAppIcon);
        viewHolder.sync(listItem);

        return listItem;
    }

    static class ViewHolder {
        private Context mActivity;
        private RowNotificationLogBinding binding;
        private NotificationEntity notificationEntity;
        private Map<Integer, String> causesTranslationsMap;
        private Map<String, Drawable> appsIconsMap;
        private Drawable fallbackAppIcon;

        ViewHolder(NotificationsLogActivity activity,
                   RowNotificationLogBinding binding,
                   NotificationEntity notificationEntity,
                   Map<Integer, String> causesTranslationsMap,
                   Map<String, Drawable> appsIconsMap,
                   Drawable fallbackAppIcon) {
            this.binding = binding;
            this.mActivity = activity;
            this.notificationEntity = notificationEntity;
            this.causesTranslationsMap = causesTranslationsMap;
            this.appsIconsMap = appsIconsMap;
            this.fallbackAppIcon = fallbackAppIcon;
        }

        void sync(View view) {
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
                case (Constants.FILTER_NOTIFICATIONS_DISABLED): {
                    causeText = causesTranslationsMap.get(R.string.notification_disabled);
                    break;
                }
                case (Constants.FILTER_SCREENON): {
                    causeText = causesTranslationsMap.get(R.string.notification_screenon);
                    break;
                }
                case (Constants.FILTER_SCREENLOCKED): {
                    causeText = causesTranslationsMap.get(R.string.notification_screenlocked);
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
                case (Constants.FILTER_MAPS): {
                    causeText = causesTranslationsMap.get(R.string.notification_maps);
                    break;
                }
                case (Constants.FILTER_UNGROUP): {
                    causeText = causesTranslationsMap.get(R.string.notification_ungroup);
                    break;
                }
                case (Constants.FILTER_LOCALOK): {
                    causeText = causesTranslationsMap.get(R.string.notification_localok);
                    break;
                }
                case (Constants.FILTER_SILENCE): {
                    causeText = causesTranslationsMap.get(R.string.notification_silenced);
                    break;
                }
                case (Constants.FILTER_TEXT): {
                    causeText = causesTranslationsMap.get(R.string.notification_text_filter);
                    break;
                }
            }

            String dateText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(notificationEntity.getDate()));
            binding.rowNotificationLogDate.setText(dateText);

            String packageName = notificationEntity.getPackageName();
            Drawable appIcon = appsIconsMap.get(packageName);
            if (appIcon == null) {
                try {
                    appIcon = mActivity.getPackageManager().getApplicationIcon(packageName);
                    appsIconsMap.put(packageName, appIcon);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    appIcon = fallbackAppIcon;
                }
            }
            binding.rowNotificationLogAppIcon.setImageDrawable(appIcon);
            binding.rowNotificationLogPackage.setText(packageName);
            binding.rowNotificationLogCause.setText(causeText);
        }
    }
}
