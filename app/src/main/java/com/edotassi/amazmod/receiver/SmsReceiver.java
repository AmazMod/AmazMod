package com.edotassi.amazmod.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.edotassi.amazmod.Constants;

public class SmsReceiver extends BroadcastReceiver {

    private final static String TAG_LOCAL = " SmsReceiver ";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(Constants.TAG+TAG_LOCAL, "started");

    }
}
