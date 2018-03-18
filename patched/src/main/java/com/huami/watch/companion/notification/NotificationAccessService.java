package com.huami.watch.companion.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.media.RemoteController;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.notification.NotificationManager;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.notification.data.StatusBarNotificationData;

import java.util.ArrayList;
import java.util.List;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexAppend;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexPrepend;
import lanchon.dexpatcher.annotation.DexReplace;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 26/01/18.
 */

@SuppressLint({"NewApi", "OverrideAbstract"})
@DexEdit(defaultAction = DexAction.IGNORE)
public class NotificationAccessService extends NotificationListenerService
        implements RemoteController.OnClientUpdateListener {

    @DexAdd
    public static Context context;

    @DexPrepend
    public void onCreate() {
        context = this;
    }

    @DexAdd
    private List<StatusBarNotification> notificationsSent;

    @DexWrap
    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        if (PreferenceManager.getBoolean(this, Constants.PREFERENCE_DISABLE_NOTIFICATIONS_MOD, false)) {
            onNotificationPosted(statusBarNotification);
        } else {
            if (!processNotificationPosted(statusBarNotification)) {
                onNotificationPosted(statusBarNotification);
            }
        }
    }

    @DexWrap
    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        if (PreferenceManager.getBoolean(this, Constants.PREFERENCE_DISABLE_NOTIFICATIONS_MOD, false)) {
            onNotificationPosted(statusBarNotification, rankingMap);
        } else {
            if (!processNotificationPosted(statusBarNotification)) {
                onNotificationPosted(statusBarNotification, rankingMap);
            }
        }
    }

    @DexAdd
    private boolean processNotificationPosted(StatusBarNotification statusBarNotification) {
        if (notificationsSent == null) {
            notificationsSent = new ArrayList<>();
        }

        boolean notificationWillBeBlocked = NotificationManager.sharedInstance(this).notificationPosted(statusBarNotification);

        if (!notificationWillBeBlocked) {
            Log.d(Constants.TAG_NOTIFICATION_SERVICE, "posted: " + statusBarNotification.getKey());
            notificationsSent.add(statusBarNotification);
        }

        return notificationWillBeBlocked;
    }

    @DexWrap
    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        if (PreferenceManager.getBoolean(this, Constants.PREFERENCE_DISABLE_NOTIFICATIONS_MOD, false)) {
            onNotificationRemoved(statusBarNotification);
        } else {

            if (notificationsSent == null) {
                notificationsSent = new ArrayList<>();
            }

            String packageName = statusBarNotification.getPackageName();
            List<StatusBarNotification> notificationsRemain = new ArrayList<>();

            for (StatusBarNotification statusBarNotificationSent : notificationsSent) {
                if (statusBarNotificationSent.getPackageName().equals(packageName)) {
                    Log.d(Constants.TAG_NOTIFICATION_SERVICE, "removed: " + statusBarNotificationSent.getKey());

                    onNotificationRemoved(statusBarNotificationSent);
                } else {
                    Log.d(Constants.TAG_NOTIFICATION_SERVICE, "not removed: " + statusBarNotificationSent.getKey());

                    notificationsRemain.add(statusBarNotificationSent);
                }
            }

            notificationsSent = notificationsRemain;
        }
    }

    @DexReplace
    private static String f(StatusBarNotification object) {
        Notification notification = object.getNotification();

        if (Build.VERSION.SDK_INT < 20) {
            return object.getPackageName() + "|" + object.getId() + "|" + object.getTag() + "|" + (notification != null ? notification.when : "");
        }
        return object.getKey() + "|" + (notification != null ? notification.when : "");
    }


    @DexIgnore
    @Override
    public void onClientChange(boolean clearing) {
    }

    @DexIgnore
    @Override
    public void onClientPlaybackStateUpdate(int state) {
    }

    @DexIgnore
    @Override
    public void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed) {
    }

    @DexIgnore
    @Override
    public void onClientTransportControlUpdate(int transportControlFlags) {
    }

    @DexIgnore
    @Override
    public void onClientMetadataUpdate(RemoteController.MetadataEditor metadataEditor) {
    }
}