package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.ResultDeleteFileData;

public class ResultDeleteFile {

    private ResultDeleteFileData resultDeleteFileData;

    public ResultDeleteFile(DataBundle dataBundle) {
        resultDeleteFileData = ResultDeleteFileData.fromDataBundle(dataBundle);
    }

    public ResultDeleteFileData getResultDeleteFileData() {
        return resultDeleteFileData;
    }
}
