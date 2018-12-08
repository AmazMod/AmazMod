package com.amazmod.service.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazmod.service.R;
import com.amazmod.service.support.AppInfo;

import java.util.List;

public class NotificationListAdapter extends WearableListView.Adapter {
    private final List<AppInfo> items;
    private final LayoutInflater mInflater;

    public NotificationListAdapter(Context context, List<AppInfo> items) {
        mInflater = LayoutInflater.from(context);
        this.items = items;
    }

    @Override
    public @NonNull WearableListView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ItemViewHolder(mInflater.inflate(R.layout.row_notification, null));
    }

    @Override
    public void onBindViewHolder(@NonNull WearableListView.ViewHolder viewHolder, int position) {
        //System.out.println("MenuListAdapter onBindViewHodler position: " + position);
        ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
        final AppInfo item = items.get(position);
        itemViewHolder.notificationIcon.setImageDrawable(item.getIcon());
        itemViewHolder.notificationTitle.setText(item.getAppName());
        itemViewHolder.notificationTime.setText(item.getPackageName());

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static class ItemViewHolder extends WearableListView.ViewHolder {
        ImageView notificationIcon;
        TextView notificationTitle;
        TextView notificationTime;

        ItemViewHolder(View itemView) {
            super(itemView);
            notificationIcon = itemView.findViewById(R.id.row_notification_icon);
            notificationTitle = itemView.findViewById(R.id.row_notification_title);
            notificationTime = itemView.findViewById(R.id.row_notification_time);
        }
    }

    public void clear() {
        int size = this.items.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                items.remove(0);
            }
            this.notifyItemRangeRemoved(0, size);
        }
    }

}