package com.amazmod.service.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amazmod.service.R;

public class AdminReceiver extends DeviceAdminReceiver {
    public static final String ACTION_DISABLED = "device_admin_action_disabled";
    public static final String ACTION_ENABLED = "device_admin_action_enabled";

    void showToast(Context context, String msg) {
        String status = context.getResources().getString(R.string.admin_receiver_status, msg);
        Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(
                new Intent(ACTION_DISABLED));
        showToast(context, context.getResources().getString(R.string.disabled));
    }
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(
                new Intent(ACTION_ENABLED));
        showToast(context, context.getResources().getString(R.string.enabled));
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getResources().getString(R.string.admin_receiver_status_disable_warning);
    }

}