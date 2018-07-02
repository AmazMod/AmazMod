package com.edotassi.amazmodcompanionservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edotassi.amazmodcompanionservice.events.NightscoutDataEvent;
import com.edotassi.amazmodcompanionservice.events.incoming.Brightness;
import com.edotassi.amazmodcompanionservice.events.incoming.IncomingNotificationEvent;
import com.edotassi.amazmodcompanionservice.events.incoming.RequestBatteryStatus;
import com.edotassi.amazmodcompanionservice.events.incoming.RequestWatchStatus;
import com.edotassi.amazmodcompanionservice.events.incoming.SyncSettings;
import com.edotassi.amazmodcompanionservice.notifications.NotificationsReceiver;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import amazmod.com.transport.Transport;
import xiaofei.library.hermeseventbus.HermesEventBus;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class MainService extends Service implements Transporter.ChannelListener, Transporter.ServiceConnectionListener {

    private Transporter companionTransporter;

    private MessagesListener messagesListener;
    private NotificationsReceiver notificationsReceiver;

    private Map<String, Class> messages = new HashMap<String, Class>() {{
        put(Constants.ACTION_NIGHTSCOUT_SYNC, NightscoutDataEvent.class);
        put(Transport.SYNC_SETTINGS, SyncSettings.class);
        put(Transport.INCOMING_NOTIFICATION, IncomingNotificationEvent.class);
        put(Transport.REQUEST_WATCHSTATUS, RequestWatchStatus.class);
        put(Transport.REQUEST_BATTERYSTATUS, RequestBatteryStatus.class);
        put(Transport.BRIGHTNESS, Brightness.class);
    }};

    @Override
    public void onCreate() {
        Log.d(Constants.TAG, "EventBus init");

        messagesListener = new MessagesListener(this);

        notificationsReceiver = new NotificationsReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_ACTION_REPLY);

        registerReceiver(notificationsReceiver, filter);

        HermesEventBus.getDefault().init(this);
        HermesEventBus.getDefault().register(messagesListener);
    }

    @Override
    public void onDestroy() {
        HermesEventBus.getDefault().unregister(messagesListener);

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
        companionTransporter = TransporterClassic.get(this, Transport.NAME);
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

        messagesListener.setTransporter(companionTransporter);
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
}
