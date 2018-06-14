package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.WatchStatusData;

public class WatchStatus {

    private WatchStatusData watchStatusData;

    public WatchStatus(DataBundle dataBundle) {
        this.watchStatusData = WatchStatusData.fromDataBundle(dataBundle);
    }

    public WatchStatusData getWatchStatusData() {
        return watchStatusData;
    }
}
