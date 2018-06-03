package com.edotassi.amazmodcompanionservice.notifications;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import com.edotassi.amazmodcompanionservice.Constants;
import com.edotassi.amazmodcompanionservice.settings.SettingsManager;
import com.edotassi.amazmodcompanionservice.util.DeviceUtil;
import com.huami.watch.notification.data.NotificationData;
import com.huami.watch.notification.data.StatusBarNotificationData;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;

public class NotificationSpecFactory {

    public static NotificationSpec getNotificationSpec(Context context, DataBundle dataBundle) {
        if (dataBundle == null) {
            return null;
        }

        SettingsManager settingsManager = new SettingsManager(context);

        StatusBarNotificationData statusBarNotificationData = dataBundle.getParcelable("payload");
        if (statusBarNotificationData == null) {
            return null;
        }

        /*
        NotificationSpec notificationSpec = extractDataStatusBarNotificationData(statusBarNotificationData);
        if (notificationSpec == null) {
            return null;
        }

        notificationSpec.setVibration(settingsManager.getInt(Constants.PREF_NOTIFICATION_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATION_VIBRATION));
        notificationSpec.setTimeoutRelock(settingsManager.getInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, Constants.PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT));
        notificationSpec.setDeviceLocked(DeviceUtil.isDeviceLocked(context));

        return notificationSpec;
        */
        return null;
    }

    private static NotificationSpec extractDataStatusBarNotificationData(StatusBarNotificationData statusBarNotificationData) {
        NotificationData notificationData = statusBarNotificationData.notification;
        if (notificationData == null) {
            return null;
        }

        NotificationSpec notificationSpec = new NotificationSpec();

        notificationSpec.setKey(statusBarNotificationData.key);
        notificationSpec.setTitle(notificationData.title);
        notificationSpec.setText(notificationData.text);
        //notificationSpec.setIcon(notificationData.smallIcon);
        notificationSpec.setId(statusBarNotificationData.id);

        return notificationSpec;
    }

    public static Bundle toBundle(NotificationSpec notificationSpec) {
        return toBundle(new Bundle(), notificationSpec);
    }

    public static Bundle toBundle(Bundle bundle, NotificationSpec notificationSpec) {
        bundle.putParcelable("notificationSpec", notificationSpec);
        return bundle;
    }
}
