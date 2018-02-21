package com.huami.watch.notification.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;


import com.edotasx.amazfit.Constants;
import com.huami.watch.transport.SafeParcelable;

import java.util.UUID;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 19/02/18.
 */

@SuppressLint("ParcelCreator")
@DexEdit(defaultAction = DexAction.IGNORE)
public class StatusBarNotificationData implements SafeParcelable {

    @DexIgnore
    public String pkg;
    @DexIgnore
    public String tag;
    @DexIgnore
    public String targetPkg;
    @DexIgnore
    public String groupKey;
    @DexIgnore
    public String key;
    @DexIgnore
    public NotificationData notification;

    @DexEdit(target = "from")
    public static StatusBarNotificationData source_from(StatusBarNotification pStatusBarNotification) {
        return null;
    };

    @DexAdd
    public static StatusBarNotificationData from(StatusBarNotification pStatusBarNotification) {
        StatusBarNotificationData statusBarNotificationData = source_from(pStatusBarNotification);

        statusBarNotificationData.pkg = UUID.randomUUID().toString();
        statusBarNotificationData.tag = UUID.randomUUID().toString();
        statusBarNotificationData.targetPkg = UUID.randomUUID().toString();
        statusBarNotificationData.key = /* statusBarNotificationData.key + "|" + */ UUID.randomUUID().toString();
        statusBarNotificationData.groupKey = /* statusBarNotificationData.groupKey + "|" +*/ UUID.randomUUID().toString();

        Log.d(Constants.TAG_NOTIFICATION, "key: " + statusBarNotificationData.toString());

        return statusBarNotificationData;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @DexAdd
    public static String getCompleteKey(Context context, StatusBarNotification statusBarNotification) {
        StatusBarNotificationData statusBarNotificationData = source_from(statusBarNotification);
        NotificationKeyData notificationKeyData = NotificationKeyData.from(statusBarNotificationData);
        NotificationData notificationData = NotificationData.from(context, notificationKeyData, statusBarNotification.getNotification(), false);

        return notificationData != null ?
                statusBarNotificationData.key + "|" + notificationData.text :
                statusBarNotificationData.key;
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
}
