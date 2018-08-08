package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class RequestBatteryStatus {

    private DataBundle dataBundle;

    private String phoneBattery;
    private String phoneAlarm;

    public RequestBatteryStatus(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
