package com.amazmod.service.events.incoming;

import com.huami.watch.transport.DataBundle;

public class RequestUploadFileChunk {

    private DataBundle dataBundle;

    public RequestUploadFileChunk(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
