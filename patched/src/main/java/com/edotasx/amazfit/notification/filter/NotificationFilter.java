package com.edotasx.amazfit.notification.filter;

import android.service.notification.StatusBarNotification;

/**
 * Created by edoardotassinari on 29/03/18.
 */

public interface NotificationFilter {

    String getPackage();

    boolean filter(StatusBarNotification statusBarNotification);
}
