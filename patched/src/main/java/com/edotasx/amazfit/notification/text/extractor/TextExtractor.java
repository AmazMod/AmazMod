package com.edotasx.amazfit.notification.text.extractor;

import android.app.Notification;

import com.huami.watch.notification.data.NotificationData;

/**
 * Created by edoardotassinari on 25/02/18.
 */

public interface TextExtractor {

    String extractText(Notification notification, NotificationData notificationData);
}
