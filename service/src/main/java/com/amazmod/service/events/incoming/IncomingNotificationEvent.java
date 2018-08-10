package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class IncomingNotificationEvent {

    private DataBundle dataBundle;

    public IncomingNotificationEvent(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
