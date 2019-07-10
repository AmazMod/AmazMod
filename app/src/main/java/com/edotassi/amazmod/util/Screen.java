package com.edotassi.amazmod.util;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import java.util.Arrays;
import java.util.prefs.Preferences;

import amazmod.com.transport.Constants;

import static android.content.Context.POWER_SERVICE;

public class Screen {

    private static final String TAG_LOCAL = "Screen ";

    public static boolean isInteractive(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);

        boolean isScreenOn = false;
        try {
            isScreenOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
                    ? powerManager.isInteractive()
                    : powerManager.isScreenOn();
        } catch (NullPointerException e) {
            Logger.error(TAG_LOCAL+"isInteractive exception: " + e.toString());
        }
        Logger.info(TAG_LOCAL+"isInteractive: " + isScreenOn);
        return isScreenOn;
    }

    public static boolean isDarkTheme() {
        return Prefs.getBoolean(Constants.PREF_AMAZMOD_DARK_THEME, Constants.PREF_AMAZMOD_DARK_THEME_DEFAULT);
    }

    public static boolean isDeviceLocked(Context context) {

        boolean isLocked = false;

        // First we check the locked state
        try {
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            boolean inKeyguardRestrictedInputMode = keyguardManager.inKeyguardRestrictedInputMode();

            if (inKeyguardRestrictedInputMode) {
                isLocked = true;

            } else {
                // If password is not set in the settings, the inKeyguardRestrictedInputMode() returns false,
                // so we need to check if screen on for this case
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                    isLocked = !powerManager.isInteractive();
                } else {
                    //no inspection deprecation
                    isLocked = !powerManager.isScreenOn();
                }
            }
        } catch (NullPointerException e) {
            Logger.error("iDeviceLocked exception: " + e.toString());
        }

        Logger.info(TAG_LOCAL+"isDeviceLocked: " + isLocked);
        return isLocked;
    }

    public static boolean isDNDActive(Context context, ContentResolver cr) {

        boolean dndEnabled;
        try {

            int zenModeValue = Settings.Global.getInt(cr, "zen_mode");

            switch (zenModeValue) {
                case 0:
                    Logger.info(TAG_LOCAL+"DnD lowSDK: OFF");
                    dndEnabled = false;
                    break;
                case 1:
                    Logger.info(TAG_LOCAL+"DnD lowSDK: ON - Priority Only");
                    dndEnabled = true;
                    break;
                case 2:
                    Logger.info(TAG_LOCAL+"DnD lowSDK: ON - Total Silence");
                    dndEnabled = true;
                    break;
                case 3:
                    Logger.info(TAG_LOCAL+"DnD lowSDK: ON - Alarms Only");
                    dndEnabled = true;
                    break;
                default:
                    Logger.info(TAG_LOCAL+"DnD lowSDK Unexpected Value: " + zenModeValue);
                    dndEnabled = false;
            }
        } catch (Settings.SettingNotFoundException e) {
            Logger.error("DnD lowSDK exception: " + e.toString());
            dndEnabled = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            try {
                int i = mNotificationManager.getCurrentInterruptionFilter();
                switch (i) {
                    case NotificationManager.INTERRUPTION_FILTER_UNKNOWN:
                        Logger.info(TAG_LOCAL+"DnD highSDK: Unknown");
                        dndEnabled = false;
                        break;
                    case NotificationManager.INTERRUPTION_FILTER_ALL:
                        Logger.info(TAG_LOCAL+ "DnD highSDK: OFF (ALL)");
                        dndEnabled = false;
                        break;
                    case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                        Logger.info(TAG_LOCAL+"DnD highSDK: ON - Priority");
                        dndEnabled = true;
                        break;
                    case NotificationManager.INTERRUPTION_FILTER_NONE:
                        Logger.info(TAG_LOCAL+"DnD highSDK: ON - None");
                        dndEnabled = true;
                        break;
                    case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                        Logger.info(TAG_LOCAL+"DnD highSDK: ON - Alarms");
                        dndEnabled = true;
                        break;
                    default:
                        Logger.info(TAG_LOCAL+"DnD lowSDK Unexpected Value: " + i);
                        dndEnabled = false;
                }
            } catch (NullPointerException e){
                Logger.error(TAG_LOCAL+"DnD highSDK exception: " + e.toString());
                dndEnabled = false;
            }
        }
        return dndEnabled;
    }

    public static boolean isVerge(){
        String model = Prefs.getString(Constants.PREF_HUAMI_MODEL, "-");
        boolean isVerge = Arrays.asList(Constants.BUILD_VERGE_MODELS).contains(model);
        Logger.debug("DeviceUtil isVerge: checking if model " + model + " is an Amazfit Verge: " + isVerge);
        return isVerge;
    }


}
