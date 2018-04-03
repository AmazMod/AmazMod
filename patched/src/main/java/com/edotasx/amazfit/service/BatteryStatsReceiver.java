package com.edotasx.amazfit.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.edotasx.amazfit.db.model.BatteryRead;
import com.huami.watch.companion.battery.BatteryInfoHelper;
import com.huami.watch.companion.battery.bean.BatteryInfo;
import com.huami.watch.companion.sync.SyncUtil;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.raizlabs.android.dbflow.config.FlowManager;

/**
 * Created by edoardotassinari on 02/04/18.
 */

public class BatteryStatsReceiver extends BroadcastReceiver {

    private Transporter transporter;
    private Transporter.DataListener dataListener;

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("BatteryAlarm", "onReceive");

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
        }

        SyncUtil.syncRequestBattery(transporter, true);
    }
}
