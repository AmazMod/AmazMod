package com.edotassi.amazmod.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
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
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity_Table;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
import com.edotassi.amazmod.notification.factory.NotificationFactory;
import com.edotassi.amazmod.support.Logger;
import com.edotassi.amazmod.support.SilenceApplicationHelper;
import com.edotassi.amazmod.util.Screen;
import com.edotassi.amazmod.watch.Watch;
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

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.NotificationReplyData;

public class NotificationJobService extends JobService {

    JobParameters params;

    private static final long BLOCK_INTERVAL = 60000 * 60L; //One hour
    private static final long MAPS_INTERVAL = 60000 * 3L; //Three minutes
    private static final long VOICE_INTERVAL = 5000L; //Five seconds

    public static final String NOTIFICATION_KEY = "notification_key";
    public static final String NOTIFICATION_MODE = "notification_mode";
    public static final String NOTIFICATION_POSTED = "posted";
    public static final String NOTIFICATION_REMOVED= "removed";

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

    //Remove notification from watch if it was removed from phone
    private static Hashtable<Integer, int[]> grouped_notifications = new Hashtable<Integer, int[]>();

    @Override
    public boolean onStartJob(JobParameters params) {

        this.params = params;

        EventBus.getDefault().register(this);

        notificationsAvailableToReply = new HashMap<>();

        String key = params.getExtras().getString(NOTIFICATION_KEY, null);

        Log.d(Constants.TAG, "NotificationJobService onStartJob key: " + key);

        /*
        *
        if (key != null) {

            switch (params.getExtras().getString(NOTIFICATION_MODE, null)) {

                case NOTIFICATION_POSTED:
                    processNotificationPosted(NotificationService.getNotification(key), key);
                    break;

                case NOTIFICATION_REMOVED:
                    processNotificationRemoved(NotificationService.getNotification(key), key);
                    break;

                default:
                    Log.e(Constants.TAG, "NotificationJobService onStartJob error: no NOTIFICATION_MODE found!");

            }
        }
        *
        */

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(Constants.TAG, "NotificationJobService onStopJob");

        return true;
    }

    private void processNotificationPosted(StatusBarNotification statusBarNotification, String key) {
        Log.d(Constants.TAG, "NotificationJobService processNotificationPosted");

        String notificationPackage = statusBarNotification.getPackageName();
        if (!isPackageAllowed(notificationPackage)) {
            Log.d(Constants.TAG, "NotificationJobService blocked: " + notificationPackage + " / "
                    + Character.toString((char) (byte) Constants.FILTER_PACKAGE));
            storeForStats(statusBarNotification, Constants.FILTER_PACKAGE);
            return;
        }

        if (isPackageSilenced(notificationPackage)) {
            Log.d(Constants.TAG, "NotificationJobService blocked: " + notificationPackage + " / "
                    + Character.toString((char) (byte) Constants.FILTER_SILENCE));
            storeForStats(statusBarNotification, Constants.FILTER_SILENCE);
            return;
        }

        if (isPackageFiltered(statusBarNotification)) {
            Log.d(Constants.TAG, "NotificationJobService blocked: " + notificationPackage + " / "
                    + Character.toString((char) (byte) Constants.FILTER_TEXT));
            storeForStats(statusBarNotification, Constants.FILTER_TEXT);
            return;
        }

        if (isNotificationsDisabled()) {
            Log.d(Constants.TAG, "NotificationJobService blocked: " + notificationPackage + " / "
                    + Character.toString((char) (byte) Constants.FILTER_NOTIFICATIONS_DISABLED));
            storeForStats(statusBarNotification, Constants.FILTER_NOTIFICATIONS_DISABLED);
            return;
        }

        if (isNotificationsDisabledWhenScreenOn()) {
            if (!Screen.isDeviceLocked(this)) {
                Log.d(Constants.TAG, "NotificationJobService blocked: " + notificationPackage + " / "
                        + Character.toString((char) (byte) Constants.FILTER_SCREENON));
                storeForStats(statusBarNotification, Constants.FILTER_SCREENON);
                return;
            } else if (!isNotificationsEnabledWhenScreenLocked()) {
                Log.d(Constants.TAG, "NotificationJobService blocked: " + notificationPackage + " / "
                        + Character.toString((char) (byte) Constants.FILTER_SCREENLOCKED));
                storeForStats(statusBarNotification, Constants.FILTER_SCREENLOCKED);
                return;
            }
        }

        byte filterResult = filter(statusBarNotification);

        boolean notificationSent = false;

        Log.d(Constants.TAG, "NotificationJobService notificationPackage:" + notificationPackage
                + " / filterResult: " + Character.toString((char) (byte) filterResult));

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
                Log.d(Constants.TAG, "NotificationJobService sent: " + notificationPackage
                        + " / " + Character.toString((char) (byte) filterResult));
                storeForStats(statusBarNotification, filterResult);
            } else {
                Log.d(Constants.TAG, "NotificationJobService blocked (FILTER_RETURN): "
                        + notificationPackage + " / " + Character.toString((char) (byte) filterResult));
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
                Log.d(Constants.TAG, "NotificationJobService blocked: " + notificationPackage
                        + " / " + Character.toString((char) (byte) filterResult));
                storeForStats(statusBarNotification, filterResult);
            }
        }

        //NotificationService.removeNotification(key);
        jobFinished(params, false);

    }


    public void processNotificationRemoved(StatusBarNotification statusBarNotification, String key) {

        if (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS, false) ||
                (Prefs.getBoolean(Constants.PREF_DISABLE_REMOVE_NOTIFICATIONS, false))) {
            return;
        }

        Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved id: " + statusBarNotification.getId());

        if (isPackageAllowed(statusBarNotification.getPackageName())) {

            DataBundle dataBundle = new DataBundle();

            if (grouped_notifications.containsKey(statusBarNotification.getId())) {
                //initial array
                int[] grouped = grouped_notifications.get(statusBarNotification.getId());
                for (int id : grouped) {
                    Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved id: " + statusBarNotification.getId()
                            + " \\ groupedId: " + id );
                    StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                            statusBarNotification.getId() + id,
                            statusBarNotification.getTag(), 0, 0, 0,
                            statusBarNotification.getNotification(), statusBarNotification.getUser(),
                            statusBarNotification.getPostTime());
                    dataBundle.putParcelable("data", StatusBarNotificationData.from(this, sbn, false));
                }
                grouped_notifications.remove(statusBarNotification.getId());

            } else {
                Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved single id: " + statusBarNotification.getId());
                dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));
            }

            //Connect transporter
            Transporter notificationTransporter = TransporterClassic.get(this, "com.huami.action.notification");
            notificationTransporter.connectTransportService();

            notificationTransporter.send("del", dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved: " + dataTransportResult.toString());
                }
            });

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

        //NotificationService.removeNotification(key);
        jobFinished(params, false);

    }

    private void sendNotificationWithCustomUI(StatusBarNotification statusBarNotification) {
        NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
        if (isStandardDisabled()) {
            notificationData.setVibration(getDefaultVibration());
        } else notificationData.setVibration(0);
        notificationData.setHideReplies(false);
        notificationData.setHideButtons(true);
        notificationData.setForceCustom(false);

        extractImagesFromNotification(statusBarNotification, notificationData);

        notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);

        Watch.get().postNotification(notificationData);
        Log.i(Constants.TAG, "NotificationJobService CustomUI: " + notificationData.toString());
    }

    private void extractImagesFromNotification(StatusBarNotification statusBarNotification, NotificationData notificationData) {
        Bundle bundle = statusBarNotification.getNotification().extras;

        if (!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_DISABLE_LARGE_ICON, false)) {
            extractLargeIcon(bundle, notificationData);
        }

        if (!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_DISABLE_PICTURE, false)) {
            extractPicture(bundle, notificationData);
        }
    }

    private void extractLargeIcon(Bundle bundle, NotificationData notificationData) {
        try {
            Bitmap largeIcon = (Bitmap) bundle.get("android.largeIcon");
            if (largeIcon != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                largeIcon.compress(Bitmap.CompressFormat.PNG, 80, stream);
                byte[] byteArray = stream.toByteArray();

                notificationData.setLargeIcon(byteArray);
                notificationData.setLargeIconWidth(largeIcon.getWidth());
                notificationData.setLargeIconHeight(largeIcon.getHeight());
            }
        } catch (Exception exception) {
            Log.e(Constants.TAG, exception.getMessage(), exception);
        }
    }

    private void extractPicture(Bundle bundle, NotificationData notificationData) {
        try {
            Bitmap originalBitmap = (Bitmap) bundle.get("android.picture");
            if (originalBitmap != null) {
                Bitmap scaledBitmap = scaleBitmap(originalBitmap);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
                byte[] byteArray = stream.toByteArray();

                notificationData.setPicture(byteArray);
                notificationData.setPictureWidth(scaledBitmap.getWidth());
                notificationData.setPictureHeight(scaledBitmap.getHeight());
            }
        } catch (Exception exception) {
            Log.e(Constants.TAG, exception.getMessage(), exception);
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap) {
        if (bitmap.getWidth() <= 320) {
            return bitmap;
        }

        float horizontalScaleFactor = bitmap.getWidth() / 320f;
        float destHeight = bitmap.getHeight() / horizontalScaleFactor;

        return Bitmap.createScaledBitmap(bitmap, 320, (int) destHeight, false);
    }

    private ArrayList<Object> values(Bundle bundle) {
        ArrayList<Object> values = new ArrayList<>();
        Set<String> keys = bundle.keySet();

        for (String key : keys) {
            values.add(bundle.get(key));
        }

        return values;
    }

    private void sendNotificationWithStandardUI(byte filterResult, StatusBarNotification statusBarNotification) {

        Log.d(Constants.TAG, "NotificationJobService sendNotificationWithStandardUI id: " + statusBarNotification.getId());
        DataBundle dataBundle = new DataBundle();

        if (filterResult == Constants.FILTER_UNGROUP && Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_UNGROUP, false)) {
            int nextId = (int) (long) (System.currentTimeMillis() % 10000L);
            StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                    statusBarNotification.getId() + nextId,
                    statusBarNotification.getTag(), 0, 0, 0,
                    statusBarNotification.getNotification(), statusBarNotification.getUser(),
                    statusBarNotification.getPostTime());
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, sbn, false));

            if (grouped_notifications.containsKey(statusBarNotification.getId())) {
                //initial array
                int[] grouped = grouped_notifications.get(statusBarNotification.getId());
                //new value
                int newValue = nextId;
                //define the new array
                int[] newArray = new int[grouped.length + 1];
                //copy values into new array
                for (int i = 0; i < grouped.length; i++)
                    newArray[i] = grouped[i];
                newArray[newArray.length - 1] = newValue;
                grouped_notifications.put(statusBarNotification.getId(), newArray);
                Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved id: " + statusBarNotification.getId()
                        + " \\ newArray1: " + newArray );
            } else {
                grouped_notifications.put(statusBarNotification.getId(), new int[]{nextId});
                Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved id: " + statusBarNotification.getId()
                        + " \\ newArray2: " + nextId );
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
                Log.d(Constants.TAG, dataTransportResult.toString());
            }
        });

        //Disconnect transporter to avoid leaking
        notificationTransporter.disconnectTransportService();

        Log.i(Constants.TAG, "NotificationJobService StandardUI: " + dataBundle.toString());
    }

    private int getAudioManagerMode() {
        return ((AudioManager) Objects.requireNonNull(getSystemService(Context.AUDIO_SERVICE))).getMode();
    }

    private int getDefaultVibration() {
        return Integer.valueOf(Prefs.getString(Constants.PREF_NOTIFICATIONS_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATIONS_VIBRATION));
    }

    private boolean isRinging() {
        boolean isRinging = false;
        try {
            final int mode = getAudioManagerMode();
            if (AudioManager.MODE_IN_CALL == mode) {
                Log.d(Constants.TAG, "NotificationJobService Ringer: CALL");
            } else if (AudioManager.MODE_IN_COMMUNICATION == mode) {
                Log.d(Constants.TAG, "NotificationJobService Ringer: COMMUNICATION");
            } else if (AudioManager.MODE_RINGTONE == mode) {
                Log.d(Constants.TAG, "NotificationJobService Ringer: RINGTONE");
                isRinging = true;
            } else {
                Log.d(Constants.TAG, "NotificationJobService Ringer: SOMETHING ELSE");
            }
        } catch (NullPointerException e) {
            Log.e(Constants.TAG, "NotificationJobService getMode Exception: " + e.toString());
        }

        return isRinging;
    }

    private void handleCall(StatusBarNotification statusBarNotification, String notificationPackage) {
        Log.d(Constants.TAG, "NotificationJobService VoiceCall: " + notificationPackage);
        while (isRinging()) {
            if (System.currentTimeMillis() - lastTimeNotificationSent > VOICE_INTERVAL) {

                NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
                //notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);

                final PackageManager pm = getApplicationContext().getPackageManager();
                ApplicationInfo ai;
                try {
                    ai = pm.getApplicationInfo(notificationPackage, 0);
                } catch (final PackageManager.NameNotFoundException e) {
                    Log.e(Constants.TAG, "NotificationJobService getApplicationInfo Exception: " + e.toString());
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
            return Constants.FILTER_PACKAGE;
        }

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        List<NotificationCompat.Action> actions = wearableExtender.getActions();

        if (NotificationCompat.isGroupSummary(notification)) {
            Log.d(Constants.TAG, "NotificationJobService isGroupSummary: " + notificationPackage);
            if (Arrays.binarySearch(APP_WHITELIST, notificationPackage) < 0) {
                Log.d(Constants.TAG, "NotificationJobService notification blocked FLAG_GROUP_SUMMARY");
                return Constants.FILTER_GROUP;
            } else whitelistedApp = true;
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            Log.d(Constants.TAG, "NotificationJobService notification blocked FLAG_ONGOING_EVENT");
            return Constants.FILTER_ONGOING;
        }

        if (NotificationCompat.getLocalOnly(notification)) {
            Log.d(Constants.TAG, "NotificationJobService getLocalOnly: " + notificationPackage);
            if ((!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_LOCAL_ONLY, false) && !whitelistedApp) ||
                    ((Arrays.binarySearch(APP_WHITELIST, notificationPackage) >= 0) && !whitelistedApp)) {
                Log.d(Constants.TAG, "NotificationJobService notification blocked because is LocalOnly");
                return Constants.FILTER_LOCAL;
            } else if (!whitelistedApp) {
                localAllowed = true;
            }
        }

        CharSequence bigText = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TEXT);
        if (bigText != null) {
            text = bigText.toString();
        }
        Log.d(Constants.TAG, "NotificationJobService notificationPackage: " + notificationPackage + " / text: " + text);

        if (notificationTimeGone.containsKey(notificationId)) {
            String previousText = notificationTimeGone.get(notificationId);
            if ((previousText != null) && (previousText.equals(text)) && (!notificationPackage.equals("com.microsoft.office.outlook"))
                    && ((System.currentTimeMillis() - lastTimeNotificationArrived) < BLOCK_INTERVAL)) {
                Log.d(Constants.TAG, "NotificationJobService blocked text");
                return Constants.FILTER_BLOCK;
            } else {
                notificationTimeGone.put(notificationId, text);
                lastTimeNotificationArrived = System.currentTimeMillis();
                Log.d(Constants.TAG, "NotificationJobService allowed1: " + notificationPackage);
                if (localAllowed) return Constants.FILTER_LOCALOK;
                    //else if (whitelistedApp) return returnFilterResult(Constants.FILTER_CONTINUE);
                else return Constants.FILTER_UNGROUP;
            }
        }

        notificationTimeGone.put(notificationId, text);
        Log.d(Constants.TAG, "NotificationJobService allowed2: " + notificationPackage);

        if (localAllowed) {
            return Constants.FILTER_LOCALOK;
        }

        return Constants.FILTER_CONTINUE;

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

        NotificationPreferencesEntity app = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle();
        return app != null;
    }

    private boolean isPackageSilenced(String packageName) {
        NotificationPreferencesEntity app = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle();
        return app != null && app.getSilenceUntil() > SilenceApplicationHelper.getCurrentTimeSeconds();
    }


    private boolean isPackageFiltered(StatusBarNotification statusBarNotification) {
        String packageName = statusBarNotification.getPackageName();
        NotificationPreferencesEntity app = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle();
        if (app != null && app.getFilter() != null) {
            String notificationText = "";
            CharSequence text = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TEXT);
            CharSequence bigText = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TEXT);

            if (bigText != null) {
                notificationText = bigText.toString();
            }else{
                if (text != null && !text.toString().isEmpty()){
                    notificationText = text.toString();
                }
            }

            String[] filters = app.getFilter().split("\\r?\\n");
            for(String filter : filters) {
                Log.d(Constants.TAG, String.format("NotificationJobService isPakacgeFiltered Checking if '%s' contains '%s'",
                        notificationText, filter));
                if (!filter.isEmpty() && notificationText.contains(filter)){
                    Log.d(Constants.TAG, String.format("NotificationJobService isPakacgeFiltered Package '%s' filterered because '%s' contains '%s'",
                            packageName, notificationText,filter));
                    return true;
                }
            }
            return false;
        }else{
            return false;
        }
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

    private void storeForStats(StatusBarNotification statusBarNotification, byte filterResult) {
        try {
            NotificationEntity notificationEntity = new NotificationEntity();
            notificationEntity.setPackageName(statusBarNotification.getPackageName());
            notificationEntity.setDate(System.currentTimeMillis());
            notificationEntity.setFilterResult(filterResult);

            FlowManager.getModelAdapter(NotificationEntity.class).insert(notificationEntity);
        } catch (Exception ex) {
            Log.e(Constants.TAG, "NotificationJobService storeForStats Exception: " + ex.toString());
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
            Log.w(Constants.TAG, "NotificationJobService Notification not found to reply notificationId: " + notificationId);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void replyToNotification(StatusBarNotification statusBarNotification, String message) {

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
                    Log.e(Constants.TAG, "NotificationJobService replyToNotification error: " + e.getLocalizedMessage());
                }
            }
        }

    }

    private void mapNotification(StatusBarNotification statusBarNotification) {

        Log.d(Constants.TAG, "NotificationJobService maps: " + statusBarNotification.getPackageName());

        NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
        RemoteViews rmv = statusBarNotification.getNotification().contentView;
        if (rmv != null) {

            //Get text from RemoteView using reflection
            List<String> txt = extractText(rmv);
            if ((txt.size() > 0) && ((!(txt.get(0).isEmpty()) && !(txt.get(0).equals(lastTxt)))
                    || ((System.currentTimeMillis() - lastTimeNotificationSent) > MAPS_INTERVAL))) {

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
                    Log.i(Constants.TAG, "NotificationJobService mapNotification bitmap dimensions: " + width + " x " + height);

                    notificationData.setIcon(intArray);
                    notificationData.setIconWidth(width);
                    notificationData.setIconHeight(height);
                } catch (NullPointerException e) {
                    notificationData.setIcon(new int[]{});
                    Log.e(Constants.TAG, "NotificationJobService mapNotification failed to get bitmap %s" + e.toString());
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
                Log.d(Constants.TAG, "NotificationJobService maps lastTxt:  " + lastTxt);
            }

        } else {
            Log.w(Constants.TAG, "NotificationJobService maps null remoteView");
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
            Logger.get(NotificationJobService.class).e(e, "NotificationJobService extractText Exception: %s", e.toString());
        }
        return text;
    }

}