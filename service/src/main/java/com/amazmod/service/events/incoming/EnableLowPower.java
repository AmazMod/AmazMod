package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class EnableLowPower {

    private DataBundle dataBundle;

    public EnableLowPower(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
