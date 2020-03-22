package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.WidgetsData;

public class OtherData {

    private DataBundle data;

    public OtherData(DataBundle dataBundle) {
        data = dataBundle;
    }

    public DataBundle getOtherData() {
        return data;
    }
}
