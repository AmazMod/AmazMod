package com.edotassi.amazmod.util;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.edotassi.amazmod.Constants;

import static android.content.Context.POWER_SERVICE;

public class Screen {

    public static boolean isInteractive(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
                ? powerManager.isInteractive()
                : powerManager.isScreenOn();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            try {
                int i = mNotificationManager.getCurrentInterruptionFilter();
                switch (i) {
                    case NotificationManager.INTERRUPTION_FILTER_UNKNOWN:
                        Log.d(Constants.TAG, "DnD highSDK: Unknown");
                        dndEnabled = false;
                        break;
                    case NotificationManager.INTERRUPTION_FILTER_ALL:
                        Log.d(Constants.TAG, "DnD highSDK: OFF (ALL)");
                        dndEnabled = false;
                        break;
                    case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                        Log.d(Constants.TAG, "DnD highSDK: ON - Priority");
                        dndEnabled = true;
                        break;
                    case NotificationManager.INTERRUPTION_FILTER_NONE:
                        Log.d(Constants.TAG, "DnD highSDK: ON - None");
                        dndEnabled = true;
                        break;
                    case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                        Log.d(Constants.TAG, "DnD highSDK: ON - Alarms");
                        dndEnabled = true;
                        break;
                    default:
                        Log.d(Constants.TAG, "DnD lowSDK Unexpected Value: " + i);
                        dndEnabled = false;
                }
            } catch (NullPointerException e){
                Log.d(Constants.TAG, "DnD highSDK exception: " + e.toString());
                dndEnabled = false;
            }
        }
        return dndEnabled;
    }

}
