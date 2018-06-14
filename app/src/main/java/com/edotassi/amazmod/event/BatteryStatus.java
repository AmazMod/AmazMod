package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.BatteryData;

public class BatteryStatus {

    private BatteryData batteryData;

    public BatteryStatus(DataBundle dataBundle) {
        batteryData = BatteryData.fromDataBundle(dataBundle);
    }

    public BatteryData getBatteryData() {
        return batteryData;
    }
}
