package com.edotasx.amazfit.notification;

import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;

import com.edotasx.amazfit.notification.filter.DefaultNotificationFilter;
import com.edotasx.amazfit.notification.filter.NotificationFilter;
import com.edotasx.amazfit.notification.filter.WhatsappNotificationFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public class NotificationManager {

    private Map<String, NotificationFilter> mNotificationFilters;
    private NotificationFilter mDefaultNotificationFilter;

    private static NotificationManager mInstace;

    public static NotificationManager sharedInstance() {
        if (mInstace == null) {
            mInstace = new NotificationManager();
        }

        return mInstace;
    }

    private NotificationManager() {
        mNotificationFilters = new HashMap<>();

        WhatsappNotificationFilter lWhatsappNotificationFilter = new WhatsappNotificationFilter();

        mNotificationFilters.put(lWhatsappNotificationFilter.getPackage(), lWhatsappNotificationFilter);

        mDefaultNotificationFilter = new DefaultNotificationFilter();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean filter(StatusBarNotification pStatusBarNotification) {
        NotificationFilter lNotificationFilter = getNotificationFilter(pStatusBarNotification.getPackageName());
        return lNotificationFilter.filter(pStatusBarNotification);
    }

    private NotificationFilter getNotificationFilter(String packageName) {
        NotificationFilter lNotificationFilter = mNotificationFilters.get(packageName);

        return lNotificationFilter == null ? mDefaultNotificationFilter : lNotificationFilter;
    }
}
