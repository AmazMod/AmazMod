package com.edotassi.amazmodcompanionservice.util;

import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.edotassi.amazmodcompanionservice.Constants;

public class DeviceUtil {

    public static boolean isDeviceLocked(Context context) {
        boolean isLocked = false;

        // First we check the locked state
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean inKeyguardRestrictedInputMode = keyguardManager.inKeyguardRestrictedInputMode();

        if (inKeyguardRestrictedInputMode) {
            isLocked = true;

        } else {
            // If password is not set in the settings, the inKeyguardRestrictedInputMode() returns false,
            // so we need to check if screen on for this case

            PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                isLocked = !powerManager.isInteractive();
            } else {
                //noinspection deprecation
                isLocked = !powerManager.isScreenOn();
            }
        }

        return isLocked;
    }
    public static boolean isDNDActive(Context context, ContentResolver cr) {

        boolean dndEnabled = false;
        try {

            int zenModeValue = Settings.Global.getInt(cr, "zen_mode");

            switch (zenModeValue) {
                case 0:
                    Log.d(Constants.TAG, "DnD lowSDK: OFF");
                    dndEnabled = false;
                    break;
                case 1:
                    Log.d(Constants.TAG, "DnD lowSDK: ON - Priority Only");
                    dndEnabled = true;
                    break;
                case 2:
                    Log.d(Constants.TAG, "DnD lowSDK: ON - Total Silence");
                    dndEnabled = true;
                    break;
                case 3:
                    Log.d(Constants.TAG, "DnD lowSDK: ON - Alarms Only");
                    dndEnabled = true;
                    break;
                default:
                    Log.d(Constants.TAG, "DnD lowSDK Unexpected Value: " + zenModeValue);
                    dndEnabled = false;
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.d(Constants.TAG, "DnD lowSDK exception: " + e.toString());
            dndEnabled = false;
        }

        return dndEnabled;
    }

}
