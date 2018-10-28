package com.edotassi.amazmod.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import amazmod.com.transport.Constants;

import com.crashlytics.android.Crashlytics;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.NotficationSentEntity;
import com.edotassi.amazmod.db.model.NotficationSentEntity_Table;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.notification.factory.NotificationFactory;
import com.edotassi.amazmod.util.Screen;
import com.edotassi.amazmod.watch.Watch;
import com.google.gson.Gson;
import com.huami.watch.notification.data.StatusBarNotificationData;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.NotificationReplyData;

public class NotificationService extends NotificationListenerService {

    private Logger log = Logger.get(NotificationService.class);

    public static final int FLAG_WEARABLE_REPLY = 0x00000001;
    private static final long BLOCK_INTERVAL = 60000 * 60L; //One hour
    private static final long MAPS_INTERVAL = 60000 * 3L; //Three minutes
    private static final long VOICE_INTERVAL = 5000L; //Five seconds

    private static final String[] APP_WHITELIST = { //apps that do not fit some filter
            "com.contapps.android",
            "com.microsoft.office.outlook",
            "com.skype.raider"
    };

    private Map<String, String> notificationTimeGone;
    private Map<String, StatusBarNotification> notificationsAvailableToReply;

    private static long lastTimeNotificationArrived = 0;
    private static long lastTimeNotificationSent = 0;
    private static String lastTxt = "";

    @Override
    public void onCreate() {
        super.onCreate();

        EventBus.getDefault().register(this);

        notificationsAvailableToReply = new HashMap<>();

        log.d("NotificationService onCreate");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        log.d("NotificationService onListenerConnected");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.d("NotificationService onStarCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        log.d("NotificationService onDestroy");
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        log.d("notificationPosted: %s", statusBarNotification.getKey());

        String notificationPackage = statusBarNotification.getPackageName();
        if (!isPackageAllowed(notificationPackage)) {
            log.d("NotificationService blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_PACKAGE));
            storeForStats(statusBarNotification, Constants.FILTER_PACKAGE);
            return;
        }

        if (isNotificationsDisabled()) {
            log.d("NotificationService blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_NOTIFICATIONS_DISABLED));
            storeForStats(statusBarNotification, Constants.FILTER_NOTIFICATIONS_DISABLED);
            return;
        }

        if (isNotificationsDisabledWhenScreenOn()) {
            if (!Screen.isDeviceLocked(this)) {
                log.d("NotificationService blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_SCREENON));
                storeForStats(statusBarNotification, Constants.FILTER_SCREENON);
                return;
            } else if (!isNotificationsEnabledWhenScreenLocked()) {
                log.d("NotificationService blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_SCREENLOCKED));
                storeForStats(statusBarNotification, Constants.FILTER_SCREENLOCKED);
                return;
            }
        }

        byte filterResult = filter(statusBarNotification);

        boolean notificationSent = false;

        log.d("NotificationService notificationPackage:" + notificationPackage + " / filterResult: " + Character.toString((char) (byte) filterResult));

        if (filterResult == Constants.FILTER_CONTINUE ||
                filterResult == Constants.FILTER_UNGROUP ||
                filterResult == Constants.FILTER_LOCALOK) {

            if (!isStandardDisabled()) {
                sendNotificationWithStandardUI(filterResult, statusBarNotification);
                notificationSent = true;
            }

            if (isCustomUIEnabled() && filterResult != Constants.FILTER_LOCALOK) {
                sendNotificationWithCustomUI(statusBarNotification);
                notificationSent = true;
            }

            if (notificationSent) {
                log.d("NotificationService sent: " + notificationPackage + " / " + Character.toString((char) (byte) filterResult));
                storeForStats(statusBarNotification, filterResult);
            } else {
                log.d("NotificationService blocked (FILTER_RETURN): " + notificationPackage + " / " + Character.toString((char) (byte) filterResult));
                storeForStats(statusBarNotification, Constants.FILTER_RETURN);
            }

        } else {
            //Messenger voice call notifications
            if (isRingingNotification(statusBarNotification.getNotification())) {
                handleCall(statusBarNotification, notificationPackage);
            } else if (isMapsNotification(statusBarNotification.getNotification(), notificationPackage)) {
                mapNotification(statusBarNotification);
                //storeForStats(statusBarNotification, Constants.FILTER_MAPS); <- It is handled in the method
            } else {
                log.d("NotificationService blocked: " + notificationPackage + " / " + Character.toString((char) (byte) filterResult));
                storeForStats(statusBarNotification, filterResult);
            }
        }
    }

    //Remove notification from watch if it was removed from phone
    Hashtable<Integer, int[]> grouped_notifications = new Hashtable<Integer, int[]>();
    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        if (statusBarNotification == null) {
            return;
        }

        log.d("notificationRemoved: %s", statusBarNotification.getKey());

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
                    log.d(dataTransportResult.toString());
                }
            });

            if(grouped_notifications.containsKey(statusBarNotification.getId())){
                //initial array
                int[] grouped = grouped_notifications.get(statusBarNotification.getId());
                for(int id : grouped) {
                    dataBundle = new DataBundle();
                    StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                            statusBarNotification.getId() + id,
                            statusBarNotification.getTag(), 0, 0, 0,
                            statusBarNotification.getNotification(), statusBarNotification.getUser(),
                            statusBarNotification.getPostTime());
                    dataBundle.putParcelable("data", StatusBarNotificationData.from(this, sbn, false));
                    notificationTransporter.send("del", dataBundle, new Transporter.DataSendResultCallback() {
                        @Override
                        public void onResultBack(DataTransportResult dataTransportResult) {
                            log.d(dataTransportResult.toString());
                        }
                    });
                }
                grouped_notifications.remove(statusBarNotification.getId());
            }

            //Disconnect transporter to avoid leaking
            notificationTransporter.disconnectTransportService();

            //Reset time of last notification when notification is removed
            if (lastTimeNotificationArrived > 0) {
                lastTimeNotificationArrived = 0;
            }
            if (lastTimeNotificationSent > 0) {
                lastTimeNotificationSent = 0;
            }
        }


    }

    private void sendNotificationWithCustomUI(StatusBarNotification statusBarNotification) {
        NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
        if (isStandardDisabled()) {
            notificationData.setVibration(getDefaultVibration());
        } else notificationData.setVibration(0);
        notificationData.setHideReplies(false);
        notificationData.setHideButtons(true);
        notificationData.setForceCustom(false);
        notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);

        Watch.get().postNotification(notificationData);
        log.i("NotificationService CustomUI: " + notificationData.toString());
    }

    private void sendNotificationWithStandardUI(byte filterResult, StatusBarNotification statusBarNotification) {
        DataBundle dataBundle = new DataBundle();

        if (filterResult == Constants.FILTER_UNGROUP && Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_UNGROUP, false)) {
            int nextId = (int) (long) (System.currentTimeMillis() % 10000L);
            StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                    statusBarNotification.getId() + nextId,
                    statusBarNotification.getTag(), 0, 0, 0,
                    statusBarNotification.getNotification(), statusBarNotification.getUser(),
                    statusBarNotification.getPostTime());
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, sbn, false));

            if(grouped_notifications.containsKey(statusBarNotification.getId())){
                //initial array
                int[] grouped = grouped_notifications.get(statusBarNotification.getId());
                //new value
                int newValue = nextId;
                //define the new array
                int[] newArray = new int[grouped.length + 1];
                //copy values into new array
                for(int i=0;i < grouped.length;i++)
                    newArray[i] = grouped[i];
                newArray[newArray.length-1] = newValue;
                grouped_notifications.put(statusBarNotification.getId(), newArray);
            }else{
                grouped_notifications.put(statusBarNotification.getId(), new int[]{nextId});
            }
        } else {
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));
        }

        //Connect transporter
        Transporter notificationTransporter = TransporterClassic.get(this, "com.huami.action.notification");
        notificationTransporter.connectTransportService();

        notificationTransporter.send("add", dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                log.d(dataTransportResult.toString());
            }
        });

        //Disconnect transporter to avoid leaking
        notificationTransporter.disconnectTransportService();

        log.i("NotificationService StandardUI: " + dataBundle.toString());
    }

    private int getAudioManagerMode() {
        return ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getMode();
    }

    private int getDefaultVibration() {
        return Integer.valueOf(Prefs.getString(Constants.PREF_NOTIFICATIONS_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATIONS_VIBRATION));
    }

    private boolean isRinging() {
        boolean isRinging = false;
        try {
            final int mode = getAudioManagerMode();
            if (AudioManager.MODE_IN_CALL == mode) {
                log.d("NotificationService Ringer: CALL");
            } else if (AudioManager.MODE_IN_COMMUNICATION == mode) {
                log.d("NotificationService Ringer: COMMUNICATION");
            } else if (AudioManager.MODE_RINGTONE == mode) {
                log.d("NotificationService Ringer: RINGTONE");
                isRinging = true;
            } else {
                log.d("NotificationService Ringer: SOMETHING ELSE");
            }
        } catch (NullPointerException e) {
            log.e(e, "NotificationService getMode Exception: %s", e.toString());
        }

        return isRinging;
    }

    private void handleCall(StatusBarNotification statusBarNotification, String notificationPackage) {
        log.d("NotificationService VoiceCall: " + notificationPackage);
        while (isRinging()) {
            if (System.currentTimeMillis() - lastTimeNotificationSent > VOICE_INTERVAL) {

                NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
                //notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);

                final PackageManager pm = getApplicationContext().getPackageManager();
                ApplicationInfo ai;
                try {
                    ai = pm.getApplicationInfo(notificationPackage, 0);
                } catch (final PackageManager.NameNotFoundException e) {
                    log.e(e, "NotificationService getApplicationInfo Exception: %s", e.toString());
                    ai = null;
                }
                final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

                notificationData.setText(notificationData.getText() + "\n" + applicationName);
                notificationData.setVibration(getDefaultVibration());
                notificationData.setHideReplies(true);
                notificationData.setHideButtons(false);
                notificationData.setForceCustom(true);

                Watch.get().postNotification(notificationData);
                lastTimeNotificationSent = System.currentTimeMillis();

                final int mode = getAudioManagerMode();
                if (AudioManager.MODE_RINGTONE != mode) {
                    storeForStats(statusBarNotification, Constants.FILTER_VOICE);
                }
            }
        }
    }

    private byte filter(StatusBarNotification statusBarNotification) {
        if (notificationTimeGone == null) {
            notificationTimeGone = new HashMap<>();
        }
        String notificationPackage = statusBarNotification.getPackageName();
        String notificationId = statusBarNotification.getKey();
        Notification notification = statusBarNotification.getNotification();
        String text = "";
        int flags = 0;
        boolean localAllowed = false;
        boolean whitelistedApp = false;

        if (!isPackageAllowed(notificationPackage)) {
            return returnFilterResult(Constants.FILTER_PACKAGE);
        }

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        List<NotificationCompat.Action> actions = wearableExtender.getActions();

        if (NotificationCompat.isGroupSummary(notification)) {
            log.d("NotificationService isGroupSummary: " + notificationPackage);
            if (Arrays.binarySearch(APP_WHITELIST, notificationPackage) < 0) {
                log.d("notification blocked FLAG_GROUP_SUMMARY");
                return returnFilterResult(Constants.FILTER_GROUP);
            } else whitelistedApp = true;
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            log.d("notification blocked FLAG_ONGOING_EVENT");
            return returnFilterResult(Constants.FILTER_ONGOING);
        }

        if (NotificationCompat.getLocalOnly(notification)) {
            log.d("NotificationService getLocalOnly: " + notificationPackage);
            if ((!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_LOCAL_ONLY, false) && !whitelistedApp) ||
                    ((Arrays.binarySearch(APP_WHITELIST, notificationPackage) >= 0) && !whitelistedApp)) {
                log.d("notification blocked because is LocalOnly");
                return returnFilterResult(Constants.FILTER_LOCAL);
            } else if (!whitelistedApp) {
                localAllowed = true;
            }
        }

        //Bundle extras = statusBarNotification.getNotification().extras;
        CharSequence bigText = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TEXT);
        if (bigText != null) {
            text = bigText.toString();
        }
        log.d("NotificationService notificationPackage: " + notificationPackage + " / text: " + text);
        //Old code gives "java.lang.ClassCastException: android.text.SpannableString cannot be cast to java.lang.String"
        //String text = extras != null ? extras.getString(Notification.EXTRA_TEXT) : "";
        if (notificationTimeGone.containsKey(notificationId)) {
            String previousText = notificationTimeGone.get(notificationId);
            if ((previousText != null) && (previousText.equals(text)) && (!notificationPackage.equals("com.microsoft.office.outlook"))
                    && ((System.currentTimeMillis() - lastTimeNotificationArrived) < BLOCK_INTERVAL)) {
                log.d("NotificationService blocked text");
                //Logger.debug("notification blocked by key: %s, id: %s, flags: %s, time: %s", notificationId, statusBarNotification.getId(), statusBarNotification.getNotification().flags, (System.currentTimeMillis() - statusBarNotification.getPostTime()));
                return returnFilterResult(Constants.FILTER_BLOCK);
            } else {
                notificationTimeGone.put(notificationId, text);
                lastTimeNotificationArrived = System.currentTimeMillis();
                log.d("NotificationService allowed1: " + notificationPackage);
                //Logger.debug("notification allowed");
                if (localAllowed) return returnFilterResult(Constants.FILTER_LOCALOK);
                    //else if (whitelistedApp) return returnFilterResult(Constants.FILTER_CONTINUE);
                else return returnFilterResult(Constants.FILTER_UNGROUP);
            }
        } else {
            notificationTimeGone.put(notificationId, text);
            log.d("NotificationService allowed2: " + notificationPackage);
            if (localAllowed) return returnFilterResult(Constants.FILTER_LOCALOK);
            else return returnFilterResult(Constants.FILTER_CONTINUE);
        }

        /* Disabled because it is blocking some notifications
        NotficationSentEntity notificationSentEntity = SQLite
                .select()
                .from(NotficationSentEntity.class)
                .where(NotficationSentEntity_Table.id.eq(notificationId))
                .querySingle();

        log.d("NotificationService filter notificationPackage: " + notificationPackage
                + " / notificationId:" + notificationId + " / notificationSentEntity: " + notificationSentEntity);

        if (notificationSentEntity == null) {
            NotficationSentEntity notficationSentEntity = new NotficationSentEntity();
            notficationSentEntity.setDate(System.currentTimeMillis());
            notficationSentEntity.setId(notificationId);
            notficationSentEntity.setPackageName(notificationPackage);

            try {
                FlowManager.getModelAdapter(NotficationSentEntity.class).insert(notficationSentEntity);
            } catch (Exception ex) {
                log.e(ex,"NotificationService notificationSentEntity exception: " + ex.toString());
                Crashlytics.logException(ex);
            }

            notificationTimeGone.put(notificationId, text);

            if (localAllowed) {
                return returnFilterResult(Constants.FILTER_LOCALOK);
            } else {
                return returnFilterResult(Constants.FILTER_CONTINUE);
            }
        } else {
            return returnFilterResult(Constants.FILTER_BLOCK);
        }
        */
    }

    private boolean isNotificationsDisabled() {
        return Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS, false) ||
                (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_WHEN_DND, false) &&
                        Screen.isDNDActive(this, getContentResolver()));
    }

    private boolean isNotificationsDisabledWhenScreenOn() {
        return Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFATIONS_WHEN_SCREEN_ON, false)
                && Screen.isInteractive(this);
    }

    private boolean isNotificationsEnabledWhenScreenLocked() {
        return Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_WHEN_LOCKED, true);
    }

    private boolean isPackageAllowed(String packageName) {
        String packagesJson = Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");
        Gson gson = new Gson();

        String[] packagesList = gson.fromJson(packagesJson, String[].class);

        return Arrays.binarySearch(packagesList, packageName) >= 0;

    }

    private boolean isCustomUIEnabled() {
        return Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI, false);
    }

    private boolean isStandardDisabled() {
        return Prefs.getBoolean(Constants.PREF_DISABLE_STANDARD_NOTIFICATIONS, false);
    }

    private boolean isRingingNotification(Notification notification) {
        return (notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT
                && Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_VOICE_APPS, false) && isRinging();
    }

    private boolean isMapsNotification(Notification notification, String notificationPackage) {
        return (notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT
                && (notificationPackage.contains("android.apps.maps"));
    }

    private byte returnFilterResult(byte result) {
        log.d("NotificationService _");
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
            log.e(ex, "Failed to store notifications stats");
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
            log.w("Notification %s not found to reply", notificationId);
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
                    log.e(e, "replyToLastNotification error: " + e.getLocalizedMessage());
                }
            }
        }

        //List<Notification> pages = wearableExtender.getPages();
        //notificationWear.pages.addAll(pages);

        //notificationWear.bundle = statusBarNotification.getNotification().extras;
        //notificationWear.tag = statusBarNotification.getTag();//TODO find how to pass Tag with sending PendingIntent, might fix Hangout problem

        //notificationWear.pendingIntent = statusBarNotification.getNotification().contentIntent;

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
        }
        */
    }

    private void mapNotification(StatusBarNotification statusBarNotification) {

        log.d("NotificationService maps: " + statusBarNotification.getPackageName());

        NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
        RemoteViews rmv = statusBarNotification.getNotification().contentView;
        if (rmv != null) {

            //Get text from RemoteView using reflection
            List<String> txt = extractText(rmv);
            if ((txt.size() > 0) && ((!(txt.get(0).isEmpty()) && !(txt.get(0).equals(lastTxt))) || ((System.currentTimeMillis() - lastTimeNotificationSent) > MAPS_INTERVAL))) {

                //Get navigation icon from a child View drawn on Canvas
                try {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    View layout = inflater.inflate(R.layout.nav_layout, null);
                    ViewGroup frame = layout.findViewById(R.id.layout_navi);
                    frame.removeAllViews();
                    View newView = rmv.apply(getApplicationContext(), frame);
                    frame.addView(newView);
                    View viewImage = ((ViewGroup) newView).getChildAt(0);
                    //View outerLayout = ((ViewGroup) newView).getChildAt(1);
                    viewImage.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    Bitmap bitmap = Bitmap.createBitmap(viewImage.getMeasuredWidth(), viewImage.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    viewImage.layout(0, 0, viewImage.getMeasuredWidth(), viewImage.getMeasuredHeight());
                    viewImage.draw(canvas);
                    bitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);

                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int[] intArray = new int[width * height];
                    bitmap.getPixels(intArray, 0, width, 0, 0, width, height);
                    log.i("NotificationService mapNotification bitmap dimensions: " + width + " x " + height);

                    notificationData.setIcon(intArray);
                    notificationData.setIconWidth(width);
                    notificationData.setIconHeight(height);
                } catch (NullPointerException e) {
                    notificationData.setIcon(new int[]{});
                    log.e(e, "NotificationService mapNotification failed to get bitmap %s", e.toString());
                }

                notificationData.setTitle(txt.get(0));
                if (txt.size() > 1)
                    notificationData.setText(txt.get(1));
                else
                    notificationData.setText("");
                notificationData.setVibration(getDefaultVibration());
                notificationData.setHideReplies(true);
                notificationData.setHideButtons(false);
                notificationData.setForceCustom(true);

                Watch.get().postNotification(notificationData);

                lastTxt = txt.get(0);
                lastTimeNotificationSent = System.currentTimeMillis();
                storeForStats(statusBarNotification, Constants.FILTER_MAPS);
                log.d("NotificationService maps lastTxt:  " + lastTxt);
            }

        } else {
            log.w("NotificationService maps null remoteView");
        }
    }

    public static List<String> extractText(RemoteViews views) {
        // Use reflection to examine the m_actions member of the given RemoteViews object.
        List<String> text = new ArrayList<>();
        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            // Find the setText() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;

                // View ID
                parcel.readInt();

                String methodName = parcel.readString();
                if (methodName == null) continue;

                    // Save strings
                else {

                    if (methodName.equals("setText")) {
                        // Parameter type (10 = Character Sequence)
                        parcel.readInt();

                        // Store the actual string
                        String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                        text.add(t);
                    }
                }
                parcel.recycle();

            }
        }
        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e) {
            Logger.get(NotificationService.class).e(e, "NotificationClassifier: %s", e.toString());
        }
        return text;
    }
}
