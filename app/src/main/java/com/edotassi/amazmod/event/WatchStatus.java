package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

public class WatchStatus {

    private DataBundle dataBundle;

    public WatchStatus(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
