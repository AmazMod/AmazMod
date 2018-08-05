package com.edotassi.amazmod.notification;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.event.OutcomingNotification;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.notification.factory.NotificationFactory;
import com.edotassi.amazmod.util.Screen;
import com.google.gson.Gson;
import com.huami.watch.notification.data.StatusBarNotificationData;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
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

    private Map<String, String> notificationTimeGone;
    private Map<String, StatusBarNotification> notificationsAvailableToReply;

    private long lastVoiceCallNotificationTime = 0;
    private boolean connected = false;
    private long timeLastNotification = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        HermesEventBus.getDefault().register(this);

        notificationsAvailableToReply = new HashMap<>();

        Log.d(Constants.TAG,"NotificationService onCreate: " + connected);
//        if (!connected) {
//            toggleNotificationService();
//        }

    }

    @Override
    public void onListenerConnected(){
        super.onListenerConnected();
        connected = true;
        Log.d(Constants.TAG,"NotificationService onListenerConnected");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(Constants.TAG,"NotificationService onStarCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        HermesEventBus.getDefault().unregister(this);
        Log.d(Constants.TAG,"NotificationService onDestroy");
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        Logger.debug("notificationPosted: %s", statusBarNotification.getKey());

        String notificationPackage = statusBarNotification.getPackageName();
        if (!isPackageAllowed(notificationPackage)) {
            storeForStats(statusBarNotification, Constants.FILTER_PACKAGE);
            return;
        }

        if (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS, false) ||
                (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_WHEN_DND, false) &&
                        Screen.isDNDActive(this, getContentResolver()))) {
            storeForStats(statusBarNotification, Constants.FILTER_RETURN);
            return;
        }

//            Log.d(Constants.TAG, "NotificationService prefEWL: "
//                    + Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_WHEN_LOCKED, true)
//                    + " / isDeviceLocked: " + Screen.isDeviceLocked(this));

        if (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFATIONS_WHEN_SCREEN_ON, false)
                && Screen.isInteractive(this)) {

            if (!Screen.isDeviceLocked(this)) {
                storeForStats(statusBarNotification, Constants.FILTER_RETURN);
                return;
            } else if (!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_WHEN_LOCKED, true)) {
                storeForStats(statusBarNotification, Constants.FILTER_RETURN);
                return;
            }
        }

        byte filterResult = filter(statusBarNotification);

        if (filterResult == Constants.FILTER_CONTINUE || filterResult == Constants.FILTER_UNGROUP) {

            if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI, false)) {
                //Use Custom UI
                NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
                notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);
                HermesEventBus.getDefault().post(new OutcomingNotification(notificationData));
                Log.i(Constants.TAG, "NotificationService CustomUI: " + notificationData.toString());
            }

            //Disabled for RC1
            //} else {
                //Use standard UI

//                String pkg, String opPkg, int id,
//                String tag, int uid, int initialPid, Notification notification, UserHandle user,
//                        String overrideGroupKey, long postTime
                DataBundle dataBundle = new DataBundle();

                if (filterResult == Constants.FILTER_UNGROUP && Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_UNGROUP, false)) {
                    int nextId = (int) (long) (System.currentTimeMillis() % 10000L);
                    StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                            statusBarNotification.getId()+ nextId,
                            statusBarNotification.getTag(), 0, 0, 0,
                            statusBarNotification.getNotification(), statusBarNotification.getUser(),
                            statusBarNotification.getPostTime());
                    dataBundle.putParcelable("data", StatusBarNotificationData.from(this, sbn, false));
                } else dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));

                //Connect transporter
                Transporter notificationTransporter = TransporterClassic.get(this, "com.huami.action.notification");
                notificationTransporter.connectTransportService();

                notificationTransporter.send("add", dataBundle, new Transporter.DataSendResultCallback() {
                    @Override
                    public void onResultBack(DataTransportResult dataTransportResult) {
                        Logger.debug(dataTransportResult.toString());
                    }
                });

                //Disconnect transporter to avoid leaking
                notificationTransporter.disconnectTransportService();

                Log.i(Constants.TAG, "NotificationService StandardUI: " + dataBundle.toString());

                //Disabled for RC1
                //            }

            storeForStats(statusBarNotification, filterResult);

        } else {

            Notification notification = statusBarNotification.getNotification();

            boolean isRinging = false;
            AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            try {
                final int mode = am.getMode();
                if (AudioManager.MODE_IN_CALL == mode) {
                    Log.d(Constants.TAG, "NotificationService Ringer: CALL");
                } else if (AudioManager.MODE_IN_COMMUNICATION == mode) {
                    Log.d(Constants.TAG, "NotificationService Ringer: COMMUNICATION");
                } else if (AudioManager.MODE_RINGTONE == mode) {
                    Log.d(Constants.TAG, "NotificationService Ringer: RINGTONE");
                    isRinging = true;
                } else {
                    Log.d(Constants.TAG, "NotificationService Ringer: SOMETHING ELSE");
                }
            } catch (NullPointerException e) {
                Log.e(Constants.TAG, "NotificationService getMode Exception: " + e.toString());
            }

            //Messenger voice call notifications
            if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT
                    && Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_VOICE_APPS, false)
                    && isRinging) {

                Log.d(Constants.TAG, "NotificationService VoiceCall: " + notificationPackage);
                while (isRinging) {
                    if (System.currentTimeMillis() - lastVoiceCallNotificationTime > 5000) {

                        NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
                        //notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);

                        final PackageManager pm = getApplicationContext().getPackageManager();
                        ApplicationInfo ai;
                        try {
                            ai = pm.getApplicationInfo(notificationPackage, 0);
                        } catch (final PackageManager.NameNotFoundException e) {
                            Log.e(Constants.TAG, "NotificationService getApplicationInfo Exception: " + e.toString());
                            ai = null;
                        }
                        final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

                        notificationData.setText(notificationData.getText() + "\n" + applicationName);
                        notificationData.setHideReplies(true);
                        notificationData.setHideButtons(true);
                        notificationData.setForceCustom(true);
                        notificationData.setVibration(500);

                        HermesEventBus.getDefault().post(new OutcomingNotification(notificationData));
                        lastVoiceCallNotificationTime = System.currentTimeMillis();

                        final int mode = am.getMode();
                        if (AudioManager.MODE_RINGTONE != mode) {
                            storeForStats(statusBarNotification, Constants.FILTER_VOICE);
                            isRinging = false;
                        }
                    }
                }
            } else if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT
                    && (notificationPackage.contains("android.apps.maps"))) {

                Log.d(Constants.TAG, "NotificationService maps: " + notificationPackage);

                if (System.currentTimeMillis() - lastVoiceCallNotificationTime > 5000) {

                    //Disabled for RC1
                    //NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
                    //HermesEventBus.getDefault().post(new OutcomingNotification(notificationData));
                    lastVoiceCallNotificationTime = System.currentTimeMillis();
                    storeForStats(statusBarNotification, Constants.FILTER_MAPS);
                }

            }

            else {
                Log.d(Constants.TAG, "NotificationService blocked: " + notificationPackage);
                storeForStats(statusBarNotification, filterResult);
            }
        }
    }

    //Remove notification from watch if it was removed from phone
    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        Logger.debug("notificationRemoved: %s", statusBarNotification.getKey());

        if (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS, false) ||
                (Prefs.getBoolean(Constants.PREF_DISABLE_REMOVE_NOTIFICATIONS, false))) {
            return;
        }

        if (isPackageAllowed(statusBarNotification.getPackageName())) {

            //Connect transporter
            Transporter notificationTransporter = TransporterClassic.get(this, "com.huami.action.notification");
            notificationTransporter.connectTransportService();

            DataBundle dataBundle = new DataBundle();
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));
            notificationTransporter.send("del", dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Logger.debug(dataTransportResult.toString());
                }
            });

            //Disconnect transporter to avoid leaking
            notificationTransporter.disconnectTransportService();

            //Reset time of last voice call notification when notification is removed
            if (lastVoiceCallNotificationTime > 0) {
                lastVoiceCallNotificationTime = 0;
            }
        }


    }

    private byte filter(StatusBarNotification statusBarNotification) {
        if (notificationTimeGone == null) {
            notificationTimeGone = new HashMap<>();
        }
        String notificationPackage = statusBarNotification.getPackageName();
        String notificationId = statusBarNotification.getKey();

        if (!isPackageAllowed(notificationPackage)) {
            return returnFilterResult(Constants.FILTER_PACKAGE);
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
            return returnFilterResult(Constants.FILTER_GROUP);
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            Logger.debug("notification blocked FLAG_ONGOING_EVENT");
            return returnFilterResult(Constants.FILTER_ONGOING);
        }

        if (NotificationCompat.getLocalOnly(notification)) {
            if (!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_LOCAL_ONLY, false)) {
                Logger.debug("notification blocked because is LocalOnly");
                return returnFilterResult(Constants.FILTER_LOCAL);
            }
        }

        Bundle extras = statusBarNotification.getNotification().extras;
        String text = extras != null ? extras.getString(Notification.EXTRA_TEXT) : "";
        if (!NotificationCompat.isGroupSummary(notification) && notificationTimeGone.containsKey(notificationId)) {
            String previousText = notificationTimeGone.get(notificationId);
            if ((previousText != null) && (previousText.equals(text)) && ((System.currentTimeMillis() - timeLastNotification) < 999)) {
                Log.d(Constants.TAG, "NotificationService blocked text");
                //Logger.debug("notification blocked by key: %s, id: %s, flags: %s, time: %s", notificationId, statusBarNotification.getId(), statusBarNotification.getNotification().flags, (System.currentTimeMillis() - statusBarNotification.getPostTime()));
                return returnFilterResult(Constants.FILTER_BLOCK);
            } else {
                notificationTimeGone.put(notificationId, text);
                Log.d(Constants.TAG, "NotificationService allowed1");
                //Logger.debug("notification allowed");
                timeLastNotification = System.currentTimeMillis();
                return returnFilterResult(Constants.FILTER_UNGROUP);
            }
        } else {
            notificationTimeGone.put(notificationId, text);
            Log.d(Constants.TAG, "NotificationService allowed2");
            timeLastNotification = System.currentTimeMillis();
            //Logger.debug("notification allowed");
            return returnFilterResult(Constants.FILTER_CONTINUE);
        }
    }

    private boolean isPackageAllowed(String packageName) {
        String packagesJson = Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");
        Gson gson = new Gson();

        String[] packagesList = gson.fromJson(packagesJson, String[].class);

        return Arrays.binarySearch(packagesList, packageName) >= 0;

    }

    private byte returnFilterResult(byte result) {
        //Logger.debug("_");
        //Logger.debug("_");
        Log.d(Constants.TAG, "NotificationService _");
        return result;
    }

    private void storeForStats(StatusBarNotification statusBarNotification, byte filterResult) {
        try {
            NotificationEntity notificationEntity = new NotificationEntity();
            notificationEntity.setPackageName(statusBarNotification.getPackageName());
            notificationEntity.setDate(System.currentTimeMillis());
            notificationEntity.setFilterResult(filterResult);

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

    private void toggleNotificationService() {
        Log.d(Constants.TAG, "toggleNotificationService() called");
        ComponentName thisComponent = new ComponentName(this, NotificationService.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    }
}
