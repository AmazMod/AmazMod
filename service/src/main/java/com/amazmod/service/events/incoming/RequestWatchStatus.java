package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class RequestWatchStatus {

    private DataBundle dataBundle;

    public RequestWatchStatus(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
