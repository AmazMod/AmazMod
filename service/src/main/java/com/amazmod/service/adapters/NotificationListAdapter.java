package com.amazmod.service.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.annotation.NonNull;
import androidx.emoji.widget.EmojiTextView;

import com.amazmod.service.R;
import com.amazmod.service.support.NotificationInfo;
import com.amazmod.service.util.FragmentUtil;

import org.tinylog.Logger;

import java.util.List;

public class NotificationListAdapter extends WearableListView.Adapter {
    private final List<NotificationInfo> items;
    private final LayoutInflater mInflater;
    private Context mContext;
    private FragmentUtil util;

    public NotificationListAdapter(Context context, List<NotificationInfo> items) {
        mInflater = LayoutInflater.from(context);
        this.items = items;
        mContext = context;

        // Load FragmentUtil because for font change function is needed
        util = new FragmentUtil(mContext);
    }

    @Override
    public @NonNull WearableListView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ItemViewHolder(mInflater.inflate(R.layout.row_notification, null));
    }

    @Override
    public void onBindViewHolder(@NonNull WearableListView.ViewHolder viewHolder, int position) {
        //System.out.println("MenuListAdapter onBindViewHodler position: " + position);

        // Populate notification
        ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
        final NotificationInfo item = items.get(position);
        populateNotificationIcon(itemViewHolder.notificationIcon,itemViewHolder.notificationIconBadge,item);
        itemViewHolder.notificationTitle.setText(item.getNotificationTitle());
        itemViewHolder.notificationContentPreview.setText(item.getNotificationText());
        itemViewHolder.notificationTime.setText(item.getNotificationTime());

        // Code changed to identify special languages (eg Hebrew)
        util.setFontLocale(itemViewHolder.notificationTitle, item.getNotificationTitle());
        util.setFontLocale(itemViewHolder.notificationContentPreview, item.getNotificationText());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static class ItemViewHolder extends WearableListView.ViewHolder {
        ImageView notificationIcon;
        ImageView notificationIconBadge;
        EmojiTextView notificationTitle;
        EmojiTextView notificationContentPreview;
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
            Logger.error(exception, "populateNotificationIcon exception: {}", exception.getMessage());
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