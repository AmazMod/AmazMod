package com.edotasx.amazfit.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;

/**
 * Created by edoardotassinari on 06/02/18.
 */

public class PermissionManager {

    private static PermissionManager mInstance;

    public static PermissionManager sharedInstance() {
        if (mInstance == null) {
            mInstance = new PermissionManager();
        }

        return mInstance;
    }

    public void requestPermissions(Context context) {
        TedPermission.with(context)
                .setDeniedMessage(context.getString(R.string.not_all_permission_granted_warning))
                .setPermissionListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                    }

                    @Override
                    public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                    }
                })
                .setPermissions(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_CALENDAR,
                        Manifest.permission.WRITE_CALL_LOG,
                        Manifest.permission.GET_ACCOUNTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_CONTACTS
                )
                .check();
    }
}
