package com.edotassi.amazmod.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

public class Permissions {

    private static final String TAG_LOCAL = " Permissions ";

    public static boolean hasNotificationAccess(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = context.getPackageName();

        return !(enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName));
    }

    public static boolean hasPermission(Context context, String permission) {
        return Build.VERSION.SDK_INT <= 22 || PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(permission);
    }
}
