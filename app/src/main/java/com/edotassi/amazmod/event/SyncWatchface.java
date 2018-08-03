package com.edotassi.amazmod.event;

import amazmod.com.transport.data.WatchfaceData;

public class SyncWatchface {

    private WatchfaceData watchfaceData;

    public SyncWatchface(WatchfaceData watchfaceData) {
        this.watchfaceData = watchfaceData;
    }

    public WatchfaceData getWatchfaceData() {
        return watchfaceData;
    }
}
