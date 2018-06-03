package com.edotassi.amazmod.transport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.support.annotation.Nullable;

import com.edotassi.amazmod.event.OutcomingNotification;
import com.edotassi.amazmod.event.RequestWatchStatus;
import com.edotassi.amazmod.log.Logger;
import com.edotassi.amazmod.log.LoggerScoped;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.SafeParcelable;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class TransportService extends Service implements Transporter.DataListener {

    private final String TRANSPORTER_NAME = "com.edotassi.amazmod";
    private final String PAYLOAD_NAME = "payload";

    private final String ACTION_INCOMING_NOTIFICATION = "incoming_notification";
    private final String ACTION_REQUEST_WATCHSTATUS = "request_watchstatus";

    private LoggerScoped logger = LoggerScoped.get(TransportService.class);
    private Transporter transporter;

    @Override
    public void onCreate() {
        this.logger.debug("created");
        super.onCreate();

        HermesEventBus.getDefault().register(this);

        transporter = TransporterClassic.get(this, TRANSPORTER_NAME);
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
            //dataBundle.putParcelable(PAYLOAD_NAME, parcelable);

            /*
            Parcel parcel = Parcel.obtain();
            parcelable.writeToParcel(parcel, 0);
            */

            transporter.send(action, dataBundle, new Transporter.DataSendResultCallback() {
                @Override
                public void onResultBack(DataTransportResult dataTransportResult) {
                    Logger.debug("Send result: %s", dataTransportResult.getResultCode());
                    Logger.debug("Send result: %s", dataTransportResult.toString());
                }
            });
        } else {
            transporter.send(action);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void outcomingNotification(OutcomingNotification outcomingNotification) {
        send(ACTION_INCOMING_NOTIFICATION, outcomingNotification.getNotificationData());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void requestWatchStatus(RequestWatchStatus requestWatchStatus) {
        send(ACTION_REQUEST_WATCHSTATUS);
    }
}
