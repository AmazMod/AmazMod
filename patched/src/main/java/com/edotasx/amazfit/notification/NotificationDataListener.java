package com.edotasx.amazfit.notification;

import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.huami.watch.companion.notification.NotificationAccessService;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;

public class NotificationDataListener implements Transporter.DataListener {

    private NotificationAccessService notificationAccessService;

    public NotificationDataListener(NotificationAccessService notificationAccessService) {
        this.notificationAccessService = notificationAccessService;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onDataReceived(TransportDataItem transportDataItem) {
        String action = transportDataItem.getAction();

        if (action == null) {
            return;
        }

        switch (action) {
            case "reply": {
                DataBundle dataBundle = transportDataItem.getData();
                String key = dataBundle.getString("key");
                String message = dataBundle.getString("message");

                Log.d(Constants.TAG_NOTIFICATION, "action reply, key: " + key + ", message: " + message);

                if (notificationAccessService.notificationsSent == null) {
                    Log.d(Constants.TAG_NOTIFICATION, "notificationsSent is null!");
                    return;
                }

                StatusBarNotification statusBarNotification = notificationAccessService.notificationsSent.get(key);
                if (statusBarNotification == null) {
                    Log.d(Constants.TAG_NOTIFICATION, "statusbarNotification is null!");
                    return;
                }

                NotificationManager
                        .sharedInstance(notificationAccessService)
                        .replyToNotification(statusBarNotification, message);
            }
        }
    }
}
