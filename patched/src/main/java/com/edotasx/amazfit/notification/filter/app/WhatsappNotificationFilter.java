package com.edotasx.amazfit.notification.filter.app;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.notification.filter.NotificationFilter;
import com.huami.watch.companion.util.DeviceCompatibility;
import com.huami.watch.notification.data.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by edoardotassinari on 18/02/18.
 */

public class WhatsappNotificationFilter implements NotificationFilter {

    private Map<String, Boolean> mNotificationsSent;

    private boolean isMIUI;

    public WhatsappNotificationFilter(Context context) {
        mNotificationsSent = new HashMap<>();

        isMIUI = DeviceCompatibility.MIUI.isMIUI(context);
    }

    @Override
    public String getPackage() {
        return Constants.WHATSAPP_PACKAGE;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean filter(StatusBarNotification statusBarNotification) {
        Bundle bundle = NotificationCompat.getExtras(statusBarNotification.getNotification());

        dumpBundle(bundle);

        if (bundle == null) {
            return false;
        } else {
            Bundle extraBundle = bundle.getBundle("android.wearable.EXTENSIONS");

            if (isMIUI) {
                String summaryText = bundle.getString("android.summaryText");

                return extraBundle == null || summaryText != null;
            } else {
                return extraBundle == null;
            }
        }

        /*
        String packageName = statusBarNotification.getPackageName();
        String TAG_NOTIFICATION = statusBarNotification.getTag();

        Log.d(Constants.TAG_NOTIFICATION, "id(" + statusBarNotification.getId() + ")");
        Log.d(Constants.TAG_NOTIFICATION, "packageName(" + packageName + ")");
        Log.d(Constants.TAG_NOTIFICATION, "postTime(" + statusBarNotification.getPostTime() + ")");
        Log.d(Constants.TAG_NOTIFICATION, "key(" + statusBarNotification.getKey() + ")");
        //Log.d(Constants.TAG_NOTIFICATION, "groupKey(" + statusBarNotification.getGroupKey() + ")");
        //Log.d(Constants.TAG_NOTIFICATION, "isOngoing(" + statusBarNotification.isOngoing() + ")");
        Log.d(Constants.TAG_NOTIFICATION, "tag(" + TAG_NOTIFICATION + ")");
        Log.d(Constants.TAG_NOTIFICATION, "bundle: " + dumpBundle(NotificationCompat.getExtras(statusBarNotification.getNotification())));
        Log.d(Constants.TAG_NOTIFICATION, "----");
        Log.d(Constants.TAG_NOTIFICATION, "----");

        if ((packageName != null) && packageName.toLowerCase().equals(Constants.WHATSAPP_PACKAGE)) {
            // se la notification è di whatsapp e non ha TAG_NOTIFICATION allora è da saltare
            return TAG_NOTIFICATION == null;
        }

        return false;
        */
    }

    private void dumpBundle(Bundle bundle) {
        Log.d("Whatsapp", "=========");

        if (bundle == null) {
            Log.d("Whatsapp", "bundle == null");
        } else {
            Iterator<String> iterator = bundle.keySet().iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                Object value = bundle.get(next);

                Log.d("Whatsapp", next + " => " + (value == null ? "null" : value.toString()));


                if (next.equals("android.template")) {
                    Object template = bundle.get("android.template");
                    Boolean templateIsInbox = template == null ? false : template instanceof Notification.InboxStyle;
                    Log.d("Whatsapp", "\ttemplateInbox => " + templateIsInbox.toString());
                }

                if (next.equals("android.wearable.EXTENSIONS")) {
                    Bundle extensionsBundle = bundle.getBundle("android.wearable.EXTENSIONS");
                    if (extensionsBundle != null) {
                        Iterator<String> extensionsIterator = extensionsBundle.keySet().iterator();
                        while (extensionsIterator.hasNext()) {
                            String nextExtensionKey = extensionsIterator.next();
                            Object nextExtensionValue = extensionsBundle.get(nextExtensionKey);

                            Log.d("Whatsapp", "\t" + nextExtensionKey + " => " + (nextExtensionValue == null ? "null" : nextExtensionValue.toString()));
                        }
                    }
                }
            }
        }

        Log.d("Whatsapp", "=========");
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private String buildKey(StatusBarNotification statusBarNotification) {
        Bundle bundle = NotificationCompat.getExtras(statusBarNotification.getNotification());

        String tag = statusBarNotification.getTag();
        if (tag == null) {
            return null;
        }

        String text = bundle.getString(Notification.EXTRA_TEXT);

        return tag + ":" + text;
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
