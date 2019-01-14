package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class RequestShellCommand {

    private DataBundle dataBundle;

    public RequestShellCommand(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
