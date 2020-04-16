package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

public class OtherData {

    private DataBundle data;

    public OtherData(DataBundle dataBundle) {
        data = dataBundle;
    }

    public DataBundle getOtherData() {
        return data;
    }
}
