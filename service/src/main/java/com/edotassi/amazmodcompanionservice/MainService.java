package com.edotassi.amazmodcompanionservice;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edotassi.amazmodcompanionservice.events.NightscoutDataEvent;
import com.edotassi.amazmodcompanionservice.events.SyncSettingsEvent;
import com.edotassi.amazmodcompanionservice.events.incoming.IncomingNotificationEvent;
import com.edotassi.amazmodcompanionservice.events.incoming.RequestBatteryStatus;
import com.edotassi.amazmodcompanionservice.events.incoming.RequestWatchStatus;
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

    private Map<String, Class> messages = new HashMap<String, Class>() {{
        put(Constants.ACTION_NIGHTSCOUT_SYNC, NightscoutDataEvent.class);
        put(Transport.SYNC_SETTINGS, SyncSettingsEvent.class);
        put(Transport.INCOMING_NOTIFICATION, IncomingNotificationEvent.class);
        put(Transport.REQUEST_WATCHSTATUS, RequestWatchStatus.class);
        put(Transport.REQUEST_BATTERYSTATUS, RequestBatteryStatus.class);
    }};

    @Override
    public void onCreate() {
        Log.d(Constants.TAG, "EventBus init");

        messagesListener = new MessagesListener(this);

        HermesEventBus.getDefault().init(this);
        HermesEventBus.getDefault().register(messagesListener);
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
