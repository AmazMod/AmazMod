package com.edotassi.amazmodcompanionservice.events.incoming;

import com.huami.watch.transport.DataBundle;

public class SyncSettings {

    private DataBundle dataBundle;

    public SyncSettings(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
