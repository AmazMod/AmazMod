package com.edotassi.amazmod.watch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.Brightness;
import com.edotassi.amazmod.event.OutcomingNotification;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.transport.TransportService;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.SettingsData;

public class Watch {

    private static Watch instance;

    private Context context;
    private TransportService transportService;

    public static void init(Context context) {
        instance = new Watch();
        instance.context = context;
    }

    public static Watch get() {
        if (instance == null) {
            throw new RuntimeException("Watch not initialized");
        }

        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public Task<WatchStatus> getStatus() {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<WatchStatus>>() {
            @Override
            public Task<WatchStatus> then(@NonNull Task<TransportService> task) throws Exception {
                return task.getResult().sendWithResult(Transport.REQUEST_WATCHSTATUS, Transport.WATCH_STATUS);
            }
        });
    }

    public Task<BatteryStatus> getBatteryStatus() {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<BatteryStatus>>() {
            @Override
            public Task<BatteryStatus> then(@NonNull Task<TransportService> task) throws Exception {
                return task.getResult().sendWithResult(Transport.REQUEST_BATTERYSTATUS, Transport.BATTERY_STATUS);
            }
        });
    }

    public Task<Void> incomingNotification(final OutcomingNotification outcomingNotification) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) throws Exception {
                task.getResult().send(Transport.INCOMING_NOTIFICATION, outcomingNotification.getNotificationData(), taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Void> syncSettings(final SettingsData settingsData) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) throws Exception {
                task.getResult().send(Transport.SYNC_SETTINGS, settingsData, taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Void> setBrightness(final Brightness brightness) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) throws Exception {
                task.getResult().send(Transport.BRIGHTNESS, brightness.getBrightnessData(), taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Void> lowPower() {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) throws Exception {
                task.getResult().send(Transport.LOW_POWER, null, taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    private Task<TransportService> getServiceInstance() {
        if (transportService != null) {
            return Tasks.forResult(transportService);
        }

        final TaskCompletionSource<TransportService> taskCompletionSource = new TaskCompletionSource<>();

        Intent intent = new Intent(context, TransportService.class);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                transportService = ((TransportService.LocalBinder) binder).getService();
                taskCompletionSource.setResult(transportService);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                transportService = null;
            }
        }, 0);

        return taskCompletionSource.getTask();
    }
}
