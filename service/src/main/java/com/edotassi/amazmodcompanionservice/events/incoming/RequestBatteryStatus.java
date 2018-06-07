package com.edotassi.amazmodcompanionservice.events.incoming;

import android.provider.ContactsContract;

import com.huami.watch.transport.DataBundle;

public class RequestBatteryStatus {

    private DataBundle dataBundle;

    public RequestBatteryStatus(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
