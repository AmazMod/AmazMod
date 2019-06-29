package com.edotassi.amazmod.util;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;

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

    public static boolean checkWriteExternalStoragePermission(final Context context, final Activity activity) {
        if (Build.VERSION.SDK_INT <= 22)
            return true;
        else {
            if (PackageManager.PERMISSION_GRANTED == context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                return true;
            else {
                new MaterialDialog.Builder(context)
                        .title(R.string.activity_files_no_write_permission)
                        .content(R.string.activity_files_backup_error)
                        .positiveText(R.string.continue_label)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                ActivityCompat.requestPermissions(activity,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                            }
                        })
                        .show();
                return false;
            }
        }
    }

}
