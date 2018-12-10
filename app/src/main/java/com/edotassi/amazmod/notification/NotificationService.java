package com.edotassi.amazmod.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
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
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
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
import com.edotassi.amazmod.util.NotificationUtils;
import com.edotassi.amazmod.util.Screen;
import com.edotassi.amazmod.watch.Watch;
import com.huami.watch.notification.data.StatusBarNotificationData;
import com.huami.watch.transport.DataBundle;
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

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.NotificationReplyData;

import static java.lang.Math.abs;

public class NotificationService extends NotificationListenerService {

    private Logger log = Logger.get(NotificationService.class);

    public static final int FLAG_WEARABLE_REPLY = 0x00000001;
    private static final long BLOCK_INTERVAL = 60000 * 60L; //One hour
    private static final long MAPS_INTERVAL = 60000 * 3L; //Three minutes
    private static final long VOICE_INTERVAL = 5000L; //Five seconds

    private static final long JOB_INTERVAL = 5 * 1000L; //Five seconds
    private static final long JOB_MAX_INTERVAL = 60 * 1000L; //1 minute
    private static final long CUSTOMUI_LATENCY = 1350L;

    private static final String[] APP_WHITELIST = { //apps that do not fit some filter
            "com.contapps.android",
            "com.microsoft.office.outlook",
            "com.skype.raider"
    };

    private static final String[] VOICE_APP_LIST = { //apps may use voice calls without notifications
            "com.skype.m2",
            "com.skype.raider",
            "org.telegram.messenger",
            "org.thunderdog.challegram"
    };

    private Map<String, String> notificationTimeGone;
    private Map<String, StatusBarNotification> notificationsAvailableToReply;

    private static long lastTimeNotificationArrived = 0;
    private static long lastTimeNotificationSent = 0;
    private static String lastTxt = "";

    private static ComponentName serviceComponent;
    private static JobScheduler jobScheduler;

    @Override
    public void onCreate() {
        super.onCreate();

        EventBus.getDefault().register(this);

        notificationsAvailableToReply = new HashMap<>();

        NotificationStore notificationStore = new NotificationStore();

        serviceComponent = new ComponentName(getApplicationContext(), NotificationJobService.class);
        jobScheduler = (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);

        log.d("NotificationService onCreate");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        log.d("NotificationService onListenerConnected");

        startPersistentNotification();

        scheduleJob(0, 0, null);


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

        if (isPackageSilenced(notificationPackage)) {
            log.d("NotificationService blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_SILENCE));
            storeForStats(statusBarNotification, Constants.FILTER_SILENCE);
            return;
        }

        if (isPackageFiltered(statusBarNotification)) {
            log.d("NotificationService blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_TEXT));
            storeForStats(statusBarNotification, Constants.FILTER_TEXT);
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

        log.d("NotificationService notificationPackage: " + notificationPackage + " / filterResult: " + Character.toString((char) (byte) filterResult));

        if (filterResult == Constants.FILTER_CONTINUE ||
                filterResult == Constants.FILTER_UNGROUP ||
                filterResult == Constants.FILTER_LOCALOK) {

            if (!isStandardDisabled()) {
                sendNotificationWithStandardUI(filterResult, statusBarNotification);
                notificationSent = true;
            }

            if (isCustomUIEnabled() && (filterResult != Constants.FILTER_LOCALOK)) {
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
            Log.d(Constants.TAG, "NotificationService onNotificationPosted: " + notificationPackage + " / " + Character.toString((char) (byte) filterResult));

            //Messenger voice call notifications
            if (isRingingNotification(filterResult, notificationPackage)) {
                Log.d(Constants.TAG, "NotificationService onNotificationPosted isRingingNotification: " + Character.toString((char) (byte) filterResult));
                handleCall(statusBarNotification, notificationPackage);

            //Maps notification
            } else if (isMapsNotification(filterResult, notificationPackage)) {
                Log.d(Constants.TAG, "NotificationService onNotificationPosted isMapsNotification: " + Character.toString((char) (byte) filterResult));
                mapNotification(statusBarNotification);
                //storeForStats(statusBarNotification, Constants.FILTER_MAPS); <- It is handled in the method

            //Blocked
            } else {
                log.d("NotificationService blocked: " + notificationPackage + " / " + Character.toString((char) (byte) filterResult));
                storeForStats(statusBarNotification, filterResult);
            }
        }
    }

    //Remove notification from watch if it was removed from phone
    Hashtable<Integer, int[]> grouped_notifications = new Hashtable<Integer, int[]>();

    @Override
    public void onNotificationRemoved(final StatusBarNotification statusBarNotification) {
        if (statusBarNotification == null) {
            return;
        }

        log.d("notificationRemoved: %s", statusBarNotification.getKey());

        if (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS, false)
                || (Prefs.getBoolean(Constants.PREF_DISABLE_REMOVE_NOTIFICATIONS, false))) {
            Log.d(Constants.TAG, "NotificationService onNotificationRemoved returning due to Settings");
            return;
        }

        if (isPackageAllowed(statusBarNotification.getPackageName())
                && (!NotificationCompat.isGroupSummary(statusBarNotification.getNotification()))
                && (!((statusBarNotification.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT))) {

            /*
            * Disabled while testing JobScheduler
            *
            //Connect transporter
            Transporter notificationTransporter = TransporterClassic.get(this, "com.huami.action.notification");
            notificationTransporter.connectTransportService();
            */

            DataBundle dataBundle = new DataBundle();
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));

            NotificationStore.addStandardNotification(statusBarNotification.getKey(), dataBundle);
            int id = NotificationJobService.NOTIFICATION_REMOVED;
            int jobId = id + abs((int) (long) (statusBarNotification.getId() % 10000L));

            scheduleJob(id, jobId, statusBarNotification.getKey());

            log.i("NotificationService notificationRemoved jobScheduled: " + jobId);

            /*
            * Disabled while testing JobScheduler
            *
            notificationTransporter.send("del", dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    log.d(dataTransportResult.toString());
                    Log.d(Constants.TAG, "NotificationService onNotificationRemoved id: " + statusBarNotification.getId());
                }
            });
            */

            if (grouped_notifications.containsKey(statusBarNotification.getId())) {
                //initial array
                int[] grouped = grouped_notifications.get(statusBarNotification.getId());
                for (int i : grouped) {
                    int nextId = abs((int) (long) (statusBarNotification.getId() % 10000L)) + i;
                    Log.d(Constants.TAG, "NotificationService onNotificationRemoved grouped id: " + nextId);
                    dataBundle = new DataBundle();
                    StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                            nextId, statusBarNotification.getTag(), 0, 0, 0,
                            statusBarNotification.getNotification(), statusBarNotification.getUser(),
                            statusBarNotification.getPostTime());
                    dataBundle.putParcelable("data", StatusBarNotificationData.from(this, sbn, false));

                    NotificationStore.addStandardNotification(statusBarNotification.getKey(), dataBundle);

                    scheduleJob(id, jobId, statusBarNotification.getKey());

                    log.i("NotificationService notificationRemoved grouped jobScheduled: " + jobId);

                    /*
                    * Disabled while testing JobScheduler
                    *
                    notificationTransporter.send("del", dataBundle, new Transporter.DataSendResultCallback() {
                        @Override
                        public void onResultBack(DataTransportResult dataTransportResult) {
                            log.d(dataTransportResult.toString());
                        }
                    });
                    */
                }
                grouped_notifications.remove(statusBarNotification.getId());
            }

            /*
            * Disabled while testing JobScheduler
            *
            //Disconnect transporter to avoid leaking
            notificationTransporter.disconnectTransportService();
            */

            //Reset time of last notification when notification is removed
            if (lastTimeNotificationArrived > 0) {
                lastTimeNotificationArrived = 0;
            }
            if (lastTimeNotificationSent > 0) {
                lastTimeNotificationSent = 0;
            }
        } else
            Log.d(Constants.TAG, "NotificationService onNotificationRemoved ignored: P || G || O");

    }

    private void sendNotificationWithCustomUI(StatusBarNotification statusBarNotification) {
        final String key = statusBarNotification.getKey();
        NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
        notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);

        if (isStandardDisabled()) {
            notificationData.setVibration(getDefaultVibration());
        } else notificationData.setVibration(0);
        notificationData.setHideReplies(false);
        notificationData.setHideButtons(true);
        notificationData.setForceCustom(false);

        NotificationStore.addCustomNotification(key, notificationData);
        NotificationStore.addNotificationBundle(key, statusBarNotification.getNotification().extras);
        int id = NotificationJobService.NOTIFICATION_POSTED_CUSTOM_UI;
        int jobId = id + abs((int) (long) (statusBarNotification.getId() % 10000L));

        scheduleJob(id, jobId, key);

        /*
        * Disabled while testing JobScheduler
        *
        extractImagesFromNotification(statusBarNotification, notificationData);

        Watch.get().postNotification(notificationData);
        */

        log.i("NotificationService CustomUI jobScheduled: " + jobId);
    }

    /*
    * Disabled while testing JobScheduler
    *
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
    */

    private void sendNotificationWithStandardUI(byte filterResult, StatusBarNotification statusBarNotification) {
        DataBundle dataBundle = new DataBundle();
        int id = NotificationJobService.NOTIFICATION_POSTED_STANDARD_UI;
        int jobId = id;

        if (filterResult == Constants.FILTER_UNGROUP && Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_UNGROUP, false)) {
            int nextId = abs((int) (long) (System.currentTimeMillis() % 10000L));
            jobId += abs((int) (long) (statusBarNotification.getId() % 10000L))+ nextId;
            StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                    statusBarNotification.getId() + nextId,
                    statusBarNotification.getTag(), 0, 0, 0,
                    statusBarNotification.getNotification(), statusBarNotification.getUser(),
                    statusBarNotification.getPostTime());
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, sbn, false));

            if (grouped_notifications.containsKey(statusBarNotification.getId())) {
                //initial array
                int[] grouped = grouped_notifications.get(statusBarNotification.getId());
                //define the new array
                int[] newArray = new int[grouped.length + 1];
                //copy values into new array
                System.arraycopy(grouped, 0, newArray, 0, grouped.length);
                newArray[newArray.length - 1] = nextId;
                grouped_notifications.put(statusBarNotification.getId(), newArray);
            } else {
                grouped_notifications.put(statusBarNotification.getId(), new int[]{nextId});
            }
        } else {
            jobId += abs((int) (long) (statusBarNotification.getId() % 10000L));
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));
        }

        NotificationStore.addStandardNotification(statusBarNotification.getKey(), dataBundle);

        scheduleJob(id, jobId, statusBarNotification.getKey());


        /*
        * Disabled while testing JobScheduler
        *

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
        */

        log.i("NotificationService StandardUI jobScheduled: " + jobId);

    }

    private void scheduleJob(int id, int jobId, String key) {

        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(NotificationJobService.NOTIFICATION_MODE, id);
        bundle.putString(NotificationJobService.NOTIFICATION_KEY, key);

        JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);

        if (jobId == 0) {
            builder.setPeriodic(120 * 1000L);

        } else {
            if (id == NotificationJobService.NOTIFICATION_POSTED_CUSTOM_UI
                    && (!Prefs.getBoolean(Constants.PREF_DISABLE_STANDARD_NOTIFICATIONS, false)))
                builder.setMinimumLatency(CUSTOMUI_LATENCY);
            else
                builder.setMinimumLatency(0);

            builder.setBackoffCriteria(JOB_INTERVAL, JobInfo.BACKOFF_POLICY_LINEAR);
            builder.setOverrideDeadline(JOB_MAX_INTERVAL);
            builder.setExtras(bundle);
        }

        jobScheduler.schedule(builder.build());
    }

    private int getAudioManagerMode() {
        try {
            return ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getMode();
        } catch (NullPointerException e) {
            log.e(e, "NotificationService isRinging Exception: %s", e.toString());
            return AudioManager.MODE_INVALID;
        }
    }

    private int getDefaultVibration() {
        return Integer.valueOf(Prefs.getString(Constants.PREF_NOTIFICATIONS_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATIONS_VIBRATION));

    }

    private int isRinging() {

        //Log.d(Constants.TAG, "NotificationJobService isRinging AudioManager.MODE_IN_CALL = " + AudioManager.MODE_IN_CALL);
        //Log.d(Constants.TAG, "NotificationJobService isRinging AudioManager.MODE_IN_COMMUNICATION = " + AudioManager.MODE_IN_COMMUNICATION);
        //Log.d(Constants.TAG, "NotificationJobService isRinging AudioManager.MODE_RINGTONE = " + AudioManager.MODE_RINGTONE);
        //Log.d(Constants.TAG, "NotificationJobService isRinging AudioManager.MODE_CURRENT = " + AudioManager.MODE_CURRENT);
        //Log.d(Constants.TAG, "NotificationJobService isRinging AudioManager.MODE_INVALID = " + AudioManager.MODE_INVALID);
        //Log.d(Constants.TAG, "NotificationJobService isRinging AudioManager.MODE_NORMAL = " + AudioManager.MODE_NORMAL);

        final int mode = getAudioManagerMode();
        if (AudioManager.MODE_IN_CALL == mode) {
            log.d("NotificationService Ringer: CALL");
        } else if (AudioManager.MODE_IN_COMMUNICATION == mode) {
            log.d("NotificationService Ringer: COMMUNICATION");
        } else if (AudioManager.MODE_RINGTONE == mode) {
            log.d("NotificationService Ringer: RINGTONE");
        } else {
            log.d("NotificationService Ringer: SOMETHING ELSE \\ mode: " + mode);
        }

        return mode;
    }

    private void handleCall(StatusBarNotification statusBarNotification, String notificationPackage) {
        log.d("NotificationService VoiceCall: " + notificationPackage);
        int mode = 0;
        if (notificationPackage.equals("org.thunderdog.challegram"))
            mode = 1;
        else if (notificationPackage.equals("org.telegram.messenger"))
            mode = 2;
        else if (notificationPackage.contains("skype"))
            mode = 3;
        int counter = 0;

        while (((isRinging() == AudioManager.MODE_RINGTONE) && (mode == 0))
                || ((mode == 1) && (counter < 3))
                || ((mode == 2) && ((counter < 3) && isRinging() != AudioManager.MODE_IN_COMMUNICATION))
                || ((mode == 3) && (counter < 3))) {
            long timeSinceLastNotification = (System.currentTimeMillis() - lastTimeNotificationSent);
            //Log.d(Constants.TAG, "NotificationService handleCall timeSinceLastNotification: " + timeSinceLastNotification);
            if (timeSinceLastNotification > VOICE_INTERVAL) {

                counter++;

                NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);

                final String key = statusBarNotification.getKey();

                final PackageManager pm = getApplicationContext().getPackageManager();

                //Log.d(Constants.TAG, "NotificationService handleCall notificationPackage: " + notificationPackage);

                ApplicationInfo ai;
                try {
                    ai = pm.getApplicationInfo(notificationPackage, 0);
                } catch (final PackageManager.NameNotFoundException e) {
                    log.e(e, "NotificationService getApplicationInfo Exception: %s", e.toString());
                    ai = null;
                }
                final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

                //Log.d(Constants.TAG, "NotificationService handleCall applicationName: " + applicationName);

                notificationData.setText(notificationData.getText() + "\n" + applicationName);
                notificationData.setVibration(getDefaultVibration());
                notificationData.setHideReplies(true);
                notificationData.setHideButtons(false);
                notificationData.setForceCustom(true);

                NotificationJobService.sendCustomNotification(this, notificationData);

                /*
                NotificationStore.addCustomNotification(key, notificationData);
                int id = NotificationJobService.NOTIFICATION_POSTED_VOICE;
                int jobId = id + abs((int) (long) (statusBarNotification.getId() % 10000L));

                scheduleJob(id, jobId, key);
                 */

                //Watch.get().postNotification(notificationData);

                lastTimeNotificationSent = System.currentTimeMillis();

                //Log.d(Constants.TAG, "NotificationService handleCall notificationData.getText: " + notificationData.getText());

                final int audioMode = getAudioManagerMode();

                Log.d(Constants.TAG, "NotificationService handleCall audioMode: " + audioMode + " \\ counter: " + counter);

                if (((AudioManager.MODE_RINGTONE != audioMode) && mode == 0) || ((counter == 2) && (mode == 1 || mode == 2 || mode == 3))) {
                    storeForStats(statusBarNotification, Constants.FILTER_VOICE);
                }
            } else
                SystemClock.sleep(300);
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
            log.d("NotificationService isGroupSummary: " + notificationPackage);
            if (Arrays.binarySearch(APP_WHITELIST, notificationPackage) < 0) {
                log.d("notification blocked FLAG_GROUP_SUMMARY");
                return Constants.FILTER_GROUP;
            } else whitelistedApp = true;
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            log.d("notification blocked FLAG_ONGOING_EVENT");
            return Constants.FILTER_ONGOING;
        }

        if (NotificationCompat.getLocalOnly(notification)) {
            log.d("NotificationService getLocalOnly: " + notificationPackage);
            if ((!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_LOCAL_ONLY, false) && !whitelistedApp) ||
                    ((Arrays.binarySearch(APP_WHITELIST, notificationPackage) >= 0) && !whitelistedApp)) {
                log.d("notification blocked because is LocalOnly");
                return Constants.FILTER_LOCAL;
            } else if (!whitelistedApp) {
                localAllowed = true;
            }
        }

        CharSequence bigText = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TEXT);
        if (bigText != null) {
            text = bigText.toString();
        }
        log.d("NotificationService notificationPackage: " + notificationPackage + " \\ text: " + text);

        if (notificationTimeGone.containsKey(notificationId)) {
            String previousText = notificationTimeGone.get(notificationId);
            if ((previousText != null) && (previousText.equals(text)) && (!notificationPackage.equals("com.microsoft.office.outlook"))
                    && ((System.currentTimeMillis() - lastTimeNotificationArrived) < BLOCK_INTERVAL)) {
                log.d("NotificationService blocked text");
                //Logger.debug("notification blocked by key: %s, id: %s, flags: %s, time: %s", notificationId, statusBarNotification.getId(), statusBarNotification.getNotification().flags, (System.currentTimeMillis() - statusBarNotification.getPostTime()));
                return Constants.FILTER_BLOCK;
            } else {
                notificationTimeGone.put(notificationId, text);
                lastTimeNotificationArrived = System.currentTimeMillis();
                log.d("NotificationService allowed1: " + notificationPackage);
                //Logger.debug("notification allowed");
                if (localAllowed) return Constants.FILTER_LOCALOK;
                    //else if (whitelistedApp) return returnFilterResult(Constants.FILTER_CONTINUE);
                else return Constants.FILTER_UNGROUP;
            }
        }

        notificationTimeGone.put(notificationId, text);
        log.d("NotificationService allowed2: " + notificationPackage);

        if (localAllowed) {
            return Constants.FILTER_LOCALOK;
        }

        return Constants.FILTER_CONTINUE;

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

    private boolean isCustomUIEnabled() {
        return Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI, false);
    }

    private boolean isStandardDisabled() {
        return Prefs.getBoolean(Constants.PREF_DISABLE_STANDARD_NOTIFICATIONS, false);
    }

    private boolean isRingingNotification(byte filterResult, String notificationPackage) {

        final boolean prefs = Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_VOICE_APPS, false);
        final int ring = isRinging();
        return ((filterResult == Constants.FILTER_ONGOING)
                && ((prefs && (ring == AudioManager.MODE_RINGTONE))
                || ((Arrays.binarySearch(VOICE_APP_LIST, notificationPackage) >= 0) && (ring == AudioManager.MODE_NORMAL))
                || ((notificationPackage.contains("skype")) && (ring == AudioManager.MODE_IN_COMMUNICATION))));
    }

    private boolean isMapsNotification(byte filterResult, String notificationPackage) {
        return ((filterResult == Constants.FILTER_ONGOING) && notificationPackage.contains("android.apps.maps"));
    }

    private boolean isPackageAllowed(String packageName) {
        /*
        String packagesJson = Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");
        Gson gson = new Gson();
        String[] packagesList = gson.fromJson(packagesJson, String[].class);
        return Arrays.binarySearch(packagesList, packageName) >= 0;
        */
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

            CharSequence title = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TITLE);
            CharSequence bigTitle = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TITLE_BIG);

            String notificationTitle = "";

            if (bigTitle != null){
                notificationTitle = bigTitle.toString();
            }else{
                if (title != null && !title.toString().isEmpty()){
                    notificationTitle = title.toString();
                }
            }

            if (bigText != null) {
                notificationText = bigText.toString();
            }else{
                if (text != null && !text.toString().isEmpty()){
                    notificationText = text.toString();
                }
            }

            String[] filters = app.getFilter().split("\\r?\\n");
            for(String filter : filters) {
                log.d("Checking if '%s' contains '%s'", notificationText, filter);
                if (!filter.isEmpty()){
                    filter = filter.toLowerCase();
                    if (notificationTitle.toLowerCase().contains(filter)) {
                        log.d("Package '%s' filterered because TITLE ('%s') contains '%s'", packageName, notificationTitle, filter);
                        return true;
                    }
                    if (notificationText.toLowerCase().contains(filter)) {
                        log.d("Package '%s' filterered because CONTENTS ('%s') contains '%s'", packageName, notificationText, filter);
                        return true;
                    }
                }
            }
            return false;
        }else{
            return false;
        }
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
    public void replyToNotificationLocal(ReplyToNotificationLocal replyToNotificationLocal) {
        NotificationReplyData notificationReplyData = replyToNotificationLocal.getNotificationReplyData();
        String notificationId = notificationReplyData.getNotificationId();
        String reply = notificationReplyData.getReply();

        Log.d(Constants.TAG, "NotificationService replyToNotificationLocal notificationId: " + notificationId);

        StatusBarNotification statusBarNotification = notificationsAvailableToReply.get(notificationId);
        if (statusBarNotification != null) {
            notificationsAvailableToReply.remove(notificationId);

            Action action = NotificationUtils.getQuickReplyAction(statusBarNotification.getNotification(), statusBarNotification.getPackageName());
            if (action != null)
                try {
                    action.sendReply(this, reply);
                } catch (PendingIntent.CanceledException e) {
                    Log.e(Constants.TAG, "NotificationService replyToNotificationLocal exception: " + e.toString());
                }

            /*
            NotificationWear notificationWear = getNotificationWear(statusBarNotification);
            if (notificationWear != null) {
                reply(notificationWear, statusBarNotification.getKey(), reply);
                Log.d(Constants.TAG, "NotificationService replyToNotificationLocal sent reply: " + notificationId);
            }
            else {
                replyToNotification(statusBarNotification, reply);
                Log.d(Constants.TAG, "NotificationService replyToNotificationLocal sent replyToNotification: " + notificationId);
            }
            */

        } else {
            log.w("replyToNotificationLocal Notification %s not found to reply", notificationId);
        }
    }

    private void mapNotification(StatusBarNotification statusBarNotification) {

        log.d("NotificationService maps: " + statusBarNotification.getPackageName());

        RemoteViews rmv = statusBarNotification.getNotification().contentView;

        NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);

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
            //int counter = 0;

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
                if (methodName == null)
                    continue;
                    // Save strings
                else {

                    if (methodName.equals("setText")) {
                        // Parameter type (10 = Character Sequence)
                        parcel.readInt();

                        // Store the actual string
                        String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                        text.add(t);
                        //Log.d(Constants.TAG, "NotificationService extractText " + counter + " t: " + t);
                        //counter++;
                    }
                }
                parcel.recycle();

            }
        }
        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e) {
            Logger.get(NotificationService.class).e(e, "extractText: %s", e.toString());
        }
        return text;
    }

    private void startPersistentNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final String model = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(Constants.PREF_WATCH_MODEL, "");

            PersistentNotification persistentNotification = new PersistentNotification(this, model);
            Notification notification = persistentNotification.createPersistentNotification();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (mNotificationManager != null) {
                    mNotificationManager.notify(persistentNotification.getNotificationId(), notification);
                }
                startForeground(persistentNotification.getNotificationId(), notification);
            }

        }
    }

}
