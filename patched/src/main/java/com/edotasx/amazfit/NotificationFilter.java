package com.edotasx.amazfit;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.huami.watch.dataflow.model.health.process.Const;
import com.huami.watch.notification.data.Utils;

import java.util.Iterator;

/**
 * Created by edoardotassinari on 26/01/18.
 */

@SuppressLint("NewApi")
public class NotificationFilter {

    private static NotificationFilter mInstance;

    public static NotificationFilter sharedInstance() {
        if (mInstance == null) {
            mInstance = new NotificationFilter();
        }

        return mInstance;
    }


}
