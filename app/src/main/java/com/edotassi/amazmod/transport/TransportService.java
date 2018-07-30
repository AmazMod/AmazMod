package com.edotassi.amazmod.transport;

import android.app.Service;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.edotassi.amazmod.db.model.BatteryStatusEntity;
import com.edotassi.amazmod.db.model.BatteryStatusEntity_Table;
import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.Brightness;
import com.edotassi.amazmod.event.NotificationReply;
import com.edotassi.amazmod.event.OutcomingNotification;
import com.edotassi.amazmod.event.RequestBatteryStatus;
import com.edotassi.amazmod.event.RequestWatchStatus;
import com.edotassi.amazmod.event.SyncSettings;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.log.LoggerScoped;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import amazmod.com.transport.Transport;
import amazmod.com.transport.Transportable;
import amazmod.com.transport.data.BatteryData;
import amazmod.com.transport.data.NotificationReplyData;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class TransportService extends Service implements Transporter.DataListener {

    private LoggerScoped logger = LoggerScoped.get(TransportService.class);
    private Transporter transporter;

    private Map<String, Class> messages = new HashMap<String, Class>() {{
        put(Transport.WATCH_STATUS, WatchStatus.class);
        put(Transport.BATTERY_STATUS, BatteryStatus.class);
        put(Transport.REPLY, NotificationReply.class);
    }};

    @Override
    public void onCreate() {
        this.logger.debug("created");
        super.onCreate();

        //HermesEventBus.getDefault().connectApp(this, Constants.PACKAGE);
        HermesEventBus.getDefault().register(this);

        transporter = TransporterClassic.get(this, Transport.NAME);
        transporter.addDataListener(this);

        if (!transporter.isTransportServiceConnected()) {
            this.logger.debug("not connected, connecting...");

            transporter.connectTransportService();
        } else {
            this.logger.debug("yet connected");
        }
    }

    @Override
    public void onDestroy() {

        HermesEventBus.getDefault().unregister(this);

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

        Logger.debug("[TransportService] action: %s", action);

        Class messageClass = messages.get(action);

        if (messageClass != null) {
            Class[] args = new Class[1];
            args[0] = DataBundle.class;

            try {
                Constructor eventContructor = messageClass.getDeclaredConstructor(args);
                Object event = eventContructor.newInstance(transportDataItem.getData());

                HermesEventBus.getDefault().post(event);
            } catch (NoSuchMethodException e) {
                Logger.warn("event mapped with action \"" + action + "\" doesn't have constructor with DataBundle as parameter");
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

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void replyToNotification(NotificationReply notificationReply) {
        ReplyToNotificationLocal replyToNotificationLocal = new ReplyToNotificationLocal(notificationReply.getNotificationReplyData());

        HermesEventBus.getDefault().post(replyToNotificationLocal);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void incomingNotification(OutcomingNotification outcomingNotification) {
        send(Transport.INCOMING_NOTIFICATION, outcomingNotification.getNotificationData());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void requestWatchStatus(RequestWatchStatus requestWatchStatus) {
        send(Transport.REQUEST_WATCHSTATUS);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void requestBatteryStatus(RequestBatteryStatus requestBatteryStatus) {
        send(Transport.REQUEST_BATTERYSTATUS);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void syncSettings(SyncSettings syncSettings) {
        send(Transport.SYNC_SETTINGS, syncSettings.getSettingsData());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void brightness(Brightness brightness) {
        send(Transport.BRIGHTNESS, brightness.getBrightnessData());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void batteryStatus(BatteryStatus batteryStatus) {
        BatteryData batteryData = batteryStatus.getBatteryData();
        logger.debug("batteryStatus: " + batteryData.getLevel());
        logger.debug("charging: " + batteryData.isCharging());
        logger.debug("usb: " + batteryData.isUsbCharge());
        logger.debug("ac: " + batteryData.isAcCharge());
        logger.debug("dateLastCharge: " + batteryData.getDateLastCharge());

        long date = System.currentTimeMillis();

        BatteryStatusEntity batteryStatusEntity = new BatteryStatusEntity();
        batteryStatusEntity.setAcCharge(batteryData.isAcCharge());
        batteryStatusEntity.setCharging(batteryData.isCharging());
        batteryStatusEntity.setDate(date);
        batteryStatusEntity.setLevel(batteryData.getLevel());
        batteryStatusEntity.setDateLastCharge(batteryData.getDateLastCharge());

        try {
            BatteryStatusEntity storeBatteryStatusEntity = SQLite
                    .select()
                    .from(BatteryStatusEntity.class)
                    .where(BatteryStatusEntity_Table.date.is(date))
                    .querySingle();

            if (storeBatteryStatusEntity == null) {
                FlowManager.getModelAdapter(BatteryStatusEntity.class).insert(batteryStatusEntity);
            }
        } catch (Exception ex) {
            //TODO add crashlitics
            Logger.error(ex, "");
        }
    }

    private void send(String action) {
        send(action, null);
    }

    private void send(String action, Transportable transportable) {
        if (!transporter.isTransportServiceConnected()) {
            return;
        }

        if (transportable != null) {
            DataBundle dataBundle = new DataBundle();
            transportable.toDataBundle(dataBundle);

            transporter.send(action, dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Logger.debug("Send result: %s", dataTransportResult.toString());
                }
            });
        } else {
            transporter.send(action, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Logger.debug("Send result: %s", dataTransportResult.toString());
                }
            });
        }
    }
}
