package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class RevokeAdminOwner {

    private DataBundle dataBundle;

    public RevokeAdminOwner(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
