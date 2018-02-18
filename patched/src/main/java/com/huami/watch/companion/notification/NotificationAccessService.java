package com.huami.watch.companion.notification;

import android.annotation.SuppressLint;
import android.media.RemoteController;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.notification.NotificationManager;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 26/01/18.
 */

@SuppressLint({"NewApi", "OverrideAbstract"})
@DexEdit(defaultAction = DexAction.IGNORE)
public class NotificationAccessService extends NotificationListenerService
        implements RemoteController.OnClientUpdateListener {

    @DexWrap
    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        Log.d(Constants.TAG, "onNotificationPosted");
        if (!NotificationManager.sharedInstance().filter(statusBarNotification)) {
            onNotificationPosted(statusBarNotification);
        }
    }

    @DexWrap
    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        if (!NotificationManager.sharedInstance().filter(statusBarNotification)) {
            onNotificationPosted(statusBarNotification, rankingMap);
        }
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