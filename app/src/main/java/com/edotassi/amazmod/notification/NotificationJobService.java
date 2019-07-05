package com.edotassi.amazmod.notification;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;

import androidx.collection.ArrayMap;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.event.local.IsWatchConnectedLocal;
import com.edotassi.amazmod.transport.TransportService;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.NotificationData;

import static android.service.notification.NotificationListenerService.requestRebind;

public class NotificationJobService extends JobService implements TransportService.DataTransportResultCallback {

    private JobParameters params;

    public static final String NOTIFICATION_UUID = "notification_uuid";
    public static final String NOTIFICATION_MODE = "notification_mode";
    public static final int NOTIFICATION_POSTED_STANDARD_UI = 1000;
    public static final int NOTIFICATION_POSTED_CUSTOM_UI = 2000;
    public static final int NOTIFICATION_REMOVED = 3000;

    private static int retries = 0;
    private static ArrayMap<String, JobParameters> pendingJobs = new ArrayMap<>();
    private static ArrayMap<String, JobParameters> jobParams = new ArrayMap<>();

    @Override
    public boolean onStartJob(JobParameters params) {

        this.params = params;

        final int id = params.getJobId();
        final String uuid = params.getExtras().getString(NOTIFICATION_UUID, null);
        final int mode = params.getExtras().getInt(NOTIFICATION_MODE, -1);

        if (id == 0)
            keepNotificationServiceRunning();

        int std = 0, cst = 0, rmv = 0;
        try {
            std = NotificationStore.getStandardNotificationCount();
            cst = NotificationStore.getCustomNotificationCount();
            rmv = NotificationStore.getRemovedNotificationCount();
        } catch (NullPointerException ex) {
            Logger.error(ex,"onStartJob NotificationStore NullPointerException: {}", ex.getMessage());
            NotificationStore notificationStore = new NotificationStore();
        }

        Logger.debug("onStartJob id: " + id + " \\ mode: " + mode + " \\ uuid: " + uuid);
        Logger.debug("onStartJob std#: " + std + " \\ cst#: " + cst +  " \\ rmv#: " + rmv);

        int delay = 300;

        if (uuid != null) {

            Logger.debug("onStartJob transporterNotifications.isAvailable: " + TransportService.isTransporterNotificationsAvailable());
            Logger.debug("onStartJob transporterHuami.isAvailable: " + TransportService.isTransporterHuamiAvailable());

            if (TransportService.isTransporterNotificationsAvailable() && TransportService.isTransporterHuamiAvailable())
                delay = 0;

            final Handler handler = new Handler();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    switch (mode) {

                        case NOTIFICATION_POSTED_STANDARD_UI:
                            jobParams.put(uuid, params);
                            processStandardNotificationPosted(uuid);
                            break;

                        case NOTIFICATION_POSTED_CUSTOM_UI:
                            jobParams.put(uuid, params);
                            processCustomNotificationPosted(uuid);
                            break;

                        case NOTIFICATION_REMOVED:
                            jobParams.put(uuid, params);
                            processNotificationRemoved(uuid);
                            break;

                        default:
                            Logger.error("onStartJob error: NOTIFICATION_MODE not found!");

                    }
                }
            }, delay + 1);

        } else
            Logger.error("onStartJob error: null uuid!");

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Logger.debug("onStopJob id: " + params.getJobId());

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.debug("onDestroy");
    }

    private void keepNotificationServiceRunning() {

        Logger.debug("keepNotificationServiceRunning");

        ComponentName component = new ComponentName(getApplicationContext(), NotificationService.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(component);
            Logger.debug("keepNotificationServiceRunning requestRebind");
        }

        jobFinished(params, false);

    }

    private void processStandardNotificationPosted(final String uuid) {

        Logger.debug("processStandardNotificationPosted uuid: " + uuid + " \\ try: " + retries);

        DataBundle dataBundle = NotificationStore.getStandardNotification(uuid);

        if (TransportService.isTransporterHuamiConnected()) {
            Logger.info("processStandardNotificationPosted transport already connected");
            AmazModApplication.setWatchConnected(true);
        } else {
            Logger.warn("processStandardNotificationPosted transport not connected, connecting...");
            TransportService.connectTransporterHuami();
            AmazModApplication.setWatchConnected(false);
        }

        Logger.info("processStandardNotificationPosted transporterHuami.isAvailable: " + TransportService.isTransporterHuamiAvailable());

        if (dataBundle != null) {

            TransportService.sendWithTransporterHuami("add", uuid, dataBundle, this);

            /*
            result = dataTransportResult == null ? "" : dataTransportResult.toString();

            if (result.toLowerCase().contains("ok")) {
                Logger.debug("processStandardNotificationPosted OK");
                NotificationStore.removeStandardNotification(uuid);
                if (pendingJobs.containsKey(uuid))
                    pendingJobs.remove(uuid);
                jobFinished(params, false);
            } else {
                Logger.debug("processStandardNotificationPosted try: " + retries);
                if (AmazModApplication.isWatchConnected() && retries < 4) {
                    retries++;
                    SystemClock.sleep(300);
                    processStandardNotificationPosted(uuid, mode);
                } else {
                    Logger.debug("processStandardNotificationPosted rescheduling…");
                    retries = 0;
                    pendingJobs.put(uuid, params);
                    jobFinished(params, true);
                }
            }
            */

        } else {
            if (pendingJobs.containsKey(uuid)) {
                pendingJobs.remove(uuid);
                jobParams.remove(uuid);
            }
            if (NotificationStore.standardNotifications.containsKey(uuid))
                NotificationStore.removeStandardNotification(uuid);
            jobFinished(params, false);
        }

    }

    public void processNotificationRemoved(final String uuid) {

        boolean isNotificationQueued = false;
        String key = NotificationStore.UUIDmap.get(uuid);
        Logger.debug("uuid: {} key: {}", uuid, key);

        if (key != null) {

            //Check for pending Standard notifications and remove them
            if (NotificationStore.getStandardNotificationCount() > 0)
                for (ArrayMap.Entry<String, String> pair : NotificationStore.UUIDmap.entrySet()) {
                    Logger.debug("NS.uuid: {} \\ NS.key: {}", pair.getKey(), pair.getValue());

                    if (key.equals(pair.getValue())) {
                        final String removeUUID = pair.getKey();
                        if (NotificationStore.standardNotifications.containsKey(removeUUID)) {
                            Logger.info("removing std: {}", removeUUID);
                            NotificationStore.removeStandardNotification(removeUUID);
                            if (pendingJobs.containsKey(removeUUID)) {
                                jobFinished(pendingJobs.get(removeUUID), false);
                                jobParams.remove(removeUUID);
                                pendingJobs.remove(removeUUID);
                            }
                            isNotificationQueued = true;
                        }
                    }
                }

            //Check for pensing CustomUI notifications and remove them
            if (NotificationStore.getCustomNotificationCount() > 0)
                for (ArrayMap.Entry<String, String> pair : NotificationStore.UUIDmap.entrySet()) {
                    Logger.debug("NS.uuid: {} \\ NS.key: {}", pair.getKey(), pair.getValue());

                    if (key.equals(pair.getValue())) {
                        final String removeUUID = pair.getKey();
                        if (NotificationStore.customNotifications.containsKey(removeUUID)) {
                            Logger.debug("removing cst: {}", removeUUID);
                            NotificationStore.removeCustomNotification(removeUUID);
                            if (pendingJobs.containsKey(removeUUID)) {
                                jobFinished(pendingJobs.get(removeUUID), false);
                                jobParams.remove(removeUUID);
                                pendingJobs.remove(removeUUID);
                            }
                            isNotificationQueued = true;
                        }
                    }
                }

            //If notification was queued, remove them and return, no need to push to watch
            if (isNotificationQueued) {
                Logger.info("pending uuid: {} removed", uuid);
                NotificationStore.removeRemovedNotification(uuid);
                jobFinished(params, false);
                return;
            }
        }

        Logger.debug("processNotificationRemoved uuid: " + uuid + " \\ try: " + retries);

        DataBundle dataBundle = NotificationStore.getRemovedNotification(uuid);

        if (TransportService.isTransporterHuamiConnected()) {
            Logger.info("processNotificationRemoved transport already connected");
            AmazModApplication.setWatchConnected(true);
        } else {
            Logger.warn("processNotificationRemoved transport not connected, connecting...");
            TransportService.connectTransporterHuami();
            AmazModApplication.setWatchConnected(false);
        }

        Logger.info("processNotificationRemoved transporterHuami.isAvailable: " + TransportService.isTransporterHuamiAvailable());

        if (dataBundle != null) {

            TransportService.sendWithTransporterHuami("del", uuid, dataBundle, this);

            /*
            result = dataTransportResult == null ? "" : dataTransportResult.toString();

            if (result.toLowerCase().contains("ok")) {
                Logger.debug("processNotificationRemoved OK");
                NotificationStore.removeRemovedNotification(uuid);
                if (pendingJobs.containsKey(uuid))
                    pendingJobs.remove(uuid);
                jobFinished(params, false);
            } else {
                Logger.debug("processNotificationRemoved try: " + retries);
                if (AmazModApplication.isWatchConnected() && retries < 4) {
                    retries++;
                    SystemClock.sleep(300);
                    processNotificationRemoved(uuid, mode);
                } else {
                    Logger.debug("processNotificationRemoved rescheduling…");
                    retries = 0;
                    pendingJobs.put(uuid, params);
                    jobFinished(params, true);
                }
            }
            */

        } else {
            if (pendingJobs.containsKey(uuid)) {
                pendingJobs.remove(uuid);
                jobParams.remove(uuid);
            }
            if (NotificationStore.removedNotifications.containsKey(uuid))
                NotificationStore.removeRemovedNotification(uuid);
            jobFinished(params, false);
        }

    }

    private void processCustomNotificationPosted(final String uuid) {

        Logger.debug("processCustomNotificationPosted uuid: " + uuid);

        NotificationData notificationData = NotificationStore.getCustomNotification(uuid);

        if (TransportService.isTransporterNotificationsConnected()) {
            Logger.info("processCustomNotificationPosted isTransportServiceConnected: true");
        } else {
            Logger.warn("processCustomNotificationPosted isTransportServiceConnected = false, connecting...");
            TransportService.connectTransporterNotifications();
            AmazModApplication.setWatchConnected(false);
        }

        boolean isTransportConnected = TransportService.isTransporterNotificationsConnected();

        if (!isTransportConnected) {
            if (AmazModApplication.isWatchConnected() || (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) == null)) {
                AmazModApplication.setWatchConnected(false);
                EventBus.getDefault().removeAllStickyEvents();
                EventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected()));
            }
            Logger.warn("processCustomNotificationPosted isTransportConnected: false");
        }

        Logger.info("processCustomNotificationPosted transporterNotifications.isAvailable: " + TransportService.isTransporterNotificationsAvailable());

        if (notificationData != null) {

            DataBundle dataBundle = new DataBundle();
            notificationData.toDataBundle(dataBundle);
            TransportService.sendWithTransporterNotifications(Transport.INCOMING_NOTIFICATION, uuid, dataBundle, this);

            /*
            result = dataTransportResult == null ? "" : dataTransportResult.toString();

            if (result.toLowerCase().contains("ok")) {
                Logger.debug("processCustomNotificationPosted OK");
                NotificationStore.removeCustomNotification(uuid);
                if (pendingJobs.containsKey(uuid))
                    pendingJobs.remove(uuid);
                if (mode == NOTIFICATION_POSTED_CUSTOM_UI)
                    NotificationStore.removeNotificationBundle(uuid);
                jobFinished(params, false);
            } else {
                Logger.debug("processCustomNotificationPosted try: " + retries);
                if (AmazModApplication.isWatchConnected() && retries < 4) {
                    retries++;
                    SystemClock.sleep(300);
                    processCustomNotificationPosted(uuid, mode);
                } else {
                    retries = 0;
                    pendingJobs.put(uuid, params);
                    Logger.debug("processCustomNotificationPosted rescheduling…");
                    jobFinished(params, true);
                }
            }
            */

        } else {
            if (pendingJobs.containsKey(uuid)) {
                pendingJobs.remove(uuid);
                jobParams.remove(uuid);
            }
            if (NotificationStore.customNotifications.containsKey(uuid)) {
                NotificationStore.removeCustomNotification(uuid);
            }
            jobFinished(params, false);
        }

        /*
        Watch.get().postNotification(notificationData).continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(@NonNull Task<Void> task) throws Exception {
                if (task.isSuccessful())
                    result = "ok";
                else
                    result = "failed";

                if (task.getException() != null) {
                    Log.e(Constants.TAG, "processCustomNotificationPosted throw: " + task.getException().toString());
                    if (AmazModApplication.isWatchConnected && retries < 4) {
                        Log.d(Constants.TAG, "processCustomNotificationPosted try: " + retries);
                        retries++;
                        processCustomNotificationPosted(uuid, mode);
                    } else {
                        retries = 0;
                        NotificationStore.removeCustomNotification(uuid);
                        NotificationStore.removeNotificationBundle(uuid);
                        jobFinished(params, false);
                    }
                    throw task.getException();
                    }

                Log.i(Constants.TAG, "processCustomNotificationPosted result: " + result);

                if (result.toLowerCase().contains("ok")) {
                    Log.d(Constants.TAG, "processCustomNotificationPosted OK");
                    NotificationStore.removeCustomNotification(uuid);
                    NotificationStore.removeNotificationBundle(uuid);
                    jobFinished(params, false);

                } else {
                    if (AmazModApplication.isWatchConnected && retries < 4) {
                        Log.d(Constants.TAG, "processCustomNotificationPosted try: " + retries);
                        retries++;
                        processCustomNotificationPosted(uuid, mode);
                    } else {
                        retries = 0;
                        NotificationStore.removeCustomNotification(uuid);
                        NotificationStore.removeNotificationBundle(uuid);
                        jobFinished(params, false);
                    }
                }
                return null;
            }
        });
        */
    }

    @Override
    public void onSuccess(DataTransportResult dataTransportResult, String uuid) {
        final String result = dataTransportResult.toString();
        final JobParameters params = jobParams.get(uuid);
        Logger.trace("current jobId: {}", this.params.getJobId());
        Logger.debug("uuid: {} result: {}", uuid, result);

        if (params != null) {
            final int mode = params.getExtras().getInt(NOTIFICATION_MODE, -1);
            Logger.debug("id: {} mode: {} uuid: {} result: {}", params.getJobId(), mode, uuid, result);

            if (result.toLowerCase().contains("ok")) {
                Logger.debug("OK removing: {}", uuid);
                retries = 0;
                removeNotification(mode, uuid);
                pendingJobs.remove(uuid);
                jobParams.remove(uuid);
                jobFinished(params, false);
            } else {
                Logger.debug("try: {}", retries);
                if (AmazModApplication.isWatchConnected() && retries < 4) {
                    retries++;
                    SystemClock.sleep(300);
                    processNotification(mode, uuid);
                } else {
                    Logger.debug("rescheduling {}…", params.getJobId());
                    retries = 0;
                    pendingJobs.put(uuid, params);
                    jobFinished(params, true);
                }
            }
        } else
            Logger.error("null JobParameters!");

    }

    private void removeNotification(int mode, String uuid) {
        Logger.debug("id: {} mode: {} uuid: {}", params.getJobId(), mode, uuid);

        switch (mode) {

            case NOTIFICATION_POSTED_STANDARD_UI:
                NotificationStore.removeStandardNotification(uuid);
                break;

            case NOTIFICATION_POSTED_CUSTOM_UI:
                NotificationStore.removeCustomNotification(uuid);
                break;

            case NOTIFICATION_REMOVED:
                NotificationStore.removeRemovedNotification(uuid);
                break;

            default:
                Logger.error("NOTIFICATION_MODE not found!");
        }
    }

    private void processNotification(int mode, String uuid) {
        Logger.debug("id: {} mode: {} uuid: {}", params.getJobId(), mode, uuid);

        switch (mode) {

            case NOTIFICATION_POSTED_STANDARD_UI:
                processStandardNotificationPosted(uuid);
                break;

            case NOTIFICATION_POSTED_CUSTOM_UI:
                processCustomNotificationPosted(uuid);
                break;

            case NOTIFICATION_REMOVED:
                processNotificationRemoved(uuid);
                break;

            default:
                Logger.error("NOTIFICATION_MODE not found!");
        }
    }

    @Override
    public void onFailure(String error, String uuid) {
        Logger.debug("uuid: {} error: {}", uuid, error);

    }
}