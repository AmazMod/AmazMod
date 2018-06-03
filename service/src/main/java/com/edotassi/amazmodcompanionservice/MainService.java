package com.edotassi.amazmodcompanionservice;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.edotassi.amazmodcompanionservice.events.NightscoutDataEvent;
import com.edotassi.amazmodcompanionservice.events.NightscoutRequestSyncEvent;
import com.edotassi.amazmodcompanionservice.events.ReplyNotificationEvent;
import com.edotassi.amazmodcompanionservice.events.SyncSettingsEvent;
import com.edotassi.amazmodcompanionservice.events.inbound.InfoInboundEvent;
import com.edotassi.amazmodcompanionservice.events.incoming.IncomingNotificationEvent;
import com.edotassi.amazmodcompanionservice.notifications.NotificationService;
import com.edotassi.amazmodcompanionservice.notifications.NotificationSpec;
import com.edotassi.amazmodcompanionservice.notifications.NotificationSpecFactory;
import com.edotassi.amazmodcompanionservice.notifications.NotificationsReceiver;
import com.edotassi.amazmodcompanionservice.settings.SettingsManager;
import com.edotassi.amazmodcompanionservice.util.DeviceUtil;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import xiaofei.library.hermeseventbus.HermesEventBus;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class MainService extends Service implements Transporter.ChannelListener, Transporter.ServiceConnectionListener {

    private Transporter companionTransporter;

    private NotificationsReceiver notificationsReceiver;

    private SettingsManager settingsManager;
    private NotificationService notificationManager;

    private Map<String, Class> messages = new HashMap<String, Class>() {{
        put(Constants.ACTION_NIGHTSCOUT_SYNC, NightscoutDataEvent.class);
        put(Constants.ACTION_SETTINGS_SYNC, SyncSettingsEvent.class);
        put(Constants.ACTION_INBOUND_INFO, InfoInboundEvent.class);
        put("incoming_notification", IncomingNotificationEvent.class);
    }};

    @Override
    public void onCreate() {
        Log.d(Constants.TAG, "EventBus init");

        HermesEventBus.getDefault().init(this);
        HermesEventBus.getDefault().register(this);

        notificationManager = new NotificationService(this);
        notificationsReceiver = new NotificationsReceiver();
        settingsManager = new SettingsManager(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.TAG, "MainService started");

        if (companionTransporter == null) {
            initTransporter();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onChannelChanged(boolean b) {
    }

    private void initTransporter() {
        companionTransporter = TransporterClassic.get(this, Constants.TRANSPORTER_MODULE);
        companionTransporter.addChannelListener(this);
        companionTransporter.addServiceConnectionListener(this);
        companionTransporter.addDataListener(new Transporter.DataListener() {
            @Override
            public void onDataReceived(TransportDataItem transportDataItem) {
                String action = transportDataItem.getAction();

                Log.d(Constants.TAG, "action: " + action + ", module: " + transportDataItem.getModuleName());

                if (action == null) {
                    return;
                }

                Class messageClass = messages.get(action);

                if (messageClass != null) {
                    Class[] args = new Class[1];
                    args[0] = DataBundle.class;

                    try {
                        Constructor eventContructor = messageClass.getDeclaredConstructor(args);
                        Object event = eventContructor.newInstance(transportDataItem.getData());

                        Log.d(Constants.TAG, "posting event " + event.toString());
                        HermesEventBus.getDefault().post(event);
                    } catch (NoSuchMethodException e) {
                        Log.w(Constants.TAG, "event mapped with action \"" + action + "\" doesn't have constructor with DataBundle as parameter");
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
        });

        if (!companionTransporter.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "connecting companionTransporter to transportService");
            companionTransporter.connectTransportService();
        }
    }

    @Override
    public void onServiceConnected(Bundle bundle) {
    }

    @Override
    public void onServiceConnectionFailed(Transporter.ConnectionResult connectionResult) {
    }

    @Override
    public void onServiceDisconnected(Transporter.ConnectionResult connectionResult) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestNightscoutSync(NightscoutRequestSyncEvent event) {
        Log.d(Constants.TAG, "requested nightscout sync");
        companionTransporter.send(Constants.ACTION_NIGHTSCOUT_SYNC, new DataBundle());
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

        companionTransporter.send(Constants.ACTION_REPLY, dataBundle);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void inboundInfo(InfoInboundEvent event) {
        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("version", BuildConfig.VERSION_NAME);

        companionTransporter.send(Constants.ACTION_OUTBOUND_INFO, dataBundle);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void incomingNotification(IncomingNotificationEvent incomingNotificationEvent) {
        //NotificationSpec notificationSpec = NotificationSpecFactory.getNotificationSpec(MainService.this, incomingNotificationEvent.getDataBundle());
        NotificationSpec notificationSpec = NotificationSpec.fromDataBundle(incomingNotificationEvent.getDataBundle());

        notificationSpec.setVibration(settingsManager.getInt(Constants.PREF_NOTIFICATION_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATION_VIBRATION));
        notificationSpec.setTimeoutRelock(settingsManager.getInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, Constants.PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT));
        notificationSpec.setDeviceLocked(DeviceUtil.isDeviceLocked(this));

        if (notificationSpec != null) {
            notificationManager.post(notificationSpec);
        } else {
            //TODO warn about notification null
        }
    }
}
