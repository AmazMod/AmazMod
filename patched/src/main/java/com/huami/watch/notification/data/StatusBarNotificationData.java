package com.huami.watch.notification.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;


import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.notification.NotificationManager;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.companion.CompanionApplication;
import com.huami.watch.companion.notification.NotificationAccessService;
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
    public NotificationData notification;

    @DexEdit(target = "from")
    public static StatusBarNotificationData source_from(StatusBarNotification pStatusBarNotification) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @DexAdd
    public static StatusBarNotificationData from(StatusBarNotification pStatusBarNotification) {
        StatusBarNotificationData statusBarNotificationData = source_from(pStatusBarNotification);

        Context context = CompanionApplication.getContext() == null ? NotificationAccessService.context : CompanionApplication.getContext();
        if (!PreferenceManager.getBoolean(context, Constants.PREFERENCE_DISABLE_NOTIFICATIONS_MOD, false)) {
            statusBarNotificationData.pkg = statusBarNotificationData.pkg + "|" + pStatusBarNotification.getNotification().when;
        }

        return statusBarNotificationData;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @DexAdd
    public static String getUniqueKey(StatusBarNotification statusBarNotification) {
        StatusBarNotificationData statusBarNotificationData = source_from(statusBarNotification);
        return statusBarNotificationData.pkg + "|" + statusBarNotification.getNotification().when;
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
