package com.edotassi.amazmod.notification;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.pixplicity.easyprefs.library.Prefs;

import java.io.ByteArrayOutputStream;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.NotificationData;

public class NotificationJobService extends JobService {

    JobParameters params;

    public static final String NOTIFICATION_KEY = "notification_key";
    public static final String NOTIFICATION_MODE = "notification_mode";
    public static final int NOTIFICATION_POSTED_STANDARD_UI = 0;
    public static final int NOTIFICATION_POSTED_CUSTOM_UI = 1;
    public static final int NOTIFICATION_POSTED_VOICE = 3;
    public static final int NOTIFICATION_REMOVED = 2;

    private static String result;
    private static int retries;

    private static Transporter transporter;


    @Override
    public boolean onStartJob(JobParameters params) {

        this.params = params;

        //Initiate transporter
        transporter = TransporterClassic.get(this, "com.huami.action.notification");

        Log.d(Constants.TAG, "NotificationJobService onStartJob isTransportServiceConnected: " + transporter.isTransportServiceConnected());
        //Force transporter leak to re-create it later
        transporter.connectTransportService();

        int id = params.getJobId();
        String key = params.getExtras().getString(NOTIFICATION_KEY, null);
        int mode = params.getExtras().getInt(NOTIFICATION_MODE, -1);
        retries = 0;

        int std = NotificationStore.getStandardNotificationCount();
        int cst = NotificationStore.getCustomNotificationCount();
        int bs = NotificationStore.getNotificationBundleCount();

        Log.d(Constants.TAG, "NotificationJobService onStartJob id: " + id + " \\ mode: " + mode + " \\ key: " + key);
        Log.d(Constants.TAG, "NotificationJobService onStartJob std#: " + std + " \\ cst#: " + cst +  " \\ bs#: " + bs);

        if (key != null) {

            switch (mode) {

                case NOTIFICATION_POSTED_STANDARD_UI:
                    processStandardNotificationPosted(key, mode);
                    break;

                case NOTIFICATION_POSTED_CUSTOM_UI:
                    processCustomNotificationPosted(key, mode);
                    break;

                case NOTIFICATION_POSTED_VOICE:
                    processCustomNotificationPosted(key, mode);
                    break;

                case NOTIFICATION_REMOVED:
                    processNotificationRemoved(key, mode);
                    break;

                default:
                    Log.e(Constants.TAG, "NotificationJobService onStartJob error: no NOTIFICATION_MODE found!");

            }
        } else
            Log.e(Constants.TAG, "NotificationJobService onStartJob error: null key!");

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(Constants.TAG, "NotificationJobService onStopJob id: " + params.getJobId());

        //Disconnect transport
        if (transporter.isTransportServiceConnected()) {
            Log.i(Constants.TAG, "NotificationJobService onStartJob disconnecting transport…");
            transporter.disconnectTransportService();
        }

        return true;
    }

    @Override
    public void onDestroy() {
        //Disconnect transport
        if (transporter.isTransportServiceConnected()) {
            Log.i(Constants.TAG, "NotificationJobService onDestroy disconnecting transport…");
            transporter.disconnectTransportService();
        }
        super.onDestroy();
    }

    private void processStandardNotificationPosted(final String key, final int mode) {

        Log.d(Constants.TAG, "NotificationJobService processStandardNotificationPosted key: " + key + " \\ try: " + retries);

        DataBundle dataBundle = NotificationStore.getStandardNotification(key);

        if (transporter.isTransportServiceConnected()) {
            Log.i(Constants.TAG, "NotificationJobService processStandardNotificationPosted transport already connected");
            AmazModApplication.isWatchConnected = true;
        } else {
            Log.w(Constants.TAG, "NotificationJobService processStandardNotificationPosted transport not connected, connecting...");
            transporter.connectTransportService();
            AmazModApplication.isWatchConnected = false;
        }

        transporter.send("add", dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                result = dataTransportResult.toString();
                Log.d(Constants.TAG, "NotificationJobService processStandardNotificationPosted result: " +result);
                transporter.disconnectTransportService();

                if (result.toLowerCase().contains("ok")) {
                    Log.d(Constants.TAG, "NotificationJobService processStandardNotificationPosted OK");
                    NotificationStore.removeStandardNotification(key);
                    jobFinished(params, false);
                } else {
                    Log.d(Constants.TAG, "NotificationJobService processStandardNotificationPosted try: " + retries);
                    if (AmazModApplication.isWatchConnected && retries < 4) {
                        retries++;
                        processStandardNotificationPosted(key, mode);
                    } else {
                        Log.d(Constants.TAG, "NotificationJobService processStandardNotificationPosted rescheduling…");
                        retries = 0;
                        jobFinished(params, true);
                    }
                }
            }
        });

    }


    public void processNotificationRemoved(final String key, final int mode) {

        Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved key: " + key + " \\ try: " + retries);
        result = "";

        DataBundle dataBundle = NotificationStore.getStandardNotification(key);

        if (transporter.isTransportServiceConnected()) {
            Log.i(Constants.TAG, "NotificationJobService processNotificationRemoved transport already connected");
            AmazModApplication.isWatchConnected = true;
        } else {
            Log.w(Constants.TAG, "NotificationJobService processNotificationRemoved transport not connected, connecting...");
            transporter.connectTransportService();
            AmazModApplication.isWatchConnected = false;
        }

        transporter.send("del", dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                result = dataTransportResult.toString();
                Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved result: " + result);
                transporter.disconnectTransportService();

                if (result.toLowerCase().contains("ok")) {
                    Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved OK");
                    NotificationStore.removeStandardNotification(key);
                    jobFinished(params, false);
                } else {
                    Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved try: " + retries);
                    if (AmazModApplication.isWatchConnected && retries < 4) {
                        retries++;
                        processNotificationRemoved(key, mode);
                    } else {
                        Log.d(Constants.TAG, "NotificationJobService processNotificationRemoved rescheduling…");
                        retries = 0;
                        jobFinished(params, true);
                    }
                }
            }
        });

    }

    private void processCustomNotificationPosted(final String key, final int mode) {

        Log.d(Constants.TAG, "NotificationJobService processCustomNotificationPosted key: " + key);

        NotificationData notificationData = NotificationStore.getCustomNotification(key);

        if (mode == NOTIFICATION_POSTED_CUSTOM_UI) {
            Bundle bundle = NotificationStore.getNotificationBundle(key);
            extractImagesFromNotification(bundle, notificationData);
        }

        Watch.get().postNotification(notificationData).continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(@NonNull Task<Void> task) throws Exception {
                if (task.isSuccessful())
                    result = "ok";
                else
                    result = "failed";

                if (task.getException() != null) {
                    Log.e(Constants.TAG, "NotificationJobService processCustomNotificationPosted throw: " + task.getException().toString());
                    if (AmazModApplication.isWatchConnected && retries < 4) {
                        Log.d(Constants.TAG, "NotificationJobService processCustomNotificationPosted try: " + retries);
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

                Log.i(Constants.TAG, "NotificationJobService processCustomNotificationPosted result: " + result);

                if (result.toLowerCase().contains("ok")) {
                    Log.d(Constants.TAG, "NotificationJobService processCustomNotificationPosted OK");
                    NotificationStore.removeCustomNotification(key);
                    NotificationStore.removeNotificationBundle(key);
                    jobFinished(params, false);

                } else {
                    if (AmazModApplication.isWatchConnected && retries < 4) {
                        Log.d(Constants.TAG, "NotificationJobService processCustomNotificationPosted try: " + retries);
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
    }

    private void extractImagesFromNotification(Bundle bundle, NotificationData notificationData) {

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

}