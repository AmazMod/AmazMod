package com.huami.watch.notification.data;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.notification.NotificationManager;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.companion.ui.MainActivity;
import com.huami.watch.dataflow.model.health.process.Const;
import com.huami.watch.notification.NotificationHandler;

import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 25/01/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class NotificationData implements Parcelable {

    @DexEdit
    public static boolean DEBUG = true;
    @DexIgnore
    public String title;
    @DexIgnore
    public String text;
    @DexIgnore
    public NotificationKeyData key;
    @DexIgnore
    public Notification originalNotification;
    @DexIgnore
    public WearableExtrasData wearableExtras;

    @DexIgnore
    public static final Creator<NotificationData> CREATOR = null;

    @DexIgnore
    @Override
    public int describeContents() {
        return 0;
    }

    @DexIgnore
    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @DexWrap
    public static NotificationData from(Context var0, NotificationKeyData var1, Notification var2, boolean var3) {
        DEBUG = true;
        NotificationData notificationData = from(var0, var1, var2, var3);

        if (!PreferenceManager.getBoolean(var0, Constants.PREFERENCE_DISABLE_NOTIFICATIONS_MOD, false)) {
            notificationData.text = NotificationManager.sharedInstance().extractText(var2, notificationData);
        }

        /*

        notificationData.key.key = notificationData.key.key + ":" + UUID.randomUUID().toString();
        if (notificationData.key.tag != null) {
            notificationData.key.tag = notificationData.key.tag + ":" + UUID.randomUUID().toString();
        }
        */

        /*
        Log.d(Constants.TAG_NOTIFICATION, "pkg: " + notificationData.key.pkg);
        Log.d(Constants.TAG_NOTIFICATION, "id: " + notificationData.key.id);
        Log.d(Constants.TAG_NOTIFICATION, "key: " + notificationData.key.key);
        Log.d(Constants.TAG_NOTIFICATION, "tag: " + notificationData.key.tag);
        Log.d(Constants.TAG_NOTIFICATION, "targetPkg: " + notificationData.key.targetPkg);
        */

        if (PreferenceManager.getBoolean(var0, Constants.PREFERENCE_REVERSE_HEBREW_NOTIFCATIONS, false)) {
            /*
            Pattern pattern = Pattern.compile("\\p{InHebrew}", Pattern.UNICODE_CASE);
            if (pattern.matcher(notificationData.text).matches()) {
                notificationData.text = new StringBuilder(notificationData.text).reverse().toString();
            }

            if (pattern.matcher(notificationData.title).matches()) {
                notificationData.title = new StringBuilder(notificationData.title).reverse().toString();
            }
            */

            notificationData.text = reverse(notificationData.text);
            notificationData.title = reverse(notificationData.title);

            Log.d("ReverseNot", "reversed text: " + notificationData.text);
            Log.d("ReverseNot", "reversed title: " + notificationData.title);
        }

        /*
        Bundle bundle = var2.extras;
        Iterator<String> keys = bundle.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = bundle.getString(key);
            Log.d("Noti-Data", key + ": " + value);
        }
        */


        /*
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Bundle bundle = var2.extras.getBundle("android.wearable.EXTENSIONS");
            if (bundle != null) {
                Iterator<String> keys = bundle.keySet().iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = bundle.getString(key);
                    Log.d("Noti-Data", key + ": " + value);
                }
            } else {
                Log.d("Noti-Data", "wearable extensions null!!");
            }
        }
        */

        RemoteInputData remoteInput = new RemoteInputData();
        remoteInput.choices = new String[]{
                "A", "B"
        };
        remoteInput.label = "input";
        remoteInput.resultKey = "remoteKey";

        ActionData actionData = new ActionData();
        actionData.title = "title";
        actionData.icon = BitmapFactory.decodeResource(var0.getResources(), R.drawable.strava);
        actionData.remoteInputs = new RemoteInputData[]{remoteInput};

        WearableExtrasData wearableExtrasData = new WearableExtrasData();
        wearableExtrasData.actions = new ActionData[]{actionData};

        notificationData.wearableExtras = wearableExtrasData;

        return notificationData;
    }

    @DexAdd
    private static String reverse(String s) {
        if (s == null || s.length() < 2) return s;

        StringBuilder sb = new StringBuilder();
        for (int i = s.length() - 1; i >= 0; --i) {
            if (Character.isLowSurrogate(s.charAt(i))) {
                --i;
                sb.append(s.charAt(i)).append(s.charAt(i + 1));
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    @SuppressLint("ParcelCreator")
    @DexEdit(defaultAction = DexAction.IGNORE)
    public static class ActionData implements Parcelable {

        @DexIgnore
        public Bitmap icon;
        @DexIgnore
        public RemoteInputData[] remoteInputs;
        @DexIgnore
        public String title;

        @DexReplace
        public ActionData() {
        }

        @DexIgnore
        public int describeContents() {
            return 0;
        }

        @DexIgnore
        public boolean getIconBitmapRecycled() {
            return false;
        }

        @DexIgnore
        public void writeToParcel(Parcel parcel, int n2) {
        }

    }

    @SuppressLint("ParcelCreator")
    @DexEdit(defaultAction = DexAction.IGNORE)
    public static class WearableExtrasData implements Parcelable {

        @DexReplace
        public WearableExtrasData() {
        }

        @DexIgnore
        @Override
        public int describeContents() {
            return 0;
        }

        @DexIgnore
        @Override
        public void writeToParcel(Parcel parcel, int i) {
        }

        @DexIgnore
        public NotificationData.ActionData[] actions;
    }
}