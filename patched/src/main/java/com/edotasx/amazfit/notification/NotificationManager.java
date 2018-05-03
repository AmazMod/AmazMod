package com.edotasx.amazfit.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.db.model.BatteryRead;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.companion.battery.BatteryInfoHelper;
import com.huami.watch.companion.battery.bean.BatteryInfo;
import com.huami.watch.companion.sync.SyncUtil;
import com.huami.watch.notification.data.StatusBarNotificationData;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public class NotificationManager {

    public static final int FLAG_WEARABLE_REPLY = 0x00000001;

    private static final boolean BLOCK_NOTIFICATION = true;
    private static final boolean CONTINUE_NOTIFICATION = false;

    private long lastNotificationTime = -1;

    private Transporter transporter;
    private Transporter.DataListener dataListener;

    private Map<String, Boolean> notificationTimeGone;

    private Context context;

    private static NotificationManager mInstace;

    public static NotificationManager initialize(Context context) {
        if (mInstace == null) {
            mInstace = new NotificationManager(context);
        }

        return mInstace;
    }

    public static NotificationManager sharedInstance(Context context) {
        return initialize(context);
    }

    public static NotificationManager sharedInstance() {
        return mInstace;
    }

    private NotificationManager(Context context) {
        this.context = context;

        notificationTimeGone = new HashMap<>();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean notificationPosted(StatusBarNotification statusBarNotification) {
        boolean filtered = filter(statusBarNotification);

        if (!filtered) {
            updateBatteryLevel();

            lastNotificationTime = System.currentTimeMillis();
        }

        return filtered;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean filter(StatusBarNotification pStatusBarNotification) {
        Log.d(Constants.TAG_NOTIFICATION, "notification from: " + pStatusBarNotification.getPackageName());

        String notificationId = StatusBarNotificationData.getUniqueKey(pStatusBarNotification);
        /*
        if (notificationTimeGone.containsKey(notificationId)) {
            Log.d(Constants.TAG_NOTIFICATION, "notification blocked by key: " + notificationId);
            return BLOCK_NOTIFICATION;
        }
        */

        notificationTimeGone.put(notificationId, true);

        Notification notification = pStatusBarNotification.getNotification();

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        List<NotificationCompat.Action> actions = wearableExtender.getActions();

        int flags = 0;

        for (NotificationCompat.Action act : actions) {
            if (act != null && act.getRemoteInputs() != null) {
                flags |= FLAG_WEARABLE_REPLY;
                break;
            }
        }

        if ((flags & FLAG_WEARABLE_REPLY) == 0 && NotificationCompat.isGroupSummary(notification)) {
            Log.d(Constants.TAG_NOTIFICATION, "notification blocked FLAG_WEARABLE_REPLY");
            return BLOCK_NOTIFICATION;
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            Log.d(Constants.TAG_NOTIFICATION, "notification blocked FLAG_ONGOING_EVENT");
            return BLOCK_NOTIFICATION;
        }

        if (NotificationCompat.getLocalOnly(notification)) {
            Log.d(Constants.TAG_NOTIFICATION, "notification blocked because is LocalOnly");
            return BLOCK_NOTIFICATION;
        }

        Log.d(Constants.TAG_NOTIFICATION, "notification allowd");
        Log.d(Constants.TAG_NOTIFICATION, "_");
        Log.d(Constants.TAG_NOTIFICATION, "_");
        return CONTINUE_NOTIFICATION;
    }

    private void updateBatteryLevel() {
        if (PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_BATTERY_CHART, false)) {
            return;
        }

        int intervalWithNotifications = 1000 * 60 * PreferenceManager.getInt(context, Constants.PREFERENCE_BATTERY_SYNC_WITH_NOTIFICATIONS, 5);
        if ((lastNotificationTime == -1) || (System.currentTimeMillis() - lastNotificationTime) > intervalWithNotifications) {
            Log.d("BatteryAlarm", "notification trigger");

            if (transporter == null) {
                transporter = Transporter.get(context, "com.huami.watch.companion");

                dataListener = new Transporter.DataListener() {
                    @Override
                    public void onDataReceived(TransportDataItem transportDataItem) {
                        String action = transportDataItem.getAction();

                        Log.d("BatteryAlarm", "action: " + action);

                        if ("com.huami.watch.companion.transport.SyncBattery".equals(action)) {
                            BatteryInfo batteryInfo = BatteryInfoHelper.getBatteryInfo(transportDataItem.getData());

                            Log.d("BatteryAlarm", "batteryLvl: " + batteryInfo.getBatteryLevel());

                            FlowManager.init(context);

                            BatteryRead batteryRead = new BatteryRead();
                            batteryRead.setDate(System.currentTimeMillis());
                            batteryRead.setLevel(batteryInfo.getBatteryLevel());
                            batteryRead.setCharging(batteryInfo.isBatteryCharging());

                            FlowManager.getModelAdapter(BatteryRead.class).insert(batteryRead);

                            transporter.removeDataListener(dataListener);
                        }
                    }
                };

                transporter.addDataListener(dataListener);
                transporter.connectTransportService();
            }

            SyncUtil.syncRequestBattery(transporter, true);
        }
    }
}
