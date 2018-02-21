package com.edotasx.amazfit.notification;

import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;

import com.edotasx.amazfit.notification.filter.DefaultNotificationFilter;
import com.edotasx.amazfit.notification.filter.DefaultPreNotificationFilter;
import com.edotasx.amazfit.notification.filter.NotificationFilter;
import com.edotasx.amazfit.notification.filter.PreNotificationFilter;
import com.edotasx.amazfit.notification.filter.WhatsappNotificationFilter;
import com.huami.watch.notification.data.StatusBarNotificationData;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public class NotificationManager {

    private Map<String, NotificationFilter> mNotificationFilters;
    private PreNotificationFilter mDefaultPreNotificationFilter;
    private NotificationFilter mDefaultNotificationFilter;

    private Context context;

    private static NotificationManager mInstace;

    public static NotificationManager sharedInstance(Context context) {
        if (mInstace == null) {
            mInstace = new NotificationManager(context);
        }

        return mInstace;
    }

    private NotificationManager(Context context) {
        this.context = context;

        mNotificationFilters = new HashMap<>();

        WhatsappNotificationFilter lWhatsappNotificationFilter = new WhatsappNotificationFilter();

        mNotificationFilters.put(lWhatsappNotificationFilter.getPackage(), lWhatsappNotificationFilter);

        mDefaultPreNotificationFilter = new DefaultPreNotificationFilter(context);
        mDefaultNotificationFilter = new DefaultNotificationFilter();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean filter(StatusBarNotification pStatusBarNotification) {
        if (mDefaultPreNotificationFilter.filter(pStatusBarNotification)) {
            return true;
        }

        NotificationFilter lNotificationFilter = getNotificationFilter(pStatusBarNotification.getPackageName());
        return lNotificationFilter.filter(pStatusBarNotification);
    }


    private NotificationFilter getNotificationFilter(String packageName) {
        NotificationFilter lNotificationFilter = mNotificationFilters.get(packageName);

        return lNotificationFilter == null ? mDefaultNotificationFilter : lNotificationFilter;
    }
}
