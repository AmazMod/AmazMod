package com.amazmod.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import com.amazmod.service.events.HardwareButtonEvent;
import com.amazmod.service.events.NightscoutDataEvent;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.events.incoming.Brightness;
import com.amazmod.service.events.incoming.IncomingNotificationEvent;
import com.amazmod.service.events.incoming.LowPower;
import com.amazmod.service.events.incoming.RequestBatteryStatus;
import com.amazmod.service.events.incoming.RequestWatchStatus;
import com.amazmod.service.events.incoming.SyncSettings;
import com.amazmod.service.music.MusicControlInputListener;
import com.amazmod.service.notifications.NotificationService;
import com.amazmod.service.notifications.NotificationsReceiver;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.springboard.WidgetSettings;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.SystemProperties;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.ingenic.iwds.slpt.SlptClockClient;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.BatteryData;
import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.SettingsData;
import amazmod.com.transport.data.WatchStatusData;
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
        put(Transport.LOW_POWER, LowPower.class);
    }};

    private Transporter transporter;

    private Context context;
    public Context baseContext;
    private SettingsManager settingsManager;
    private NotificationService notificationManager;
    private static long dateLastCharge;
    private float batteryPct;
    private WidgetSettings settings;

    private BatteryData batteryData;
    private WatchStatusData watchStatusData;
    private DataBundle dataBundle;

    private SlptClockClient slptClockClient;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;
        baseContext = this.getBaseContext();
        settingsManager = new SettingsManager(context);
        notificationManager = new NotificationService(context);

        settings = new WidgetSettings(Constants.TAG, context);
        batteryData = new BatteryData();

        watchStatusData = new WatchStatusData();
        dataBundle = new DataBundle();

        Log.d(Constants.TAG, "MainService HermesEventBus connect");
        HermesEventBus.getDefault().register(this);

        //Register power disconnect receiver
        IntentFilter powerDisconnectedFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        powerDisconnectedFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Update date of last charge if power was disconnected and battery is full
                if (batteryPct > 0.98) {
                    dateLastCharge = currentTimeMillis();
                    settings.set(Constants.PREF_DATE_LAST_CHARGE, dateLastCharge);
                    Log.d(Constants.TAG, "MainService dateLastCharge saved: " + dateLastCharge);
                }
            }
        }, powerDisconnectedFilter);

        notificationsReceiver = new NotificationsReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_ACTION_REPLY);
        registerReceiver(notificationsReceiver, filter);

        transporter = TransporterClassic.get(this, Transport.NAME);
        transporter.addDataListener(this);

        if (!transporter.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "MainService Transporter not connected, connecting...");
            transporter.connectTransportService();
        } else {
            Log.d(Constants.TAG, "MainService Transported yet connected");
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
    }

    @Override
    public void onDestroy() {
        HermesEventBus.getDefault().unregister(this);

        if (notificationsReceiver != null) {
            unregisterReceiver(notificationsReceiver);
            notificationsReceiver = null;
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

        Log.d(Constants.TAG, "MainService action: " + action);

        Class messageClass = messages.get(action);

        if (messageClass != null) {
            Class[] args = new Class[1];
            args[0] = DataBundle.class;

            try {
                Constructor eventContructor = messageClass.getDeclaredConstructor(args);
                Object event = eventContructor.newInstance(transportDataItem.getData());

                // Update phone data
                if (action.equals(Transport.REQUEST_BATTERYSTATUS)) {
                    save_phone_data(transportDataItem.getData());
                }

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


    private static final String LEVEL = "level";
    private static final String CHARGING = "charging";
    private static final String USB_CHARGE = "usb_charge";
    private static final String AC_CHARGE = "ac_charge";
    private static final String DATE_LAST_CHARGE = "date_last_charge"; // that is always 0

    public void save_phone_data(DataBundle dataBundle) {
        String phoneBattery = Integer.toString((int) (dataBundle.getFloat(LEVEL) * 100));
        String phoneAlarm = "";
        Log.d("DinoDevs-GreatFit", "Updating phone's data, battery:" + phoneBattery);

        String data = Settings.System.getString(context.getContentResolver(), "CustomWatchfaceData");

        if (data == null || data.equals("")) {
            Settings.System.putString(context.getContentResolver(), "CustomWatchfaceData", "{}");//default
        }

        try {
            // Extract data from JSON
            JSONObject json_data = new JSONObject(data);
            json_data.put("phoneBattery", phoneBattery);
            json_data.put("phoneAlarm", phoneAlarm);

            Settings.System.putString(context.getContentResolver(), "CustomWatchfaceData", json_data.toString());
        } catch (JSONException e) {
            Settings.System.putString(context.getContentResolver(), "CustomWatchfaceData", "{\"phoneBattery\":\"" + phoneBattery + "\",\"phoneAlarm\":\"" + phoneAlarm + "\"}");//default
        }
    }

   @Subscribe(threadMode = ThreadMode.MAIN)
    public void lowPower(LowPower lowPower) {
       SystemProperties.goToSleep(this);
       slptClockClient.enableLowBattery();
       slptClockClient.enableSlpt();
       SystemProperties.setSystemProperty("sys.state.powerlow", String.valueOf(true));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void settingsSync(SyncSettings event) {
        SettingsData settingsData = SettingsData.fromDataBundle(event.getDataBundle());
        settingsManager.sync(settingsData);

        setupHardwareKeysMusicControl(settingsData.isEnableHardwareKeysMusicControl());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void reply(ReplyNotificationEvent event) {
        Log.d(Constants.TAG, "MainService reply to notification, key: " + event.getKey() + ", message: " + event.getMessage());

        dataBundle.putString("key", event.getKey());
        dataBundle.putString("message", event.getMessage());

        send(Transport.REPLY, dataBundle);
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
        watchStatusData.setRoBuildDate(SystemProperties.get(WatchStatusData.RO_BUILD_DATE, "-"));
        watchStatusData.setRoBuildDescription(SystemProperties.get(WatchStatusData.RO_BUILD_DESCRIPTION, "-"));
        watchStatusData.setRoBuildDisplayId(SystemProperties.get(WatchStatusData.RO_BUILD_DISPLAY_ID, "-"));
        watchStatusData.setRoBuildHuamiModel(SystemProperties.get(WatchStatusData.RO_BUILD_HUAMI_MODEL, "-"));
        watchStatusData.setRoBuildHuamiNumber(SystemProperties.get(WatchStatusData.RO_BUILD_HUAMI_NUMBER, "-"));
        watchStatusData.setRoProductDevice(SystemProperties.get(WatchStatusData.RO_PRODUCT_DEVICE, "-"));
        watchStatusData.setRoProductManufacter(SystemProperties.get(WatchStatusData.RO_PRODUCT_MANUFACTER, "-"));
        watchStatusData.setRoProductModel(SystemProperties.get(WatchStatusData.RO_PRODUCT_MODEL, "-"));
        watchStatusData.setRoProductName(SystemProperties.get(WatchStatusData.RO_PRODUCT_NAME, "-"));
        watchStatusData.setRoRevision(SystemProperties.get(WatchStatusData.RO_REVISION, "-"));
        watchStatusData.setRoSerialno(SystemProperties.get(WatchStatusData.RO_SERIALNO, "-"));
        watchStatusData.setRoBuildFingerprint(SystemProperties.get(WatchStatusData.RO_BUILD_FINGERPRINT, "-"));

        Log.d(Constants.TAG, "MainService requestWatchStatus watchStatusData: " + watchStatusData.toString());
        send(Transport.WATCH_STATUS, watchStatusData.toDataBundle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestBatteryStatus(RequestBatteryStatus requestBatteryStatus) {

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        batteryPct = level / (float) scale;

        //Get data of last full charge from settings
        //Use WidgetSettings to share data with Springboard widget (SharedPreferences didn't work)
        if (dateLastCharge == 0) {
            dateLastCharge = settings.get(Constants.PREF_DATE_LAST_CHARGE, 0L);
            Log.d(Constants.TAG, "MainService dateLastCharge loaded: " + dateLastCharge);
        }

        //Update battery level (used in widget)
        //settings.set(Constants.PREF_BATT_LEVEL, Math.round(batteryPct * 100.0));
        Log.d(Constants.TAG, "MainService dateLastCharge: " + dateLastCharge + " batteryPct: " + Math.round(batteryPct * 100f));

        batteryData.setLevel(batteryPct);
        batteryData.setCharging(isCharging);
        batteryData.setUsbCharge(usbCharge);
        batteryData.setAcCharge(acCharge);
        batteryData.setDateLastCharge(dateLastCharge);

        send(Transport.BATTERY_STATUS, batteryData.toDataBundle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void brightness(Brightness brightness) {
        BrightnessData brightnessData = BrightnessData.fromDataBundle(brightness.getDataBundle());
        Log.d(Constants.TAG, "MainService setting brightness to " + brightnessData.getLevel());

        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessData.getLevel());
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
        if (!transporter.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "MainService Transport Service Not Connected");
            return;
        }

        if (dataBundle != null) {
            Log.d(Constants.TAG, "MainService send1: " + action);
            transporter.send(action, dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Log.d(Constants.TAG, "Send result: " + dataTransportResult.toString());
                }
            });

        } else {
            Log.d(Constants.TAG, "MainService send2: " + action);
            transporter.send(action, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Log.d(Constants.TAG, "Send result: " + dataTransportResult.toString());
                }
            });
        }
    }
}
