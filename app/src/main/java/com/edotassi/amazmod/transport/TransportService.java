package com.edotassi.amazmod.transport;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.event.NextMusic;
import com.edotassi.amazmod.event.NotificationReply;
import com.edotassi.amazmod.event.ToggleMusic;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.event.local.IsWatchConnectedLocal;
import com.edotassi.amazmod.notification.PersistentNotification;
import com.edotassi.amazmod.support.Logger;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import amazmod.com.transport.Transport;
import amazmod.com.transport.Transportable;

public class TransportService extends Service implements Transporter.DataListener {

    private Logger logger = Logger.get(TransportService.class);
    private Transporter transporter;
    private PersistentNotification persistentNotification;
    public static String model;

    private LocalBinder localBinder = new LocalBinder();
    private TransportListener transportListener;

    private Map<String, Class> messages = new HashMap<String, Class>() {{
        put(Transport.WATCH_STATUS, WatchStatus.class);
        put(Transport.BATTERY_STATUS, BatteryStatus.class);
        put(Transport.REPLY, NotificationReply.class);
        put(Transport.NEXT_MUSIC, NextMusic.class);
        put(Transport.TOGGLE_MUSIC, ToggleMusic.class);
        put(Transport.DIRECTORY, Directory.class);
    }};

    private Map<String, Object> pendingResults = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        transportListener = new TransportListener(this);
        EventBus.getDefault().register(transportListener);

        transporter = TransporterClassic.get(this, Transport.NAME);
        transporter.addDataListener(this);

        if (transporter.isTransportServiceConnected()) {
            this.logger.i("TransportService onCreate already connected");
        } else {
            this.logger.w("TransportService onCreate not connected, connecting...");
            transporter.connectTransportService();
            AmazModApplication.isWatchConnected = false;
        }

        // Add persistent notification if it is enabled in Settings or running on Oreo+
        boolean enableNotification = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(Constants.PREF_ENABLE_PERSISTENT_NOTIFICATION, true);
        model = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Constants.PREF_WATCH_MODEL, "");
        if (enableNotification || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            persistentNotification = new PersistentNotification(this, model);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(persistentNotification.getNotificationId(), persistentNotification.createPersistentNotification());
            } else {
                persistentNotification.createPersistentNotification();
            }
        }

    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        EventBus.getDefault().unregister(this);
        transporter.removeDataListener(this);
        transporter.disconnectTransportService();
        this.logger.d("TransportService onDestroy");
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

        this.logger.d("TransportService action: " + action);

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
        }
    }

    public <T extends Object> Task<T> sendWithResult(String action, String actionResult) {
        return sendWithResult(action, actionResult, null);
    }

    public <T> Task<T> sendWithResult(final String action, String actionResult, Transportable transportable) {
        final TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>();
        pendingResults.put(actionResult, taskCompletionSource);

        TaskCompletionSource<Void> waiter = new TaskCompletionSource<>();
        send(action, transportable, waiter);

        waiter.getTask().continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    pendingResults.remove(action);
                    taskCompletionSource.setException(task.getException());
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

    public void send(String action, Transportable transportable, final TaskCompletionSource<Void> waiter) {
        boolean isTransportConnected = transporter.isTransportServiceConnected();
        if (!isTransportConnected) {
            if (AmazModApplication.isWatchConnected != isTransportConnected || (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) == null)) {
                AmazModApplication.isWatchConnected = isTransportConnected;
                EventBus.getDefault().removeAllStickyEvents();
                EventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected));
                persistentNotification.updatePersistentNotification(AmazModApplication.isWatchConnected);
            }
            this.logger.w("TransportService send Transport Service Not Connected");
            return;
        }

        DataBundle dataBundle = new DataBundle();
        if (transportable != null) {
            transportable.toDataBundle(dataBundle);
        }

        this.logger.d("TransportService send1: " + action);
        transporter.send(action, dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                TransportService.this.logger.i("Send result: " + dataTransportResult.toString());

                switch (dataTransportResult.getResultCode()) {
                    case (DataTransportResult.RESULT_FAILED_TRANSPORT_SERVICE_UNCONNECTED):
                    case (DataTransportResult.RESULT_FAILED_CHANNEL_UNAVAILABLE):
                    case (DataTransportResult.RESULT_FAILED_IWDS_CRASH):
                    case (DataTransportResult.RESULT_FAILED_LINK_DISCONNECTED): {
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
                            AmazModApplication.isWatchConnected = true;
                            EventBus.getDefault().removeAllStickyEvents();
                            EventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected));
                            persistentNotification.updatePersistentNotification(AmazModApplication.isWatchConnected);
                            TransportService.this.logger.d("TransportService send1 isConnected: " + AmazModApplication.isWatchConnected);
                        }
                        break;
                    }
                }
            }
        });
    }

    private Object dataToClass(TransportDataItem transportDataItem) {
        String action = transportDataItem.getAction();

        this.logger.d("TransportService action: " + action);

        Class messageClass = messages.get(action);

        if (messageClass != null) {
            Class[] args = new Class[1];
            args[0] = DataBundle.class;

            try {
                Constructor eventContructor = messageClass.getDeclaredConstructor(args);
                Object event = eventContructor.newInstance(transportDataItem.getData());

                this.logger.d("Transport onDataReceived: " + event.toString());

                return event;
            } catch (NoSuchMethodException e) {
                this.logger.d("Transport event mapped with action \"" + action + "\" doesn't have constructor with DataBundle as parameter");
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

    public class LocalBinder extends Binder {
        public TransportService getService() {
            return TransportService.this;
        }
    }
}
