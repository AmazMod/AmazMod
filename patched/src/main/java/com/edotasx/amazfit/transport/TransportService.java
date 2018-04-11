package com.edotasx.amazfit.transport;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.nightscout.NightscoutHelper;
import com.edotasx.amazfit.nightscout.NightscoutService;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

/**
 * Created by edoardotassinari on 07/04/18.
 */

public class TransportService {

    private static TransportService instance;

    private Transporter transporter;

    private TransportService(final Context context) {
        transporter = TransporterClassic.get(context, Constants.TRASPORTER_MODULE);

        if (!transporter.isTransportServiceConnected()) {
            Log.d(Constants.TAG, "transport service not connected");
            transporter.connectTransportService();
        } else {
            Log.d(Constants.TAG, "transport service connected");
        }

        transporter.addDataListener(new Transporter.DataListener() {
            @Override
            public void onDataReceived(TransportDataItem transportDataItem) {
                String action = transportDataItem.getAction();
                Log.d(Constants.TAG, "action: " + action);

                handleAction(context, action, transportDataItem);
            }
        });
    }

    public static TransportService sharedInstance(Context context) {
        if (instance == null) {
            instance = new TransportService(context);
        }

        return instance;
    }

    public void send(String action) {
        transporter.send(action);
    }

    public void send(String action, DataBundle dataBundle) {
        transporter.send(action, dataBundle);
    }

    private void handleAction(Context context, String action, TransportDataItem transportDataItem) {
        if (action == null) {
            return;
        }

        if (action.equals(Constants.NIGHTSCOUT_SYNC_ACTION)) {
            Intent nightscoutServiceIntent = new Intent(context, NightscoutService.class);
            context.startService(nightscoutServiceIntent);
        }
    }
}
