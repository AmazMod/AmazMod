package com.amazmod.service.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;

import com.amazmod.service.Constants;
import com.amazmod.service.events.ReplyNotificationEvent;

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
        Log.d(Constants.TAG, "NotificationsReceiver action: " + action + ", notificationKey: " + key + ", reply: " + reply);

        Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if(vibe != null) {
//            vibe.vibrate(100);
            Log.w(Constants.TAG, "NotificationsReceiver - vibRRRRRRRRRRRRate");
        }

    }
}
