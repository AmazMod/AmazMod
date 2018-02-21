package com.edotasx.amazfit.notification.filter;

import android.service.notification.StatusBarNotification;

/**
 * Created by edoardotassinari on 21/02/18.
 */

public interface PreNotificationFilter {
    boolean filter(StatusBarNotification statusBarNotification);
}
