package com.edotassi.amazmodcompanionservice.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.edotassi.amazmodcompanionservice.Constants;

/**
 * Created by edoardotassinari on 25/04/18.
 */

public class NotificationsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        int notificationId = intent.getIntExtra("id", -1);
        String reply = intent.getStringExtra("reply");


        Log.d(Constants.TAG, "action: " +action + ", notificationId: " + notificationId + ", reply: " + reply);
    }
}
