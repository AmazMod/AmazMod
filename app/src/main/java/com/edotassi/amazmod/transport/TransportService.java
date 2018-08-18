package com.edotassi.amazmod.transport;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.Constants;
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
import com.edotassi.amazmod.event.local.IsWatchConnectedLocal;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
//import com.edotassi.amazmod.log.Logger;
//import com.edotassi.amazmod.log.LoggerScoped;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.pixplicity.easyprefs.library.Prefs;
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

    //private LoggerScoped logger = LoggerScoped.get(TransportService.class);
    private Transporter transporter;
    private Context context;

    private Map<String, Class> messages = new HashMap<String, Class>() {{
        put(Transport.WATCH_STATUS, WatchStatus.class);
        put(Transport.BATTERY_STATUS, BatteryStatus.class);
        put(Transport.REPLY, NotificationReply.class);
    }};

    @Override
    public void onCreate() {
        //this.logger.debug("created");
        super.onCreate();

        context = this;

        //HermesEventBus.getDefault().connectApp(this, Constants.PACKAGE);
        //HermesEventBus.getDefault().init(this);
        HermesEventBus.getDefault().register(this);

        transporter = TransporterClassic.get(this, Transport.NAME);
        transporter.addDataListener(this);

        if (transporter.isTransportServiceConnected()) {
            Log.i(Constants.TAG,"TransportService onCreate already connected");
        } else {
            Log.w(Constants.TAG,"TransportService onCreate not connected, connecting...");
            transporter.connectTransportService();
            AmazModApplication.isWatchConnected = false;
        }
    }

    @Override
    public void onDestroy() {

        HermesEventBus.getDefault().unregister(this);
        transporter.removeDataListener(this);
        transporter.disconnectTransportService();
        Log.d(Constants.TAG,"TransportService onDestroy");
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

        Log.d(Constants.TAG,"TransportService action: "+ action);

        Class messageClass = messages.get(action);

        if (messageClass != null) {
            Class[] args = new Class[1];
            args[0] = DataBundle.class;

            try {
                Constructor eventContructor = messageClass.getDeclaredConstructor(args);
                Object event = eventContructor.newInstance(transportDataItem.getData());

                Log.d(Constants.TAG, "TransportService onDataReceived: " + event.toString());
                HermesEventBus.getDefault().post(event);
            } catch (NoSuchMethodException e) {
                Log.d(Constants.TAG, "TransportService event mapped with action \"" + action + "\" doesn't have constructor with DataBundle as parameter");
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
        Log.d(Constants.TAG, "TransportService replyToNotification: " + notificationReply.toString());
        HermesEventBus.getDefault().post(replyToNotificationLocal);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void incomingNotification(OutcomingNotification outcomingNotification) {
        Log.d(Constants.TAG, "TransportService incomingNotification: " + outcomingNotification.toString());
        send(Transport.INCOMING_NOTIFICATION, outcomingNotification.getNotificationData());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void requestWatchStatus(RequestWatchStatus requestWatchStatus) {
        Log.d(Constants.TAG, "TransportService requestWatchStatus: " + requestWatchStatus.toString());
        send(Transport.REQUEST_WATCHSTATUS);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void requestBatteryStatus(RequestBatteryStatus requestBatteryStatus) {
        Log.d(Constants.TAG, "TransportService requestBatteryStatus: " + requestBatteryStatus.toString());
        send(Transport.REQUEST_BATTERYSTATUS, requestBatteryStatus.getPhoneBattery(context));
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void syncSettings(SyncSettings syncSettings) {
        Log.d(Constants.TAG, "TransportService syncSettings: " + syncSettings.toString());
        send(Transport.SYNC_SETTINGS, syncSettings.getSettingsData());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void brightness(Brightness brightness) {
        Log.d(Constants.TAG, "TransportService brightness: " + brightness.toString());
        send(Transport.BRIGHTNESS, brightness.getBrightnessData());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void batteryStatus(BatteryStatus batteryStatus) {
        BatteryData batteryData = batteryStatus.getBatteryData();
        Log.d(Constants.TAG,"batteryStatus: " + batteryData.getLevel());
        Log.d(Constants.TAG,"charging: " + batteryData.isCharging());
        Log.d(Constants.TAG,"usb: " + batteryData.isUsbCharge());
        Log.d(Constants.TAG,"ac: " + batteryData.isAcCharge());
        Log.d(Constants.TAG,"dateLastCharge: " + batteryData.getDateLastCharge());

        long date = System.currentTimeMillis();

        BatteryStatusEntity batteryStatusEntity = new BatteryStatusEntity();
        batteryStatusEntity.setAcCharge(batteryData.isAcCharge());
        batteryStatusEntity.setCharging(batteryData.isCharging());
        batteryStatusEntity.setDate(date);
        batteryStatusEntity.setLevel(batteryData.getLevel());
        batteryStatusEntity.setDateLastCharge(batteryData.getDateLastCharge());

        //Log.d(Constants.TAG,"TransportService batteryStatus: " + batteryStatus.toString());

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
            Log.e(Constants.TAG,"TransportService batteryStatus exception: " + ex.toString());
        }
        //Save time of last sync
        Prefs.putLong(Constants.PREF_TIME_LAST_SYNC, SystemClock.elapsedRealtime());
    }

    private void send(String action) {
        send(action, null);
    }

    private void send(String action, Transportable transportable) {
        boolean isTransportConnected = transporter.isTransportServiceConnected();
        if (!isTransportConnected) {
            if (AmazModApplication.isWatchConnected != isTransportConnected || (HermesEventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class)==null)) {
                AmazModApplication.isWatchConnected = isTransportConnected;
                HermesEventBus.getDefault().removeAllStickyEvents();
                HermesEventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected));
            }
            Log.w(Constants.TAG,"TransportService send Transport Service Not Connected");
            return;
        }

        if (transportable != null) {
                DataBundle dataBundle = new DataBundle();
                transportable.toDataBundle(dataBundle);
                Log.d(Constants.TAG,"TransportService send1: " + action);
                transporter.send(action, dataBundle, new Transporter.DataSendResultCallback() {
                    @Override
                    public void onResultBack(DataTransportResult dataTransportResult) {
                        Log.i(Constants.TAG,"Send result: " + dataTransportResult.toString());
                    }
                });

        } else {
                Log.d(Constants.TAG,"TransportService send2: " + action);
                transporter.send(action, new Transporter.DataSendResultCallback() {
                    @Override
                    public void onResultBack(DataTransportResult dataTransportResult) {
                        Log.i(Constants.TAG,"Send result: " + dataTransportResult.toString());
                        boolean check = (dataTransportResult.toString().contains("FAILED"));
                        if (AmazModApplication.isWatchConnected != check || (HermesEventBus.getDefault().getStickyEvent(IsWatchConnectedLocal.class)==null)) {
                            AmazModApplication.isWatchConnected = !check;
                            HermesEventBus.getDefault().removeAllStickyEvents();
                            HermesEventBus.getDefault().postSticky(new IsWatchConnectedLocal(AmazModApplication.isWatchConnected));
                            Log.d(Constants.TAG, "TransportService send2 isConnected: " + AmazModApplication.isWatchConnected);
                        }
                    }
                });
        }

    }
}
