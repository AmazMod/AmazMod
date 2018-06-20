package com.edotassi.amazmodcompanionservice.events.incoming;

import com.huami.watch.transport.DataBundle;

public class Brightness {

    private DataBundle dataBundle;

    public Brightness(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
