package com.edotassi.amazmod.transport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edotassi.amazmod.event.OutcomingNotification;
import com.edotassi.amazmod.event.RequestWatchStatus;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.log.LoggerScoped;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import amazmod.com.transport.Transport;
import amazmod.com.transport.Transportable;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class TransportService extends Service implements Transporter.DataListener {

    private LoggerScoped logger = LoggerScoped.get(TransportService.class);
    private Transporter transporter;

    private Map<String, Class> messages = new HashMap<String, Class>() {{
        put(Transport.WATCH_STATUS, WatchStatus.class);
    }};

    @Override
    public void onCreate() {
        this.logger.debug("created");
        super.onCreate();

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

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void incomingNotification(OutcomingNotification outcomingNotification) {
        send(Transport.INCOMING_NOTIFICATION, outcomingNotification.getNotificationData());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void requestWatchStatus(RequestWatchStatus requestWatchStatus) {
        send(Transport.REQUEST_WATCHSTATUS);
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
            transporter.send(action);
        }
    }
}
