package com.edotasx.amazfit.notification;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.db.model.BatteryRead;
import com.edotasx.amazfit.notification.filter.TimeNotificationFilter;
import com.edotasx.amazfit.notification.filter.UniqueKeyNotificationFilter;
import com.edotasx.amazfit.notification.filter.NotificationFilter;
import com.edotasx.amazfit.notification.filter.app.WhatsappNotificationFilter;
import com.edotasx.amazfit.notification.text.extractor.DefaultTextExtractor;
import com.edotasx.amazfit.notification.text.extractor.TelegramTextExtractor;
import com.edotasx.amazfit.notification.text.extractor.TextExtractor;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.companion.battery.BatteryInfoHelper;
import com.huami.watch.companion.battery.bean.BatteryInfo;
import com.huami.watch.companion.sync.SyncUtil;
import com.huami.watch.notification.data.NotificationData;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public class NotificationManager {

    private long lastNotificationTime = -1;

    private Transporter transporter;
    private Transporter.DataListener dataListener;

    private UniqueKeyNotificationFilter uniqueKeyNotificationFilter;
    private TimeNotificationFilter timeNotificationFilter;
    private WhatsappNotificationFilter whatsappNotificationFilter;

    private Map<String, TextExtractor> textExtractors;
    private TextExtractor defaultTextExtractor;

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

        textExtractors = new HashMap<>();

        defaultTextExtractor = new DefaultTextExtractor();
        textExtractors.put(Constants.TELEGRAM_PACKAGE, new TelegramTextExtractor());

        whatsappNotificationFilter = new WhatsappNotificationFilter(context);

        uniqueKeyNotificationFilter = new UniqueKeyNotificationFilter(context);
        timeNotificationFilter = new TimeNotificationFilter();
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean filter(StatusBarNotification pStatusBarNotification) {
        boolean filter = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            filter = timeNotificationFilter.filter(pStatusBarNotification);
        }

        if (filter) {
            return filter;
        }

        if (pStatusBarNotification.getPackageName().equals(Constants.WHATSAPP_PACKAGE)) {
            filter = whatsappNotificationFilter.filter(pStatusBarNotification);
        }

        return filter;
    }

    public String extractText(Notification notification, NotificationData notificationData) {
        String packageName = notificationData.key.pkg;
        TextExtractor textExtractor = textExtractors.get(packageName) == null ?
                defaultTextExtractor :
                textExtractors.get(packageName);

        return textExtractor.extractText(notification, notificationData);
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
