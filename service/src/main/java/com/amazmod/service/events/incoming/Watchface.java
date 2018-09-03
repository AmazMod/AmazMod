package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class Watchface {

    private DataBundle dataBundle;

    public Watchface(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
