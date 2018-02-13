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

    public boolean filter(StatusBarNotification statusBarNotification) {
        String key = buildKey(statusBarNotification);
        Log.d(Constants.TAG, "notificationFilter -> key(" + key + ")");

        String packageName = statusBarNotification.getPackageName();
        String tag = statusBarNotification.getTag();

        Log.d(Constants.TAG, "id(" + statusBarNotification.getId() + ")");
        Log.d(Constants.TAG, "packageName(" + packageName + ")");
        Log.d(Constants.TAG, "postTime(" + statusBarNotification.getPostTime() + ")");
        Log.d(Constants.TAG, "key(" + statusBarNotification.getKey() + ")");
        Log.d(Constants.TAG, "groupKey(" + statusBarNotification.getGroupKey() + ")");
        Log.d(Constants.TAG, "isOngoing(" + statusBarNotification.isOngoing() + ")");
        Log.d(Constants.TAG, "overrideGroupKey(" + statusBarNotification.getOverrideGroupKey() + ")");
        Log.d(Constants.TAG, "tag(" + tag + ")");
        Log.d(Constants.TAG, "isGroup(" + statusBarNotification.isGroup() + ")");
        Log.d(Constants.TAG, "bundle: " + dumpBundle(NotificationCompat.getExtras(statusBarNotification.getNotification())));
        Log.d(Constants.TAG, "--------------------------");
        Log.d(Constants.TAG, "--------------------------");

        if ((packageName != null) && packageName.toLowerCase().equals(Constants.PACKAGE_WHATSAPP)) {
            // se la notification è di whatsapp e non ha tag allora è da saltare
            return tag == null;
        }

        return false;
    }

    private String dumpBundle(Bundle bundle) {
        String ris = "";

        Iterator<String> iterator = bundle.keySet().iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            String value = bundle.getString(next);

            ris += next + "(" + value + "), ";
        }

        return ris;
    }

    private String buildKey(StatusBarNotification statusBarNotification) {
        Bundle bundle = NotificationCompat.getExtras(statusBarNotification.getNotification());

        String packageName = statusBarNotification.getPackageName();
        String title = getTitle(bundle);
        String text = getText(bundle);

        return packageName + ":" + title + ":" + text;
    }

    private String getTitle(Bundle bundle) {
        return bundle.getString(Notification.EXTRA_TITLE);
    }

    private String getText(Bundle bundle) {
        String text = Utils.toString(bundle.getCharSequence("android.text"));
        if (TextUtils.isEmpty(text)) {
            CharSequence[] lines = bundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null) {
                int index = lines.length - 1;
                text = lines[index].toString();
            } else {
                text = Utils.toString(bundle.getCharSequence(Notification.EXTRA_BIG_TEXT));
                if (TextUtils.isEmpty((CharSequence) text)) {
                    text = Utils.toString(bundle.getCharSequence(Notification.EXTRA_INFO_TEXT));
                    if (TextUtils.isEmpty((CharSequence) text)) {
                        text = Utils.toString(bundle.getCharSequence(Notification.EXTRA_SUB_TEXT));
                        if (TextUtils.isEmpty((CharSequence) text)) {
                            text = Utils.toString(bundle.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
                            if (TextUtils.isEmpty((CharSequence) text)) {
                                text = "";
                            }
                        }
                    }
                }
            }
        }

        return text;
    }
}
