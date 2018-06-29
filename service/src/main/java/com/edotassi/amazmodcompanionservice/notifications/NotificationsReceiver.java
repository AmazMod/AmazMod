package com.edotassi.amazmodcompanionservice.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.edotassi.amazmodcompanionservice.Constants;
import com.edotassi.amazmodcompanionservice.events.ReplyNotificationEvent;

import xiaofei.library.hermeseventbus.HermesEventBus;

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
        String reply = intent.getStringExtra(Constants.EXTRA_REPLY);
        String key = intent.getStringExtra(Constants.EXTRA_NOTIFICATION_KEY);

        HermesEventBus.getDefault().post(new ReplyNotificationEvent(key, reply));
        Log.d(Constants.TAG, "action: " + action + ", notificationKey: " + key + ", reply: " + reply);
    }
}
