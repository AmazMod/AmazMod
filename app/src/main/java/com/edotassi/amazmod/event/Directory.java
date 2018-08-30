package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.DirectoryData;

public class Directory {

    private DirectoryData directoryData;

    public Directory(DataBundle dataBundle) {
        directoryData = DirectoryData.fromDataBundle(dataBundle);
    }

    public DirectoryData getDirectoryData() {
        return directoryData;
    }
}
