package com.edotassi.amazmod.notification;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.event.local.IsWatchConnectedLocal;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.pixplicity.easyprefs.library.Prefs;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.data.NotificationData;

import static android.service.notification.NotificationListenerService.requestRebind;

public class NotificationJobService extends JobService {

    JobParameters params;

    private static Transporter transporterNotifications, transporterHuami;

    public static final String NOTIFICATION_KEY = "notification_key";
    public static final String NOTIFICATION_MODE = "notification_mode";
    public static final int NOTIFICATION_POSTED_STANDARD_UI = 1000;
    public static final int NOTIFICATION_POSTED_CUSTOM_UI = 2000;
    public static final int NOTIFICATION_REMOVED = 3000;

    private static String result;
    private static int retries = 0;
    private static Map<String, JobParameters> pendingJobs;

    @Override
    public boolean onStartJob(JobParameters params) {

        this.params = params;

        pendingJobs = new HashMap<>();

        final int id = params.getJobId();
        final String key = params.getExtras().getString(NOTIFICATION_KEY, null);
        final int mode = params.getExtras().getInt(NOTIFICATION_MODE, -1);

        if (id == 0)
            keepNotificationServiceRunning();

        int std = 0, cst = 0, bs = 0;
        try {
            std = NotificationStore.getStandardNotificationCount();
            cst = NotificationStore.getCustomNotificationCount();
            bs = NotificationStore.getNotificationBundleCount();
        } catch (NullPointerException ex) {
            Logger.error(ex,"onStartJob NotificationStore NullPointerException: " + ex.getMessage());
            NotificationStore notificationStore = new NotificationStore();
        }

        transporterNotifications = TransporterClassic.get(this, Transport.NAME_NOTIFICATION);

        if (!transporterNotifications.isTransportServiceConnected()) {
            Logger.warn("onStartJob transporterNotifications not connected, connecting...");
            transporterNotifications.connectTransportService();
        } else {
            Logger.debug("onStartJob transporterNotifications already connected");
        }

        transporterHuami = TransporterClassic.get(this, "com.huami.action.notification");
        if (!transporterHuami.isTransportServiceConnected()) {
            Logger.warn("onStartJob transporterHuami not connected, connecting...");
            transporterHuami.connectTransportService();
        } else {
            Logger.debug("onStartJob transportedHuami already connected");
        }

        Logger.debug("onStartJob id: " + id + " \\ mode: " + mode + " \\ key: " + key);
        Logger.debug("onStartJob std#: " + std + " \\ cst#: " + cst +  " \\ bs#: " + bs);

        int delay = 300;

        if (key != null) {

            Logger.debug("onStartJob transporterNotifications.isAvailable: " + transporterNotifications.isAvailable());
            Logger.debug("onStartJob transporterHuami.isAvailable: " + transporterHuami.isAvailable());

            if (transporterNotifications.isAvailable() && transporterHuami.isAvailable())
                delay = 0;

            final Handler handler = new Handler();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    switch (mode) {

                        case NOTIFICATION_POSTED_STANDARD_UI:
                            processStandardNotificationPosted(key, mode);
                            break;

                        case NOTIFICATION_POSTED_CUSTOM_UI:
                            processCustomNotificationPosted(key, mode);
                            break;

                        case NOTIFICATION_REMOVED:
                            processNotificationRemoved(key, mode);
                            break;

                        default:
                            Logger.error("onStartJob error: NOTIFICATION_MODE not found!");

                    }
                }
            }, delay + 1);

        } else
            Logger.error("onStartJob error: null key!");

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
        //disconnectTransports();
        Logger.debug("onDestroy");
    }

    private void disconnectTransports() {

        if (transporterNotifications.isTransportServiceConnected()) {
            Logger.info("disconnectTransports disconnecting transporterNotifications…");
            transporterNotifications.disconnectTransportService();
            transporterNotifications = null;
        }

        if (transporterHuami.isTransportServiceConnected()) {
            Logger.info("disconnectTransports disconnecting transporterHuami…");
            transporterHuami.disconnectTransportService();
            transporterHuami = null;
        }
    }

    private void keepNotificationServiceRunning() {

        Logger.debug("keepNotificationServiceRunning");

        ComponentName component = new ComponentName(this, NotificationService.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ComponentName componentName = new ComponentName(getApplicationContext(), NotificationService.class);
            requestRebind(componentName);
            Logger.debug("keepNotificationServiceRunning requestRebind");
        }

        jobFinished(params, false);

    }

    private void processStandardNotificationPosted(final String key, final int mode) {

        Logger.debug("processStandardNotificationPosted key: " + key + " \\ try: " + retries);

        DataBundle dataBundle = NotificationStore.getStandardNotification(key);

        if (transporterHuami.isTransportServiceConnected()) {
            Logger.info("processStandardNotificationPosted transport already connected");
            AmazModApplication.setWatchConnected(true);
        } else {
            Logger.warn("processStandardNotificationPosted transport not connected, connecting...");
            transporterHuami.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }

        Logger.info("processStandardNotificationPosted transporterHuami.isAvailable: " + transporterHuami.isAvailable());

        if (dataBundle != null) {

            transporterHuami.send("add", dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    result = dataTransportResult.toString();
                    Logger.debug("processStandardNotificationPosted result: " + result);
                    //transporterHuami.disconnectTransportService();

                    if (result.toLowerCase().contains("ok")) {
                        Logger.debug("processStandardNotificationPosted OK");
                        NotificationStore.removeStandardNotification(key);
                        if (pendingJobs.containsKey(key))
                            pendingJobs.remove(key);
                        jobFinished(params, false);
                    } else {
                        Logger.debug("processStandardNotificationPosted try: " + retries);
                        if (AmazModApplication.isWatchConnected() && retries < 4) {
                            retries++;
                            SystemClock.sleep(300);
                            processStandardNotificationPosted(key, mode);
                        } else {
                            Logger.debug("processStandardNotificationPosted rescheduling…");
                            retries = 0;
                            pendingJobs.put(key, params);
                            jobFinished(params, true);
                        }
                    }
                }
            });

        } else {
            jobFinished(params, false);
            if (pendingJobs.containsKey(key))
                pendingJobs.remove(key);
            if (NotificationStore.standardNotifications.containsKey(key))
                NotificationStore.removeStandardNotification(key);
        }

    }

    public void processNotificationRemoved(final String key, final int mode) {

        boolean isNotificationQueued = false;

        if (NotificationStore.standardNotifications.containsKey(key)) {
            NotificationStore.removeStandardNotification(key);
            if (pendingJobs.containsKey(key))
                jobFinished(pendingJobs.get(key), false);
            isNotificationQueued = true;
        }

        if (NotificationStore.customNotifications.containsKey(key)) {
            NotificationStore.removeCustomNotification(key);
            NotificationStore.removeNotificationBundle(key);
            if (pendingJobs.containsKey(key))
                jobFinished(pendingJobs.get(key), false);
            isNotificationQueued = true;
        }

        if (isNotificationQueued) {
            Logger.debug("processNotificationRemoved notificationQueued key: " + key);
            NotificationStore.removeRemovedNotification(key);
            jobFinished(params, false);
            return;
        }

        Logger.debug("processNotificationRemoved key: " + key + " \\ try: " + retries);
        result = "";

        DataBundle dataBundle = NotificationStore.getRemovedNotification(key);

        if (transporterHuami.isTransportServiceConnected()) {
            Logger.info("processNotificationRemoved transport already connected");
            AmazModApplication.setWatchConnected(true);
        } else {
            Logger.warn("processNotificationRemoved transport not connected, connecting...");
            transporterHuami.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }

        Logger.info("processNotificationRemoved transporterHuami.isAvailable: " + transporterHuami.isAvailable());

        if (dataBundle != null) {

            transporterHuami.send("del", dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    result = dataTransportResult.toString();
                    Logger.debug("processNotificationRemoved result: " + result);
                    //transporterHuami.disconnectTransportService();

                    if (result.toLowerCase().contains("ok")) {
                        Logger.debug("processNotificationRemoved OK");
                        NotificationStore.removeRemovedNotification(key);
                        if (pendingJobs.containsKey(key))
                            pendingJobs.remove(key);
                        jobFinished(params, false);
                    } else {
                        Logger.debug("processNotificationRemoved try: " + retries);
                        if (AmazModApplication.isWatchConnected() && retries < 4) {
                            retries++;
                            SystemClock.sleep(300);
                            processNotificationRemoved(key, mode);
                        } else {
                            Logger.debug("processNotificationRemoved rescheduling…");
                            retries = 0;
                            pendingJobs.put(key, params);
                            jobFinished(params, true);
                        }
                    }
                }
            });

        } else {
            jobFinished(params, false);
            if (pendingJobs.containsKey(key))
                pendingJobs.remove(key);
            if (NotificationStore.removedNotifications.containsKey(key))
                NotificationStore.removeRemovedNotification(key);
        }

    }

    private void processCustomNotificationPosted(final String key, final int mode) {

        Logger.debug("processCustomNotificationPosted key: " + key);

        NotificationData notificationData = NotificationStore.getCustomNotification(key);

        if (mode == NOTIFICATION_POSTED_CUSTOM_UI) {
            Bundle bundle = NotificationStore.getNotificationBundle(key);
            if (bundle != null)
                NotificationJobService.extractImagesFromNotification(bundle, notificationData);
        }

        //final Transporter transporter2 = TransporterClassic.get(this, Transport.NAME_NOTIFICATION);

        if (transporterNotifications.isTransportServiceConnected()) {
            Logger.info("processCustomNotificationPosted isTransportServiceConnected: true");
        } else {
            Logger.warn("processCustomNotificationPosted isTransportServiceConnected = false, connecting...");
            transporterNotifications.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }

        boolean isTransportConnected = transporterNotifications.isTransportServiceConnected();
        result = null;
        if (!isTransportConnected) {
            if (AmazModApplication.isWatchConnected() || (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) == null)) {
                AmazModApplication.setWatchConnected(false);
                EventBus.getDefault().removeAllStickyEvents();
                EventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected()));
            }
            Logger.warn("processCustomNotificationPosted isTransportConnected: false");
        }

        Logger.info("processCustomNotificationPosted transporterNotifications.isAvailable: " + transporterNotifications.isAvailable());

        if (notificationData != null) {
            DataBundle dataBundle = new DataBundle();
            notificationData.toDataBundle(dataBundle);
            transporterNotifications.send(Transport.INCOMING_NOTIFICATION, dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    result = dataTransportResult.toString();
                    Logger.info("processCustomNotificationPosted send result: " + result);
                    //transporterNotifications.disconnectTransportService();

                    if (result.toLowerCase().contains("ok")) {
                        Logger.debug("processCustomNotificationPosted OK");
                        NotificationStore.removeCustomNotification(key);
                        if (pendingJobs.containsKey(key))
                            pendingJobs.remove(key);
                        if (mode == NOTIFICATION_POSTED_CUSTOM_UI)
                            NotificationStore.removeNotificationBundle(key);
                        jobFinished(params, false);
                    } else {
                        Logger.debug("processCustomNotificationPosted try: " + retries);
                        if (AmazModApplication.isWatchConnected() && retries < 4) {
                            retries++;
                            SystemClock.sleep(300);
                            processCustomNotificationPosted(key, mode);
                        } else {
                            retries = 0;
                            pendingJobs.put(key, params);
                            Logger.debug("processCustomNotificationPosted rescheduling…");
                            jobFinished(params, true);
                        }
                    }
                }
            });
        } else {
            jobFinished(params, false);
            if (pendingJobs.containsKey(key))
                pendingJobs.remove(key);
            if (NotificationStore.customNotifications.containsKey(key))
                NotificationStore.removeCustomNotification(key);
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
                        processCustomNotificationPosted(key, mode);
                    } else {
                        retries = 0;
                        NotificationStore.removeCustomNotification(key);
                        NotificationStore.removeNotificationBundle(key);
                        jobFinished(params, false);
                    }
                    throw task.getException();
                    }

                Log.i(Constants.TAG, "processCustomNotificationPosted result: " + result);

                if (result.toLowerCase().contains("ok")) {
                    Log.d(Constants.TAG, "processCustomNotificationPosted OK");
                    NotificationStore.removeCustomNotification(key);
                    NotificationStore.removeNotificationBundle(key);
                    jobFinished(params, false);

                } else {
                    if (AmazModApplication.isWatchConnected && retries < 4) {
                        Log.d(Constants.TAG, "processCustomNotificationPosted try: " + retries);
                        retries++;
                        processCustomNotificationPosted(key, mode);
                    } else {
                        retries = 0;
                        NotificationStore.removeCustomNotification(key);
                        NotificationStore.removeNotificationBundle(key);
                        jobFinished(params, false);
                    }
                }
                return null;
            }
        });
        */
    }

    public static void extractImagesFromNotification(Bundle bundle, NotificationData notificationData) {

        if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_LARGE_ICON, Constants.PREF_NOTIFICATIONS_LARGE_ICON_DEFAULT)) {
            extractLargeIcon(bundle, notificationData);
        }

        if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_IMAGES, Constants.PREF_NOTIFICATIONS_IMAGES_DEFAULT)) {
            extractPicture(bundle, notificationData);
        }
    }

    private static void extractLargeIcon(Bundle bundle, NotificationData notificationData) {
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
            Logger.error(exception,exception.getMessage());
        }
    }

    private static void extractPicture(Bundle bundle, NotificationData notificationData) {
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
            Logger.error(exception,exception.getMessage());
        }
    }

    private static Bitmap scaleBitmap(Bitmap bitmap) {
        if (bitmap.getWidth() <= 320) {
            return bitmap;
        }

        float horizontalScaleFactor = bitmap.getWidth() / 320f;
        float destHeight = bitmap.getHeight() / horizontalScaleFactor;

        return Bitmap.createScaledBitmap(bitmap, 320, (int) destHeight, false);
    }


    //Send CustomUI without scheduling
    public static void sendCustomNotification(final Context context, final NotificationData notificationData) {

        Logger.debug("sendCustomNotification key: " + notificationData.getKey());

        //final Transporter transporter2 = TransporterClassic.get(context, Transport.NAME_NOTIFICATION);

        if (transporterNotifications.isTransportServiceConnected()) {
            Logger.info("sendCustomNotification isTransportServiceConnected: true");
        } else {
            Logger.warn("sendCustomNotification isTransportServiceConnected = false, connecting...");
            transporterNotifications.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }

        boolean isTransportConnected = transporterNotifications.isTransportServiceConnected();
        result = null;
        if (!isTransportConnected) {
            if (AmazModApplication.isWatchConnected() || (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) == null)) {
                AmazModApplication.setWatchConnected(false);
                EventBus.getDefault().removeAllStickyEvents();
                EventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected()));
            }
            Logger.warn("sendCustomNotification isTransportConnected: false");
        }

        Logger.info("sendCustomNotification transporterNotifications.isAvailable: " + transporterNotifications.isAvailable());

        DataBundle dataBundle = new DataBundle();
        notificationData.toDataBundle(dataBundle);
        transporterNotifications.send(Transport.INCOMING_NOTIFICATION, dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                result = dataTransportResult.toString();
                Logger.info("sendCustomNotification send result: " + result);
                transporterNotifications.disconnectTransportService();

                if (result.toLowerCase().contains("ok")) {
                    Logger.debug("sendCustomNotification OK");

                } else {
                    Logger.debug("sendCustomNotification try: " + retries);
                    if (AmazModApplication.isWatchConnected() && retries < 4) {
                        retries++;
                        sendCustomNotification(context, notificationData);
                    } else {
                        Logger.debug("sendCustomNotification finishing…");
                        retries = 0;
                    }
                }
            }
        });
    }

}