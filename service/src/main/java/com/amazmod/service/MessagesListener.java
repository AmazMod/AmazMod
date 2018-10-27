package com.amazmod.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import android.provider.Settings.System;

import com.amazmod.service.events.NightscoutRequestSyncEvent;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.events.incoming.Brightness;
import com.amazmod.service.events.incoming.IncomingNotificationEvent;
import com.amazmod.service.events.incoming.RequestBatteryStatus;
import com.amazmod.service.events.incoming.RequestWatchStatus;
import com.amazmod.service.events.incoming.SyncSettings;
import com.amazmod.service.notifications.NotificationService;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.springboard.WidgetSettings;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.SystemProperties;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.Transporter;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.BatteryData;
import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.SettingsData;
import amazmod.com.transport.data.WatchStatusData;

import static java.lang.System.currentTimeMillis;

/**
 * DEPRECATED
 **/

public class MessagesListener {

    private Transporter transporter;

    private Context context;
    private SettingsManager settingsManager;
    private NotificationService notificationManager;
    private long dateLastCharge = 0;
    private float batteryPct;
    private WidgetSettings settings;

    private BatteryData batteryData;
    private WatchStatusData watchStatusData;
    private DataBundle dataBundle;

    public MessagesListener(Context context) {

        this.context = context;
        settingsManager = new SettingsManager(context);
        notificationManager = new NotificationService(context);

        settings = new WidgetSettings(Constants.TAG, context);
        batteryData = new BatteryData();

        watchStatusData = new WatchStatusData();
        dataBundle = new DataBundle();

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
                    Log.d(Constants.TAG, "MessagesListener dateLastCharge saved: " + dateLastCharge);
                }
            }
        }, powerDisconnectedFilter);

    }

    public void setTransporter(Transporter transporter) {
        this.transporter = transporter;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestNightscoutSync(NightscoutRequestSyncEvent event) {
        Log.d(Constants.TAG, "MessagesListener requested nightscout sync");

        send(Constants.ACTION_NIGHTSCOUT_SYNC, new DataBundle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void settingsSync(SyncSettings event) {

        SettingsData settingsData = SettingsData.fromDataBundle(event.getDataBundle());

        Log.d(Constants.TAG, "MessagesListener sync settings");
        Log.d(Constants.TAG, "MessagesListener vibration: " + settingsData.getVibration());
        Log.d(Constants.TAG, "MessagesListener timeout: " + settingsData.getScreenTimeout());
        Log.d(Constants.TAG, "MessagesListener replies: " + settingsData.getReplies());
        Log.d(Constants.TAG, "MessagesListener enableCustomUi: " + settingsData.isNotificationsCustomUi());

        settingsManager.sync(settingsData);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void reply(ReplyNotificationEvent event) {
        Log.d(Constants.TAG, "MessagesListener reply to notification, key: " + event.getKey() + ", message: " + event.getMessage());

        dataBundle.putString("key", event.getKey());
        dataBundle.putString("message", event.getMessage());

        send(Transport.REPLY, dataBundle);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void incomingNotification(IncomingNotificationEvent incomingNotificationEvent) {
        //NotificationSpec notificationSpec = NotificationSpecFactory.getNotificationSpec(MainService.this, incomingNotificationEvent.getDataBundle());
        NotificationData notificationData = NotificationData.fromDataBundle(incomingNotificationEvent.getDataBundle());

        notificationData.setVibration(settingsManager.getInt(Constants.PREF_NOTIFICATION_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATION_VIBRATION));
        notificationData.setTimeoutRelock(settingsManager.getInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, Constants.PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT));
        notificationData.setDeviceLocked(DeviceUtil.isDeviceLocked(context));

        Log.d(Constants.TAG, "MessagesListener incomingNotification: " + notificationData.toString());
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

        Log.d(Constants.TAG, "MessagesListener requestWatchStatus watchStatusData: " + watchStatusData.toString());
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
            Log.d(Constants.TAG, "MessagesListener dateLastCharge loaded: " + dateLastCharge);
        }

        //Update battery level (used in widget)
        //settings.set(Constants.PREF_BATT_LEVEL, Math.round(batteryPct * 100.0));
        Log.d(Constants.TAG, "MessagesListener dateLastCharge: " + dateLastCharge + " batteryPct: " + Math.round(batteryPct * 100f));

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
        Log.d(Constants.TAG, "MessagesListener setting brightness to " + brightnessData.getLevel());

        if (brightnessData.getLevel()==  -1){
            System.putInt(context.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE, System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }else{
            System.putInt(context.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE, System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            System.putInt(context.getContentResolver(), System.SCREEN_BRIGHTNESS, brightnessData.getLevel());
        }
    }

    private void send(String action) {
        send(action, null);
    }

    private void send(String action, DataBundle dataBundle) {
        if (transporter == null) {
            Log.w(Constants.TAG, "MessagesListener transporter not ready");
            return;
        }
        transporter.send(action, dataBundle);
        Log.d(Constants.TAG, "MessagesListener send: " + action);
    }
}
