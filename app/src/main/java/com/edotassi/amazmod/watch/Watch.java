package com.edotassi.amazmod.watch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.event.ResultDeleteFile;
import com.edotassi.amazmod.event.ResultDownloadFileChunk;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.event.Watchface;
import com.edotassi.amazmod.transport.TransportService;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.RequestDeleteFileData;
import amazmod.com.transport.data.RequestDirectoryData;
import amazmod.com.transport.data.RequestDownloadFileChunkData;
import amazmod.com.transport.data.RequestUploadFileChunkData;
import amazmod.com.transport.data.ResultDeleteFileData;
import amazmod.com.transport.data.ResultDownloadFileChunkData;
import amazmod.com.transport.data.SettingsData;
import amazmod.com.transport.data.WatchfaceData;

public class Watch {

    private static Watch instance;

    private Context context;
    private TransportService transportService;

    private ThreadPoolExecutor threadPoolExecutor;

    private Watch() {
        threadPoolExecutor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
    }

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

    public Task<Directory> listDirectory(final RequestDirectoryData requestDirectoryData) {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<Directory>>() {
            @Override
            public Task<Directory> then(@NonNull Task<TransportService> task) throws Exception {
                return task.getResult().sendWithResult(Transport.REQUEST_DIRECTORY, Transport.DIRECTORY, requestDirectoryData);
            }
        });
    }

    public Task<ResultDeleteFile> deleteFile(final RequestDeleteFileData requestDeleteFileData) {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<ResultDeleteFile>>() {
            @Override
            public Task<ResultDeleteFile> then(@NonNull Task<TransportService> task) throws Exception {
                return task.getResult().sendWithResult(Transport.REQUEST_DELETE_FILE, Transport.RESULT_DELETE_FILE, requestDeleteFileData);
            }
        });
    }

    public Task<Void> downloadFile(final String path, final long size, final CancellationToken cancellationToken) {
        final TaskCompletionSource taskCompletionSource = new TaskCompletionSource<Void>();

        Tasks.call(threadPoolExecutor, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                TransportService transportService = Tasks.await(getServiceInstance());

                long lastChunnkSize = size % Constants.CHUNK_SIZE;
                long totalChunks = size / Constants.CHUNK_SIZE;
                long startedAt = System.currentTimeMillis();

                for (int i = 0; i < totalChunks; i++) {
                    if (cancellationToken.isCancellationRequested()) {
                        //TODO handle cancellation
                    }

                    RequestDownloadFileChunkData requestDownloadFileChunkData = new RequestDownloadFileChunkData();
                    requestDownloadFileChunkData.setPath(path);
                    requestDownloadFileChunkData.setIndex(i);
                    ResultDownloadFileChunk resultDownloadFileChunk = (ResultDownloadFileChunk) Tasks.await(transportService.sendWithResult(Transport.REQUEST_DOWNLOAD_FILE, Transport.RESULT_DOWNLOAD_FILE_CHUNK, requestDownloadFileChunkData));

                    ResultDownloadFileChunkData resultDownloadFileChunkData = resultDownloadFileChunk.getResultDownloadFileChunkData();

                    //TODO write bytes in resultDownloadFileChunkData to dest file
                }

                return null;
            }
        });

        return taskCompletionSource.getTask();
    }

    public Task<Void> uploadFile(final File file, final String destPath, final OperationProgress operationProgress, final CancellationToken cancellationToken) {
        final TaskCompletionSource taskCompletionSource = new TaskCompletionSource<Void>();

        Tasks.call(threadPoolExecutor, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                TransportService transportService = Tasks.await(getServiceInstance());
                long size = file.length();
                long lastChunnkSize = size % Constants.CHUNK_SIZE;
                long totalChunks = size / Constants.CHUNK_SIZE;
                long startedAt = System.currentTimeMillis();

                for (int i = 0; i < totalChunks; i++) {
                    if (cancellationToken.isCancellationRequested()) {
                        RequestDeleteFileData requestDeleteFileData = new RequestDeleteFileData();
                        requestDeleteFileData.setPath(destPath);
                        Tasks.await(Watch.get().deleteFile(requestDeleteFileData));

                        taskCompletionSource.setException(new CancellationException());
                        return null;
                    }

                    RequestUploadFileChunkData requestUploadFileChunkData = RequestUploadFileChunkData.fromFile(file, destPath, Constants.CHUNK_SIZE, i, Constants.CHUNK_SIZE);
                    Tasks.await(transportService.sendAndWait(Transport.REQUEST_UPLOAD_FILE_CHUNK, requestUploadFileChunkData));

                    double progress = (((double) (i + 1)) / totalChunks) * 100f;
                    long duration = System.currentTimeMillis() - startedAt;
                    long byteSent = (i + 1) * Constants.CHUNK_SIZE;
                    double speed = ((double) byteSent) / ((double) duration); // byte/ms
                    long remainingBytes = size - byteSent;
                    long remainTime = (long) (remainingBytes / (speed * 1000));

                    operationProgress.update(duration, byteSent, remainTime, progress);
                }

                if (lastChunnkSize > 0) {
                    RequestUploadFileChunkData requestUploadFileChunkData = RequestUploadFileChunkData.fromFile(file, destPath, Constants.CHUNK_SIZE, totalChunks, (int) lastChunnkSize);
                    Tasks.await(transportService.sendAndWait(Transport.REQUEST_UPLOAD_FILE_CHUNK, requestUploadFileChunkData));
                }

                taskCompletionSource.setResult(null);

                return null;
            }
        });

        return taskCompletionSource.getTask();
    }

    public Task<Void> postNotification(final NotificationData notificationData) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) throws Exception {
                task.getResult().send(Transport.INCOMING_NOTIFICATION, notificationData, taskCompletionSource);
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

    public Task<Void> setBrightness(final BrightnessData brightnessData) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) throws Exception {
                task.getResult().send(Transport.BRIGHTNESS, brightnessData, taskCompletionSource);
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

    public Task<Watchface> sendWatchfaceData(final WatchfaceData watchfaceData) {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<Watchface>>() {
            @Override
            public Task<Watchface> then(@NonNull Task<TransportService> task) throws Exception {
                return task.getResult().sendWithResult(Transport.WATCHFACE_DATA, Transport.WATCHFACE_DATA, watchfaceData);
            }
        });
    }

    public interface OperationProgress {
        void update(long duration, long byteSent, long remainingTime, double progress);
    }
}
