package com.amazmod.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.amazmod.service.Constants;
import com.amazmod.service.events.ReplyNotificationEvent;

import org.tinylog.Logger;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by edoardotassinari on 25/04/18.
 */

public class NotificationReplyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Logger.warn("NotificationReplyReceiver null action, returning...");
            return;
        }

        String action = intent.getAction();
        Logger.debug("NotificationReplyReceiver action: {}", action);

        if (Constants.INTENT_ACTION_REPLY.equals(action)) {
            String reply = intent.getStringExtra(Constants.EXTRA_REPLY);
            String key = intent.getStringExtra(Constants.EXTRA_NOTIFICATION_KEY);

            EventBus.getDefault().post(new ReplyNotificationEvent(key, reply));
            Logger.debug("NotificationReplyReceiver action: {} \\ notificationKey: {} \\ reply: {}", action, key, reply);

        /* USed for testing purposes only
        Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if(vibe != null) {
            vibe.vibrate(100);
            Logger.warn("NotificationReplyReceiver - vibRRRRRRRRRRRRate");
        }
        */
        }

    }
}
