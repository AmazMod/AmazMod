package com.edotassi.amazmod.support;

import android.util.Log;

import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity_Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.text.SimpleDateFormat;
import java.util.Date;

import amazmod.com.transport.Constants;

public class SilenceApplicationHelper {

    public static void silenceAppFromNotification(String notificationKey, int minutes) {
        Log.d(Constants.TAG, "SilenceApplicationHelper silenceApp: " + notificationKey + " / Minutes: " + String.valueOf(minutes));
        String packageName = notificationKey.split("\\|")[1];
        silenceApp(packageName, minutes);
    }

    public static void silenceApp(String packageName, int minutes) {
        Log.d(Constants.TAG, "SilenceApplicationHelper silenceApp: " + packageName + " / Minutes: " + String.valueOf(minutes));
        NotificationPreferencesEntity pref = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle();

        if (pref != null) {
            long seconds = minutes * 60;
            long silenced = getCurrentTimeSeconds() + seconds;
            pref.setSilenceUntil(silenced);
            FlowManager
                    .getModelAdapter(NotificationPreferencesEntity.class)
                    .update(pref);
            Log.d(Constants.TAG, "SilenceApplicationHelper silenceApp: silenced " + packageName + " until " + getTimeSecondsReadable(silenced));
        } else {
            Log.d(Constants.TAG, "SilenceApplicationHelper silenceApp: package " + packageName + " not found in NotificationPreference table");
        }
    }

    public static long getCurrentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static String getTimeSecondsReadable() {
        return getTimeSecondsReadable(getCurrentTimeSeconds());
    }

    public static String getTimeSecondsReadable(long timeSeconds) {
        if (timeSeconds == 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-dd-MM - HH:mm");
        Date resultdate = new Date(timeSeconds * 1000);
        return sdf.format(resultdate);
    }
}
