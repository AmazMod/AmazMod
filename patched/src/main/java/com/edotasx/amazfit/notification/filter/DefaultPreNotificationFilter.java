package com.edotasx.amazfit.notification.filter;

import android.content.Context;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.huami.watch.notification.data.StatusBarNotificationData;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by edoardotassinari on 21/02/18.
 */

public class DefaultPreNotificationFilter implements PreNotificationFilter {

    private Map<String, Long> notificationsLetGo;
    private Context context;

    public DefaultPreNotificationFilter(Context context) {
        notificationsLetGo = new HashMap<>();
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean filter(StatusBarNotification statusBarNotification) {
        String completeKey = StatusBarNotificationData.getCompleteKey(context, statusBarNotification);

        Log.d("CompleteKey", completeKey);

        if (notificationsLetGo.containsKey(completeKey)) {
            return true;
        } else {
            notificationsLetGo.put(completeKey, System.currentTimeMillis());
            return false;
        }
    }
}
