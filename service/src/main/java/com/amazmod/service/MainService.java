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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.amazmod.service.db.model.BatteryDbEntity;
import com.amazmod.service.db.model.BatteryDbEntity_Table;
import com.amazmod.service.events.HardwareButtonEvent;
import com.amazmod.service.events.NightscoutDataEvent;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.events.SilenceApplicationEvent;
import com.amazmod.service.events.incoming.Brightness;
import com.amazmod.service.events.incoming.EnableLowPower;
import com.amazmod.service.events.incoming.IncomingNotificationEvent;
import com.amazmod.service.events.incoming.RequestBatteryStatus;
import com.amazmod.service.events.incoming.RequestDeleteFile;
import com.amazmod.service.events.incoming.RequestDirectory;
import com.amazmod.service.events.incoming.RequestDownloadFileChunk;
import com.amazmod.service.events.incoming.RequestShellCommand;
import com.amazmod.service.events.incoming.RequestUploadFileChunk;
import com.amazmod.service.events.incoming.RequestWatchStatus;
import com.amazmod.service.events.incoming.RevokeAdminOwner;
import com.amazmod.service.events.incoming.SyncSettings;
import com.amazmod.service.events.incoming.Watchface;
import com.amazmod.service.music.MusicControlInputListener;
import com.amazmod.service.notifications.NotificationService;
import com.amazmod.service.notifications.NotificationsReceiver;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.springboard.WidgetSettings;
import com.amazmod.service.support.BatteryJobService;
import com.amazmod.service.support.CommandLine;
import com.amazmod.service.ui.ConfirmationWearActivity;
import com.amazmod.service.ui.PhoneConnectionActivity;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.FileDataFactory;
import com.amazmod.service.util.SystemProperties;
import com.amazmod.service.util.WidgetsUtil;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.ingenic.iwds.slpt.SlptClockClient;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import xiaofei.library.hermeseventbus.HermesEventBus;

import static java.lang.System.currentTimeMillis;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class MainService extends Service implements Transporter.DataListener {

    private NotificationsReceiver notificationsReceiver;

    private Map<String, Class> messages = new HashMap<String, Class>() {{
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
    }};

    private static Transporter transporterGeneral, transporterNotifications, transporterHuami;

    private Context context;
    private SettingsManager settingsManager;
    private NotificationService notificationManager;
    private static IntentFilter batteryFilter;
    private static long dateLastCharge;
    private static int count = 0;
    private static boolean isPhoneConnectionAlertEnabled;
    private static boolean isPhoneConnectionStandardAlertEnabled;
    private static boolean isSpringboardObserverEnabled;
    private static boolean wasSpringboardSaved;
    private float batteryPct;
    private static WidgetSettings settings;
    private static JobScheduler jobScheduler;

    private static final long BATTERY_SYNC_INTERVAL = 60*60*1000L; //One hour
    private static final int BATTERY_JOB_ID = 0;

    private BatteryData batteryData;
    private WatchStatusData watchStatusData;

    private SlptClockClient slptClockClient;
    private ContentObserver phoneConnectionObserver;
    private ContentObserver springboardObserver;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;
        settingsManager = new SettingsManager(context);
        notificationManager = new NotificationService(context);

        batteryData = new BatteryData();
        watchStatusData = new WatchStatusData();

        //Initialize widgetSettings
        settings = new WidgetSettings(Constants.TAG, context);
        settings.reload();


        Log.d(Constants.TAG, "MainService onCreate HermesEventBus connect");
        HermesEventBus.getDefault().register(this);

        batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        //Register power disconnect receiver
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
                    dateLastCharge = currentTimeMillis();
                    settings.set(Constants.PREF_DATE_LAST_CHARGE, dateLastCharge);
                    Log.d(Constants.TAG, "MainService onCreate dateLastCharge saved: " + dateLastCharge);
                }
            }
        }, powerDisconnectedFilter);

        notificationsReceiver = new NotificationsReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_ACTION_REPLY);
        registerReceiver(notificationsReceiver, filter);

        transporterGeneral = TransporterClassic.get(this, Transport.NAME);
        transporterGeneral.addDataListener(this);

        if (!transporterGeneral.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "MainService onCreate transporterGeneral not connected, connecting...");
            transporterGeneral.connectTransportService();
        } else {
            Log.d(Constants.TAG, "MainService onCreate transporterGeneral already connected");
        }

        transporterNotifications = TransporterClassic.get(this, Transport.NAME_NOTIFICATION);
        transporterNotifications.addDataListener(this);

        if (!transporterNotifications.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "MainService onCreate transporterNotifications not connected, connecting...");
            transporterNotifications.connectTransportService();
        } else {
            Log.d(Constants.TAG, "MainService onCreate transporterNotifications already connected");
        }

        // Catch huami's notifications
        transporterHuami = TransporterClassic.get(this, "com.huami.action.notification");
        transporterHuami.addDataListener(this);
        if (!transporterHuami.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "MainService onCreate transporterHuami not connected, connecting...");
            transporterHuami.connectTransportService();
        } else {
            Log.d(Constants.TAG, "MainService onCreate transportedHuami already connected");
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

        //Register phone connect/disconnect monitor
        isPhoneConnectionAlertEnabled = settingsManager.getBoolean(Constants.PREF_PHONE_CONNECTION_ALERT, false);
        isPhoneConnectionStandardAlertEnabled = settingsManager.getBoolean(Constants.PREF_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION, false);
        if (isPhoneConnectionAlertEnabled) {
            registerConnectionMonitor(true);
        }

        //Register springboard observer if AmazMod as First Widget is enabled in Preferences
        isSpringboardObserverEnabled = settingsManager.getBoolean(Constants.PREF_AMAZMOD_FIRST_WIDGET, true);
        wasSpringboardSaved = false;
        if (isSpringboardObserverEnabled) {
            Log.d(Constants.TAG, "MainService isSpringboardObserverEnabled: true ");
            registerSpringBoardMonitor(true);
        }else{
            Log.d(Constants.TAG, "MainService isSpringboardObserverEnabled: false");
        }

        //Set battery db record JobService if watch never synced with phone
        String defaultLocale = settingsManager.getString(Constants.PREF_DEFAULT_LOCALE, "");
        long timeSinceLastBatterySync = System.currentTimeMillis() - settings.get(Constants.PREF_DATE_LAST_BATTERY_SYNC, 0);
        Log.d(Constants.TAG, "MainService onCreate defaultLocale: " + defaultLocale);
        jobScheduler = (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);

        if ((defaultLocale.isEmpty()) || (timeSinceLastBatterySync > BATTERY_SYNC_INTERVAL)) {

            Log.d(Constants.TAG, "MainService onCreate ***** starting BatteryJobService");
            ComponentName serviceComponent = new ComponentName(getApplicationContext(), BatteryJobService.class);

            if (jobScheduler != null) {

                cancelPendingJobs(BATTERY_JOB_ID);
                JobInfo.Builder builder = new JobInfo.Builder(BATTERY_JOB_ID, serviceComponent);
                builder.setPeriodic(BATTERY_SYNC_INTERVAL);
                jobScheduler.schedule(builder.build());

            } else
                Log.e(Constants.TAG, "MainService error staring BatteryJobService: null jobScheduler!");
        }

    }

    @Override
    public void onDestroy() {
        HermesEventBus.getDefault().unregister(this);

        if (notificationsReceiver != null) {
            unregisterReceiver(notificationsReceiver);
            notificationsReceiver = null;
        }

        // Unregister content observers
        registerConnectionMonitor(false);
        registerSpringBoardMonitor(false);

        //Disconnect transporters
        if (transporterGeneral.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "MainService onDestroy transporterGeneral disconnecting...");
            transporterGeneral.disconnectTransportService();
        }

        if (transporterNotifications.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "MainService onDestroy transporterNotifications disconnecting...");
            transporterNotifications.disconnectTransportService();
        }

        if (transporterHuami.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "MainService onDestroy transporterHuami disconnecting...");
            transporterHuami.disconnectTransportService();
        }

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

        Log.d(Constants.TAG, "MainService action: " + action);

        Class messageClass = messages.get(action);

        if (messageClass != null) {
            Class[] args = new Class[1];
            args[0] = DataBundle.class;

            try {
                Constructor eventContructor = messageClass.getDeclaredConstructor(args);
                Object event = eventContructor.newInstance(transportDataItem.getData());

                Log.d(Constants.TAG, "MainService onDataReceived: " + event.toString());
                HermesEventBus.getDefault().post(event);
            } catch (NoSuchMethodException e) {
                Log.d(Constants.TAG, "MainService event mapped with action \"" + action + "\" doesn't have constructor with DataBundle as parameter");
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void watchface(Watchface watchface) {
        WatchfaceData watchfaceData = WatchfaceData.fromDataBundle(watchface.getDataBundle());

        // Data from phone
        int phoneBattery = watchfaceData.getBattery();
        String phoneAlarm = watchfaceData.getAlarm();
        String calendarEvents = watchfaceData.getCalendarEvents();
        Log.d(Constants.TAG, "Updating phone's data, battery:" + phoneBattery + ", alarm:" + phoneAlarm + ", events:" + calendarEvents);

        // Update Time
        long updateTime = Calendar.getInstance().getTimeInMillis();

        // Watchface data
        // Get already saved data
        String data = Settings.System.getString(context.getContentResolver(), "CustomWatchfaceData");
        if (data == null || data.equals("")) {
            Settings.System.putString(context.getContentResolver(), "CustomWatchfaceData", "{}");//default
        }
        // Update the data
        try {
            // Extract data from JSON
            JSONObject json_data = new JSONObject(data);
            json_data.put("phoneBattery", phoneBattery);
            json_data.put("phoneAlarm", phoneAlarm);
            json_data.put("updateTime", updateTime);

            Settings.System.putString(context.getContentResolver(), "CustomWatchfaceData", json_data.toString());
        } catch (JSONException e) {
            //default
            Settings.System.putString(context.getContentResolver(), "CustomWatchfaceData", "{\"phoneBattery\":\"" + phoneBattery + "\",\"phoneAlarm\":\"" + phoneAlarm + "\",\"updateTime\":"+updateTime+"}");
        }

        // Calendar data
        if (calendarEvents != null && !calendarEvents.equals("")) {
            try {
                // Check if correct form of JSON
                JSONObject json_data = new JSONObject(calendarEvents);
                json_data.put("updateTime", updateTime);
                // Update data
                Settings.System.putString(context.getContentResolver(), "CustomCalendarData", json_data.toString());
            } catch (JSONException e) {
                //default
                Settings.System.putString(context.getContentResolver(), "CustomCalendarData", "{}");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void enableLowPower(EnableLowPower lp) {
        //SystemProperties.goToSleep(this);
        count++;
        Log.d(Constants.TAG, "MainService lowPower count: " + count);
        if (count < 2) {
            Toast.makeText(context, "lowPower: true", Toast.LENGTH_SHORT).show();
            BluetoothAdapter btmgr = BluetoothAdapter.getDefaultAdapter();
            Log.i(Constants.TAG, "MainService lowPower disable BT");
            btmgr.disable();
            try {
                WifiManager wfmgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wfmgr != null) {
                    if (wfmgr.isWifiEnabled()) {
                        Log.i(Constants.TAG, "MainService lowPower disable WiFi");
                        wfmgr.setWifiEnabled(false);
                    }
                }
            } catch (NullPointerException e) {
                Log.e(Constants.TAG, "MainService lowPower exception: " + e.toString());
            }

            slptClockClient.enableLowBattery();
            slptClockClient.enableSlpt();
            SystemProperties.setSystemProperty("sys.state.powerlow", String.valueOf(true));
            final DevicePolicyManager mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (mDPM != null) {
                final Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
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
            SystemProperties.setSystemProperty("sys.state.powerlow", String.valueOf(false));
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
            Log.e(Constants.TAG, "MainService revokeAdminOwner NullPointerException: " + e.toString());
        } catch (SecurityException e) {
            Log.e(Constants.TAG, "MainService revokeAdminOwner SecurityException: " + e.toString());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void settingsSync(SyncSettings event) {
        Log.w(Constants.TAG, "MainService SyncSettings ***** event received *****");
        SettingsData settingsData = SettingsData.fromDataBundle(event.getDataBundle());
        settingsManager.sync(settingsData);

        //Toggle phone connect/disconnect monitor if settings changed
        boolean iPCA = settingsData.isPhoneConnectionAlert();
        isPhoneConnectionStandardAlertEnabled = settingsData.isPhoneConnectionAlertStandardNotification();
        if (isPhoneConnectionAlertEnabled != iPCA)
            registerConnectionMonitor(iPCA);


        iPCA = settingsData.isAmazModFirstWidget();
        settings.reload();
        settings.set(Constants.PREF_AMAZMOD_FIRST_WIDGET, iPCA);
        if (isSpringboardObserverEnabled != iPCA)
            registerSpringBoardMonitor(iPCA);

        setupHardwareKeysMusicControl(settingsData.isEnableHardwareKeysMusicControl());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void string(String event) {
        Log.w(Constants.TAG, "MainService string ***** event received *****");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void reply(ReplyNotificationEvent event) {
        Log.d(Constants.TAG, "MainService reply to notification, key: " + event.getKey() + ", message: " + event.getMessage());

        DataBundle dataBundle = new DataBundle();

        dataBundle.putString("key", event.getKey());
        dataBundle.putString("message", event.getMessage());

        send(Transport.REPLY, dataBundle);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void silence(SilenceApplicationEvent event) {
        Log.d(Constants.TAG, "MainService silence application, package: " + event.getPackageName() + ", minutes: " + event.getMinutes());

        DataBundle dataBundle = new DataBundle();

        dataBundle.putString("package", event.getPackageName());
        dataBundle.putString("minutes", event.getMinutes());

        send(Transport.SILENCE, dataBundle);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void incomingNotification(IncomingNotificationEvent incomingNotificationEvent) {
        //NotificationSpec notificationSpec = NotificationSpecFactory.getNotificationSpec(MainService.this, incomingNotificationEvent.getDataBundle());
        NotificationData notificationData = NotificationData.fromDataBundle(incomingNotificationEvent.getDataBundle());

        //Changed for RC1
        if (notificationData.getVibration() > 0) {
            Log.d(Constants.TAG, "MainService incomingNotification vibration: " + notificationData.getVibration());
        } else notificationData.setVibration(0);
        //notificationData.setVibration(settingsManager.getInt(Constants.PREF_NOTIFICATION_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATION_VIBRATION));
        notificationData.setTimeoutRelock(settingsManager.getInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, Constants.PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT));

        notificationData.setDeviceLocked(DeviceUtil.isDeviceLocked(context));

        Log.d(Constants.TAG, "MainService incomingNotification: " + notificationData.toString());
        notificationManager.post(notificationData);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestWatchStatus(RequestWatchStatus requestWatchStatus) {
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

        int b = 0;
        int bm = Constants.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        try {
            b = android.provider.Settings.System.getInt(getContentResolver(), Constants.SCREEN_BRIGHTNESS);
            bm = Settings.System.getInt(getContentResolver(), Constants.SCREEN_BRIGHTNESS_MODE);

        } catch (Settings.SettingNotFoundException e) {
            Log.e(Constants.TAG, "MainService requestWatchStatus SettingsNotFoundExeception: " + e.toString());
        }
        watchStatusData.setScreenBrightness(b);
        watchStatusData.setScreenBrightnessMode(bm);

        Log.d(Constants.TAG, "MainService requestWatchStatus watchStatusData: " + watchStatusData.toString());
        send(Transport.WATCH_STATUS, watchStatusData.toDataBundle());
    }

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
                Log.d(Constants.TAG, "MainService dateLastCharge loaded: " + dateLastCharge);
            }

            Log.d(Constants.TAG, "MainService dateLastCharge: " + dateLastCharge + " | batteryPct: " + Math.round(batteryPct * 100f));
            cancelPendingJobs(BATTERY_JOB_ID);
            saveBatteryDb(batteryPct, false);

            batteryData.setLevel(batteryPct);
            batteryData.setCharging(isCharging);
            batteryData.setUsbCharge(usbCharge);
            batteryData.setAcCharge(acCharge);
            batteryData.setDateLastCharge(dateLastCharge);

            send(Transport.BATTERY_STATUS, batteryData.toDataBundle());
        } else
            Log.e(Constants.TAG, "MainService requestBatteryStatus: register receiver error!");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void brightness(Brightness brightness) {
        BrightnessData brightnessData = BrightnessData.fromDataBundle(brightness.getDataBundle());
        final int brightnessLevel = brightnessData.getLevel();
        Log.d(Constants.TAG, "MainService setting brightness to " + brightnessLevel);

        if (brightnessLevel == -1)
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 1);
        else {
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessLevel);
        }
    }

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestDirectory(RequestDirectory requestDirectory) {
        try {
            RequestDirectoryData requestDirectoryData = RequestDirectoryData.fromDataBundle(requestDirectory.getDataBundle());

            String path = requestDirectoryData.getPath();
            Log.d(Constants.TAG, "path: " + path);
            DirectoryData directoryData = getFilesByPath(path);
            send(Transport.DIRECTORY, directoryData.toDataBundle());
        } catch (Exception ex) {
            DirectoryData directoryData = new DirectoryData();
            directoryData.setResult(Transport.RESULT_UNKNOW_ERROR);

            send(Transport.DIRECTORY, directoryData.toDataBundle());
        }

    }

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
            Log.e(Constants.TAG, ex.getMessage());
        }
    }

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
            Log.e(Constants.TAG, ex.getMessage());
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void executeShellCommand(final RequestShellCommand requestShellCommand) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    RequestShellCommandData requestShellCommandData = RequestShellCommandData.fromDataBundle(requestShellCommand.getDataBundle());
                    String command = requestShellCommandData.getCommand();

                    if (!requestShellCommandData.isWaitOutput()) {

                        Log.d(Constants.TAG, "MainService executeShellCommand command: " + command);
                        int code = 0;
                        String errorMsg = "";

                        if (command.contains("install_apk ")) {

                            String apkFile = command.replace("install_apk ", "");
                            final File apk = new File(apkFile);

                            if (apk.exists()) {

                                showConfirmationWearActivity("Installing APK", "0");
                                DeviceUtil.installApkAdb(context, apk, requestShellCommandData.isReboot());

                            } else {
                                code = -1;
                                errorMsg = String.format("%s not found!", apkFile);
                            }

                        } else if (command.contains("install_amazmod_update ")) {
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
                                Log.d(Constants.TAG, "MainService shell process returned " + code);

                            } else {
                                if (command.contains("AmazMod-service-") && command.contains("adb install -r")) {
                                    showConfirmationWearActivity("Service update", "0");
                                    Thread.sleep(3000);
                                }
                                Runtime.getRuntime().exec(command);
                            }
                        }
                        ResultShellCommandData resultShellCommand = new ResultShellCommandData();
                        resultShellCommand.setResult(code);
                        resultShellCommand.setOutputLog("");
                        resultShellCommand.setErrorLog(errorMsg);
                        send(Transport.RESULT_SHELL_COMMAND, resultShellCommand.toDataBundle());

                    } else {
                        String filename = null;
                        if (command.contains("screencap")) {
                            File file = new File("/sdcard/Pictures/Screenshots");
                            boolean saveDirExists = false;
                            saveDirExists = file.exists() || file.mkdir();
                            if (saveDirExists) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
                                String dateStamp = sdf.format(new Date());
                                filename = "/sdcard/Pictures/Screenshots/ss_" + dateStamp + ".png";
                                command = command + " " + filename;
                            }
                        }

                        Log.d(Constants.TAG, "MainService executeShellCommand process: " + command);
                        long startedAt = System.currentTimeMillis();

                        String[] args = CommandLine.translateCommandline(command);
                        ProcessBuilder processBuilder = new ProcessBuilder(args);
                        processBuilder.redirectErrorStream(true);

                        Process process = processBuilder.start();

                        StringBuilder outputLog = new StringBuilder();
                        int returnValue;

                        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                outputLog.append(line).append("\n");
                            }

                            returnValue = process.waitFor();
                        }

                        ResultShellCommandData resultShellCommand = new ResultShellCommandData();
                        resultShellCommand.setResult(returnValue);
                        resultShellCommand.setOutputLog(outputLog.toString());
                        resultShellCommand.setDuration(System.currentTimeMillis() - startedAt);
                        resultShellCommand.setCommand(command);

                        send(Transport.RESULT_SHELL_COMMAND, resultShellCommand.toDataBundle());

                        if (command.contains("screencap")) {
                            if (filename != null) {
                                File file = new File(filename);
                                if (file.exists()) {
                                    Log.d(Constants.TAG, "MainService executeShellCommand file.exists: " + file.exists() + " " + file.getName());
                                    FileUploadData fileUploadData = new FileUploadData(file.getAbsolutePath(), file.getName(), file.length());
                                    send(Transport.FILE_UPLOAD, fileUploadData.toDataBundle());
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    Log.e(Constants.TAG, ex.getMessage(), ex);

                    ResultShellCommandData resultShellCommand = new ResultShellCommandData();
                    resultShellCommand.setResult(-1);
                    resultShellCommand.setErrorLog(ex.getMessage());

                    send(Transport.RESULT_SHELL_COMMAND, resultShellCommand.toDataBundle());
                }
            }
        }).start();
    }

    private void getBatteryPct(Intent batteryStatus) {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        batteryPct = level / (float) scale;
    }

    public static boolean saveBatteryDb(float batteryPct, boolean updateSettings) {

        settings.reload();
        long date = System.currentTimeMillis();
        boolean result = false;
        Log.d(Constants.TAG, "MainService saveBatteryDb date: " + date + " \\ batteryPct: " + batteryPct);

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
            Log.e(Constants.TAG, "MainService saveBatteryDb exception: " + ex.toString());
        }

        //Update battery level (used in widget)
        if (updateSettings)
            settings.set(Constants.PREF_BATT_LEVEL, Integer.toString(Math.round(batteryPct * 100f)) + "%");

        return result;

    }

    private void cancelPendingJobs(int id) {
        List<JobInfo> jobInfoList = jobScheduler.getAllPendingJobs();
        final int pendingJobs = jobInfoList.size();
        Log.d(Constants.TAG, "MainService cancelPendingJobs pendingJobs: " + pendingJobs);
        if (pendingJobs > 0)
            for (JobInfo jobInfo : jobInfoList) {
                Log.d(Constants.TAG, "MainService cancelPendingJobs jobInfo: " + jobInfo.toString());
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
            Log.d(Constants.TAG, "MainService Transport Service Not Connected");
            return;
        }

        if (dataBundle != null) {
            Log.d(Constants.TAG, "MainService send1: " + action);
            transporterGeneral.send(action, dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Log.d(Constants.TAG, "Send result: " + dataTransportResult.toString());
                }
            });

        } else {
            Log.d(Constants.TAG, "MainService send2: " + action);
            transporterGeneral.send(action, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Log.d(Constants.TAG, "Send result: " + dataTransportResult.toString());
                }
            });
        }
    }

    private void registerConnectionMonitor(boolean status) {
        Log.d(Constants.TAG, "MainService registerConnectionMonitor status: " + status);
        if (status) {
            if (phoneConnectionObserver != null)
                return;
            ContentResolver contentResolver = getContentResolver();
            Uri setting = Settings.System.getUriFor("com.huami.watch.extra.DEVICE_CONNECTION_STATUS");
            phoneConnectionObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    if (isPhoneConnectionStandardAlertEnabled) {
                        Log.d(Constants.TAG, "MainService registerConnectionMonitor1 standardAlert: " + isPhoneConnectionStandardAlertEnabled);
                        sendStandardAlert();
                    } else {
                        Log.d(Constants.TAG, "MainService registerConnectionMonitor2 standardAlert: " + isPhoneConnectionStandardAlertEnabled);
                        Intent intent = new Intent(context, PhoneConnectionActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                        context.startActivity(intent);
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

    private void sendStandardAlert() {

        NotificationData notificationData = new NotificationData();
        final int vibrate;

        final String notificationTime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Calendar.getInstance().getTime());

        notificationData.setId(9979);
        notificationData.setKey("amazmod|test|9979");
        notificationData.setTitle(getString(R.string.phone_connection_alert));
        notificationData.setTime(notificationTime);
        notificationData.setVibration(0);
        notificationData.setForceCustom(false);
        notificationData.setHideReplies(true);
        notificationData.setHideButtons(false);

        final Drawable drawable;

        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        if (android.provider.Settings.System.getString(getContentResolver(), "com.huami.watch.extra.DEVICE_CONNECTION_STATUS").equals("0")) {
            // Phone disconnected
            drawable = getDrawable(R.drawable.ic_outline_phonelink_erase);
            notificationData.setText(getString(R.string.phone_disconnected));
            vibrate = Constants.VIBRATION_LONG;
        } else {
            // Phone connected
            drawable = getDrawable(R.drawable.ic_outline_phonelink_ring);
            notificationData.setText(getString(R.string.phone_connected));
            vibrate = Constants.VIBRATION_SHORT;
        }

        try {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] intArray = new int[width * height];
            bitmap.getPixels(intArray, 0, width, 0, 0, width, height);

            notificationData.setIcon(intArray);
            notificationData.setIconWidth(width);
            notificationData.setIconHeight(height);
        } catch (Exception e) {
            notificationData.setIcon(new int[]{});
            Log.e(Constants.TAG, "MainService sendStandardAlert exception: " + e.toString());
        }

        notificationManager.post(notificationData);
        final Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                try {
                    if (vibrator != null) {
                        vibrator.vibrate(vibrate);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 1500);
    }

    // Count notifications
    public void notificationCounter(int n) {
        int notifications = 0;

        // Get already saved data
        String data = Settings.System.getString(getContentResolver(), "CustomWatchfaceData");
        if (data == null || data.equals("")) {
            Settings.System.putString(getContentResolver(), "CustomWatchfaceData", "{}");//default
        }

        // Get data
        try {
            // Extract data from JSON
            JSONObject json_data = new JSONObject(data);
            notifications = json_data.getInt("notifications");
        } catch (JSONException e) {
            //Nothing, notifications are never saved before
        }

        // Update notifications (but always > -1)
        notifications = (notifications + n > -1) ? notifications + n : 0;

        Log.d(Constants.TAG, "Updating notifications: " + notifications);

        // Save the data
        try {
            // Extract data from JSON
            JSONObject json_data = new JSONObject(data);
            json_data.put("notifications", notifications);

            Settings.System.putString(getContentResolver(), "CustomWatchfaceData", json_data.toString());
        } catch (JSONException e) {
            //default
            Settings.System.putString(getContentResolver(), "CustomWatchfaceData", "{\"notifications\":" + notifications + "}");
        }
    }

    private void acquireWakelock() {
        Log.d(Constants.TAG, "MainService acquireWakelock");
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            PowerManager.WakeLock wakeLockScreenOn = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP, "AmazMod::NotificationScreenOn");
            if (!wakeLockScreenOn.isHeld())
                wakeLockScreenOn.acquire(9*1000L /* 9s */);
        } else
            Log.e(Constants.TAG, "MainService aquireWakelock null powerManager!");
    }

    private void registerSpringBoardMonitor(boolean status) {
        Log.d(Constants.TAG, "MainService registerSpringBoardMonitor status: " + status);
        if (status) {
            if (springboardObserver != null)
                return;
            ContentResolver contentResolver = getContentResolver();
            Uri setting = Settings.System.getUriFor(Constants.WIDGET_ORDER_IN);
            springboardObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    Log.d(Constants.TAG, "MainService registerSpringBoardMonitor onChange");
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

    public static boolean getWasSpringboardSaved() {
        return wasSpringboardSaved;
    }

    public static void setWasSpringboardSaved(boolean b) {
        wasSpringboardSaved = b;
    }


}
