package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class RequestDownloadFileChunk {

    private DataBundle dataBundle;

    public RequestDownloadFileChunk(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
