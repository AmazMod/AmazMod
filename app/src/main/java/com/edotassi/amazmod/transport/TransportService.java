package com.edotassi.amazmod.transport;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.event.NextMusic;
import com.edotassi.amazmod.event.NotificationReply;
import com.edotassi.amazmod.event.RequestFileUpload;
import com.edotassi.amazmod.event.ResultDeleteFile;
import com.edotassi.amazmod.event.ResultDownloadFileChunk;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.event.ResultWidgets;
import com.edotassi.amazmod.event.SilenceApplication;
import com.edotassi.amazmod.event.ToggleMusic;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.event.local.IsWatchConnectedLocal;
import com.edotassi.amazmod.notification.NotificationService;
import com.edotassi.amazmod.notification.PersistentNotification;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.Transportable;

import static android.service.notification.NotificationListenerService.requestRebind;

public class TransportService extends Service implements Transporter.DataListener {

    private Transporter transporter;
    private PersistentNotification persistentNotification;
    public static String model;

    private LocalBinder localBinder = new LocalBinder();
    private TransportListener transportListener;

    int numCores = Runtime.getRuntime().availableProcessors();
    ThreadPoolExecutor executor = new ThreadPoolExecutor(numCores * 2, numCores * 2,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private Map<String, Class> messages = new HashMap<String, Class>() {{
        put(Transport.WATCH_STATUS, WatchStatus.class);
        put(Transport.BATTERY_STATUS, BatteryStatus.class);
        put(Transport.REPLY, NotificationReply.class);
        put(Transport.NEXT_MUSIC, NextMusic.class);
        put(Transport.TOGGLE_MUSIC, ToggleMusic.class);
        put(Transport.DIRECTORY, Directory.class);
        put(Transport.RESULT_DELETE_FILE, ResultDeleteFile.class);
        put(Transport.RESULT_DOWNLOAD_FILE_CHUNK, ResultDownloadFileChunk.class);
        put(Transport.RESULT_SHELL_COMMAND, ResultShellCommand.class);
        put(Transport.FILE_UPLOAD, RequestFileUpload.class);
        put(Transport.SILENCE,SilenceApplication.class);
        put(Transport.WIDGETS_DATA, ResultWidgets.class);
    }};

    private Map<String, Object> pendingResults = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.debug("TransportService onCreate");


        //Make sure services are running and persistent
        startPersistentNotification();
        tryReconnectNotificationService();

        transportListener = new TransportListener(this);
        EventBus.getDefault().register(transportListener);

        transporter = TransporterClassic.get(this, Transport.NAME);
        transporter.addDataListener(this);

        if (transporter.isTransportServiceConnected()) {
            Logger.info("TransportService onCreate already connected");
        } else {
            Logger.warn("TransportService onCreate not connected, connecting...");
            transporter.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Logger.debug("TransportService onStartCommand");

        startPersistentNotification();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        EventBus.getDefault().unregister(transportListener);
        transporter.removeDataListener(this);
        transporter.disconnectTransportService();
        Logger.debug("TransportService onDestroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public void onDataReceived(TransportDataItem transportDataItem) {
        String action = transportDataItem.getAction();
        Object event = dataToClass(transportDataItem);

        Logger.debug("TransportService action: " + action);

        if (action != null) {
            TaskCompletionSource<Object> taskCompletionSourcePendingResult = (TaskCompletionSource<Object>) pendingResults.get(action);
            if (taskCompletionSourcePendingResult != null) {
                taskCompletionSourcePendingResult.setResult(event);
                pendingResults.remove(action);
            } else {
                EventBus.getDefault().post(event);
            }
        } else {
            //TODO handle null action
            Log.e(Constants.TAG, "TransportService onDataReceived null action!");
        }
    }

    private void startPersistentNotification() {

        // Add persistent notification if it is enabled in Settings or running on Oreo+
        boolean enableNotification = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION, true);

        model = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Constants.PREF_WATCH_MODEL, "");

        if (enableNotification || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            persistentNotification = new PersistentNotification(this, model);
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

    public <T extends Object> Task<T> sendWithResult(String action, String actionResult) {
        return sendWithResult(action, actionResult, null);
    }

    public <T> Task<T> sendWithResult(final String action, final String actionResult, final Transportable transportable) {
        final TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>();

        Tasks.call(executor, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                pendingResults.put(actionResult, taskCompletionSource);

                send(action, transportable, null);

                try {
                    Tasks.await(taskCompletionSource.getTask(), 15000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException timeoutException) {
                    taskCompletionSource.setException(timeoutException);
                }

                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Void> sendAndWait(String action, Transportable transportable) {
        TaskCompletionSource<Void> waiter = new TaskCompletionSource<>();

        send(action, transportable, waiter);

        return waiter.getTask();
    }

    public void send(String action) {
        send(action, null, null);
    }

    public void send(final String action, Transportable transportable, final TaskCompletionSource<Void> waiter) {
        boolean isTransportConnected = transporter.isTransportServiceConnected();
        if (!isTransportConnected) {
            if (AmazModApplication.isWatchConnected() != isTransportConnected || (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) == null)) {
                AmazModApplication.setWatchConnected(isTransportConnected);
                EventBus.getDefault().removeAllStickyEvents();
                EventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected()));
                persistentNotification.updatePersistentNotification(AmazModApplication.isWatchConnected());
            }
            Logger.warn("TransportService send Transport Service Not Connected");
            return;
        }

        DataBundle dataBundle = new DataBundle();
        if (transportable != null) {
            transportable.toDataBundle(dataBundle);
        }

        Logger.debug("TransportService send1: " + action);
        transporter.send(action, dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                Logger.info("Send result: " + dataTransportResult.toString());

                switch (dataTransportResult.getResultCode()) {
                    case (DataTransportResult.RESULT_FAILED_TRANSPORT_SERVICE_UNCONNECTED):
                    case (DataTransportResult.RESULT_FAILED_CHANNEL_UNAVAILABLE):
                    case (DataTransportResult.RESULT_FAILED_IWDS_CRASH):
                    case (DataTransportResult.RESULT_FAILED_LINK_DISCONNECTED): {
                        TaskCompletionSource<Object> taskCompletionSourcePendingResult = (TaskCompletionSource<Object>) pendingResults.get(action);
                        if (taskCompletionSourcePendingResult != null) {
                            taskCompletionSourcePendingResult.setException(new RuntimeException("TransporterError: " + dataTransportResult.toString()));
                            pendingResults.remove(action);
                        }

                        if (waiter != null) {
                            waiter.setException(new RuntimeException("TransporterError: " + dataTransportResult.toString()));
                        }
                        break;
                    }
                    case (DataTransportResult.RESULT_OK): {
                        if (waiter != null) {
                            waiter.setResult(null);
                        }

                        if (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) == null) {
                            AmazModApplication.setWatchConnected(true);
                            EventBus.getDefault().removeAllStickyEvents();
                            EventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected()));
                            persistentNotification.updatePersistentNotification(AmazModApplication.isWatchConnected());
                            Logger.debug("TransportService send1 isConnected: " + AmazModApplication.isWatchConnected());
                        }
                        break;
                    }
                }
            }
        });
    }

    private Object dataToClass(TransportDataItem transportDataItem) {
        String action = transportDataItem.getAction();

        Logger.debug("TransportService action: " + action);

        Class messageClass = messages.get(action);

        if (messageClass != null) {
            Class[] args = new Class[1];
            args[0] = DataBundle.class;

            try {
                Constructor eventContructor = messageClass.getDeclaredConstructor(args);
                Object event = eventContructor.newInstance(transportDataItem.getData());

                Logger.debug("Transport onDataReceived: " + event.toString());

                return event;
            } catch (NoSuchMethodException e) {
                Logger.debug("Transport event mapped with action \"" + action + "\" doesn't have constructor with DataBundle as parameter");
                e.printStackTrace();
                return null;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return null;
            } catch (InstantiationException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public void tryReconnectNotificationService() {

        Log.d(Constants.TAG, "TransportService tryReconnectNotificationService");

        toggleNotificationService();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ComponentName componentName = new ComponentName(getApplicationContext(), NotificationService.class);
            requestRebind(componentName);
            Log.d(Constants.TAG, "TransportService tryReconnectNotificationService requestRebind");
        }
    }

    private void toggleNotificationService() {

        Log.i(Constants.TAG, "TransportService toggleNotificationService");

        ComponentName component = new ComponentName(this, NotificationService.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    }

    public class LocalBinder extends Binder {
        public TransportService getService() {
            return TransportService.this;
        }
    }
}
