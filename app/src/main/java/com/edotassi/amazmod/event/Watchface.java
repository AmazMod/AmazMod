package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.WatchfaceData;


public class Watchface {

    private WatchfaceData watchfaceData;

    public Watchface (DataBundle dataBundle) {
        watchfaceData = WatchfaceData.fromDataBundle(dataBundle);
    }

    public WatchfaceData getWatchfaceData() {
        return watchfaceData;
    }
}
