package com.huami.watch.notification.data;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.notification.NotificationManager;
import com.edotasx.amazfit.preference.PreferenceManager;

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

        return notificationData;
    }
}