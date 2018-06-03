package com.edotasx.amazfit;

import android.annotation.SuppressLint;

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
