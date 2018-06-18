package com.edotassi.amazmod.notification;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.event.OutcomingNotification;
import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.notification.factory.NotificationFactory;
import com.google.gson.Gson;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amazmod.com.transport.data.NotificationData;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class NotificationService extends NotificationListenerService {

    public static final int FLAG_WEARABLE_REPLY = 0x00000001;
    private static final boolean BLOCK = true;
    private static final boolean CONTINUE = false;

    private Map<String, String> notificationTimeGone;

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        Logger.debug("notificationPosted: %s", statusBarNotification.getKey());

        boolean filterResult = filter(statusBarNotification);

        if (filterResult == CONTINUE) {
            NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
            HermesEventBus.getDefault().post(new OutcomingNotification(notificationData));
        }
    }

    private boolean filter(StatusBarNotification statusBarNotification) {
        if (notificationTimeGone == null) {
            notificationTimeGone = new HashMap<>();
        }
        String notificationPackage = statusBarNotification.getPackageName();
        String notificationId = statusBarNotification.getKey();

        if (!isPackageAllowed(notificationPackage)) {
            return returnFilterResult(BLOCK);
        }

        Notification notification = statusBarNotification.getNotification();

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        List<NotificationCompat.Action> actions = wearableExtender.getActions();

        int flags = 0;

        for (NotificationCompat.Action act : actions) {
            if (act != null && act.getRemoteInputs() != null) {
                flags |= FLAG_WEARABLE_REPLY;
                break;
            }
        }

        if (/*(flags & FLAG_WEARABLE_REPLY) == 0 &&*/ NotificationCompat.isGroupSummary(notification)) {
            Logger.debug("notification blocked FLAG_GROUP_SUMMARY");
            return returnFilterResult(BLOCK);
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            Logger.debug("notification blocked FLAG_ONGOING_EVENT");
            return returnFilterResult(BLOCK);
        }

        if (NotificationCompat.getLocalOnly(notification)) {
            Logger.debug("notification blocked because is LocalOnly");
            return returnFilterResult(BLOCK);
        }

        Bundle extras = statusBarNotification.getNotification().extras;
        String text = extras != null ? extras.getString(Notification.EXTRA_TEXT) : "";
        if (!NotificationCompat.isGroupSummary(notification) && notificationTimeGone.containsKey(notificationId)) {
            String previousText = notificationTimeGone.get(notificationId);
            if ((previousText != null) && (previousText.equals(text))) {
                Logger.debug("notification blocked by key: %s, id: %s, flags: %s, time: %s", notificationId, statusBarNotification.getId(), statusBarNotification.getNotification().flags, (System.currentTimeMillis() - statusBarNotification.getPostTime()));
                return returnFilterResult(BLOCK);
            } else {
                notificationTimeGone.put(notificationId, text);

                Logger.debug("notification allowed");
                return returnFilterResult(CONTINUE);
            }
        } else {
            notificationTimeGone.put(notificationId, text);

            Logger.debug("notification allowed");
            return returnFilterResult(CONTINUE);
        }
    }

    private boolean isPackageAllowed(String packageName) {
        String packagesJson = Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");
        Gson gson = new Gson();

        String[] packagesList = gson.fromJson(packagesJson, String[].class);

        return Arrays.binarySearch(packagesList, packageName) >= 0;
    }

    private boolean returnFilterResult(boolean result) {
        Logger.debug("_");
        Logger.debug("_");
        return result;
    }
}
