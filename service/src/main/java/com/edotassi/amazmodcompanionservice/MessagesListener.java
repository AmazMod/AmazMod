package com.edotassi.amazmodcompanionservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.provider.Settings.System;

import com.edotassi.amazmodcompanionservice.events.NightscoutRequestSyncEvent;
import com.edotassi.amazmodcompanionservice.events.ReplyNotificationEvent;
import com.edotassi.amazmodcompanionservice.events.incoming.Brightness;
import com.edotassi.amazmodcompanionservice.events.incoming.IncomingNotificationEvent;
import com.edotassi.amazmodcompanionservice.events.incoming.RequestBatteryStatus;
import com.edotassi.amazmodcompanionservice.events.incoming.RequestWatchStatus;
import com.edotassi.amazmodcompanionservice.events.incoming.SyncSettings;
import com.edotassi.amazmodcompanionservice.notifications.NotificationService;
import com.edotassi.amazmodcompanionservice.settings.SettingsManager;
import com.edotassi.amazmodcompanionservice.springboard.WidgetSettings;
import com.edotassi.amazmodcompanionservice.util.DeviceUtil;
import com.edotassi.amazmodcompanionservice.util.SystemProperties;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.Transporter;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.xml.transform.sax.TemplatesHandler;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.BatteryData;
import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.SettingsData;
import amazmod.com.transport.data.WatchStatusData;

import static java.lang.System.currentTimeMillis;

public class MessagesListener {

    private Transporter transporter;

    private Context context;
    private SettingsManager settingsManager;
    private NotificationService notificationManager;
    private long dateLastCharge = 0;
    private long lcd = 1;
    private long dateDisconnect = 0;
    private boolean setDateLastCharge = false;

    public MessagesListener(Context context) {
        this.context = context;

        settingsManager = new SettingsManager(context);
        notificationManager = new NotificationService(context);
    }

    public long getDateLastCharge() {
        Log.d(Constants.TAG, "lastChargeDate getter: " + this.dateLastCharge + " / " + dateLastCharge);
        return this.dateLastCharge;
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
    public void settingsSync(SyncSettings event) {

        SettingsData settingsData = SettingsData.fromDataBundle(event.getDataBundle());

        Log.d(Constants.TAG, "sync settings");
        Log.d(Constants.TAG, "vibration: " + settingsData.getVibration());
        Log.d(Constants.TAG, "timeout: " + settingsData.getScreenTimeout());
        Log.d(Constants.TAG, "replies: " + settingsData.getReplies());
        Log.d(Constants.TAG, "enableCustomUi: " + settingsData.isNotificationsCustomUi());

        settingsManager.sync(settingsData);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void reply(ReplyNotificationEvent event) {
        Log.d(Constants.TAG, "reply to notification, key: " + event.getKey() + ", message: " + event.getMessage());

        DataBundle dataBundle = new DataBundle();
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

        // register our power status receivers
        IntentFilter powerDisconnectedFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        context.registerReceiver(PowerStatusReceiver, powerDisconnectedFilter);
        powerDisconnectedFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        //Get data of last full charge from settings and update file used by widget to display it
        // Use WidgetSettings to share data with Springboard widget (SharedPreferences didn't work)

        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        //this.dateLastCharge = sharedPreferences.getLong(Constants.PREF_DATE_LAST_CHARGE, 0L);

        WidgetSettings settings = new WidgetSettings(Constants.TAG, context);
        if (this.dateLastCharge == 0) {
            this.dateLastCharge = settings.get(Constants.PREF_DATE_LAST_CHARGE, 0L);
        }

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

        float batteryPct = level / (float) scale;


        //Update date of last charge if it is detected that the charger was disconnected
        if (setDateLastCharge && (batteryPct > 0.98)) {
            this.dateLastCharge = dateDisconnect;
            setDateLastCharge = false;
            settings.set(Constants.PREF_DATE_LAST_CHARGE, this.dateLastCharge);
            //SharedPreferences.Editor editor = sharedPreferences.edit();
            //editor.putLong(Constants.PREF_DATE_LAST_CHARGE, this.dateLastCharge);
            //editor.apply();
        }

        batteryData.setLevel(batteryPct);
        batteryData.setCharging(isCharging);
        batteryData.setUsbCharge(usbCharge);
        batteryData.setAcCharge(acCharge);
        batteryData.setDateLastCharge(this.dateLastCharge);

        send(Transport.BATTERY_STATUS, batteryData.toDataBundle());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void brightness(Brightness brightness) {
        BrightnessData brightnessData = BrightnessData.fromDataBundle(brightness.getDataBundle());
        Log.d(Constants.TAG, "setting brightness to " + brightnessData.getLevel());

        System.putInt(context.getContentResolver(), System.SCREEN_BRIGHTNESS, brightnessData.getLevel());
    }

    //Receiver for Charger Disconnected
    private BroadcastReceiver PowerStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            setDateLastCharge = true;
            dateDisconnect = currentTimeMillis();
        }
    };

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
