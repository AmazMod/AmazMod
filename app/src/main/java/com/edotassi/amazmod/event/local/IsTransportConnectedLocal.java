package com.edotassi.amazmod.event.local;

import amazmod.com.transport.data.NotificationReplyData;

public class IsTransportConnectedLocal {

    private boolean isTransportConnected;

    public IsTransportConnectedLocal(boolean isTransportConnected) {
        this.isTransportConnected = isTransportConnected;
    }

    public boolean getTransportStatus() {
        return isTransportConnected;
    }
}
