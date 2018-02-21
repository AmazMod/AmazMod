package com.huami.watch.notification.data;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.huami.watch.companion.ui.MainActivity;
import com.huami.watch.dataflow.model.health.process.Const;

import java.util.UUID;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 25/01/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class NotificationData implements Parcelable {

    @DexIgnore
    public String text;
    @DexIgnore
    public NotificationKeyData key;
    @DexIgnore
    public Notification originalNotification;

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
        Log.d(Constants.TAG, "enter in NotificationData:from");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent(var0, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(var0, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Action action = new Notification.Action(1, "test", pendingIntent);
            Notification.Action[] actions = new Notification.Action[1];
            actions[0] = action;
            var2.actions = actions;
        }

        NotificationData notificationData = from(var0, var1, var2, var3);

        Bundle bundle = NotificationCompat.getExtras(var2);

        CharSequence[] lines = bundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) {
            int index = lines.length - 1;
            notificationData.text = lines[index].toString();
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

        return notificationData;
    }
}