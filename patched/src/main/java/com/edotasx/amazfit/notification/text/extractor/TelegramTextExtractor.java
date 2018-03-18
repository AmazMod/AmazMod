package com.edotasx.amazfit.notification.text.extractor;

import android.app.Notification;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.huami.watch.notification.data.NotificationData;

/**
 * Created by edoardotassinari on 25/02/18.
 */

public class TelegramTextExtractor implements TextExtractor {
    @Override
    public String extractText(Notification notification, NotificationData notificationData) {

        Bundle bundle = NotificationCompat.getExtras(notification);

        CharSequence[] linesExtra = bundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (linesExtra != null) {
            return linesExtra[0].toString();
        }

        return notificationData.text;
    }
}
