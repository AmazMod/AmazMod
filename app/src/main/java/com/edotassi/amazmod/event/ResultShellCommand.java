package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.ResultShellCommandData;

public class ResultShellCommand {

    private ResultShellCommandData resultShellCommandData;

    public ResultShellCommand(DataBundle dataBundle) {
        resultShellCommandData = ResultShellCommandData.fromDataBundle(dataBundle);
    }

    public ResultShellCommandData getResultShellCommandData() {
        return resultShellCommandData;
    }
}
