package com.edotassi.amazmodcompanionservice;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.edotassi.amazmodcompanionservice.events.NightscoutRequestSyncEvent;
import com.edotassi.amazmodcompanionservice.events.ReplyNotificationEvent;
import com.edotassi.amazmodcompanionservice.events.SyncSettingsEvent;
import com.edotassi.amazmodcompanionservice.events.incoming.IncomingNotificationEvent;
import com.edotassi.amazmodcompanionservice.events.incoming.RequestBatteryStatus;
import com.edotassi.amazmodcompanionservice.events.incoming.RequestWatchStatus;
import com.edotassi.amazmodcompanionservice.notifications.NotificationService;
import com.edotassi.amazmodcompanionservice.settings.SettingsManager;
import com.edotassi.amazmodcompanionservice.util.DeviceUtil;
import com.edotassi.amazmodcompanionservice.util.SystemProperties;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.Transporter;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.BatteryData;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.WatchStatusData;

public class MessagesListener {

    private Transporter transporter;

    private Context context;
    private SettingsManager settingsManager;
    private NotificationService notificationManager;

    public MessagesListener(Context context) {
        this.context = context;

        settingsManager = new SettingsManager(context);
        notificationManager = new NotificationService(context);
    }

    public void setTransporter(Transporter transporter) {
        this.transporter = transporter;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestNightscoutSync(NightscoutRequestSyncEvent event) {
        Log.d(Constants.TAG, "requested nightscout sync");

        send(Constants.ACTION_NIGHTSCOUT_SYNC, new DataBundle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void settingsSync(SyncSettingsEvent event) {
        Log.d(Constants.TAG, "sync settings");
        Log.d(Constants.TAG, "vibration: " + event.getNotificationVibration());
        Log.d(Constants.TAG, "timeout: " + event.getNotificationScreenTimeout());
        Log.d(Constants.TAG, "replies: " + event.getNotificationCustomReplies());

        settingsManager.sync(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void reply(ReplyNotificationEvent event) {
        Log.d(Constants.TAG, "reply to notification, key: " + event.getKey() + ", message: " + event.getMessage());

        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("key", event.getKey());
        dataBundle.putString("message", event.getMessage());

        send(Constants.ACTION_REPLY, dataBundle);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void incomingNotification(IncomingNotificationEvent incomingNotificationEvent) {
        //NotificationSpec notificationSpec = NotificationSpecFactory.getNotificationSpec(MainService.this, incomingNotificationEvent.getDataBundle());
        NotificationData notificationData = NotificationData.fromDataBundle(incomingNotificationEvent.getDataBundle());

        notificationData.setVibration(settingsManager.getInt(Constants.PREF_NOTIFICATION_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATION_VIBRATION));
        notificationData.setTimeoutRelock(settingsManager.getInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, Constants.PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT));
        notificationData.setDeviceLocked(DeviceUtil.isDeviceLocked(context));

        notificationManager.post(notificationData);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestWatchStatus(RequestWatchStatus requestWatchStatus) {
        WatchStatusData watchStatusData = new WatchStatusData();

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

        send(Transport.WATCH_STATUS, watchStatusData.toDataBundle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestBatteryStatus(RequestBatteryStatus requestBatteryStatus) {
        BatteryData batteryData = new BatteryData();

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

        float batteryPct = level / (float)scale;

        batteryData.setLevel(batteryPct);
        batteryData.setCharging(isCharging);
        batteryData.setUsbCharge(usbCharge);
        batteryData.setAcCharge(acCharge);

        send(Transport.BATTERY_STATUS, batteryData.toDataBundle());
    }

    private void send(String action) {
        send(action, null);
    }

    private void send(String action, DataBundle dataBundle) {
        if (transporter == null) {
            Log.w(Constants.TAG, "transporter not ready");
            return;
        }

        transporter.send(action, dataBundle);
    }
}
