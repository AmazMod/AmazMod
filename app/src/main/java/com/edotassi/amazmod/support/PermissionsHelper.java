package com.edotassi.amazmod.support;

import android.Manifest;
import android.app.Activity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

public class PermissionsHelper {

    public static Task checkPermission(Activity activity, String permission) {
        final TaskCompletionSource taskCompletionSource = new TaskCompletionSource();

        Dexter
                .withActivity(activity)
                .withPermission(permission)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        taskCompletionSource.setResult(null);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        taskCompletionSource.setException(new Exception(response.getPermissionName()));
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .check();

        return taskCompletionSource.getTask();
    }
}
