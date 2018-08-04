package com.edotassi.amazmodcompanionservice.events.incoming;

import android.provider.ContactsContract;
import android.provider.Settings;

import com.edotassi.amazmodcompanionservice.MainService;
import com.huami.watch.transport.DataBundle;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestBatteryStatus {

    private DataBundle dataBundle;

    private String phoneBattery;
    private String phoneAlarm;

    public RequestBatteryStatus(DataBundle dataBundle) {
        this.dataBundle = dataBundle;
    }

    public DataBundle getDataBundle() {
        return dataBundle;
    }
}
