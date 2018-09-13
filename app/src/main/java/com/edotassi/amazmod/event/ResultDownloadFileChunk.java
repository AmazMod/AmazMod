package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.ResultDownloadFileChunkData;

public class ResultDownloadFileChunk {

    private ResultDownloadFileChunkData resultDownloadFileChunkData;

    public ResultDownloadFileChunk(DataBundle dataBundle) {
        resultDownloadFileChunkData = ResultDownloadFileChunkData.fromDataBundle(dataBundle);
    }

    public ResultDownloadFileChunkData getResultDownloadFileChunkData() {
        return resultDownloadFileChunkData;
    }
}
