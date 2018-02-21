package com.edotasx.amazfit.notification.filter;

import android.service.notification.StatusBarNotification;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public interface NotificationFilter extends PreNotificationFilter {
    String getPackage();
}
