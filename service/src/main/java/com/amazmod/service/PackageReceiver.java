package com.amazmod.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.PowerManager;

import com.amazmod.service.ui.DummyActivity;
import com.amazmod.service.util.DeviceUtil;

import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;

public class PackageReceiver extends BroadcastReceiver {
    
    private static boolean isAmazmodInstall = false;

    public static boolean isAmazmodInstall() {
        return isAmazmodInstall;
    }

    public static void setIsAmazmodInstall(boolean flag) {
        PackageReceiver.isAmazmodInstall = flag;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Context mContext = context.getApplicationContext();
        Logger.debug("PackageReceiver onReceive action: " + action + " // " + intent.getDataString() + " // " + intent.getExtras());

        if (action != null) try {
            //This action can be used from adb for testing purposes using adb
            if (action.contains("MY_PACKAGE_REPLACED")) {
                if (intent.getExtras() != null) {
                    if (intent.getExtras().getString("TEST_TAG") != null)
                        showInstallConfirmation(mContext, intent.getExtras().getString("TEST_TAG"));
                    else {
                        //Test for installation script in internal storage and execute it if any
                        final File script = new File("/sdcard/update_service_apk.sh");
                        if (script.exists()) {
                            String command = String.format("log -pw -tAmazMod $(sh %s 2>&1)", script.getAbsolutePath());
                            Logger.debug("PackageReceiver onReceive command: " + command);
                            try {
                                Runtime.getRuntime().exec(new String[] { "sh", "-c", command },
                                        null, Environment.getExternalStorageDirectory());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                    }
                        // Restore apk_install screen timeout
                        Logger.debug("Restore APK_INSTALL screen timeout");
                        try{
                            Runtime.getRuntime().exec("adb shell settings put system screen_off_timeout 14000");
                        } catch (IOException e) {
                            Logger.error(e,"onCreate: IOException while restoring APK_INSTALL screen timeout");
                        }
                        showInstallConfirmation(mContext, Constants.MY_APP);
                    }
                }
            }

            //Restart launcher if another APK is installed using AmazMod
            if (action.contains("PACKAGE_REPLACED") || action.contains("PACKAGE_ADDED")) {
                if (intent.getDataString() != null) {
                    if (isAmazmodInstall()) {
                        showInstallConfirmation(mContext, Constants.OTHER_APP);
                        setIsAmazmodInstall(false);
                    }
                }
            }
        } catch (NullPointerException ex) {
            Logger.debug("PackageReceiver onReceive NullPointerException: " + ex.toString());
        }
    }

    private void showInstallConfirmation(Context context, String app_tag) {
        Intent intent = new Intent(context, DummyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.APP_TAG, app_tag);
        context.startActivity(intent);
        Logger.debug("PackageReceiver showInstallConfirmation app_tag: " + app_tag);
    }
}
