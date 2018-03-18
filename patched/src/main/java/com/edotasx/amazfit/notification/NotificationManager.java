package com.edotasx.amazfit.notification;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.notification.filter.DefaultNotificationFilter;
import com.edotasx.amazfit.notification.filter.DefaultPreNotificationFilter;
import com.edotasx.amazfit.notification.filter.NotificationFilter;
import com.edotasx.amazfit.notification.filter.PreNotificationFilter;
import com.edotasx.amazfit.notification.filter.WhatsappNotificationFilter;
import com.edotasx.amazfit.notification.text.extractor.DefaultTextExtractor;
import com.edotasx.amazfit.notification.text.extractor.TelegramTextExtractor;
import com.edotasx.amazfit.notification.text.extractor.TextExtractor;
import com.huami.watch.dataflow.model.health.process.Const;
import com.huami.watch.notification.data.NotificationData;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public class NotificationManager {

    private Map<String, NotificationFilter> mNotificationFilters;
    private PreNotificationFilter mDefaultPreNotificationFilter;
    private NotificationFilter mDefaultNotificationFilter;

    private Map<String, TextExtractor> textExtractors;
    private TextExtractor defaultTextExtractor;

    private Context context;

    private static NotificationManager mInstace;

    public static NotificationManager initialize(Context context) {
        if (mInstace == null) {
            mInstace = new NotificationManager(context);
        }

        return mInstace;
    }

    public static NotificationManager sharedInstance(Context context) {
        return initialize(context);
    }

    public static NotificationManager sharedInstance() {
        return mInstace;
    }

    private NotificationManager(Context context) {
        this.context = context;

        mNotificationFilters = new HashMap<>();
        textExtractors = new HashMap<>();

        defaultTextExtractor = new DefaultTextExtractor();
        textExtractors.put(Constants.TELEGRAM_PACKAGE, new TelegramTextExtractor());

        mNotificationFilters.put(Constants.WHATSAPP_PACKAGE, new WhatsappNotificationFilter());

        mDefaultPreNotificationFilter = new DefaultPreNotificationFilter(context);
        mDefaultNotificationFilter = new DefaultNotificationFilter();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean notificationPosted(StatusBarNotification statusBarNotification) {
        boolean filtered = filter(statusBarNotification);

        return filtered;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean filter(StatusBarNotification pStatusBarNotification) {
        long timeDiff = System.currentTimeMillis() - pStatusBarNotification.getNotification().when;

        if (timeDiff > Constants.TIME_BETWEEN_NOTIFICATIONS) {
            Log.d(Constants.TAG_NOTIFICATION, "rejected by time: " + pStatusBarNotification.getPackageName() + ", time -> " + timeDiff);
            return true;
        }

        Log.d(Constants.TAG_NOTIFICATION, "accepted by time: " + pStatusBarNotification.getPackageName() + ", time -> " + timeDiff);

        if (mDefaultPreNotificationFilter.filter(pStatusBarNotification)) {
            return true;
        }

        NotificationFilter lNotificationFilter = getNotificationFilter(pStatusBarNotification.getPackageName());
        boolean filterResult = lNotificationFilter.filter(pStatusBarNotification);

        if (filterResult) {
            Log.d(Constants.TAG_NOTIFICATION, "rejected by filter: " + pStatusBarNotification.getPackageName());
        } else {
            Log.d(Constants.TAG_NOTIFICATION, "accepted by filter: " + pStatusBarNotification.getPackageName());
        }

        return filterResult;
    }

    public String extractText(Notification notification, NotificationData notificationData) {
        String packageName = notificationData.key.pkg;
        TextExtractor textExtractor = textExtractors.get(packageName) == null ?
                defaultTextExtractor :
                textExtractors.get(packageName);

        return textExtractor.extractText(notification, notificationData);
    }

    private NotificationFilter getNotificationFilter(String packageName) {
        NotificationFilter lNotificationFilter = mNotificationFilters.get(packageName);

        return lNotificationFilter == null ? mDefaultNotificationFilter : lNotificationFilter;
    }
}
