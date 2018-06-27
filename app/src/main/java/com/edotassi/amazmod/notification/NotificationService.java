package com.edotassi.amazmod.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.event.OutcomingNotification;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.notification.factory.NotificationFactory;
import com.edotassi.amazmod.util.Screen;
import com.google.gson.Gson;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.NotificationReplyData;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class NotificationService extends NotificationListenerService {

    public static final int FLAG_WEARABLE_REPLY = 0x00000001;
    private static final boolean BLOCK = true;
    private static final boolean CONTINUE = false;

    private Map<String, String> notificationTimeGone;

    private Map<String, StatusBarNotification> notificationsAvailableToReply;

    @Override
    public void onCreate() {
        super.onCreate();

        HermesEventBus.getDefault().register(this);

        notificationsAvailableToReply = new HashMap<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        HermesEventBus.getDefault().unregister(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        Logger.debug("notificationPosted: %s", statusBarNotification.getKey());

        boolean filterResult = filter(statusBarNotification);

        if (filterResult == CONTINUE) {
            if (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFATIONS_WHEN_SCREEN_ON, false) && Screen.isInteractive(this)) {
                return;
            }

            NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);

            notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);

            HermesEventBus.getDefault().post(new OutcomingNotification(notificationData));

            storeForStats(statusBarNotification);
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

    private void storeForStats(StatusBarNotification statusBarNotification) {
        try {
            NotificationEntity notificationEntity = new NotificationEntity();
            notificationEntity.setPackageName(statusBarNotification.getPackageName());
            notificationEntity.setDate(System.currentTimeMillis());

            FlowManager.getModelAdapter(NotificationEntity.class).insert(notificationEntity);
        } catch (Exception ex) {
            Logger.error(ex, "Failed to store notifications stats");
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void replyToNotification(ReplyToNotificationLocal replyToNotificationLocal) {
        NotificationReplyData notificationReplyData = replyToNotificationLocal.getNotificationReplyData();
        String notificationId = notificationReplyData.getNotificationId();
        String reply = notificationReplyData.getReply();

        StatusBarNotification statusBarNotification = notificationsAvailableToReply.get(notificationId);
        if (statusBarNotification != null) {
            notificationsAvailableToReply.remove(notificationId);

            replyToNotification(statusBarNotification, reply);
        } else {
            Logger.warn("Notification %s not found to reply", notificationId);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void replyToNotification(StatusBarNotification statusBarNotification, String message) {
        //NotificationWear notificationWear = new NotificationWear();
        //notificationWear.packageName = statusBarNotification.getPackageName();

        Bundle localBundle = statusBarNotification.getNotification().extras;

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(statusBarNotification.getNotification());
        List<NotificationCompat.Action> actions = wearableExtender.getActions();
        for (NotificationCompat.Action act : actions) {
            if (act != null && act.getRemoteInputs() != null) {
                for (RemoteInput remoteInput : act.getRemoteInputs()) {
                    localBundle.putCharSequence(remoteInput.getResultKey(), message);
                }

                Intent localIntent = new Intent();
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                RemoteInput.addResultsToIntent(act.getRemoteInputs(), localIntent, localBundle);
                try {
                    act.actionIntent.send(this, 0, localIntent);
                } catch (PendingIntent.CanceledException e) {
                    Logger.error(e, "replyToLastNotification error: " + e.getLocalizedMessage());
                }
            }
        }

        //List<Notification> pages = wearableExtender.getPages();
        //notificationWear.pages.addAll(pages);

        //notificationWear.bundle = statusBarNotification.getNotification().extras;
        //notificationWear.tag = statusBarNotification.getTag();//TODO find how to pass Tag with sending PendingIntent, might fix Hangout problem

        //notificationWear.pendingIntent = statusBarNotification.getNotification().contentIntent;


        //Log.d(Constants.TAG_NOTIFICATION, "notWear, remoteInputs: " + notificationWear.remoteInputs.size());

        //RemoteInput[] remoteInputs = new RemoteInput[notificationWear.remoteInputs.size()];

        /*
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle localBundle = notificationWear.bundle;
        int i = 0;
        for (RemoteInput remoteIn : notificationWear.remoteInputs) {
            //getDetailsOfNotification(remoteIn);
            remoteInputs[i] = remoteIn;
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), message);//This work, apart from Hangouts as probably they need additional parameter (notification_tag?)
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            notificationWear.pendingIntent.send(context, 0, localIntent);
        } catch (PendingIntent.CanceledException e) {
            Log.e(Constants.TAG_NOTIFICATION, "replyToLastNotification error: " + e.getLocalizedMessage());
        }
        */
    }
}
