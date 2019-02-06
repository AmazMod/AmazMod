package com.amazmod.service.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.support.NotificationInfo;

import java.util.List;

public class NotificationListAdapter extends WearableListView.Adapter {
    private final List<NotificationInfo> items;
    private final LayoutInflater mInflater;
    private Context mContext;

    public NotificationListAdapter(Context context, List<NotificationInfo> items) {
        mInflater = LayoutInflater.from(context);
        this.items = items;
        mContext = context;
    }

    @Override
    public @NonNull WearableListView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ItemViewHolder(mInflater.inflate(R.layout.row_notification, null));
    }

    @Override
    public void onBindViewHolder(@NonNull WearableListView.ViewHolder viewHolder, int position) {
        //System.out.println("MenuListAdapter onBindViewHodler position: " + position);
        ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
        final NotificationInfo item = items.get(position);
        populateNotificationIcon(itemViewHolder.notificationIcon,itemViewHolder.notificationIconBadge,item);
        itemViewHolder.notificationTitle.setText(item.getNotificationTitle());
        itemViewHolder.notificationContentPreview.setText(item.getNotificationText());
        itemViewHolder.notificationTime.setText(item.getNotificationTime());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static class ItemViewHolder extends WearableListView.ViewHolder {
        ImageView notificationIcon;
        ImageView notificationIconBadge;
        TextView notificationTitle;
        TextView notificationContentPreview;
        TextView notificationTime;

        ItemViewHolder(View itemView) {
            super(itemView);
            notificationIcon = itemView.findViewById(R.id.row_notification_icon);
            notificationIconBadge = itemView.findViewById(R.id.row_notification_icon_badge);
            notificationTitle = itemView.findViewById(R.id.row_notification_title);
            notificationContentPreview = itemView.findViewById(R.id.row_notification_contents_preview);
            notificationTime = itemView.findViewById(R.id.row_notification_time);
        }
    }


    private void populateNotificationIcon(ImageView iconView, ImageView iconAppView, NotificationInfo item) {
        try {
            byte[] largeIconData = item.getLargeIconData();
            if ((largeIconData != null) && (largeIconData.length > 0)) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(largeIconData, 0, largeIconData.length);

                RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(mContext.getResources(), bitmap);

                roundedBitmapDrawable.setCircular(true);
                roundedBitmapDrawable.setAntiAlias(true);

                iconView.setImageDrawable(roundedBitmapDrawable);
                iconAppView.setImageDrawable(item.getIcon());
            } else {
                iconView.setImageDrawable(item.getIcon());
                iconAppView.setVisibility(View.GONE);
            }
        } catch (Exception exception) {
            Log.d(Constants.TAG, exception.getMessage(), exception);
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