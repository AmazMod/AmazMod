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

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.preference.PreferenceManager;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.event.NextMusic;
import com.edotassi.amazmod.event.NotificationReply;
import com.edotassi.amazmod.event.OtherData;
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

import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
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

    private static Transporter  transporterAmazMod,         // Used with generic data (in/out)
                                transporterNotifications,   // Used to send Custom UI notifications to watch (out)
                                transporterHuami,           // Used with Standard UI notifications (out)
                                transporterCompanion,       // Used with watch/companion data (in/out)
                                transporterSync,            // Used with sync (in/out)
                                transporterHealth,          // Used to pull data (in/out)
                                transporter;                // Used with AmazfitInternetCompanion (in/out)

    private PersistentNotification persistentNotification;
    private LocalBinder localBinder = new LocalBinder();
    private TransportListener transportListener;

    public static final char TRANSPORT_AMAZMOD = 'A';
    public static final char TRANSPORT_NOTIFICATIONS = 'N';
    public static final char TRANSPORT_HUAMI = 'H';
    public static final char TRANSPORT_COMPANION = 'C';
    public static final char TRANSPORT_HEALTH = 'D';
    public static final char TRANSPORTER_SYNC = 'S';

    public static String model;

    private static Transporter.DataListener internetListener;

    int numCores = Runtime.getRuntime().availableProcessors();
    ThreadPoolExecutor executor = new ThreadPoolExecutor(1, numCores * 1,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private ArrayMap<String, Class> messages = new ArrayMap<String, Class>() {{
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
        put(Transport.SILENCE, SilenceApplication.class);
        put(Transport.WIDGETS_DATA, ResultWidgets.class);
    }};

    private static ArrayMap<String, Object> pendingResults = new ArrayMap<>();

    public interface DataTransportResultCallback {
        void onSuccess(DataTransportResult dataTransportResult, String uuid);
        void onFailure(String error, String uuid);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.debug("onCreate is now running...");

        // Make sure services are running and persistent
        startPersistentNotification();
        tryReconnectNotificationService();

        transportListener = new TransportListener(this);
        EventBus.getDefault().register(transportListener);

        Logger.trace("connecting transporters...");
        // Amazmod Transporter
        transporterAmazMod = TransporterClassic.get(this, Transport.NAME);
        if (!transporterAmazMod.isTransportServiceConnected()) {
            transporterAmazMod.addDataListener(this); // Listen for data
            transporterAmazMod.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }
        // Amazmod Custom UI Notifications Transporter
        transporterNotifications = TransporterClassic.get(this, Transport.NAME_NOTIFICATION);
        if (!transporterNotifications.isTransportServiceConnected()) {
            transporterNotifications.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }
        // Huami Stock Notifications Transporter
        transporterHuami = TransporterClassic.get(this, "com.huami.action.notification");
        if (!transporterHuami.isTransportServiceConnected()) {
            transporterHuami.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }
        // Huami Companion Transporter
        transporterCompanion = TransporterClassic.get(this, "com.huami.watch.companion");
        if (!transporterCompanion.isTransportServiceConnected()) {
            transporterCompanion.addDataListener(this);  // Listen for data
            transporterCompanion.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }
        // Huami Health Transporter
        /*
        transporterHealth = TransporterClassic.get(this, "com.huami.watch.health");
        if (!transporterHealth.isTransportServiceConnected()) {
            transporterHealth.addDataListener(this);  // Listen for data
            transporterHealth.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }
         */

        // Amazfit Internet Companion
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ENABLE_INTERNET_COMPANION, false))
            startInternetCompanion(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Logger.debug("onStartCommand");

        startPersistentNotification();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(transportListener);

        // Remove data listeners
        transporterAmazMod.removeDataListener(this);
        //transporterCompanion.removeDataListener(this);
        //transporterHealth.removeDataListener(this);

        disconnectTransports();

        stopForeground(true);
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
        //Logger.trace(transportDataItem.toString());
        String action = transportDataItem.getAction();
        Logger.debug("Watch replied with action: {} (full reply: {})", action, transportDataItem.toString());

        // Get class type of data
        Object event = dataToClass(transportDataItem);

        if (action != null) {
            // Check if there is a pending task
            TaskCompletionSource<Object> taskCompletionSourcePendingResult = (TaskCompletionSource<Object>) pendingResults.get(action);
            if (taskCompletionSourcePendingResult != null) {
                pendingResults.remove(action);
                //Logger.trace("Executing & removing pending completion task with action {}", action);
                taskCompletionSourcePendingResult.setResult(event);
            } else {
                // No pending action, proceed...
                if( event != null ) {
                    Logger.trace("Sending EventBus event: "+event);
                    EventBus.getDefault().post(event);
                }else{
                    Logger.trace("EventBus event is null");
                }
            }
        } else {
            // TODO handle null action
            Logger.error("TransportService onDataReceived null action!");
        }
    }

    /*
    private void getTransporters(){
        Logger.trace("TransportService getTransporters");
        transporterAmazMod = TransporterClassic.get(this, Transport.NAME);
        transporterNotifications = TransporterClassic.get(this, Transport.NAME_NOTIFICATION);
        transporterHuami = TransporterClassic.get(this, "com.huami.action.notification");
        transporterCompanion = TransporterClassic.get(this, "com.huami.watch.companion");
        transporterHealth = TransporterClassic.get(this, "com.huami.watch.health");
    }

    private void connectTransporters(){
        Logger.trace("TransportService connectTransporters");
        connectTransporterAmazMod();
        connectTransporterNotifications();
        connectTransporterHuami();
        connectTransporterCompanion();
    }
    */

    private void disconnectTransports() {
        Logger.trace("TransportService disconnectTransporters");

        if (transporterAmazMod.isTransportServiceConnected()) {
            Logger.info("disconnectTransports disconnecting transporterAmazMod…");
            transporterAmazMod.disconnectTransportService();
            transporterAmazMod = null;
        }

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

        if (transporterCompanion.isTransportServiceConnected()) {
            Logger.info("disconnectTransports disconnecting transporterCompanion…");
            transporterCompanion.disconnectTransportService();
            transporterCompanion = null;
        }

        if (transporterHealth.isTransportServiceConnected()) {
            Logger.info("disconnectTransports disconnecting transporterHealth…");
            transporterHealth.disconnectTransportService();
            transporterHealth = null;
        }
    }

    public static void connectTransporterAmazMod(){
        if (transporterAmazMod.isTransportServiceConnected()) {
            Logger.info("TransportService onCreate transporterAmazMod already connected");
        } else {
            Logger.warn("TransportService onCreate transporterAmazMod not connected, connecting...");
            transporterAmazMod.connectTransportService();
            AmazModApplication.setWatchConnected(false);
        }
    }

    public static void connectTransporterNotifications(){
        if (!transporterNotifications.isTransportServiceConnected()) {
            Logger.warn("TransportService transporterNotifications not connected, connecting...");
            transporterNotifications.connectTransportService();
        } else {
            Logger.info("TransportService transporterNotifications already connected");
        }
    }

    public static void connectTransporterHuami() {
        if (!transporterHuami.isTransportServiceConnected()) {
            Logger.warn("onStartJob transporterHuami not connected, connecting...");
            transporterHuami.connectTransportService();
        } else {
            Logger.info("TransportService transporterHuami already connected");
        }
    }

    public static void connectTransporterCompanion() {
        if (!transporterCompanion.isTransportServiceConnected()) {
            Logger.warn("onStartJob transporterCompanion not connected, connecting...");
            transporterCompanion.connectTransportService();
        } else {
            Logger.info("TransportService transporterCompanion already connected");
        }
    }

    public static boolean isTransporterAmazModAvailable(){
        return transporterAmazMod.isAvailable();
    }

    public static boolean isTransporterNotificationsAvailable(){
        return transporterNotifications.isAvailable();
    }

    public static boolean isTransporterHuamiAvailable(){
        return transporterHuami.isAvailable();
    }

    public static boolean isTransporterHuamiCompanion(){
        return transporterCompanion.isAvailable();
    }

    public static boolean isTransporterAmazModConnected(){
        return transporterAmazMod.isTransportServiceConnected();
    }

    public static boolean isTransporterNotificationsConnected(){
        return transporterNotifications.isTransportServiceConnected();
    }

    public static boolean isTransporterHuamiConnected(){
        return transporterHuami.isTransportServiceConnected();
    }

    public static boolean isTransporterCompanionConnected(){
        return transporterCompanion.isTransportServiceConnected();
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

    public <T> Task<T> sendWithResult(String action, String actionResult) {
        return sendWithResult(action, actionResult, null);
    }


    public <T> Task<T> sendWithResult(final String action, final String actionResult, final Transportable transportable) {
        return sendWithResult(action, actionResult, transportable, TRANSPORT_AMAZMOD);
    }

    public <T> Task<T> sendWithResult(final String action, final String actionResult, final Transportable transportable, char transporter) {
        final TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>();
        //Logger.trace("sendWithResult action: {}", action);

        Tasks.call(executor, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Logger.trace("Sending data with action {} with result action: {}", action, actionResult);
                pendingResults.put(actionResult, taskCompletionSource);

                send(transporter, action, transportable, null);

                try {
                    Logger.trace("Data with action {} were send. Waiting for reply...", action);
                    Tasks.await(taskCompletionSource.getTask(), 30000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException timeoutException) {
                    taskCompletionSource.setException(timeoutException);
                    Logger.error(timeoutException, timeoutException.getMessage());
                }

                Logger.trace("sendWithResult return");
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
        send(TRANSPORT_AMAZMOD, action, transportable, waiter);
    }

    public void sendWithAmazMod(final String action, Transportable transportable, final TaskCompletionSource<Void> waiter) {
        send(TRANSPORT_AMAZMOD, action, transportable, waiter);
    }

    public void sendWithNotifications(final String action, Transportable transportable, final TaskCompletionSource<Void> waiter) {
        send(TRANSPORT_NOTIFICATIONS, action, transportable, waiter);
    }

    public void sendWithHuami(final String action, Transportable transportable, final TaskCompletionSource<Void> waiter) {
        send(TRANSPORT_HUAMI, action, transportable, waiter);
    }

    public void sendWithCompanion(final String action, Transportable transportable, final TaskCompletionSource<Void> waiter) {
        send(TRANSPORT_COMPANION, action, transportable, waiter);
    }

    public void send(char mode, final String action, Transportable transportable, final TaskCompletionSource<Void> waiter) {
        // Get appropriate transporter based on mode
        Transporter transporter = getTransporter(mode, action);

        if (transporter == null)
            return;

        boolean isTransportConnected = transporter.isTransportServiceConnected();
        if (!isTransportConnected) {
            if (AmazModApplication.isWatchConnected() || (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) == null)) {
                AmazModApplication.setWatchConnected(false);
                EventBus.getDefault().removeAllStickyEvents();
                EventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected()));
                persistentNotification.updatePersistentNotification(AmazModApplication.isWatchConnected());
            }
            Logger.warn("Transporter is not connected");
            return;
        }

        DataBundle dataBundle = new DataBundle();
        if (transportable != null)
            transportable.toDataBundle(dataBundle);

        Logger.debug("Send action: {}", action);
        transporter.send(action, dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                Logger.info("Send result: {}", dataTransportResult.toString());

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

                        if (waiter != null)
                            waiter.setException(new RuntimeException("TransporterError: " + dataTransportResult.toString()));
                        break;
                    }
                    case (DataTransportResult.RESULT_OK): {
                        // Data send successfully
                        if (waiter != null)
                            waiter.setResult(null);

                        // Set watch status as connected
                        if (EventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class) == null) {
                            AmazModApplication.setWatchConnected(true);
                            EventBus.getDefault().removeAllStickyEvents();
                            EventBus.getDefault().postSticky(new IsWatchConnectedLocal(true));
                            persistentNotification.updatePersistentNotification(true);
                        }

                        //Logger.debug("Action {} was sent successfully.", action);
                        break;
                    }
                    default:
                        Logger.warn("Send status result is an unknown getResultCode: {}", dataTransportResult.getResultCode());
                }
            }
        });
    }

    public static void sendWithTransporterAmazMod(String action, DataBundle dataBundle) {
        getDataTransportResult(TRANSPORT_AMAZMOD, action, null, dataBundle, null);
    }

    public static void sendWithTransporterNotifications(String action, DataBundle dataBundle) {
        getDataTransportResult(TRANSPORT_NOTIFICATIONS, action, null, dataBundle, null);
    }

    public static void sendWithTransporterHuami(String action, DataBundle dataBundle) {
        getDataTransportResult(TRANSPORT_HUAMI, action, null, dataBundle, null);
    }

    public static void sendWithTransporterCompanion(String action, DataBundle dataBundle) {
        getDataTransportResult(TRANSPORT_COMPANION, action, null, dataBundle, null);
    }

    public static void sendWithTransporterAmazMod(String action, DataBundle dataBundle, DataTransportResultCallback callback) {
        getDataTransportResult(TRANSPORT_AMAZMOD, action, null, dataBundle, callback);
    }

    public static void sendWithTransporterNotifications(String action, String uuid, DataBundle dataBundle, DataTransportResultCallback callback) {
        getDataTransportResult(TRANSPORT_NOTIFICATIONS, action, uuid, dataBundle, callback);
    }

    public static void sendWithTransporterHuami(String action, String uuid, DataBundle dataBundle, DataTransportResultCallback callback) {
        getDataTransportResult(TRANSPORT_HUAMI, action, uuid, dataBundle, callback);
    }

    public static void sendWithTransporterCompanion(String action, DataBundle dataBundle, DataTransportResultCallback callback) {
        getDataTransportResult(TRANSPORT_COMPANION, action, null, dataBundle, callback);
    }

    public static void getDataTransportResult(char mode, String action, final String uuid, DataBundle dataBundle, final DataTransportResultCallback callback) {
        // Get appropriate transporter based on mode
        Transporter t = getTransporter(mode, action);

        if(t == null){
            if (callback != null)
                callback.onFailure("Transporter mode not found", uuid);
            return;
        }

        Logger.debug("uuid: {}", uuid);
        t.send(action, dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                Logger.debug("getDataTransportResult result: {}", dataTransportResult.toString());
                if (callback != null)
                    callback.onSuccess(dataTransportResult, uuid);
            }
        });
    }

    private static Transporter getTransporter(char mode) {
        return getTransporter(mode, null);
    }

    private static Transporter getTransporter(char mode, String action) {
        switch (mode) {
            case TRANSPORT_AMAZMOD:
                Logger.debug("Sending using transporter Amazmod with action: {}", action);
                return transporterAmazMod;
            case TRANSPORT_NOTIFICATIONS:
                Logger.debug("Sending using transporter Notifications with action: {}", action);
                return transporterNotifications;
            case TRANSPORT_HUAMI:
                Logger.debug("Sending using transporter Huami with action: {}", action);
                return transporterHuami;
            case TRANSPORT_COMPANION:
                Logger.debug("Sending using transporter Companion with action: {}", action);
                return transporterCompanion;
            case TRANSPORT_HEALTH:
                Logger.debug("Sending using transporter Health with action: {}", action);
                return transporterHealth;
            default:
                Logger.error("Transporter mode not found or null");
        }
        return null;
    }

    private Object dataToClass(TransportDataItem transportDataItem) {
        String action = transportDataItem.getAction();

        //Logger.debug("action: " + action);

        // Get appropriate class to handle action
        Class messageClass = messages.get(action);
        Class[] args = {DataBundle.class};

        // It there is no class, go as general data
        if (messageClass == null)
            messageClass = OtherData.class;

        try {
            Constructor eventConstructor = messageClass.getDeclaredConstructor(args);
            Object event = eventConstructor.newInstance(transportDataItem.getData());
            Logger.debug("Watch data to class: {} -> {}", action, event.toString());

            return event;
        } catch (NoSuchMethodException e) {
            Logger.debug("Transport event mapped with action \"{}\" doesn't have constructor with DataBundle as parameter", action);
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void tryReconnectNotificationService() {

        Logger.debug("TransportService tryReconnectNotificationService");

        toggleNotificationService();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ComponentName componentName = new ComponentName(getApplicationContext(), NotificationService.class);
            requestRebind(componentName);
            Logger.debug("TransportService tryReconnectNotificationService requestRebind");
        }
    }

    private void toggleNotificationService() {

        Logger.info("TransportService toggleNotificationService");

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

    //From: https://github.com/KieronQuinn/AmazfitInternetCompanion
    public static void startInternetCompanion(Context context) {
        Logger.trace("start");
        transporter = Transporter.get(context, Transport.NAME_INTERNET);
        internetListener = new Transporter.DataListener() {
            @Override
            public void onDataReceived(TransportDataItem item) {
                Logger.debug("AmazfitInternetCompanion onDataReceived");
                if (Transport.HTTP_REQUEST.equals(item.getAction())) {
                    //Never try if it's a watch (someone made an error)
                    if ("Huami".equals(Build.BRAND)) return;
                    //Send pingback immediately to let the app know it's being handled
                    transporter.send(Transport.HTTP_PINGBACK, item.getData());
                    //Get data
                    DataBundle dataBundle = item.getData();
                    try {
                        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(dataBundle.getString("url")).openConnection();
                        httpURLConnection.setInstanceFollowRedirects(dataBundle.getBoolean("followRedirects"));
                        httpURLConnection.setRequestMethod(dataBundle.getString("requestMethod"));
                        httpURLConnection.setUseCaches(dataBundle.getBoolean("useCaches"));
                        httpURLConnection.setDoInput(dataBundle.getBoolean("doInput"));
                        httpURLConnection.setDoOutput(dataBundle.getBoolean("doOutput"));
                        try {
                            JSONArray headers = new JSONArray(dataBundle.getString("requestHeaders"));
                            for (int x = 0; x < headers.length(); x++) {
                                JSONObject header = headers.getJSONObject(x);
                                httpURLConnection.setRequestProperty(header.getString("key"), header.getString("value"));
                            }
                        } catch (JSONException e) {
                            Logger.error(e, "exception: {}", e.getMessage());
                        }
                        httpURLConnection.connect();
                        if (httpURLConnection.getInputStream() != null) {
                            byte[] inputStream = IOUtils.toByteArray(httpURLConnection.getInputStream());
                            if (inputStream != null)
                                dataBundle.putByteArray("inputStream", inputStream);
                        }
                        if (httpURLConnection.getErrorStream() != null) {
                            byte[] errorStream = IOUtils.toByteArray(httpURLConnection.getErrorStream());
                            if (errorStream != null)
                                dataBundle.putByteArray("errorStream", errorStream);
                        }
                        dataBundle.putString("responseMessage", httpURLConnection.getResponseMessage());
                        dataBundle.putInt("responseCode", httpURLConnection.getResponseCode());
                        dataBundle.putString("responseHeaders", mapToJSON(httpURLConnection.getHeaderFields()).toString());
                        //Return the data
                        transporter.send(Transport.HTTP_RESULT, dataBundle);
                        httpURLConnection.disconnect();
                    } catch (IOException e) {
                        Logger.error(e, "exception: {}", e.getMessage());
                    }
                }
            }
        };
        transporter.addDataListener(internetListener);
        transporter.connectTransportService();
    }

    public static void stopInternetCompanion() {
        Logger.trace("stop");
        if (internetListener != null)
            transporter.removeDataListener(internetListener);
        if (transporter != null)
            if (transporter.isTransportServiceConnected()) {
                Logger.info("disconnecting transporterInternet…");
                transporter.disconnectTransportService();
                transporter = null;
            }
    }

    private static JSONArray mapToJSON(Map<String, List<String>> input) {
        JSONArray headers = new JSONArray();
        for (String key : input.keySet()) {
            JSONObject item = new JSONObject();
            try {
                item.put("key", key);
                List<String> items = input.get(key);
                JSONArray itemsArray = new JSONArray();
                for (String itemValue : items) {
                    itemsArray.put(itemValue);
                }
                item.put("value", itemsArray);
            } catch (Exception e) {
                Logger.error(e, "exception: {}", e.getMessage());
            }
            headers.put(item);
        }
        return headers;
    }
}
