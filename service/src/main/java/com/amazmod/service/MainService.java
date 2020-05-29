package com.amazmod.service;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amazmod.service.db.model.BatteryDbEntity;
import com.amazmod.service.db.model.BatteryDbEntity_Table;
import com.amazmod.service.events.HardwareButtonEvent;
import com.amazmod.service.events.HourlyChime;
import com.amazmod.service.events.NightscoutDataEvent;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.events.SilenceApplicationEvent;
import com.amazmod.service.events.incoming.Brightness;
import com.amazmod.service.events.incoming.DeleteNotificationEvent;
import com.amazmod.service.events.incoming.EnableLowPower;
import com.amazmod.service.events.incoming.IncomingNotificationEvent;
import com.amazmod.service.events.incoming.RequestBatteryStatus;
import com.amazmod.service.events.incoming.RequestDeleteFile;
import com.amazmod.service.events.incoming.RequestDirectory;
import com.amazmod.service.events.incoming.RequestDownloadFileChunk;
import com.amazmod.service.events.incoming.RequestShellCommand;
import com.amazmod.service.events.incoming.RequestUploadFileChunk;
import com.amazmod.service.events.incoming.RequestWatchStatus;
import com.amazmod.service.events.incoming.RequestWidgets;
import com.amazmod.service.events.incoming.RevokeAdminOwner;
import com.amazmod.service.events.incoming.SyncSettings;
import com.amazmod.service.events.incoming.Watchface;
import com.amazmod.service.music.MusicControlInputListener;
import com.amazmod.service.notifications.NotificationService;
import com.amazmod.service.receiver.AdminReceiver;
import com.amazmod.service.receiver.NotificationReplyReceiver;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.springboard.WidgetSettings;
import com.amazmod.service.support.BatteryJobService;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.AlertsActivity;
import com.amazmod.service.ui.ConfirmationWearActivity;
import com.amazmod.service.ui.fragments.WearMenuFragment;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.ExecCommand;
import com.amazmod.service.util.FileDataFactory;
import com.amazmod.service.util.SystemProperties;
import com.amazmod.service.util.WidgetsUtil;
import com.amazmod.service.weather.Weather;
import com.huami.watch.notification.data.NotificationKeyData;
import com.huami.watch.notification.data.StatusBarNotificationData;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.ingenic.iwds.slpt.SlptClockClient;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.BatteryData;
import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.DirectoryData;
import amazmod.com.transport.data.FileData;
import amazmod.com.transport.data.FileUploadData;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.RequestDeleteFileData;
import amazmod.com.transport.data.RequestDirectoryData;
import amazmod.com.transport.data.RequestDownloadFileChunkData;
import amazmod.com.transport.data.RequestShellCommandData;
import amazmod.com.transport.data.RequestUploadFileChunkData;
import amazmod.com.transport.data.ResultDeleteFileData;
import amazmod.com.transport.data.ResultDownloadFileChunkData;
import amazmod.com.transport.data.ResultShellCommandData;
import amazmod.com.transport.data.SettingsData;
import amazmod.com.transport.data.WatchStatusData;
import amazmod.com.transport.data.WatchfaceData;
import amazmod.com.transport.data.WidgetsData;

import static amazmod.com.transport.Constants.WIDGETS_LIST_EMPTY_CODE;
import static amazmod.com.transport.Constants.WIDGETS_LIST_SAVED_CODE;
import static com.amazmod.service.util.DeviceUtil.getLocalIpAddress;
import static com.amazmod.service.util.FileDataFactory.drawableToBitmap;
import static com.amazmod.service.util.SystemProperties.isStratos3;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class MainService extends Service implements Transporter.DataListener {
    private static Transporter transporterGeneral, transporterNotifications, transporterHuami, transporterXdrip;

    private static IntentFilter batteryFilter;
    private static long dateLastCharge;
    private static int count = 0;
    private static boolean isPhoneConnectionAlertEnabled;
    private static boolean isStandardAlertEnabled;
    private static boolean isSpringboardObserverEnabled;
    private static boolean requestSelfReload;
    private static boolean wasSpringboardSaved;
    private static boolean isWeatherObserverEnabled;
    private static boolean wasWeatherSaved;
    private static long custom_weather_expire = 0L;
    private static WidgetSettings settings;
    private static JobScheduler jobScheduler;
    private static char overlayLauncherPosition;
    private static boolean notificationArrived;

    private static final long BATTERY_SYNC_INTERVAL = 60*60*1000L; //One hour
    private static final int BATTERY_JOB_ID = 0;

    private Context context;
    private SettingsManager settingsManager;
    private NotificationService notificationManager;
    private BatteryData batteryData;
    private WatchStatusData watchStatusData;
    private WidgetsData widgetsData;
    private NotificationReplyReceiver notificationReplyReceiver;
    private BroadcastReceiver screenOnReceiver;
    private SlptClockClient slptClockClient;
    private ContentObserver phoneConnectionObserver;
    private ContentObserver springboardObserver;
    private ContentObserver weatherObserver;

    private boolean watchBatteryAlreadyAlerted;
    private boolean phoneBatteryAlreadyAlerted;
    private float batteryPct;
    private int vibrate;
    private NotificationData notificationData;

    private static PowerManager.WakeLock myWakeLock;
    private static boolean isRunning = false;

    private static final Handler stdHandler = new Handler();
    private static final Handler cstHandler = new Handler();

    // Standard Notification
    private final Runnable sendStandardNotification = new Runnable() {
        public void run() {
            notificationManager.post(notificationData);
            // Vibration
            // Do not vibrate if DND is active
            if (!DeviceUtil.isDNDActive(context)) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        if (vibrator == null) return;

                        try {
                            vibrator.vibrate(vibrate);
                        } catch (Exception e) {
                            Logger.error("vibrator exception: {}", e.getMessage());
                        }
                    }
                }, 1000 /* 1s */);
            }

            isRunning = false;
        }
    };

    // Alert Notification
    private final Runnable sendAlertNotification = new Runnable() {
        public void run() {
            // Start alert activity
            Intent intent = new Intent(context, AlertsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.putExtra("type","phone_connection");
            context.startActivity(intent);
            isRunning = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;
        settingsManager = new SettingsManager(context);
        notificationManager = new NotificationService(context);

        batteryData = new BatteryData();
        watchStatusData = new WatchStatusData();
        widgetsData = new WidgetsData();

        // Register EventBus
        EventBus.getDefault().register(this);
        Logger.debug("MainService onCreate EventBus register");

        // Load settings
        settings = new WidgetSettings(Constants.TAG, context);

        // Restore system screen_off setting in case there was a service update
        //new ExecCommand(ExecCommand.ADB, "adb shell settings put system screen_off_timeout 14000");
        DeviceUtil.systemPutInt(context, Settings.System.SCREEN_OFF_TIMEOUT, 14000);
        Logger.debug("Restore APK_INSTALL screen timeout");

        /*
        try {
            if (new File("/system/xbin/su").exists()) { // Test for root
                //Runtime.getRuntime().exec("adb shell echo APK_INSTALL > /sys/power/wake_unlock;exit");
                new ExecCommand(ExecCommand.ADB, "adb shell echo APK_INSTALL > /sys/power/wake_unlock");
                Logger.debug("Disabling APK_INSTALL WAKELOCK");
            } else {
                //Runtime.getRuntime().exec("adb shell settings put system screen_off_timeout 14000;exit");
                DeviceUtil.systemPutAdb(context,"screen_off_timeout", "14000");
                Logger.debug("Restore APK_INSTALL screen timeout");
            }
        } catch (Exception e) {
            Logger.error(e, "onCreate: exception while restoring wakelock/screen timeout: {}", e.getMessage());
        }
        //new ExecCommand("adb shell \"adb kill-server\"", true);
        */

        // Register power disconnect receiver
        batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        final IntentFilter powerDisconnectedFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        powerDisconnectedFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get battery
                Intent batteryStatus = context.registerReceiver(null, batteryFilter);
                // Update battery
                getBatteryPct(batteryStatus);

                // Update date of last charge
                if (batteryPct > 0.98) {
                    dateLastCharge = System.currentTimeMillis();
                    settings.set(Constants.PREF_DATE_LAST_CHARGE, dateLastCharge);
                    Logger.debug("MainService onCreate dateLastCharge saved: " + dateLastCharge);
                }
            }
        }, powerDisconnectedFilter);

        // Register notification reply receiver
        notificationReplyReceiver = new NotificationReplyReceiver();
        IntentFilter notificationReplyFilter = new IntentFilter();
        notificationReplyFilter.addAction(Constants.INTENT_ACTION_REPLY);
        LocalBroadcastManager.getInstance(context).registerReceiver(notificationReplyReceiver, notificationReplyFilter);

        // Reboot launcher if language was change to update Amazmod widget locale
        if (settings.get(Constants.REQUEST_SELF_RELOAD, true)) {
            new ExecCommand(ExecCommand.ADB, "adb shell am force-stop com.huami.watch.launcher");
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.huami.watch.launcher");
            if (launchIntent != null) {
                startActivity(launchIntent);
                Logger.debug("isRequestSelfReload: is rebooting launcher and reload AmazMod to apply new locate ");
                settings.set(Constants.REQUEST_SELF_RELOAD, false);
            }
        }

        // Start OverlayLauncher
        if (settings.get(Constants.PREF_AMAZMOD_OVERLAY_LAUNCHER, false))
            setOverlayLauncher(true);

        // Check if hourly chime is enable
        if (settings.get(Constants.PREF_AMAZMOD_HOURLY_CHIME, false) || (WearMenuFragment.chimeEnabled))
            setHourlyChime(true);

       // Initialize battery alerts
        this.watchBatteryAlreadyAlerted = false;
        this.phoneBatteryAlreadyAlerted = false;

        // Reset notification counter to ZERO (0)
        NotificationStore.setNotificationCount(context, 0);

        // CHECK TRANSPORTERS
        // Amazmod's
        transporterGeneral = TransporterClassic.get(this, Transport.NAME);
        transporterGeneral.addDataListener(this);
        Logger.debug("MainService onCreate transporterAmazmod "+ (!transporterGeneral.isTransportServiceConnected()?"not connected, connecting...": "already connected") );
        if (!transporterGeneral.isTransportServiceConnected())
            transporterGeneral.connectTransportService();
        // Amazmod's Notifications
        transporterNotifications = TransporterClassic.get(this, Transport.NAME_NOTIFICATION);
        transporterNotifications.addDataListener(this);
        Logger.debug("MainService onCreate transporterAmazmodNotification "+ (!transporterNotifications.isTransportServiceConnected()?"not connected, connecting...": "already connected") );
        if (!transporterNotifications.isTransportServiceConnected())
            transporterNotifications.connectTransportService();
        // Huami's notifications
        transporterHuami = TransporterClassic.get(this, "com.huami.action.notification");
        transporterHuami.addDataListener(this);
        Logger.debug("MainService onCreate transporterHuamiNotification "+ (!transporterHuami.isTransportServiceConnected()?"not connected, connecting...": "already connected") );
        if (!transporterHuami.isTransportServiceConnected())
            transporterHuami.connectTransportService();
        // XDrip data
        transporterXdrip = TransporterClassic.get(this, "com.eveningoutpost.dexdrip.wearintegration");
        transporterXdrip.addDataListener(this);
        Logger.debug("MainService onCreate transporterXdrip "+ (!transporterXdrip.isTransportServiceConnected()?"not connected, connecting...": "already connected") );
        if (!transporterXdrip.isTransportServiceConnected())
            transporterXdrip.connectTransportService();


        // This is so we can enable Power Save mode
        slptClockClient = new SlptClockClient();
        slptClockClient.bindService(this, "AmazMod-MainService", new SlptClockClient.Callback() {
            @Override
            public void onServiceConnected() {}

            @Override
            public void onServiceDisconnected() {}
        });

        // Isn't this disabled/removed????? TODO
        setupHardwareKeysMusicControl(settingsManager.getBoolean(Constants.PREF_ENABLE_HARDWARE_KEYS_MUSIC_CONTROL, false));

        // Register phone connect/disconnect monitor
        isPhoneConnectionAlertEnabled = settingsManager.getBoolean(Constants.PREF_PHONE_CONNECTION_ALERT, false);
        isStandardAlertEnabled = settingsManager.getBoolean(Constants.PREF_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION, false);
        if (isPhoneConnectionAlertEnabled)
            registerConnectionMonitor(true);

        // Register springboard observer
        isSpringboardObserverEnabled = settingsManager.getBoolean(Constants.PREF_AMAZMOD_KEEP_WIDGET, true);
        if (isSpringboardObserverEnabled)
            registerSpringBoardMonitor(true);
        Logger.debug("MainService isSpringboardObserverEnabled: "+isSpringboardObserverEnabled);

        // Register weather observer
        isWeatherObserverEnabled = settingsManager.getBoolean(Constants.PREF_AMAZMOD_KEEP_WEATHER, true);
        if (isWeatherObserverEnabled)
            registerWeatherMonitor(true);

        // Set battery db record JobService if watch never synced with phone
        String defaultLocale = settingsManager.getString(Constants.PREF_DEFAULT_LOCALE, "");
        long timeSinceLastBatterySync = System.currentTimeMillis() - settings.get(Constants.PREF_DATE_LAST_BATTERY_SYNC, 0);
        Logger.debug("MainService onCreate defaultLocale: " + defaultLocale);

        if ((defaultLocale.isEmpty()) || (timeSinceLastBatterySync > BATTERY_SYNC_INTERVAL)) {
            Logger.debug("MainService onCreate ***** starting BatteryJobService");
            jobScheduler = (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                ComponentName serviceComponent = new ComponentName(getApplicationContext(), BatteryJobService.class);
                cancelPendingJobs(BATTERY_JOB_ID);
                JobInfo.Builder builder = new JobInfo.Builder(BATTERY_JOB_ID, serviceComponent);
                builder.setPeriodic(BATTERY_SYNC_INTERVAL);
                jobScheduler.schedule(builder.build());
            } else
                Logger.error("MainService error staring BatteryJobService: null jobScheduler!");
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);

        // Unregister receivers
        if (notificationReplyReceiver != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(notificationReplyReceiver);
            notificationReplyReceiver = null;
        }
        if (screenOnReceiver != null) {
            context.unregisterReceiver(screenOnReceiver);
            screenOnReceiver = null;
        }

        // Unregister content observers
        registerConnectionMonitor(false);
        registerSpringBoardMonitor(false);
        registerWeatherMonitor(false);

        // Disconnect transporters
        if (transporterGeneral.isTransportServiceConnected()) {
            Logger.debug( "MainService onDestroy transporterGeneral disconnecting...");
            transporterGeneral.disconnectTransportService();
            transporterGeneral = null;
        }
        if (transporterNotifications.isTransportServiceConnected()) {
            Logger.debug("MainService onDestroy transporterNotifications disconnecting...");
            transporterNotifications.disconnectTransportService();
            transporterNotifications = null;
        }
        if (transporterHuami.isTransportServiceConnected()) {
            Logger.debug("MainService onDestroy transporterHuami disconnecting...");
            transporterHuami.disconnectTransportService();
            transporterHuami = null;
        }
        if (transporterXdrip.isTransportServiceConnected()) {
            Logger.debug("MainService onDestroy transporterXdrip disconnecting...");
            transporterXdrip.disconnectTransportService();
            transporterXdrip = null;
        }

        // Unbind spltClockClient
        if (slptClockClient != null)
            slptClockClient.unbindService(this);

        stdHandler.removeCallbacks(sendStandardNotification);
        cstHandler.removeCallbacks(sendAlertNotification);

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private ArrayMap<String, Class> messages = new ArrayMap<String, Class>() {{
        put(Constants.ACTION_NIGHTSCOUT_SYNC, NightscoutDataEvent.class);
        put(Transport.SYNC_SETTINGS, SyncSettings.class);
        put(Transport.INCOMING_NOTIFICATION, IncomingNotificationEvent.class);
        put(Transport.REQUEST_WATCHSTATUS, RequestWatchStatus.class);
        put(Transport.REQUEST_BATTERYSTATUS, RequestBatteryStatus.class);
        put(Transport.BRIGHTNESS, Brightness.class);
        put(Transport.ENABLE_LOW_POWER, EnableLowPower.class);
        put(Transport.REVOKE_ADMIN_OWNER, RevokeAdminOwner.class);
        put(Transport.REQUEST_DIRECTORY, RequestDirectory.class);
        put(Transport.REQUEST_DELETE_FILE, RequestDeleteFile.class);
        put(Transport.REQUEST_UPLOAD_FILE_CHUNK, RequestUploadFileChunk.class);
        put(Transport.REQUEST_DOWNLOAD_FILE_CHUNK, RequestDownloadFileChunk.class);
        put(Transport.REQUEST_SHELL_COMMAND, RequestShellCommand.class);
        put(Transport.WATCHFACE_DATA, Watchface.class);
        put(Transport.REQUEST_WIDGETS, RequestWidgets.class);
        put(Transport.DELETE_NOTIFICATION, DeleteNotificationEvent.class);
    }};

    @Override
    public void onDataReceived(TransportDataItem transportDataItem) {
        String action = transportDataItem.getAction();

        if (action == null) {
            Logger.debug("MainService data received without action.");
            return;
        }

        Logger.debug("MainService action: {}", action);
        DataBundle db = transportDataItem.getData();

        // A stock notification is received
        if (action.equals("add")) {
            // Activate screen if the option is enabled in widget menus
            settings.reload();
            if (settings.get(Constants.PREF_NOTIFICATIONS_SCREEN_ON, 0) == 1)
                acquireWakelock();

            // Flag used by OverlayLauncher
            notificationArrived = true;
        } else if (Transport.INCOMING_NOTIFICATION.equals(action))
            notificationArrived = true;// Flag used by OverlayLauncher

        // Run a function based on requested action
        Class messageClass = messages.get(action);
        if (messageClass != null) {
            Class[] args = new Class[1];
            args[0] = DataBundle.class;

            try {
                Constructor eventContructor = messageClass.getDeclaredConstructor(args);
                Object event = eventContructor.newInstance(db);

                Logger.debug("MainService onDataReceived: " + event.toString());
                EventBus.getDefault().post(event);
            } catch (NoSuchMethodException e) {
                Logger.debug("MainService event mapped with action \"" + action + "\" doesn't have constructor with DataBundle as parameter");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Non-priority functions (keep at the bottom to speed-up notifications)
        switch (action) {
            case "add": // A notification is added
            case Transport.INCOMING_NOTIFICATION:   // Custom notification added
                DeviceUtil.notificationCounter(context, 1, true);
                break;
            case "xDrip_synced_SGV_data": // Xdrip data
                xdrip( db.getString("Data") );
                break;
            case Transport.LOCAL_IP: // watch's local IP request
                requestLocalIp();
                break;
            default:
        }
    }

    // Delete Notification (counter update and delete custom notification)
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void deleteNotification(DeleteNotificationEvent deleteNotificationEvent) {
        boolean enableCustomUI = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI, false);
        StatusBarNotificationData statusBarNotificationData = deleteNotificationEvent.getDataBundle().getParcelable("data");
        String key = statusBarNotificationData.key;
        Logger.warn("deleteNotification enableCustomUI: {} \\ key: {}", enableCustomUI, key);

        // Update notification Counter
        DeviceUtil.notificationCounter(context, -1);

        // Check if custom notifications are enabled
        if (!enableCustomUI) return;

        if (key != null) {
            if (NotificationStore.getCustomNotificationCount() > 0)
                for (ArrayMap.Entry<String, String> pair : NotificationStore.keyMap.entrySet()) {
                    //Logger.warn("deleteNotification NS.key: {} \\ NS.entry: {}", pair.getKey(), pair.getValue());

                    if (key.equals(pair.getValue())) {
                        Logger.warn("deleteNotification removing: {}", pair.getKey());
                        NotificationStore.removeCustomNotification(pair.getKey(), context);
                    }
                }
            else
                Logger.warn("deleteNotification empty NotificationStore");
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void requestDeleteNotification(NotificationKeyData notificationKeyData) {
        Logger.warn("requestDeleteNotification key: {}", notificationKeyData.key);
        DataBundle dataBundle = new DataBundle();
        dataBundle.putParcelable("notiKey", notificationKeyData);
        sendHuami("del", dataBundle);
    }

    // Phone data (phone battery, phone alarm, calendar events, weather)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void watchface(Watchface watchface) {

        // Data from phone
        WatchfaceData watchfaceData = WatchfaceData.fromDataBundle(watchface.getDataBundle());
        int phoneBattery = watchfaceData.getBattery();
        String phoneAlarm = watchfaceData.getAlarm();
        String calendarEvents = watchfaceData.getCalendarEvents();
        String weather_data = watchfaceData.getWeatherData();
        Long expire = watchfaceData.getExpire();

        // Logs
        String local_log_tag = "[New phone data] ";
        Logger.debug(local_log_tag+ " incoming battery data: " + phoneBattery);
        Logger.debug(local_log_tag+ " incoming alarm data:" + phoneAlarm);
        Logger.debug(local_log_tag+ " incoming calendar events:" + calendarEvents);
        Logger.debug(local_log_tag+ " incoming weather data:" + weather_data);
        Logger.debug(local_log_tag+ " data will expire at:" + expire);

        // Update Time
        long updateTime = Calendar.getInstance().getTimeInMillis();

        // Watchface data
        // Get already saved data
        String data = DeviceUtil.systemGetString(context, Constants.CUSTOM_WATCHFACE_DATA);
        if (data == null || data.equals(""))
            data = "{}";

        // Update the data
        try {
            // Extract data from JSON
            JSONObject json_data = new JSONObject(data); // load existing data to keep any extra parameters
            json_data.put("phoneBattery", phoneBattery);
            json_data.put("phoneAlarm", phoneAlarm);
            json_data.put("updateTime", updateTime);
            json_data.put("expire", expire);

            data = json_data.toString();
        } catch (JSONException e) {
            data = "{\"phoneBattery\":\"" + phoneBattery + "\",\"phoneAlarm\":\"" + phoneAlarm + "\",\"updateTime\":"+updateTime+"}";
        }
        DeviceUtil.systemPutString(context, Constants.CUSTOM_WATCHFACE_DATA, data);

        // Calendar data
        if (calendarEvents != null && !calendarEvents.equals("")) {
            try {
                // Check if correct form of JSON
                JSONObject json_data = new JSONObject(calendarEvents);
                json_data.put("updateTime", updateTime);
                // Update data
                data =  json_data.toString();
            } catch (JSONException e) {
                data = "{}";//default
            }
            DeviceUtil.systemPutString(context, "CustomCalendarData", data);
            Logger.debug(local_log_tag+ " Calendar data were updated.");
        }

        // Weather data
        if (weather_data != null && !weather_data.equals("")) {
            wasWeatherSaved = false; // Default value
            // Update system weather data
            String save_value = Weather.updateWeatherData(context, weather_data);
            if(!save_value.equals(Weather.DATA_HAVE_NOT_UPDATE)) {
                // Save new data
                settingsManager.putString(Constants.WEATHER_INFO, save_value);
                // Save that Amazmod just updated the system value (used when overwriting system weather values)
                wasWeatherSaved = true;
                // Save the expiration time (used when overwriting system weather values)
                custom_weather_expire = expire;
                Logger.debug(local_log_tag+ " Weather data were updated.");
            }else{
                Logger.debug(local_log_tag+ " Weather data were NOT updated.");
            }
        }

        // Phone Battery Alert
        if( settingsManager.getInt(Constants.PREF_BATTERY_PHONE_ALERT, 0) > 0 ){
            if( settingsManager.getInt(Constants.PREF_BATTERY_PHONE_ALERT, 0) >= phoneBattery ){
                if(!phoneBatteryAlreadyAlerted) { // Pass only if NOT already alerted
                    if (isStandardAlertEnabled) {
                        // Show standard battery alert
                        sendStandardAlert("phone_battery");
                    } else {
                        // Show battery alert
                        Intent intent = new Intent(context, AlertsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        intent.putExtra("type", "phone_battery");
                        context.startActivity(intent);
                    }
                    phoneBatteryAlreadyAlerted = true;
                }
            }else{
                // When battery is above the alert level
                phoneBatteryAlreadyAlerted = false;
            }
        }
    }

    // XDrip data
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void xdrip(String xdrip_data) {
        if(xdrip_data==null || !xdrip_data.equals(""))
            return;

        // Try to decode JSON
        try {
            // Extract data from JSON
            //JSONObject json_data = new JSONObject(xdrip_data);

            // Update system data
            DeviceUtil.systemPutString(context, "Xdrip", xdrip_data);

            // If data analysis is needed:
            /*
            // Default values
            String strike = "";
            String color = "WHITE";
            String sgv = "--";
            String delta = "--";
            String timeago = "--";
            String phonebattery = "--";
            String sgv_graph = "false";
            Long timestamp = Long.valueOf(1);
            Boolean badvalue = false;
            Boolean ishigh = false;
            Boolean islow = false;
            Boolean isstale = false;
            Boolean firstdata = false;

            if (json_data.has("sgv"))
                sgv = json_data.getString("sgv");

            if (json_data.has("delta"))
                delta = json_data.getString("delta");

            if (json_data.has("date")) {
                timestamp = Long.valueOf(json_data.getString("date"));
                //timeago = TimeAgo.using(Long.valueOf(json_data.getString("date")));
            }

            if (json_data.has("WFGraph"))
                sgv_graph = json_data.getString("WFGraph");

            if (json_data.has("phone_battery"))
                phonebattery = json_data.getString("phone_battery");

            if (json_data.has("ishigh"))
                ishigh = Boolean.valueOf(json_data.getString("ishigh"));

            if (json_data.has("islow"))
                islow = Boolean.valueOf(json_data.getString("islow"));

            if (json_data.has("isstale"))
                isstale = Boolean.valueOf(json_data.getString("isstale"));

            if (!isstale && !islow && !ishigh)
                color="BLACK";
            else
                color="WHITE";

            if( System.currentTimeMillis() > timestamp+10*60*1000 )
                strike = new String(new char[sgv.length()]).replace("\0", "─");
            else
                strike = "";
             */
        }catch (Exception e) {
            // Nothing
        }
    }

    // Request installed widgets
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestWidgets(RequestWidgets requestWidgets) {
        // Returned data
        WidgetsData widgetsData = WidgetsData.fromDataBundle(requestWidgets.getDataBundle());
        String savedOrder = widgetsData.getPackages();
        Logger.debug("MainService requestWidgets widgetsData (got): " + savedOrder);

        if (!savedOrder.isEmpty()){
            // Check is EMPTY code
            if(savedOrder.equals(WIDGETS_LIST_EMPTY_CODE)){
                // All widgets are off
                settingsManager.putString(Constants.PREF_SPRINGBOARD_ORDER, "");
                //DeviceUtil.systemPutString(context, Constants.WIDGET_ORDER_IN, "");
                Logger.debug("WidgetsRequest remove PREF_SPRINGBOARD_ORDER");
            }else{
                // Save the list here
                settingsManager.putString(Constants.PREF_SPRINGBOARD_ORDER, savedOrder);
                DeviceUtil.systemPutString(context, Constants.WIDGET_ORDER_IN, savedOrder);
                Logger.debug("WidgetsRequest save PREF_SPRINGBOARD_ORDER & widget_order_in: " + savedOrder);
            }

            // Send confirmation that data are saved
            widgetsData.setPackages(WIDGETS_LIST_SAVED_CODE);
        }else {
            // Get widgets list
            JSONArray widgetsList = WidgetsUtil.getWidgetsLists(getApplicationContext(), true);
            // Set up data
            widgetsData.setPackages(widgetsList.toString());
        }

        // Send the transmit
        Logger.debug("MainService requestWidgets widgetsData (send): " + widgetsData.getPackages());
        send(Transport.WIDGETS_DATA, widgetsData.toDataBundle());
    }

    // Request watch's local IP
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestLocalIp() {
        String localIP = getLocalIpAddress();
        Logger.debug("MainService requestLocalIp, local IP: "+localIP);
        // Send transmit
        DataBundle data = new DataBundle();
        data.putString("ip",localIP);
        send(Transport.LOCAL_IP, data);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void enableLowPower(EnableLowPower lp) {
        //SystemProperties.goToSleep(this);
        count++;
        Logger.debug("MainService lowPower count: " + count);
        if (count < 2) {
            Toast.makeText(context, getString(R.string.low_power) +": "+ getString(R.string.true_), Toast.LENGTH_SHORT).show();
            BluetoothAdapter btmgr = BluetoothAdapter.getDefaultAdapter();
            Logger.info("MainService lowPower disable BT");
            btmgr.disable();
            try {
                WifiManager wfmgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wfmgr != null) {
                    if (wfmgr.isWifiEnabled()) {
                        Logger.info("MainService lowPower disable WiFi");
                        wfmgr.setWifiEnabled(false);
                    }
                }
            } catch (NullPointerException e) {
                Logger.error("MainService lowPower exception: " + e.toString());
            }

            slptClockClient.enableLowBattery();
            slptClockClient.enableSlpt();
            Logger.debug("powerlow true = {}", SystemProperties.setSystemProperty("sys.state.powerlow", String.valueOf(true)));
            final DevicePolicyManager mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (mDPM != null) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        mDPM.lockNow();
                    }
                }, 200);
            }

        } else if (count >= 3) {
            Toast.makeText(context, getString(R.string.low_power) +": "+ getString(R.string.false_), Toast.LENGTH_SHORT).show();
            //btmgr.enable();
            //slptClockClient.disableSlpt();
            slptClockClient.disableLowBattery();
            Logger.debug("powerlow false = {}", SystemProperties.setSystemProperty("sys.state.powerlow", String.valueOf(false)));
            count = 0;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void revokeAdminOwner(RevokeAdminOwner revokeAdminOwner) {
        DevicePolicyManager mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            if (mDPM != null) {
                ComponentName componentName = new ComponentName(context, AdminReceiver.class);
                //mDPM.clearDeviceOwnerApp(context.getPackageName());
                mDPM.removeActiveAdmin(componentName);
            }
        } catch (NullPointerException e) {
            Logger.error("MainService revokeAdminOwner NullPointerException: " + e.toString());
        } catch (SecurityException e) {
            Logger.error("MainService revokeAdminOwner SecurityException: " + e.toString());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void settingsSync(SyncSettings event) {
        Logger.debug("MainService SyncSettings event received");
        SettingsData settingsData = SettingsData.fromDataBundle(event.getDataBundle());
        settingsManager.sync(settingsData);

        //Toggle phone connect/disconnect monitor if settings changed
        boolean iPCA = settingsData.isPhoneConnectionAlert();
        isStandardAlertEnabled = settingsData.isPhoneConnectionAlertStandardNotification();
        if (isPhoneConnectionAlertEnabled != iPCA)
            registerConnectionMonitor(iPCA);

        //Toggle AmazMod keep widget setting
        iPCA = settingsData.isAmazModKeepWidget();
        settings.reload();
        settings.set(Constants.PREF_AMAZMOD_KEEP_WIDGET, iPCA);
        if (isSpringboardObserverEnabled != iPCA)
            registerSpringBoardMonitor(iPCA);

        //Request self reload to update languages string
        iPCA = settingsData.isRequestSelfReload();
        Logger.debug("MainService SyncSettings isRequestSelfReload: {}", iPCA);
        settings.reload();
        settings.set(Constants.REQUEST_SELF_RELOAD, iPCA);
        if (requestSelfReload != iPCA)
            DeviceUtil.killBackgroundTasks(context, iPCA);

        //TODO Toggle AmazMod keep weather setting
        /*
        iPCA = settingsData.isAmazModKeepWeather();
        settings.reload();
        settings.set(Constants.PREF_AMAZMOD_KEEP_WEATHER, iPCA);
        if (isWeatherObserverEnabled != iPCA)
            registerWeatherMonitor(iPCA);
         */

        //Toggle OverlayLauncher service
        iPCA = settingsData.isOverlayLauncher();
        Logger.debug("MainService SyncSettings isOverlayLauncher: {}", iPCA);
        if (iPCA != settings.get(Constants.PREF_AMAZMOD_OVERLAY_LAUNCHER, false))
            setOverlayLauncher(iPCA);

        //Toggle Hourly Chime
        iPCA = settingsData.isHourlyChime();
        Logger.debug("SyncSettings isHourlyChime: {}", iPCA);
        if (iPCA != ((settings.get(Constants.PREF_AMAZMOD_HOURLY_CHIME, false)) || (WearMenuFragment.chimeEnabled)))
            setHourlyChime(iPCA);

        setupHardwareKeysMusicControl(settingsData.isEnableHardwareKeysMusicControl());
    }

    // Reply to notification
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void reply(ReplyNotificationEvent event) {
        Logger.debug("MainService reply to notification, key: " + event.getKey() + ", message: " + event.getMessage());
        // Send transmit
        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("key", event.getKey()); // Notification unique key
        dataBundle.putString("message", event.getMessage()); // Reply message
        send(Transport.REPLY, dataBundle);
    }

    // Silence specific app notifications for X minutes
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void silence(SilenceApplicationEvent event) {
        Logger.debug("MainService silence application, package: " + event.getPackageName() + ", minutes: " + event.getMinutes());
        // Send transmit
        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("package", event.getPackageName()); // app pkg
        dataBundle.putString("minutes", event.getMinutes()); // minutes to stop notifications
        send(Transport.SILENCE, dataBundle);
    }

    // Incoming Notification action
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void incomingNotification(IncomingNotificationEvent incomingNotificationEvent) {
        // Create notification data
        NotificationData notificationData = NotificationData.fromDataBundle(incomingNotificationEvent.getDataBundle());

        // Set vibration data
        // Changed for RC1
        if (notificationData.getVibration() > 0) {
            Logger.debug("MainService incomingNotification vibration: " + notificationData.getVibration());
        } else notificationData.setVibration(0);
        //notificationData.setVibration(settingsManager.getInt(Constants.PREF_NOTIFICATION_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATION_VIBRATION));

        // Set notification duration
        notificationData.setTimeoutRelock(settingsManager.getInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, Constants.PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT));
        // Check if device is locked
        notificationData.setDeviceLocked(DeviceUtil.isDeviceLocked(context));

        Logger.debug("MainService incomingNotification: " + notificationData.toString());
        notificationManager.post(notificationData);
    }

    // Watch Info/Status request
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestWatchStatus(RequestWatchStatus requestWatchStatus) {
        // Get watch info
        watchStatusData.setAmazModServiceVersion(BuildConfig.VERSION_NAME);
        //watchStatusData.setRoBuildDate(SystemProperties.get(WatchStatusData.RO_BUILD_DATE, "-"));
        //watchStatusData.setRoBuildHuamiNumber(SystemProperties.get(WatchStatusData.RO_BUILD_HUAMI_NUMBER, "-"));
        //watchStatusData.setRoProductDevice(SystemProperties.get(WatchStatusData.RO_PRODUCT_DEVICE, "-"));
        //watchStatusData.setRoProductManufacter(SystemProperties.get(WatchStatusData.RO_PRODUCT_MANUFACTER, "-"));
        //watchStatusData.setRoRevision(SystemProperties.get(WatchStatusData.RO_REVISION, "-"));
        //watchStatusData.setRoBuildFingerprint(SystemProperties.get(WatchStatusData.RO_BUILD_FINGERPRINT, "-"));
        watchStatusData.setRoSerialno(SystemProperties.get(WatchStatusData.RO_SERIALNO, "-"));
        watchStatusData.setRoBuildDisplayId(SystemProperties.get(WatchStatusData.RO_BUILD_DISPLAY_ID, "-"));
        if (!isStratos3()) { // todo reduce data until we fix Stratos 3 connection
            watchStatusData.setRoBuildHuamiModel(SystemProperties.get(WatchStatusData.RO_BUILD_HUAMI_MODEL, "-"));
            //watchStatusData.setRoBuildDescription(SystemProperties.get(WatchStatusData.RO_BUILD_DESCRIPTION, "-"));
            watchStatusData.setRoProductModel(SystemProperties.get(WatchStatusData.RO_PRODUCT_MODEL, "-"));
            watchStatusData.setRoProductName(SystemProperties.get(WatchStatusData.RO_PRODUCT_NAME, "-"));
        }
        // Get brightness
        int b = 0;
        int bm = Constants.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        try {
            b = DeviceUtil.systemGetInt(context, Constants.SCREEN_BRIGHTNESS);
            bm = DeviceUtil.systemGetInt(context, Constants.SCREEN_BRIGHTNESS_MODE);
        } catch (Settings.SettingNotFoundException e) {
            Logger.error("MainService requestWatchStatus SettingsNotFoundException: {}" + e.getMessage());
        }
        watchStatusData.setScreenBrightness(b);
        watchStatusData.setScreenBrightnessMode(bm);

        // Check if rooted
        if( new File("/system/xbin/su").exists() )
            watchStatusData.setRooted(1);

        // Get last heart rates
        final boolean isHeartrateData = settingsManager.getBoolean(Constants.PREF_HEARTRATE_DATA, true);
        if (isHeartrateData) {
            Cursor cur = null;
            String heartRates = "";
            try {
                cur = getContentResolver().query(Uri.parse("content://com.huami.watch.health.heartdata"), null, null, null, "utc_time ASC");
                // Use the cursor to step through the returned records
                while (cur != null && cur.moveToNext()) {
                    // Get the field values
                    // example: utc_time=1528485660, time_zone=0, heart_rate=96
                    long utc_time;
                    utc_time = (cur.getLong(0)) * (isStratos3()?1000:1);
                    //int time_zone = cur.getInt(1);
                    int heart_rate = cur.getInt(2);

                    heartRates += utc_time + "," + heart_rate + ",";
                }
                if(cur != null) cur.close();
            } catch (SecurityException e) {
                //Getting data error
            }

            if(!heartRates.equals("")) // send only if there are data
                watchStatusData.setLastHeartRates(heartRates);
        }

        // Get hourly chime status
        settings.reload();
        boolean isHourlyChime = settings.get(Constants.PREF_AMAZMOD_HOURLY_CHIME, false);
        Logger.debug("Sync hourly chime to transport : " + isHourlyChime);
        if(isHourlyChime) // send only if there are data
            watchStatusData.setHourlyChime(1); // 1 = on, 0 = off

        // Send transmit
        Logger.debug("MainService requestWatchStatus watchStatusData: " + watchStatusData.toString());
        send(Transport.WATCH_STATUS, watchStatusData.toDataBundle());
    }

    // Battery request
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestBatteryStatus(RequestBatteryStatus requestBatteryStatus) {
        Intent batteryStatus = context.registerReceiver(null, batteryFilter);
        if (batteryStatus == null) {
            Logger.error("MainService requestBatteryStatus: register receiver error!");
            return;
        }

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        getBatteryPct(batteryStatus);

        // Get data of last full charge from settings
        //Use WidgetSettings to share data with Springboard widget (SharedPreferences didn't work)
        if (dateLastCharge == 0) {
            settings.reload();
            dateLastCharge = settings.get(Constants.PREF_DATE_LAST_CHARGE, 0L);
            Logger.debug("MainService dateLastCharge loaded: " + dateLastCharge);
        }

        Logger.debug("MainService dateLastCharge: " + dateLastCharge + " | batteryPct: " + Math.round(batteryPct * 100f));
        cancelPendingJobs(BATTERY_JOB_ID);
        saveBatteryDb(batteryPct, false);

        batteryData.setLevel(batteryPct);
        batteryData.setCharging(isCharging);
        batteryData.setUsbCharge(usbCharge);
        batteryData.setAcCharge(acCharge);
        batteryData.setDateLastCharge(dateLastCharge);

        send(Transport.BATTERY_STATUS, batteryData.toDataBundle());
    }

    // Set brightness
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void brightness(Brightness brightness) {
        BrightnessData brightnessData = BrightnessData.fromDataBundle(brightness.getDataBundle());
        final int brightnessLevel = brightnessData.getLevel();
        Logger.debug("MainService setting brightness to " + brightnessLevel);

        // Save new brightness
        if (brightnessLevel == -1)
            // Automatic
            DeviceUtil.systemPutInt(context, Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
        else {
            DeviceUtil.systemPutInt(context, Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
            DeviceUtil.systemPutInt(context, Settings.System.SCREEN_BRIGHTNESS, brightnessLevel);
        }
    }

    // Hardware button event
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void hardwareButton(HardwareButtonEvent hardwareButtonEvent) {
        if (hardwareButtonEvent.getCode() == MusicControlInputListener.KEY_DOWN) {
            if (hardwareButtonEvent.isLongPress()) {
                // Long Press
                send(Transport.NEXT_MUSIC);
            } else {
                // Short press trigger
                send(Transport.TOGGLE_MUSIC);
            }
        }
    }

    // Directory request
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestDirectory(RequestDirectory requestDirectory) {
        try {
            RequestDirectoryData requestDirectoryData = RequestDirectoryData.fromDataBundle(requestDirectory.getDataBundle());
            String path = requestDirectoryData.getPath();
            Logger.debug("path: " + path);
            DirectoryData directoryData = getFilesByPath(path);
            send(Transport.DIRECTORY, directoryData.toDataBundle());
        } catch (Exception ex) {
            DirectoryData directoryData = new DirectoryData();
            directoryData.setResult(Transport.RESULT_UNKNOW_ERROR);
            send(Transport.DIRECTORY, directoryData.toDataBundle());
        }
    }

    // Delete file
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestDeleteFile(RequestDeleteFile requestDeleteFile) {
        ResultDeleteFileData resultDeleteFileData = new ResultDeleteFileData();

        try {
            RequestDeleteFileData requestDeleteFileData = RequestDeleteFileData.fromDataBundle(requestDeleteFile.getDataBundle());
            File file = new File(requestDeleteFileData.getPath());
            int result = file.delete() ? Transport.RESULT_OK : Transport.RESULT_UNKNOW_ERROR;
            resultDeleteFileData.setResult(result);

            // If music file is deleted, inform MediaStore's Content Provider
            if(result == Transport.RESULT_OK)
                informMediaProvider(file,true);
        } catch (SecurityException securityException) {
            resultDeleteFileData.setResult(Transport.RESULT_PERMISSION_DENIED);
        } catch (Exception ex) {
            resultDeleteFileData.setResult(Transport.RESULT_UNKNOW_ERROR);
        }

        send(Transport.RESULT_DELETE_FILE, resultDeleteFileData.toDataBundle());
    }

    // Upload file chunk
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestUploadFileChunk(RequestUploadFileChunk requestUploadFileChunk) {
        try {
            RequestUploadFileChunkData requestUploadFileChunkData = RequestUploadFileChunkData.fromDataBundle(requestUploadFileChunk.getDataBundle());
            File file = new File(requestUploadFileChunkData.getPath());
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            long position = requestUploadFileChunkData.getIndex() * requestUploadFileChunkData.getConstantChunkSize();
            randomAccessFile.seek(position);
            randomAccessFile.write(requestUploadFileChunkData.getBytes());
            randomAccessFile.close();

            // Check is file transfer has finished (last chunk less than the others)
            if ( requestUploadFileChunkData.getSize() < requestUploadFileChunkData.getConstantChunkSize() )
                informMediaProvider(file,false);

        } catch (Exception ex) {
            Logger.error(ex.getMessage());
        }
    }

    // Inform MediaStore's Content Provider about music file
    public void informMediaProvider(File file, boolean delete) {
        // todo Music files are not shown in the music app
        // If music file, inform MediaStore's Content Provider
        String filename = file.getName().toLowerCase();
        if ( filename.endsWith(".mp3") || filename.endsWith(".m4a") ) {
            Uri uri = Uri.fromFile(file);
            Logger.debug("Music file, informing MediaStore's Content Provider: " + uri);
            try {
                if (delete) {
                    // File deleted
                    getContentResolver().delete(uri, null, null);
                }else{
                    // New File
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                }
            } catch (Exception ex) {
                Logger.warn("Music file, informing MediaStore's Content Provider error: "+ex);
            }
        }
    }

    // Download file chunk
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestDownloadFileChunk(RequestDownloadFileChunk requestDownloadFileChunk) {
        try {
            RequestDownloadFileChunkData requestDownloadFileChunkData = RequestDownloadFileChunkData.fromDataBundle(requestDownloadFileChunk.getDataBundle());
            File file = new File(requestDownloadFileChunkData.getPath());
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

            long chunkSize = amazmod.com.transport.Constants.CHUNK_SIZE;
            long position = requestDownloadFileChunkData.getIndex() * chunkSize;
            randomAccessFile.seek(position);

            long delta = file.length() - position;
            int byteToRead = (delta < chunkSize) ? (int) delta : (int) chunkSize;
            byte[] bytes = new byte[byteToRead];

            randomAccessFile.read(bytes);

            ResultDownloadFileChunkData resultDownloadFileChunkData = new ResultDownloadFileChunkData();
            resultDownloadFileChunkData.setIndex(requestDownloadFileChunkData.getIndex());
            resultDownloadFileChunkData.setBytes(bytes);
            resultDownloadFileChunkData.setName(file.getName());

            send(Transport.RESULT_DOWNLOAD_FILE_CHUNK, resultDownloadFileChunkData.toDataBundle());
        } catch (Exception ex) {
            Logger.error(ex.getMessage());
        }
    }

    // New Shell Command to execute
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void executeShellCommand(final RequestShellCommand requestShellCommand) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    RequestShellCommandData requestShellCommandData = RequestShellCommandData.fromDataBundle(requestShellCommand.getDataBundle());
                    String command = requestShellCommandData.getCommand();
                    boolean isAdb = command.contains("adb ");

                    if (!requestShellCommandData.isWaitOutput()) {

                        Logger.debug("MainService executeShellCommand !isWaitOutput command: {}", command);
                        int code = 0;
                        String errorMsg = "";

                        if (command.contains("install_apk ")) {

                            String apkFile = command.replace("install_apk ", "");
                            final File apk = new File(apkFile);

                            if (apk.exists()) {
                                if (apkFile.contains("service-")) {
                                    showConfirmationWearActivity(getString(R.string.service_update), "0");
                                    //new ExecCommand("adb shell settings put system screen_off_timeout 200000");
                                    DeviceUtil.systemPutInt(context, Settings.System.SCREEN_OFF_TIMEOUT, 200000);
                                    Thread.sleep(1000);
                                    new ExecCommand("adb install -r -d " + apkFile);

                                } else {
                                    showConfirmationWearActivity(getString(R.string.installing_apk), "0");
                                    myWakeLock = DeviceUtil.installApkAdb(context, apk, requestShellCommandData.isReboot());
                                }

                            } else {
                                code = -1;
                                errorMsg = String.format("%s" + getString(R.string.not_found), apkFile);
                            }

                        } else if (command.contains("install_amazmod_update ")){
                            showConfirmationWearActivity(getString(R.string.service_update), "0");
                            DeviceUtil.installPackage(context, getPackageName(), command.replace("install_amazmod_update ", ""));

                        } else {

                            if (requestShellCommandData.isReboot()) {
                                File commandFile = new File("/sdcard/amazmod-command.sh");
                                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(commandFile));
                                outputStreamWriter.write(command + " && reboot");
                                outputStreamWriter.flush();
                                outputStreamWriter.close();
                                Process process = Runtime.getRuntime().exec("nohup sh /sdcard/amazmod-command.sh &");

                                code = process.waitFor();
                                Logger.debug("MainService shell process returned code: {}", code);

                            } else {
                                if (command.contains("AmazMod-service-") && command.contains("adb install -r")) {
                                    showConfirmationWearActivity(getString(R.string.service_update), "0");
                                    Thread.sleep(3000);
                                }
                                ExecCommand execCommand = new ExecCommand(command);
                                errorMsg = execCommand.getError();
                                code = execCommand.getResult();
                            }
                        }
                        ResultShellCommandData resultShellCommand = new ResultShellCommandData();
                        resultShellCommand.setResult(code);
                        resultShellCommand.setOutputLog("");
                        resultShellCommand.setErrorLog(errorMsg);
                        send(Transport.RESULT_SHELL_COMMAND, resultShellCommand.toDataBundle());

                    } else {

                        String filename = null;
                        PowerManager.WakeLock wakeLock = null;

                        if (command.contains("screencap")) {
                            Logger.debug("Screenshot: creating dirs");

                            File file = new File("/sdcard/Pictures/Screenshots");
                            boolean saveDirExists;
                            saveDirExists = file.exists() || file.mkdirs();
                            if (saveDirExists) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
                                String dateStamp = sdf.format(new Date());
                                filename = "/sdcard/Pictures/Screenshots/ss_" + dateStamp + ".png";
                                command = command + " " + filename;

                                // Check is screen is locked
                                if (DeviceUtil.isDeviceLocked(context)) {
                                    Logger.debug("Screenshot: trying to wake up screen...");
                                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                                    wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                                            | PowerManager.ACQUIRE_CAUSES_WAKEUP
                                            | PowerManager.ON_AFTER_RELEASE, "AmazMod:Screenshot");

                                    wakeLock.acquire(10 * 1000L /*10 seconds*/);

                                }
                            }
                        }

                        Logger.debug("MainService executeShellCommand isWaitOutput command: {}", command);
                        long startedAt = System.currentTimeMillis();
                        ResultShellCommandData resultShellCommand = new ResultShellCommandData();
                        int returnValue;

                        if (isAdb) {
                            //adb hangs if waitFor is used
                            ExecCommand execCommand = new ExecCommand(command);
                            Thread.sleep(1000);
                            resultShellCommand.setResult(execCommand.getResult());
                            resultShellCommand.setOutputLog(execCommand.getOutput());
                            resultShellCommand.setErrorLog(execCommand.getError());

                        } else {

                            /* Disabled while testing new ExecCommand class
                            String[] args = CommandLine.translateCommandline(command);
                            ProcessBuilder processBuilder = new ProcessBuilder(args);
                            processBuilder.redirectErrorStream(true);

                            Process process = processBuilder.start();
                            StringBuilder outputLog = new StringBuilder();

                            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String line;
                                while ((line = bufferedReader.readLine()) != null) {
                                    outputLog.append(line).append("\n");
                                }
                                returnValue = process.waitFor();
                            }
                            */
                            ExecCommand execCommand = new ExecCommand(ExecCommand.MAKE_ARRAY, command);
                            returnValue = execCommand.getResult();

                            resultShellCommand.setResult(returnValue);
                            //resultShellCommand.setOutputLog(outputLog.toString());
                            resultShellCommand.setOutputLog(execCommand.getOutput());
                            resultShellCommand.setErrorLog(execCommand.getError());
                        }

                        resultShellCommand.setDuration(System.currentTimeMillis() - startedAt);
                        resultShellCommand.setCommand(command);

                        send(Transport.RESULT_SHELL_COMMAND, resultShellCommand.toDataBundle());

                        if (command.contains("screencap")) {
                            if (filename != null) {
                                File file = new File(filename);
                                if (file.exists()) {
                                    Logger.debug("MainService executeShellCommand file: {} exists: {}", file.getName(), file.exists());
                                    FileUploadData fileUploadData = new FileUploadData(file.getAbsolutePath(), file.getName(), file.length());
                                    send(Transport.FILE_UPLOAD, fileUploadData.toDataBundle());
                                }
                            }
                        }

                        if (wakeLock != null && wakeLock.isHeld())
                            wakeLock.release();
                    }
                } catch (Exception ex) {
                    Logger.error(ex, ex.getMessage());

                    ResultShellCommandData resultShellCommand = new ResultShellCommandData();
                    resultShellCommand.setResult(-1);
                    resultShellCommand.setOutputLog("");
                    resultShellCommand.setErrorLog(ex.getMessage());

                    send(Transport.RESULT_SHELL_COMMAND, resultShellCommand.toDataBundle());
                }
            }
        }).start();
    }

    // Save battery to database
    public static boolean saveBatteryDb(float batteryPct, boolean updateSettings) {
        settings.reload();
        long date = System.currentTimeMillis();
        boolean result = false;
        Logger.debug("MainService saveBatteryDb date: " + date + " \\ batteryPct: " + batteryPct);

        BatteryDbEntity batteryStatusEntity = new BatteryDbEntity();
        batteryStatusEntity.setDate(date);
        batteryStatusEntity.setLevel(batteryPct);

        try {
            BatteryDbEntity storeBatteryStatusEntity = SQLite
                    .select()
                    .from(BatteryDbEntity.class)
                    .where(BatteryDbEntity_Table.date.is(date))
                    .querySingle();

            if (storeBatteryStatusEntity == null) {
                FlowManager.getModelAdapter(BatteryDbEntity.class).insert(batteryStatusEntity);
            }

            settings.set(Constants.PREF_DATE_LAST_BATTERY_SYNC, date);
            result = true;

        } catch (Exception ex) {
            Logger.error("MainService saveBatteryDb exception: " + ex.toString());
        }

        // Update battery level (used in widget)
        if (updateSettings)
            settings.set(Constants.PREF_BATT_LEVEL, Integer.toString(Math.round(batteryPct * 100f)) + "%");

        return result;
    }

    private void cancelPendingJobs(int id) {
        List<JobInfo> jobInfoList = jobScheduler.getAllPendingJobs();
        final int pendingJobs = jobInfoList.size();
        Logger.debug("MainService cancelPendingJobs pendingJobs: " + pendingJobs);
        if (pendingJobs > 0)
            for (JobInfo jobInfo : jobInfoList) {
                Logger.debug("MainService cancelPendingJobs jobInfo: " + jobInfo.toString());
                if (jobInfo.getId() == id)
                    jobScheduler.cancel(id);
            }
    }

    private void showConfirmationWearActivity(String message, String time) {
        Intent intent = new Intent(context, ConfirmationWearActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.TEXT, message);
        intent.putExtra(Constants.TIME, time);
        context.startActivity(intent);
    }

    private DirectoryData getFilesByPath(String path) {
        File directory = new File(path);

        if (!directory.exists()) {
            return FileDataFactory.notFound();
        }

        ArrayList<FileData> filesData = new ArrayList<>();
        File[] files = directory.listFiles();

        if(files==null)
            return FileDataFactory.directoryFromFile(directory, filesData);

        for (File file : files) {
            FileData fileData = FileDataFactory.fromFile(file);
            filesData.add(fileData);
        }

        return FileDataFactory.directoryFromFile(directory, filesData);
    }

    private MusicControlInputListener musicControlInputListener;

    private void setupHardwareKeysMusicControl(boolean enable) {
        if (enable) {
            if (musicControlInputListener == null) {
                musicControlInputListener = new MusicControlInputListener();
            }

            if (!musicControlInputListener.isListening()) {
                musicControlInputListener.start(this);
            }
        } else {
            if (musicControlInputListener != null) {
                musicControlInputListener.stop();
            }
        }
    }

    private void send(String action) {
        send(action, null);
    }

    private void send(String action, DataBundle dataBundle) {
        if (!transporterGeneral.isTransportServiceConnected()) {
            Logger.debug("MainService Transport Service Not Connected");
            return;
        }

        if (dataBundle != null) {
            Logger.debug("MainService send1: " + action);
            transporterGeneral.send(action, dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Logger.debug("Send result: " + dataTransportResult.toString());
                }
            });

        } else {
            Logger.debug("MainService send2: " + action);
            transporterGeneral.send(action, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Logger.debug("Send result: " + dataTransportResult.toString());
                }
            });
        }
    }

    private void sendHuami(String action, DataBundle dataBundle) {
        if (!transporterHuami.isTransportServiceConnected()) {
            Logger.debug("MainService TransporterHuami Not Connected, returning...");
            return;
        }

        if (dataBundle != null) {
            Logger.debug("MainService sendHuami: " + action);
            transporterHuami.send(action, dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Logger.debug("SendHuami result: " + dataTransportResult.toString());
                }
            });
        }
    }

    private void registerConnectionMonitor(boolean status) {
        Logger.debug("MainService registerConnectionMonitor status: {}", status);

        if (status) {

            if (phoneConnectionObserver != null)
                return;

            ContentResolver contentResolver = getContentResolver();
            Uri setting = Settings.System.getUriFor("com.huami.watch.extra.DEVICE_CONNECTION_STATUS");

            phoneConnectionObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);

                    final String connectionStatus = DeviceUtil.systemGetString(context,
                            "com.huami.watch.extra.DEVICE_CONNECTION_STATUS");

                    Logger.warn("MainService registerConnectionMonitor onChange status: {}", connectionStatus);

                    if ("0".equals(connectionStatus) && !SystemProperties.isAirplaneModeOn(getApplicationContext()))
                        saveDisconnectionLog();

                    if (isStandardAlertEnabled) {

                        Logger.trace("MainService registerConnectionMonitor send standardAlert");
                        sendStandardAlert("phone_connection");

                    } else {

                        if ("1".equals(connectionStatus) && isRunning) {
                            isRunning = false;
                            cstHandler.removeCallbacks(sendAlertNotification);
                            Logger.trace("MainService registerConnectionMonitor canceling customAlert");

                        } else if ("1".equals(connectionStatus)) {
                            Logger.trace("MainService registerConnectionMonitor sending connected customAlert");
                            cstHandler.postDelayed(sendAlertNotification, 100);
                            isRunning = true;

                        } else if ("0".equals(connectionStatus)) {
                            Logger.trace("MainService registerConnectionMonitor queueing disconnected customAlert");
                            cstHandler.postDelayed(sendAlertNotification, 1800);
                            isRunning = true;
                        }

                    }
                }

                @Override
                public boolean deliverSelfNotifications() {
                    return true;
                }
            };
            contentResolver.registerContentObserver(setting, false, phoneConnectionObserver);

        } else {

            if (phoneConnectionObserver != null)
                getContentResolver().unregisterContentObserver(phoneConnectionObserver);
            phoneConnectionObserver = null;

        }
        isPhoneConnectionAlertEnabled = status;
    }

    private void sendStandardAlert(String alert_type) {

        final String notificationTime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Calendar.getInstance().getTime());
        final String connectionStatus = DeviceUtil.systemGetString(context, "com.huami.watch.extra.DEVICE_CONNECTION_STATUS");

        notificationData = new NotificationData();

        notificationData.setId(9979);
        notificationData.setKey("amazmod|test|9979");
        notificationData.setTime(notificationTime);
        notificationData.setVibration(0);
        notificationData.setForceCustom(false);
        notificationData.setHideReplies(true);
        notificationData.setHideButtons(false);

        final Drawable drawable;

        switch (alert_type) {
            case "phone_battery":
                notificationData.setTitle(getString(R.string.phone_battery_alert));
                drawable = getDrawable(R.drawable.ic_battery_alert_black_24dp);
                notificationData.setText(getString(R.string.phone_battery, settingsManager.getInt(Constants.PREF_BATTERY_PHONE_ALERT, 0) + "%"));
                vibrate = Constants.VIBRATION_SHORT;
                break;
            case "watch_battery":
                notificationData.setTitle(getString(R.string.watch_battery_alert));
                drawable = getDrawable(R.drawable.ic_battery_alert_black_24dp);
                notificationData.setText(getString(R.string.watch_battery, settingsManager.getInt(Constants.PREF_BATTERY_PHONE_ALERT, 0) + "%"));
                vibrate = Constants.VIBRATION_SHORT;
                break;
            case "phone_connection":
            default:
                // type= phone_connection
                notificationData.setTitle(getString(R.string.phone_connection_alert));
                if (connectionStatus.equals("0")) {             // Phone disconnected
                    //Return if Airplane mode is enabled
                    if (SystemProperties.isAirplaneModeOn(getApplicationContext())) {
                        Logger.trace("MainService sendStandardAlert Airplane mode is on, returning…");
                        return;
                    }
                    setNotificationIcon(getDrawable(R.drawable.ic_outline_phonelink_erase));
                    notificationData.setText(getString(R.string.phone_disconnected));
                    vibrate = Constants.VIBRATION_LONG;

                    stdHandler.postDelayed(sendStandardNotification, 1800);
                    isRunning = true;
                    Logger.trace("MainService sendStandardAlert send disconnected alert");

                } else {                                        // Phone connected
                    //Don't send notification if it was disconnected less than 1,5s ago
                    if (isRunning) {
                        stdHandler.removeCallbacks(sendStandardNotification);
                        isRunning = false;
                        Logger.trace("MainService sendStandardAlert cancel alerts");
                        return;
                    }
                    setNotificationIcon(getDrawable(R.drawable.ic_outline_phonelink_ring));
                    notificationData.setText(getString(R.string.phone_connected));
                    vibrate = Constants.VIBRATION_SHORT;

                    stdHandler.postDelayed(sendStandardNotification, 100);
                    isRunning = true;
                    Logger.trace("MainService sendStandardAlert send connected alert");
                }
        }
    }

    private void setNotificationIcon(Drawable drawable) {
        try {
            Bitmap bitmap = drawableToBitmap(drawable);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] intArray = new int[width * height];
            bitmap.getPixels(intArray, 0, width, 0, 0, width, height);

            notificationData.setIcon(intArray);
            notificationData.setIconWidth(width);
            notificationData.setIconHeight(height);
        } catch (Exception e) {
            notificationData.setIcon(new int[]{});
            Logger.error("MainService sendStandardAlert exception: " + e.toString());
        }
    }

    private void acquireWakelock() {
        Logger.debug("MainService acquireWakelock");
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            PowerManager.WakeLock wakeLockScreenOn = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP, "AmazMod::NotificationScreenOn");
            if (!wakeLockScreenOn.isHeld())
                wakeLockScreenOn.acquire(9*1000L /* 9s */);
        } else
            Logger.error("MainService aquireWakelock null powerManager!");
    }

    // Widgets update monitor/register
    private void registerSpringBoardMonitor(boolean status) {
        Logger.debug("MainService registerSpringBoardMonitor status: " + status);

        // Register
        if (status) {
            // Check if defined already
            if (springboardObserver != null)
                return;

            // Save default order
            String widget_order_in = DeviceUtil.systemGetString(context, Constants.WIDGET_ORDER_IN);
            WidgetsUtil.saveOfficialAppOrder(context, widget_order_in);

            // Boolean to avoid self-caused re-run loop
            wasSpringboardSaved = false;

            springboardObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    Logger.debug("MainService registerSpringBoardMonitor onChange");

                    if (!wasSpringboardSaved) {
                        // Update widgets list
                        WidgetsUtil.syncWidgets(context);
                    } else
                        wasSpringboardSaved = false;
                }

                @Override
                public boolean deliverSelfNotifications() {
                    return true;
                }
            };

            // Observe changes in the WIDGET_ORDER_IN system variable
            ContentResolver contentResolver = getContentResolver();
            Uri setting = Settings.System.getUriFor(Constants.WIDGET_ORDER_IN);
            contentResolver.registerContentObserver(setting, true, springboardObserver);

        // Unregister
        } else {
            if (springboardObserver != null)
                getContentResolver().unregisterContentObserver(springboardObserver);
            springboardObserver = null;
        }

        isSpringboardObserverEnabled = status;
    }

    // Weather update monitor/register
    private void registerWeatherMonitor(boolean status) {
        Logger.debug("MainService registerWeatherMonitor status: " + status);

        // Register
        if (status) {
            // Check if defined already
            if (weatherObserver != null)
                return;

            // Boolean to avoid self-caused re-run loop (default value here)
            wasWeatherSaved = false;

            // function that runs when the system variable is changed
            weatherObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    //Logger.debug("MainService registerWeatherMonitor onChange");

                    // Check if Amazmod just changed the value
                    if (!wasWeatherSaved) {
                        // Update custom weather
                        String weather_data = settingsManager.getString(Constants.WEATHER_INFO, "");

                        // Check if data are up-to-date
                        if(!weather_data.isEmpty() && System.currentTimeMillis()<=custom_weather_expire ){

                            // Update system weather data
                            String save_value = Weather.updateWeatherData(context, weather_data);
                            if(!save_value.equals(Weather.DATA_HAVE_NOT_UPDATE)) {
                                // Save new data
                                settingsManager.putString(Constants.WEATHER_INFO, save_value);
                                // Save that Amazmod just updated the system value (used when overwriting system weather values)
                                wasWeatherSaved = true;
                                Logger.debug("[Weather Data Monitor] Weather data were overwritten.");
                            }else{
                                Logger.debug("[Weather Data Monitor]  Weather data were NOT overwritten.");
                            }
                        }else{
                            Logger.debug("[Weather Data Monitor]  Weather data were NOT overwritten since saved data have expire.");
                        }
                    } else
                        wasWeatherSaved = false;
                }

                @Override
                public boolean deliverSelfNotifications() {
                    return true;
                }
            };

            // Observe changes in the WeatherInfo system variable
            ContentResolver contentResolver = getContentResolver();
            Uri setting = Settings.System.getUriFor( Weather.getWeatherMonitorParameter() );
            contentResolver.registerContentObserver(setting, true, weatherObserver);

            // Unregister
        } else {
            if (weatherObserver != null)
                getContentResolver().unregisterContentObserver(weatherObserver);
            weatherObserver = null;
        }

        isWeatherObserverEnabled = status;
    }

    private void setOverlayLauncher(boolean status){
        Logger.debug("MainService setOverlayLauncher status: {}", status);

        final Intent overlayButton = new Intent(context, OverlayLauncher.class);

        if (status) {
            // Register
            startService(overlayButton);
            final IntentFilter screenOnFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            screenOnFilter.addAction(Intent.ACTION_SCREEN_OFF);
            screenOnFilter.addAction(Intent.ACTION_USER_PRESENT);
            screenOnReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Logger.debug("MainService setOverlayLauncher receiver action: {}", action);
                    if (Intent.ACTION_SCREEN_ON.equals(action))
                        context.startService(overlayButton);
                    else if (Intent.ACTION_SCREEN_OFF.equals(action))
                        context.stopService(overlayButton);
                }};
            context.registerReceiver(screenOnReceiver, screenOnFilter);

        } else {
            // Unregister
            if (screenOnReceiver != null) {
                context.unregisterReceiver(screenOnReceiver);
                screenOnReceiver = null;
            }
            context.stopService(overlayButton);
        }
        settings.set(Constants.PREF_AMAZMOD_OVERLAY_LAUNCHER, status);
    }

    public void setHourlyChime(boolean status){
        Logger.debug("setHourlyChime status: {}", status);

        HourlyChime.setHourlyChime(context,status);
        WearMenuFragment.chimeEnabled = status;

        settings.set(Constants.PREF_AMAZMOD_HOURLY_CHIME, status);
    }

    private void saveDisconnectionLog() {
        final int lines = settingsManager.getInt(Constants.PREF_LOG_LINES, Constants.PREF_DEFAULT_LOG_LINES);
        if (BuildConfig.VERSION_NAME.contains("dev") && lines > 128) {
            File file = new File("/sdcard/Logs");
            boolean saveDirExists;
            saveDirExists = file.exists() || file.mkdirs();
            if (saveDirExists) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                String dateStamp = sdf.format(new Date());
                String filename = "/sdcard/Logs/disconnection_log_" + dateStamp + ".txt";
                new ExecCommand(ExecCommand.ADB, "adb shell logcat -d -t " + String.valueOf(lines) + " -v long -f " + filename);
                Logger.error("**** Disconnected log saved ****");
            }
        }
    }

    public static boolean getWasSpringboardSaved() {
        return wasSpringboardSaved;
    }

    public static void setWasSpringboardSaved(boolean b) {
        wasSpringboardSaved = b;
    }

    public static char getOverlayLauncherPosition() {
        return overlayLauncherPosition;
    }

    public static void setOverlayLauncherPosition(char position) {
        overlayLauncherPosition = position;
    }

    public static boolean isNotification() {
        return notificationArrived;
    }

    public static void setIsNotification(boolean bol) {
        notificationArrived = bol;
    }

    public static void apkInstallFinish() {
        if (myWakeLock != null && myWakeLock.isHeld()) {
            myWakeLock.release();
            Logger.trace("wakelock released");
        }
    }

    // Update battery percentage (%)
    private void getBatteryPct(Intent batteryStatus) {
        if (batteryStatus == null)
            return;

        // Get battery data
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Update battery value
        this.batteryPct = level / (float) scale;

        // Watch Battery Alert
        // THIS WAS MOVED TO A PHONE NOTIFICATION
        /*
        if( settingsManager.getInt(Constants.PREF_BATTERY_WATCH_ALERT, 0) > 0 ){
            if( settingsManager.getInt(Constants.PREF_BATTERY_WATCH_ALERT, 0) >= Math.round(batteryPct * 100f) ){
                if(!watchBatteryAlreadyAlerted) { // Pass only if NOT already alerted
                    if (isStandardAlertEnabled) {
                        // Show standard battery alert
                        sendStandardAlert("watch_battery");
                    } else {
                        // Show battery alert
                        Intent intent = new Intent(context, AlertsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        intent.putExtra("type", "watch_battery");
                        context.startActivity(intent);
                    }
                    watchBatteryAlreadyAlerted = true;
                }
            }else{
                // When battery is above the alert level
                watchBatteryAlreadyAlerted = false;
            }
        }
        */
    }
}
