package com.edotassi.amazmod.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationLogAdapter extends ArrayAdapter<NotificationEntity> {

    private Context context;

    private Map<Integer, String> causesTranslationsMap;

    public NotificationLogAdapter(@NonNull Context context, int resource, @NonNull List<NotificationEntity> objects) {
        super(context, resource, objects);

        this.context = context;

        causesTranslationsMap = new HashMap<>();

        ArrayList<Integer> causes = new ArrayList<Integer>(){{
           add(R.string.notification_block);
           add(R.string.notification_continue);
           add(R.string.notification_group);
           add(R.string.notification_local);
           add(R.string.notification_ongoing);
           add(R.string.notification_return);
           add(R.string.notification_package);
           add(R.string.notification_voice);
        }};

        for(Integer stringKey : causes) {
            causesTranslationsMap.put(stringKey, context.getString(stringKey));
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.row_notification_log, parent, false);
        }

        NotificationEntity notificationEntity = getItem(position);

        ViewHolder viewHolder = new ViewHolder(notificationEntity, causesTranslationsMap);
        viewHolder.sync(listItem);

        return listItem;
    }

    static class ViewHolder {

        @BindView(R.id.row_notification_log_package)
        TextView packageName;

        @BindView(R.id.row_notification_log_cause)
        TextView cause;

        private NotificationEntity notificationEntity;
        private Map<Integer, String> causesTranslationsMap;

        ViewHolder(NotificationEntity notificationEntity,
                   Map<Integer, String> causesTranslationsMap) {
            this.notificationEntity = notificationEntity;
            this.causesTranslationsMap = causesTranslationsMap;
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

            packageName.setText(notificationEntity.getPackageName());
            cause.setText(causeText);
        }
    }
}
