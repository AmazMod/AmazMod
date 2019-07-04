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
import android.media.AudioManager;
import android.os.Build;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity_Table;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
import com.edotassi.amazmod.notification.factory.NotificationFactory;
import com.edotassi.amazmod.support.SilenceApplicationHelper;
import com.edotassi.amazmod.transport.TransportService;
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
import org.tinylog.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.NotificationReplyData;

import static java.lang.Math.abs;

public class NotificationService extends NotificationListenerService {

    public static final int FLAG_WEARABLE_REPLY = 0x00000001;
    private static final long BLOCK_INTERVAL = 60000 * 60L; //One hour
    private static final long MAPS_INTERVAL = 60000 * 3L; //Three minutes
    private static final long VOICE_INTERVAL = 5000L; //Five seconds

    private static final long JOB_INTERVAL = 5 * 1000L; //Five seconds
    private static final long JOB_MAX_INTERVAL = 60 * 1000L; //1 minute
    private static final long KEEP_SERVICE_RUNNING_INTERVAL = 60000L * 15L; //15 minutes
    private static final long CUSTOMUI_LATENCY = 1L;

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
    Hashtable<Integer, int[]> grouped_notifications = new Hashtable<>();

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

        Logger.debug("onCreate");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Logger.debug("onListenerConnected");

        startPersistentNotification();

        //Cancel all pending jobs to keep service running, then schedule a new one
        cancelPendingJobs(0);
        scheduleJob(0, 0, null);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.debug("onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        Logger.debug("onDestroy");
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        Logger.debug("onNotificationPosted notificationPosted: " + statusBarNotification.getKey());

        String notificationPackage = statusBarNotification.getPackageName();

        if (!isPackageAllowed(notificationPackage)) {
            Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_PACKAGE));
            storeForStats(notificationPackage, Constants.FILTER_PACKAGE);
            return;
        }

        if (isPackageSilenced(notificationPackage)) {
            Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_SILENCE));
            storeForStats(notificationPackage, Constants.FILTER_SILENCE);
            return;
        }

        if (isPackageFiltered(statusBarNotification)) {
            Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_TEXT));
            storeForStats(notificationPackage, Constants.FILTER_TEXT);
            return;
        }

        if (isNotificationsDisabled()) {
            Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_NOTIFICATIONS_DISABLED));
            storeForStats(notificationPackage, Constants.FILTER_NOTIFICATIONS_DISABLED);
            return;
        }

        if (isNotificationsDisabledWhenScreenOn()) {
            if (!Screen.isDeviceLocked(this)) {
                Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_SCREENON));
                storeForStats(notificationPackage, Constants.FILTER_SCREENON);
                return;
            } else if (!isNotificationsEnabledWhenScreenLocked()) {
                Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + Character.toString((char) (byte) Constants.FILTER_SCREENLOCKED));
                storeForStats(notificationPackage, Constants.FILTER_SCREENLOCKED);
                return;
            }
        }

        byte filterResult = filter(statusBarNotification);

        Logger.debug("onNotificationPosted notificationPackage: " + notificationPackage + " / filterResult: " + Character.toString((char) (byte) filterResult));

        //Log.d(Constants.TAG, "Filters: U=" + (filterResult == Constants.FILTER_UNGROUP) +" C="+ (filterResult == Constants.FILTER_CONTINUE) +" K="+ (filterResult == Constants.FILTER_LOCALOK) );
        if (filterResult == Constants.FILTER_CONTINUE ||
                filterResult == Constants.FILTER_UNGROUP ||
                filterResult == Constants.FILTER_LOCALOK) {

            StatusBarNotification sbn = null;

            if (filterResult == Constants.FILTER_UNGROUP && Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_UNGROUP, false)) {
                //Log.d(Constants.TAG, "NotificationService onNotificationPosted ungroup01 key: " + statusBarNotification.getKey()
                //        + " \\ id: " + statusBarNotification.getId());
                int nextId = statusBarNotification.getId() + newUID();
                sbn = new StatusBarNotification(notificationPackage, "", nextId,
                        statusBarNotification.getTag(), 0, 0, 0,
                        statusBarNotification.getNotification(), statusBarNotification.getUser(),
                        statusBarNotification.getPostTime());

                if (grouped_notifications.containsKey(statusBarNotification.getId())) {
                    //Log.d(Constants.TAG, "NotificationService onNotificationPosted ungroup02 id exists: " + statusBarNotification.getId()
                    //        + " \\ nextId: " + nextId);
                    // Get array
                    int[] grouped = grouped_notifications.get(statusBarNotification.getId());
                    // Define the new array
                    if (grouped != null) {
                        int[] newArray = new int[grouped.length + 1];
                        // Copy values into new array
                        System.arraycopy(grouped, 0, newArray, 0, grouped.length);
                        newArray[newArray.length - 1] = nextId;
                        grouped_notifications.put(statusBarNotification.getId(), newArray);
                        //Log.d(Constants.TAG, "NotificationService onNotificationPosted ungroup03 id exists newArray: " + Arrays.toString(newArray));
                    } else
                        Logger.error("grouped: could not create array");
                } else {
                    //Log.d(Constants.TAG, "NotificationService onNotificationPosted ungroup04 new id: " + statusBarNotification.getId()
                    //        + " \\ nextId: " + nextId);
                    // New in array
                    grouped_notifications.put(statusBarNotification.getId(), new int[]{nextId});
                }
            }

            if (sbn == null){
                sbn = statusBarNotification;
            }

            if (isCustomUIEnabled()) {
                sendNotificationWithCustomUI(filterResult, sbn);
            } else {
                sendNotificationWithStandardUI(filterResult, sbn);
            }

            Logger.debug("onNotificationPosted sent: " + notificationPackage + " / " + Character.toString((char) (byte) filterResult));
            storeForStats(notificationPackage, filterResult);

        } else {
            Logger.debug("onNotificationPosted: " + notificationPackage + " / " + Character.toString((char) (byte) filterResult));

            //Messenger voice call notifications
            if (isRingingNotification(filterResult, notificationPackage)) {
                Logger.debug("onNotificationPosted isRingingNotification: " + Character.toString((char) (byte) filterResult));
                handleCall(statusBarNotification, notificationPackage);

                //Maps notification
            } else if (isMapsNotification(filterResult, notificationPackage)) {
                Logger.debug("onNotificationPosted isMapsNotification: " + Character.toString((char) (byte) filterResult));
                mapNotification(statusBarNotification);
                //storeForStats(statusBarNotification, Constants.FILTER_MAPS); <- It is handled in the method

                //Blocked
            } else {
                Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + Character.toString((char) (byte) filterResult));
                storeForStats(notificationPackage, filterResult);
            }
        }
    }

    //Remove notification from watch if it was removed from phone
    @Override
    public void onNotificationRemoved(final StatusBarNotification statusBarNotification) {
        if (statusBarNotification == null) {
            return;
        }

        String key = statusBarNotification.getKey();

        Logger.debug("onNotificationRemoved notificationRemoved: {}", key);
        //Log.d(Constants.TAG, "NotificationService onNotificationRemoved ungroup00 key: " + key);

        if (!Prefs.getBoolean(Constants.PREF_ENABLE_NOTIFICATIONS, Constants.PREF_DEFAULT_ENABLE_NOTIFICATIONS)
                || (Prefs.getBoolean(Constants.PREF_DISABLE_REMOVE_NOTIFICATIONS, false))) {
            Logger.debug("onNotificationRemoved returning due to Settings");
            return;
        }

        if (isPackageAllowed(statusBarNotification.getPackageName())
                //&& (!NotificationCompat.isGroupSummary(statusBarNotification.getNotification()))
                && ((statusBarNotification.getNotification().flags & Notification.FLAG_ONGOING_EVENT) != Notification.FLAG_ONGOING_EVENT)) {

            /*
            * Disabled while testing JobScheduler
            *
            //Connect transporter
            Transporter notificationTransporter = TransporterClassic.get(this, "com.huami.action.notification");
            notificationTransporter.connectTransportService();
            */

            DataBundle dataBundle = new DataBundle();
            dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));

            String uuid = newKey(key);
            NotificationStore.addRemovedNotification(uuid, dataBundle);
            int id = NotificationJobService.NOTIFICATION_REMOVED;
            int jobId = statusBarNotification.getId() + newUID();

            scheduleJob(id, jobId, uuid);

            Logger.info("onNotificationRemoved jobScheduled: " + jobId + " \\ uuid: " + uuid);

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
                //Log.d(Constants.TAG, "NotificationService onNotificationRemoved ungroup01 key: " + statusBarNotification.getKey()
                //        + " \\ id: " + statusBarNotification.getId());
                // Initial array
                int[] grouped = grouped_notifications.get(statusBarNotification.getId());
                //Log.d(Constants.TAG, "NotificationService onNotificationRemoved ungroup02 key: " + statusBarNotification.getKey()
                //        + " \\ grouped: " + Arrays.toString(grouped));
                // Loop each notification in group
                assert grouped != null;
                for (int groupedId : grouped) {
                    //int nextId = abs((int) (long) (statusBarNotification.getId() % 10000L)) + i;
                    jobId = groupedId + newUID();
                    //Log.d(Constants.TAG, "NotificationService onNotificationRemoved ungroup i: " + groupedId);

                    dataBundle = new DataBundle();
                    StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                            groupedId, statusBarNotification.getTag(), 0, 0, 0,
                            statusBarNotification.getNotification(), statusBarNotification.getUser(),
                            statusBarNotification.getPostTime());
                    dataBundle.putParcelable("data", StatusBarNotificationData.from(this, sbn, false));

                    uuid = newKey(statusBarNotification.getKey());
                    NotificationStore.addRemovedNotification(uuid, dataBundle);

                    scheduleJob(id, jobId, uuid);

                    Logger.info("onNotificationRemoved ungroup jobScheduled: " + jobId + " \\ uuid: " + uuid);

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
            Logger.debug("onNotificationRemoved ignored: P || G || O");
    }

    private void sendNotificationWithCustomUI(byte filterResult, StatusBarNotification statusBarNotification) {
        final String uuid = newKey(statusBarNotification.getKey());
        Logger.debug("sendNotificationWithCustomdUI uuid: " + uuid + " \\ filterResult: " + filterResult);
        NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
        notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);

        notificationData.setVibration(getDefaultVibration());
        notificationData.setHideButtons(true);
        notificationData.setForceCustom(false);

        if (filterResult == Constants.FILTER_LOCALOK)
            notificationData.setHideReplies(true);
        else
            notificationData.setHideReplies(false);

        if (isJobSchedulerEnabled()) {
            NotificationStore.addCustomNotification(uuid, notificationData);
            int id = NotificationJobService.NOTIFICATION_POSTED_CUSTOM_UI;
            int jobId = statusBarNotification.getId() + newUID();
            scheduleJob(id, jobId, uuid);
            Logger.info("sendNotificationWithCustomUI jobScheduled: " + jobId + " \\ uuid: " + uuid);
        } else {
            Watch.get().postNotification(notificationData);
            Logger.info("sendNotificationWithCustomUI sent without schedule: " + statusBarNotification.getKey());
        }
    }

    private void sendNotificationWithStandardUI(byte filterResult, StatusBarNotification statusBarNotification) {

        String uuid = newKey(statusBarNotification.getKey());
        int notificationId = statusBarNotification.getId();
        Logger.debug("sendNotificationWithStandardUI uuid: " + uuid + " \\ filterResult: " + filterResult);
        DataBundle dataBundle = new DataBundle();
        int id = NotificationJobService.NOTIFICATION_POSTED_STANDARD_UI;
        int jobId = notificationId + newUID();
        dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));

        if (isJobSchedulerEnabled()) {
            NotificationStore.addStandardNotification(uuid, dataBundle);
            scheduleJob(id, jobId, uuid);
            Logger.info("sendNotificationWithStandardUI jobScheduled: " + jobId + " \\ uuid: " + uuid);
        } else {
            TransportService.sendWithTransporterHuami("add", dataBundle);
            Logger.info("sendNotificationWithStandardUI: " + dataBundle.toString());
        }
    }

    private void scheduleJob(int id, int jobId, String uuid) {

        Logger.debug("id: {} jobId: {} uuid: {}", id, jobId, uuid);
        JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);

        if (jobId == 0) {
            builder.setPeriodic(KEEP_SERVICE_RUNNING_INTERVAL);

        } else {
            if (id == NotificationJobService.NOTIFICATION_POSTED_CUSTOM_UI
                    && (!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI, true))) {
                builder.setMinimumLatency(CUSTOMUI_LATENCY);
            } else {
                builder.setMinimumLatency(1L);
            }

            PersistableBundle bundle = new PersistableBundle();
            bundle.putInt(NotificationJobService.NOTIFICATION_MODE, id);
            bundle.putString(NotificationJobService.NOTIFICATION_UUID, uuid);

            builder.setBackoffCriteria(JOB_INTERVAL, JobInfo.BACKOFF_POLICY_LINEAR);
            builder.setOverrideDeadline(JOB_MAX_INTERVAL);
            builder.setOverrideDeadline(1L);
            builder.setExtras(bundle);
        }
        jobScheduler.schedule(builder.build());
    }

    private int newUID() {
        return abs((int) (long) (System.currentTimeMillis() % 10000L));
    }

    private String newKey(String key) {
        return key + "|" + String.valueOf(System.currentTimeMillis());
    }

    private void cancelPendingJobs(int id) {
        List<JobInfo> jobInfoList = jobScheduler.getAllPendingJobs();
        final int pendingJobs = jobInfoList.size();
        Logger.debug("cancelPendingJobs pendingJobs: " + pendingJobs);
        if (pendingJobs > 0)
            for (JobInfo jobInfo : jobInfoList) {
                Logger.debug("cancelPendingJobs jobInfo: " + jobInfo.toString());
                if (jobInfo.getId() == id)
                    jobScheduler.cancel(id);
            }
    }

    public static void cancelPendingJobs() {
        List<JobInfo> jobInfoList = jobScheduler.getAllPendingJobs();
        final int pendingJobs = jobInfoList.size();
        Logger.debug("cancelPendingJobs pendingJobs: " + pendingJobs);
        if (pendingJobs > 0)
            for (JobInfo jobInfo : jobInfoList) {
                Logger.debug("cancelPendingJobs jobInfo: " + jobInfo.toString());
                jobScheduler.cancel(jobInfo.getId());
            }
    }

    private int getAudioManagerMode() {
        try {
            return ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getMode();
        } catch (NullPointerException e) {
            Logger.error(e, "isRinging Exception: {}", e.getMessage());
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
            Logger.debug("isRinging Ringer: CALL");
        } else if (AudioManager.MODE_IN_COMMUNICATION == mode) {
            Logger.debug("isRinging Ringer: COMMUNICATION");
        } else if (AudioManager.MODE_RINGTONE == mode) {
            Logger.debug("isRinging Ringer: RINGTONE");
        } else {
            Logger.debug("isRinging Ringer: SOMETHING ELSE \\ mode: " + mode);
        }

        return mode;
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
            Logger.debug("filter isGroupSummary: " + notificationPackage);
            if (Arrays.binarySearch(APP_WHITELIST, notificationPackage) < 0) {
                Logger.debug("filter: notification blocked FLAG_GROUP_SUMMARY");
                return Constants.FILTER_GROUP;
            } else whitelistedApp = true;
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            Logger.debug("filter: notification blocked FLAG_ONGOING_EVENT");
            return Constants.FILTER_ONGOING;
        }

        if (NotificationCompat.getLocalOnly(notification)) {
            Logger.debug("filter: getLocalOnly: " + notificationPackage);
            if ((!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_LOCAL_ONLY, false) && !whitelistedApp) ||
                    ((Arrays.binarySearch(APP_WHITELIST, notificationPackage) >= 0) && !whitelistedApp)) {
                Logger.debug("filter: notification blocked because is LocalOnly");
                return Constants.FILTER_LOCAL;
            } else if (!whitelistedApp) {
                localAllowed = true;
            }
        }

        CharSequence bigText = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TEXT);
        if (bigText != null) {
            text = bigText.toString();
        }
        Logger.debug("filter: notificationPackage: " + notificationPackage + " \\ text: " + text);

        if (notificationTimeGone.containsKey(notificationId)) {
            String previousText = notificationTimeGone.get(notificationId);
            if ((previousText != null) && (previousText.equals(text)) && (!notificationPackage.equals("com.microsoft.office.outlook"))
                    && ((System.currentTimeMillis() - lastTimeNotificationArrived) < BLOCK_INTERVAL)) {
                Logger.debug("filter: blocked text");
                //Logger.debug("notification blocked by key: %s, id: %s, flags: %s, time: %s", notificationId, statusBarNotification.getId(), statusBarNotification.getNotification().flags, (System.currentTimeMillis() - statusBarNotification.getPostTime()));
                return Constants.FILTER_BLOCK;
            } else {
                notificationTimeGone.put(notificationId, text);
                lastTimeNotificationArrived = System.currentTimeMillis();
                Logger.debug("filter: allowed1: " + notificationPackage);
                //Logger.debug("notification allowed");
                if (localAllowed) return Constants.FILTER_LOCALOK;
                    //else if (whitelistedApp) return returnFilterResult(Constants.FILTER_CONTINUE);
                else return Constants.FILTER_UNGROUP;
            }
        }

        notificationTimeGone.put(notificationId, text);
        Logger.debug("filter: allowed2: " + notificationPackage);

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
        return !Prefs.getBoolean(Constants.PREF_ENABLE_NOTIFICATIONS, Constants.PREF_DEFAULT_ENABLE_NOTIFICATIONS) ||
                (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_WHEN_DND, false) &&
                        Screen.isDNDActive(this, getContentResolver()));
    }

    private boolean isNotificationsDisabledWhenScreenOn() {
        return !Prefs.getBoolean(Constants.PREF_ENABLE_NOTIFATIONS_WHEN_SCREEN_ON, false)
                && Screen.isInteractive(this);
    }

    private boolean isJobSchedulerEnabled() {
        return Prefs.getBoolean(Constants.PREF_NOTIFICATION_SCHEDULER, Constants.PREF_NOTIFICATION_SCHEDULER_DEFAULT);
    }

    private boolean isNotificationsEnabledWhenScreenLocked() {
        return Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_WHEN_LOCKED, true);
    }

    private boolean isCustomUIEnabled() {
        return Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI, false);
    }

    /*private boolean isStandardDisabled() {
        return Prefs.getBoolean(Constants.PREF_DISABLE_STANDARD_NOTIFICATIONS, false);
    }*/

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

            if (bigTitle != null) {
                notificationTitle = bigTitle.toString();
            } else {
                if (title != null && !title.toString().isEmpty()) {
                    notificationTitle = title.toString();
                }
            }

            if (bigText != null) {
                notificationText = bigText.toString();
            } else {
                if (text != null && !text.toString().isEmpty()) {
                    notificationText = text.toString();
                }
            }

            String[] filters = app.getFilter().split("\\r?\\n");
            for (String filter : filters) {
                Logger.debug("isPackageFiltered: Checking if '{}' contains '{}'", notificationText, filter);
                if (!filter.isEmpty()) {
                    filter = filter.toLowerCase();
                    if (notificationTitle.toLowerCase().contains(filter)) {
                        Logger.debug("isPackageFiltered: Package '{}' filterered because TITLE ('{}') contains '{}'", packageName, notificationTitle, filter);
                        return true;
                    }
                    if (notificationText.toLowerCase().contains(filter)) {
                        Logger.debug("isPackageFiltered: Package '{}' filterered because CONTENTS ('{}') contains '{}'", packageName, notificationText, filter);
                        return true;
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private void storeForStats(String notificationPackage, byte filterResult) {
        try {
            NotificationEntity notificationEntity = new NotificationEntity();
            notificationEntity.setPackageName(notificationPackage);
            notificationEntity.setDate(System.currentTimeMillis());
            notificationEntity.setFilterResult(filterResult);

            FlowManager.getModelAdapter(NotificationEntity.class).insert(notificationEntity);
        } catch (Exception ex) {
            Logger.error(ex, "storeForStats: Failed to store notifications stats");
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void replyToNotificationLocal(ReplyToNotificationLocal replyToNotificationLocal) {
        NotificationReplyData notificationReplyData = replyToNotificationLocal.getNotificationReplyData();
        String notificationId = notificationReplyData.getNotificationId();
        String reply = notificationReplyData.getReply();

        Logger.debug("replyToNotificationLocal notificationId: " + notificationId);

        StatusBarNotification statusBarNotification = notificationsAvailableToReply.get(notificationId);
        if (statusBarNotification != null) {
            notificationsAvailableToReply.remove(notificationId);

            Action action = NotificationUtils.getQuickReplyAction(statusBarNotification.getNotification(), statusBarNotification.getPackageName());
            if (action != null)
                try {
                    action.sendReply(this, reply);
                } catch (PendingIntent.CanceledException e) {
                    Logger.error(e, "replyToNotificationLocal exception: " + e.toString());
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
            Logger.warn("replyToNotificationLocal Notification {} not found to reply", notificationId);
        }
    }

    private void mapNotification(StatusBarNotification statusBarNotification) {

        final String notificationPackage = statusBarNotification.getPackageName();

        Logger.debug("mapNotification package: {} key: {}", notificationPackage, statusBarNotification.getKey());

        NotificationData notificationData = NotificationFactory.getMapNotification(this, statusBarNotification);

        if (notificationData != null) {

            final String txt = notificationData.getText();

            if ( !txt.equals(lastTxt) || ((System.currentTimeMillis() - lastTimeNotificationSent) > MAPS_INTERVAL)) {

                notificationData.setVibration(getDefaultVibration());
                lastTxt = txt;

                TransportService.sendWithTransporterNotifications(Transport.INCOMING_NOTIFICATION, notificationData.toDataBundle(new DataBundle()));

                lastTimeNotificationSent = System.currentTimeMillis();
                storeForStats(notificationPackage, Constants.FILTER_MAPS);
                Logger.debug("mapNotification maps lastTxt: " + lastTxt);

            } else
                Logger.warn("same text or too soon");

        } else
            Logger.error("null notificationData");

    }


    private void handleCall(StatusBarNotification statusBarNotification, String notificationPackage) {
        Logger.debug("handleCall VoiceCall: " + notificationPackage);
        int mode = 0;
        if (notificationPackage.equals("org.thunderdog.challegram"))
            mode = 1;
        else if (notificationPackage.equals("org.telegram.messenger"))
            mode = 2;
        else if (notificationPackage.contains("skype"))
            mode = 3;
        int counter = 0;

        while (    ((mode == 0) && (isRinging() == AudioManager.MODE_RINGTONE))
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
                    Logger.error(e, "handleCall getApplicationInfo Exception: {}", e.getMessage());
                    ai = null;
                }
                final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

                //Log.d(Constants.TAG, "NotificationService handleCall applicationName: " + applicationName);

                notificationData.setText(notificationData.getText() + "\n" + applicationName);
                notificationData.setVibration(getDefaultVibration());
                notificationData.setHideReplies(true);
                notificationData.setHideButtons(false);
                notificationData.setForceCustom(true);

                //NotificationJobService.sendCustomNotification(this, notificationData);

                /*
                NotificationStore.addCustomNotification(key, notificationData);
                int id = NotificationJobService.NOTIFICATION_POSTED_VOICE;
                int jobId = id + abs((int) (long) (statusBarNotification.getId() % 10000L));

                scheduleJob(id, jobId, key);
                 */

                TransportService.sendWithTransporterNotifications(Transport.INCOMING_NOTIFICATION, notificationData.toDataBundle(new DataBundle()));

                lastTimeNotificationSent = System.currentTimeMillis();

                Logger.debug("handleCall notificationData.getText: " + notificationData.getText());

                final int audioMode = getAudioManagerMode();

                Logger.debug("handleCall audioMode: " + audioMode + " \\ counter: " + counter);

                if (((AudioManager.MODE_RINGTONE != audioMode) && mode == 0) || ((counter == 2) && (mode == 1 || mode == 2 || mode == 3))) {
                    storeForStats(notificationPackage, Constants.FILTER_VOICE);
                }
            } else
                SystemClock.sleep(300);
        }
    }

    private void startPersistentNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final String model = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(Constants.PREF_WATCH_MODEL, "");

            PersistentNotification persistentNotification = new PersistentNotification(this, model);
            Notification notification = persistentNotification.createPersistentNotification();

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null)
                mNotificationManager.notify(persistentNotification.getNotificationId(), notification);

            startForeground(persistentNotification.getNotificationId(), notification);
        }
    }

}
