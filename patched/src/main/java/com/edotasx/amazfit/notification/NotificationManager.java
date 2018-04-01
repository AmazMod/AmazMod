package com.edotasx.amazfit.notification;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.notification.filter.TimeNotificationFilter;
import com.edotasx.amazfit.notification.filter.UniqueKeyNotificationFilter;
import com.edotasx.amazfit.notification.filter.NotificationFilter;
import com.edotasx.amazfit.notification.filter.app.WhatsappNotificationFilter;
import com.edotasx.amazfit.notification.text.extractor.DefaultTextExtractor;
import com.edotasx.amazfit.notification.text.extractor.TelegramTextExtractor;
import com.edotasx.amazfit.notification.text.extractor.TextExtractor;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.notification.data.NotificationData;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public class NotificationManager {

    private UniqueKeyNotificationFilter uniqueKeyNotificationFilter;
    private TimeNotificationFilter timeNotificationFilter;
    private WhatsappNotificationFilter whatsappNotificationFilter;

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

        textExtractors = new HashMap<>();

        defaultTextExtractor = new DefaultTextExtractor();
        textExtractors.put(Constants.TELEGRAM_PACKAGE, new TelegramTextExtractor());

        whatsappNotificationFilter = new WhatsappNotificationFilter();

        uniqueKeyNotificationFilter = new UniqueKeyNotificationFilter(context);
        timeNotificationFilter = new TimeNotificationFilter();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean notificationPosted(StatusBarNotification statusBarNotification) {
        boolean filtered = filter(statusBarNotification);

        return filtered;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean filter(StatusBarNotification pStatusBarNotification) {
        boolean filter = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            filter = timeNotificationFilter.filter(pStatusBarNotification);
        }

        if (filter) {
            return filter;
        }

        if (pStatusBarNotification.getPackageName().equals(Constants.WHATSAPP_PACKAGE)) {
            filter = whatsappNotificationFilter.filter(pStatusBarNotification);
        }

        return filter;
    }

    public String extractText(Notification notification, NotificationData notificationData) {
        String packageName = notificationData.key.pkg;
        TextExtractor textExtractor = textExtractors.get(packageName) == null ?
                defaultTextExtractor :
                textExtractors.get(packageName);

        return textExtractor.extractText(notification, notificationData);
    }
}
