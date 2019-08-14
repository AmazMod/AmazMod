package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class DeleteNotificationEvent {

    private DataBundle dataBundle;

    public DeleteNotificationEvent(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
