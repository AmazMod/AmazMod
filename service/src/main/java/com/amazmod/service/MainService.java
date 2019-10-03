package com.amazmod.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.app.NotificationCompat;
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
import com.amazmod.service.receiver.AlarmReceiver;
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
import java.lang.reflect.InvocationTargetException;
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

import static com.amazmod.service.util.FileDataFactory.drawableToBitmap;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class MainService extends Service implements Transporter.DataListener {

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

    private static Transporter transporterGeneral, transporterNotifications, transporterHuami;

    private static IntentFilter batteryFilter;
    private static long dateLastCharge;
    private static int count = 0;
    private static boolean isPhoneConnectionAlertEnabled;
    private static boolean isStandardAlertEnabled;
    private static boolean isSpringboardObserverEnabled;
    private static boolean wasSpringboardSaved;
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

    private boolean watchBatteryAlreadyAlerted;
    private boolean phoneBatteryAlreadyAlerted;
    private float batteryPct;
    private int vibrate;
    private NotificationData notificationData;

    private static PowerManager.WakeLock myWakeLock;
    private static boolean isRunning = false;

    private static final Handler stdHandler = new Handler();
    private static final Handler cstHandler = new Handler();

    private final Runnable sendStandardNotification = new Runnable() {
        public void run() {

            notificationManager.post(notificationData);
            //Do not vibrate if DND is active
            if (!DeviceUtil.isDNDActive(context)) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        try {
                            if (vibrator != null) {
                                vibrator.vibrate(vibrate);
                            }
                        } catch (Exception e) {
                            Logger.error("vibrator exception: {}", e.getMessage());
                        }
                    }
                }, 1000 /* 1s */);

            }
            isRunning = false;
        }
    };

    private final Runnable sendAlertNotification = new Runnable() {
        public void run() {
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

        Logger.debug("MainService onCreate EventBus register");
        EventBus.getDefault().register(this);

        // Initialize widgetSettings
        settings = new WidgetSettings(Constants.TAG, context);
        settings.reload();

        // Restore system settings after service update
        new ExecCommand("adb shell settings put system screen_off_timeout 14000");
        Logger.debug("Restore APK_INSTALL screen timeout");
        /*
        try {
            if (new File("/system/xbin/su").exists()) { //Test for root
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
                Intent batteryStatus = context.registerReceiver(null, batteryFilter);
                if (batteryStatus != null)
                    getBatteryPct(batteryStatus);
                //Update date of last charge if power was disconnected and battery is full
                if (batteryPct > 0.98) {
                    dateLastCharge = System.currentTimeMillis();
                    settings.set(Constants.PREF_DATE_LAST_CHARGE, dateLastCharge);
                    Logger.debug("MainService onCreate dateLastCharge saved: " + dateLastCharge);
                }
            }
        }, powerDisconnectedFilter);

        notificationReplyReceiver = new NotificationReplyReceiver();
        IntentFilter notificationReplyFilter = new IntentFilter();
        notificationReplyFilter.addAction(Constants.INTENT_ACTION_REPLY);
        LocalBroadcastManager.getInstance(context).registerReceiver(notificationReplyReceiver, notificationReplyFilter);

        // Start OverlayLauncher
        if (settings.get(Constants.PREF_AMAZMOD_OVERLAY_LAUNCHER, false)) {
            setOverlayLauncher(true);
        }

        //Check if hourly chime is enable
        if ((settings.get(Constants.PREF_AMAZMOD_HOURLY_CHIME, false) || (WearMenuFragment.chimeEnabled))) {
            setHourlyChime(true);
            Logger.debug("Chime was enabled");
        }
       // Initialize battery alerts
        this.watchBatteryAlreadyAlerted = false;
        this.phoneBatteryAlreadyAlerted = false;

        // When starting AmazMod, defines notification counter as ZERO
        NotificationStore.setNotificationCount(context, 0);

        //Check transporters
        transporterGeneral = TransporterClassic.get(this, Transport.NAME);
        transporterGeneral.addDataListener(this);

        if (!transporterGeneral.isTransportServiceConnected()) {
            Logger.debug("MainService onCreate transporterGeneral not connected, connecting...");
            transporterGeneral.connectTransportService();
        } else {
            Logger.debug("MainService onCreate transporterGeneral already connected");
        }

        transporterNotifications = TransporterClassic.get(this, Transport.NAME_NOTIFICATION);
        transporterNotifications.addDataListener(this);

        if (!transporterNotifications.isTransportServiceConnected()) {
            Logger.debug("MainService onCreate transporterNotifications not connected, connecting...");
            transporterNotifications.connectTransportService();
        } else {
            Logger.debug("MainService onCreate transporterNotifications already connected");
        }

        // Catch huami's notifications
        transporterHuami = TransporterClassic.get(this, "com.huami.action.notification");
        transporterHuami.addDataListener(this);
        if (!transporterHuami.isTransportServiceConnected()) {
            Logger.debug("MainService onCreate transporterHuami not connected, connecting...");
            transporterHuami.connectTransportService();
        } else {
            Logger.debug("MainService onCreate transportedHuami already connected");
        }

        slptClockClient = new SlptClockClient();
        slptClockClient.bindService(this, "AmazMod-MainService", new SlptClockClient.Callback() {
            @Override
            public void onServiceConnected() {
            }

            @Override
            public void onServiceDisconnected() {
            }
        });

        setupHardwareKeysMusicControl(settingsManager.getBoolean(Constants.PREF_ENABLE_HARDWARE_KEYS_MUSIC_CONTROL, false));

        // Register phone connect/disconnect monitor
        isPhoneConnectionAlertEnabled = settingsManager.getBoolean(Constants.PREF_PHONE_CONNECTION_ALERT, false);
        isStandardAlertEnabled = settingsManager.getBoolean(Constants.PREF_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION, false);
        if (isPhoneConnectionAlertEnabled) {
            registerConnectionMonitor(true);
        }

        // Register springboard observer if AmazMod as First Widget is enabled in Preferences
        isSpringboardObserverEnabled = settingsManager.getBoolean(Constants.PREF_AMAZMOD_FIRST_WIDGET, true);
        wasSpringboardSaved = false;
        if (isSpringboardObserverEnabled) {
            Logger.debug("MainService isSpringboardObserverEnabled: true ");
            registerSpringBoardMonitor(true);
        }else{
            Logger.debug("MainService isSpringboardObserverEnabled: false");
        }

        // Set battery db record JobService if watch never synced with phone
        String defaultLocale = settingsManager.getString(Constants.PREF_DEFAULT_LOCALE, "");
        long timeSinceLastBatterySync = System.currentTimeMillis() - settings.get(Constants.PREF_DATE_LAST_BATTERY_SYNC, 0);
        Logger.debug("MainService onCreate defaultLocale: " + defaultLocale);
        jobScheduler = (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);

        if ((defaultLocale.isEmpty()) || (timeSinceLastBatterySync > BATTERY_SYNC_INTERVAL)) {

            Logger.debug("MainService onCreate ***** starting BatteryJobService");
            ComponentName serviceComponent = new ComponentName(getApplicationContext(), BatteryJobService.class);

            if (jobScheduler != null) {

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

        //Unregister receivers
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

        //Disconnect transporters
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

        //Unbind spltClockClient
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

    @Override
    public void onDataReceived(TransportDataItem transportDataItem) {
        String action = transportDataItem.getAction();

        // A notification is removed/added
        if (action.equals("del") || action.equals("add")) {
            notificationCounter(action.equals("del") ? -1 : 1);
            settings.reload();
        }

        // Activate screen if the option is enabled in widget menus
        if (action.equals("add") && (settings.get(Constants.PREF_NOTIFICATIONS_SCREEN_ON, 0) == 1)) {
            acquireWakelock();
        }

        Logger.debug("MainService action: {}", action);

        //Flag used by OverlayLauncher
        if (Transport.INCOMING_NOTIFICATION.equals(action) || "add".equals(action))
            notificationArrived = true;

        Class messageClass = messages.get(action);

        if (messageClass != null) {
            Class[] args = new Class[1];
            args[0] = DataBundle.class;

            try {
                Constructor eventContructor = messageClass.getDeclaredConstructor(args);
                Object event = eventContructor.newInstance(transportDataItem.getData());

                Logger.debug("MainService onDataReceived: " + event.toString());
                EventBus.getDefault().post(event);
            } catch (NoSuchMethodException e) {
                Logger.debug("MainService event mapped with action \"" + action + "\" doesn't have constructor with DataBundle as parameter");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void deleteNotification(DeleteNotificationEvent deleteNotificationEvent) {

        StatusBarNotificationData statusBarNotificationData = deleteNotificationEvent.getDataBundle().getParcelable("data");
        String key = statusBarNotificationData.key;
        boolean enableCustomUI = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI, false);
        Logger.warn("deleteNotification enableCustomUI: {} \\ key: {}", enableCustomUI, key);

        if (enableCustomUI) {

            if (key != null) {
                if (NotificationStore.getCustomNotificationCount() > 0)
                    for (ArrayMap.Entry<String, String> pair : NotificationStore.keyMap.entrySet()) {
                        Logger.warn("deleteNotification NS.key: {} \\ NS.entry: {}", pair.getKey(), pair.getValue());

                        if (key.equals(pair.getValue())) {
                            Logger.warn("deleteNotification removing: {}", pair.getKey());
                            NotificationStore.removeCustomNotification(pair.getKey());
                        }
                    }
                else
                    Logger.warn("deleteNotification empty NotificationStore");
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void requestDeleteNotification(NotificationKeyData notificationKeyData) {

        Logger.warn("requestDeleteNotification key: {}", notificationKeyData.key);

        DataBundle dataBundle = new DataBundle();
        dataBundle.putParcelable("notiKey", notificationKeyData);
        sendHuami("del", dataBundle);

    }

    // Watchface/Calendar data (phone battery/alarm)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void watchface(Watchface watchface) {
        WatchfaceData watchfaceData = WatchfaceData.fromDataBundle(watchface.getDataBundle());

        // Data from phone
        int phoneBattery = watchfaceData.getBattery();
        String phoneAlarm = watchfaceData.getAlarm();
        String calendarEvents = watchfaceData.getCalendarEvents();
        Logger.debug("Updating phone's data, battery:" + phoneBattery + ", alarm:" + phoneAlarm + ", events:" + calendarEvents);

        // Update Time
        long updateTime = Calendar.getInstance().getTimeInMillis();

        // Watchface data
        // Get already saved data
        String data = DeviceUtil.systemGetString(context, "CustomWatchfaceData");
        if (data == null || data.equals("")) {
            DeviceUtil.systemPutString(context, "CustomWatchfaceData", "{}");//default
        }
        // Update the data
        try {
            // Extract data from JSON
            JSONObject json_data = new JSONObject(data);
            json_data.put("phoneBattery", phoneBattery);
            json_data.put("phoneAlarm", phoneAlarm);
            json_data.put("updateTime", updateTime);

            DeviceUtil.systemPutString(context, "CustomWatchfaceData", json_data.toString());
        } catch (JSONException e) {
            //default
            DeviceUtil.systemPutString(context, "CustomWatchfaceData", "{\"phoneBattery\":\"" + phoneBattery + "\",\"phoneAlarm\":\"" + phoneAlarm + "\",\"updateTime\":"+updateTime+"}");
        }

        // Calendar data
        if (calendarEvents != null && !calendarEvents.equals("")) {
            try {
                // Check if correct form of JSON
                JSONObject json_data = new JSONObject(calendarEvents);
                json_data.put("updateTime", updateTime);
                // Update data
                DeviceUtil.systemPutString(context, "CustomCalendarData", json_data.toString());
            } catch (JSONException e) {
                //default
                DeviceUtil.systemPutString(context, "CustomCalendarData", "{}");
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

    // Request installed widgets
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestWidgets(RequestWidgets requestWidgets) {
        final PackageManager pm = getPackageManager();

        //WidgetsData widgetsData = WidgetsData.fromDataBundle(requestWidgets.getDataBundle());

        // Get the list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // Saved widgets
        String widget_order_in = DeviceUtil.systemGetString(context, Constants.WIDGET_ORDER_IN);

        //Create empty list
        ArrayList<String> in_widgets = new ArrayList<String>();
        ArrayList<Integer> in_widgets_pos = new ArrayList<Integer>();
        ArrayList<Boolean> in_widgets_enabled = new ArrayList<Boolean>();
        try {
            //Parse JSON
            JSONObject root = new JSONObject(widget_order_in);
            JSONArray data = root.getJSONArray("data");

            //Data array contains all the elements
            for (int x = 0; x < data.length(); x++) {
                //Get item
                JSONObject item = data.getJSONObject(x);

                //String pkg = item.getString("pkg");
                int position = Integer.parseInt(item.getString("srl"));
                boolean enable = item.getInt("enable") == 1;

                in_widgets.add(item.getString("pkg"));
                in_widgets_pos.add(position);
                in_widgets_enabled.add(enable);
            }
        } catch (JSONException e) {
            //e.printStackTrace();
        }

        // Find widgets
        String widgets = "";
        int count = 0 ;
        for (ApplicationInfo packageInfo : packages) {
            //Log.d(TAG, "Installed package :" + packageInfo.packageName);
            //Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
            //Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
            Bundle bundle = packageInfo.metaData;
            if (bundle == null) continue;
            try {
                boolean metaData = bundle.containsKey("com.huami.watch.launcher.springboard.PASSAGER_TARGET");
                if(metaData) {
                    // Get info
                    int id = bundle.getInt("com.huami.watch.launcher.springboard.PASSAGER_TARGET");
                    String name = packageInfo.loadLabel(pm).toString();

                    //String[] inArray = getResources().getStringArray(id);

                    Resources resources = getApplicationContext().getPackageManager().getResourcesForApplication(packageInfo.packageName);
                    String[] inArray = resources.getStringArray(id);

                    int position = 99;
                    boolean enabled = false;
                    if(in_widgets.contains(packageInfo.packageName)) {
                        position = in_widgets_pos.get(in_widgets.indexOf(packageInfo.packageName));
                        enabled = in_widgets_enabled.get(in_widgets.indexOf(packageInfo.packageName));
                    }

                    // Add in widgets
                    widgets += (count>0?",":"") + "{\"pkg\":\""+packageInfo.packageName+"\",\"activity\":\""+inArray[0]+"\",\"name\":\""+name+"\",\"position\":"+position+",\"enabled\":"+enabled+"}";
                    count++;
                    //inArray[0].split("/")[1]

                    // Log
                    Logger.debug("Widget found: " + packageInfo.packageName + " - " + inArray[0] +" - "+name );
                }else
                    Logger.debug("App: "+packageInfo.packageName+" is not a widget");

            } catch (Exception e) {
                Logger.error("App: "+packageInfo.packageName+" is not a widget");
            }
        }

        this.widgetsData.setPackages("{\"widgets\":["+widgets+"]}");
        // Send the transmit
        Logger.debug("MainService requestWidgets widgetsData: " + widgetsData.getPackages());
        send(Transport.WIDGETS_DATA, widgetsData.toDataBundle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void enableLowPower(EnableLowPower lp) {
        //SystemProperties.goToSleep(this);
        count++;
        Logger.debug("MainService lowPower count: " + count);
        if (count < 2) {
            Toast.makeText(context, "lowPower: true", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(context, "lowPower: false", Toast.LENGTH_SHORT).show();
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

        //Toggle AmazMod as first widget setting
        iPCA = settingsData.isAmazModFirstWidget();
        settings.reload();
        settings.set(Constants.PREF_AMAZMOD_FIRST_WIDGET, iPCA);
        if (isSpringboardObserverEnabled != iPCA)
            registerSpringBoardMonitor(iPCA);

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void reply(ReplyNotificationEvent event) {
        Logger.debug("MainService reply to notification, key: " + event.getKey() + ", message: " + event.getMessage());

        DataBundle dataBundle = new DataBundle();

        dataBundle.putString("key", event.getKey());
        dataBundle.putString("message", event.getMessage());

        send(Transport.REPLY, dataBundle);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void silence(SilenceApplicationEvent event) {
        Logger.debug("MainService silence application, package: " + event.getPackageName() + ", minutes: " + event.getMinutes());

        DataBundle dataBundle = new DataBundle();

        dataBundle.putString("package", event.getPackageName());
        dataBundle.putString("minutes", event.getMinutes());

        send(Transport.SILENCE, dataBundle);
    }

    // Incoming Notification
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void incomingNotification(IncomingNotificationEvent incomingNotificationEvent) {
        //NotificationSpec notificationSpec = NotificationSpecFactory.getNotificationSpec(MainService.this, incomingNotificationEvent.getDataBundle());
        NotificationData notificationData = NotificationData.fromDataBundle(incomingNotificationEvent.getDataBundle());

        // Changed for RC1
        if (notificationData.getVibration() > 0) {
            Logger.debug("MainService incomingNotification vibration: " + notificationData.getVibration());
        } else notificationData.setVibration(0);
        //notificationData.setVibration(settingsManager.getInt(Constants.PREF_NOTIFICATION_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATION_VIBRATION));
        notificationData.setTimeoutRelock(settingsManager.getInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, Constants.PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT));

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
        watchStatusData.setRoBuildDescription(SystemProperties.get(WatchStatusData.RO_BUILD_DESCRIPTION, "-"));
        watchStatusData.setRoBuildDisplayId(SystemProperties.get(WatchStatusData.RO_BUILD_DISPLAY_ID, "-"));
        watchStatusData.setRoBuildHuamiModel(SystemProperties.get(WatchStatusData.RO_BUILD_HUAMI_MODEL, "-"));
        //watchStatusData.setRoBuildHuamiNumber(SystemProperties.get(WatchStatusData.RO_BUILD_HUAMI_NUMBER, "-"));
        //watchStatusData.setRoProductDevice(SystemProperties.get(WatchStatusData.RO_PRODUCT_DEVICE, "-"));
        //watchStatusData.setRoProductManufacter(SystemProperties.get(WatchStatusData.RO_PRODUCT_MANUFACTER, "-"));
        watchStatusData.setRoProductModel(SystemProperties.get(WatchStatusData.RO_PRODUCT_MODEL, "-"));
        watchStatusData.setRoProductName(SystemProperties.get(WatchStatusData.RO_PRODUCT_NAME, "-"));
        //watchStatusData.setRoRevision(SystemProperties.get(WatchStatusData.RO_REVISION, "-"));
        watchStatusData.setRoSerialno(SystemProperties.get(WatchStatusData.RO_SERIALNO, "-"));
        //watchStatusData.setRoBuildFingerprint(SystemProperties.get(WatchStatusData.RO_BUILD_FINGERPRINT, "-"));

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

        // Get last heart rates
        final boolean isHeartrateData = settingsManager.getBoolean(Constants.PREF_HEARTRATE_DATA, true);
        if (isHeartrateData) {
            Cursor cur = null;
            String heartrates = "";
            try {
                cur = getContentResolver().query(Uri.parse("content://com.huami.watch.health.heartdata"), null, null, null, "utc_time ASC");
                // Use the cursor to step through the returned records
                while (cur.moveToNext()) {
                    // Get the field values
                    // example: utc_time=1528485660, time_zone=0, heart_rate=96
                    long utc_time = cur.getLong(0);
                    //int time_zone = cur.getInt(1);
                    int heart_rate = cur.getInt(2);

                    heartrates += utc_time + "," + heart_rate + ",";
                }
                cur.close();
            } catch (SecurityException e) {
                //Getting data error
            }
            watchStatusData.setLastHeartRates(heartrates);
        }

        // Send the transmit
        Logger.debug("MainService requestWatchStatus watchStatusData: " + watchStatusData.toString());
        send(Transport.WATCH_STATUS, watchStatusData.toDataBundle());

    }

    // Battery request
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestBatteryStatus(RequestBatteryStatus requestBatteryStatus) {

        settings.reload();
        Intent batteryStatus = context.registerReceiver(null, batteryFilter);

        int status = 0;
        if (batteryStatus != null) {
            status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

            getBatteryPct(batteryStatus);


            //Get data of last full charge from settings
            //Use WidgetSettings to share data with Springboard widget (SharedPreferences didn't work)
            if (dateLastCharge == 0) {
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
        } else
            Logger.error("MainService requestBatteryStatus: register receiver error!");
    }

    // Set brightness
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void brightness(Brightness brightness) {
        BrightnessData brightnessData = BrightnessData.fromDataBundle(brightness.getDataBundle());
        final int brightnessLevel = brightnessData.getLevel();
        Logger.debug("MainService setting brightness to " + brightnessLevel);

        if (brightnessLevel == -1)
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
                send(Transport.NEXT_MUSIC);
            } else {
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
        } catch (Exception ex) {
            Logger.error(ex.getMessage());
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
                                    showConfirmationWearActivity("Service Update", "0");
                                    //DeviceUtil.systemPutAdb(context,"screen_off_timeout", "200000");
                                    new ExecCommand("adb shell settings put system screen_off_timeout 200000");
                                    Thread.sleep(1000);
                                    new ExecCommand("adb install -r -d " + apkFile);

                                } else {
                                    showConfirmationWearActivity("Installing APK", "0");
                                    myWakeLock = DeviceUtil.installApkAdb(context, apk, requestShellCommandData.isReboot());
                                }

                            } else {
                                code = -1;
                                errorMsg = String.format("%s not found!", apkFile);
                            }

                        } else if (command.contains("install_amazmod_update ")){
                            showConfirmationWearActivity("Service update", "0");
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
                                    showConfirmationWearActivity("Service update", "0");
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

    // Get battery %
    private void getBatteryPct(Intent batteryStatus) {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        batteryPct = level / (float) scale;

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

        //Update battery level (used in widget)
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

        File[] files = directory.listFiles();

        ArrayList<FileData> filesData = new ArrayList<>();
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

    // Count notifications
    public void notificationCounter(int n) {
        int notifications = 0;

        // Get already saved data
        String data = DeviceUtil.systemGetString(context, "CustomWatchfaceData");
        if (data == null || data.equals("")) {
            DeviceUtil.systemPutString(context, "CustomWatchfaceData", "{}");//default
        }

        // Get data
        try {
            // Extract data from JSON
            JSONObject json_data = new JSONObject(data);
            notifications = json_data.getInt("notifications");
        } catch (JSONException e) {
            //Nothing, notifications are never saved before
            Logger.error(e, "notificationCounter JSONException01: " + e.getMessage());
        }

        // Update notifications (but always > -1)
        notifications = (notifications + n > -1) ? notifications + n : 0;

        Logger.debug("Updating notifications: " + notifications);

        // Save the data
        try {
            // Extract data from JSON
            JSONObject json_data = new JSONObject(data);
            json_data.put("notifications", notifications);

            DeviceUtil.systemPutString(context, "CustomWatchfaceData", json_data.toString());
        } catch (JSONException e) {
            //default
            DeviceUtil.systemPutString(context, "CustomWatchfaceData", "{\"notifications\":" + notifications + "}");
            Logger.error(e, "notificationCounter JSONException02: " + e.getMessage());
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

    private void registerSpringBoardMonitor(boolean status) {
        Logger.debug("MainService registerSpringBoardMonitor status: " + status);
        if (status) {
            if (springboardObserver != null)
                return;
            // if it's enabling observer, sync for a first time
            WidgetsUtil.loadWidgetList(context);
            ContentResolver contentResolver = getContentResolver();
            Uri setting = Settings.System.getUriFor(Constants.WIDGET_ORDER_IN);
            springboardObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    Logger.debug("MainService registerSpringBoardMonitor onChange");
                    //Set AmazMod as first Widget
                    if (!wasSpringboardSaved)
                        WidgetsUtil.syncWidgets(context);
                    else
                        wasSpringboardSaved = false;
                }

                @Override
                public boolean deliverSelfNotifications() {
                    return true;
                }
            };
            contentResolver.registerContentObserver(setting, true, springboardObserver);
        } else {
            if (springboardObserver != null)
                getContentResolver().unregisterContentObserver(springboardObserver);
            springboardObserver = null;
        }
        isSpringboardObserverEnabled = status;
    }

    private void setOverlayLauncher(boolean status){
        Logger.debug("MainService setOverlayLauncher status: {}", status);

        final Intent overlayButton = new Intent(context, OverlayLauncher.class);
        if (status) {
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
        if (status) {
            HourlyChime.setHourlyChime(context,true);
            WearMenuFragment.chimeEnabled = true;
        } else {
            HourlyChime.setHourlyChime(context,false);
            WearMenuFragment.chimeEnabled = false;
        }
        settings.set(Constants.PREF_AMAZMOD_HOURLY_CHIME, status);
        send(Transport.HOURLY_CHIME_SYNC, watchStatusData.toDataBundle());
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
}
