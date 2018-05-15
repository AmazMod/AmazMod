package com.edotasx.amazfit.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.db.model.BatteryRead;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.companion.battery.BatteryInfoHelper;
import com.huami.watch.companion.battery.bean.BatteryInfo;
import com.huami.watch.companion.sync.SyncUtil;
import com.huami.watch.notification.data.StatusBarNotificationData;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lanchon.dexpatcher.annotation.DexAdd;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public class NotificationManager {

    public static final int FLAG_WEARABLE_REPLY = 0x00000001;

    private static final boolean BLOCK_NOTIFICATION = true;
    private static final boolean CONTINUE_NOTIFICATION = false;

    private long lastNotificationTime = -1;

    private Transporter transporter;
    private Transporter.DataListener dataListener;

    private Map<String, String> notificationTimeGone;

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

        notificationTimeGone = new HashMap<>();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean notificationPosted(StatusBarNotification statusBarNotification) {
        return filter(statusBarNotification);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean filter(StatusBarNotification pStatusBarNotification) {
        String notificationId = pStatusBarNotification.getKey();

        Log.d(Constants.TAG_NOTIFICATION, "notification: " + notificationId);

        Notification notification = pStatusBarNotification.getNotification();

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
            Log.d(Constants.TAG_NOTIFICATION, "notification blocked FLAG_GROUP_SUMMARY");
            return returnFilterResult(BLOCK_NOTIFICATION);
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            Log.d(Constants.TAG_NOTIFICATION, "notification blocked FLAG_ONGOING_EVENT");
            return returnFilterResult(BLOCK_NOTIFICATION);
        }

        if (NotificationCompat.getLocalOnly(notification)) {
            Log.d(Constants.TAG_NOTIFICATION, "notification blocked because is LocalOnly");
            return returnFilterResult(BLOCK_NOTIFICATION);
        }

        Bundle extras = pStatusBarNotification.getNotification().extras;
        String text = extras != null ? extras.getString(Notification.EXTRA_TEXT) : "";
        if (!NotificationCompat.isGroupSummary(notification) && notificationTimeGone.containsKey(notificationId)) {
            String previousText = notificationTimeGone.get(notificationId);
            Log.d(Constants.TAG_NOTIFICATION, "text: " + text);
            if ((previousText != null) && (previousText.equals(text))) {
                Log.d(Constants.TAG_NOTIFICATION, "notification blocked by key: " + notificationId + ", id: " + pStatusBarNotification.getId() + ", flags: " + pStatusBarNotification.getNotification().flags + ", time: " + (System.currentTimeMillis() - pStatusBarNotification.getPostTime()));
                return returnFilterResult(BLOCK_NOTIFICATION);
            } else {
                notificationTimeGone.put(notificationId, text);

                Log.d(Constants.TAG_NOTIFICATION, "notification allowed");
                return returnFilterResult(CONTINUE_NOTIFICATION);
            }
        } else {
            notificationTimeGone.put(notificationId, text);

            Log.d(Constants.TAG_NOTIFICATION, "notification allowed");
            return returnFilterResult(CONTINUE_NOTIFICATION);
        }
    }

    private boolean returnFilterResult(boolean result) {
        Log.d(Constants.TAG_NOTIFICATION, "_");
        Log.d(Constants.TAG_NOTIFICATION, "_");
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @DexAdd
    private String buildKey(StatusBarNotification statusBarNotification) {
        return statusBarNotification.getPackageName() + "|" + statusBarNotification.getPostTime();
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
                //notificationWear.remoteInputs.addAll(Arrays.asList(act.getRemoteInputs()));
                Log.d(Constants.TAG_NOTIFICATION, "action: " + act.title);

                for (RemoteInput remoteInput : act.getRemoteInputs()) {
                    localBundle.putCharSequence(remoteInput.getResultKey(), message);
                }

                Intent localIntent = new Intent();
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                RemoteInput.addResultsToIntent(act.getRemoteInputs(), localIntent, localBundle);
                try {
                    act.actionIntent.send(context, 0, localIntent);
                } catch (PendingIntent.CanceledException e) {
                    Log.e(Constants.TAG_NOTIFICATION, "replyToLastNotification error: " + e.getLocalizedMessage());
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
