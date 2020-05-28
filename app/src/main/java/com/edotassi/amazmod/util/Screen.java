package com.edotassi.amazmod.util;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import java.util.Arrays;
import java.util.Objects;

import amazmod.com.transport.Constants;

import static android.content.Context.POWER_SERVICE;

public class Screen {

    private static final String TAG_LOCAL = "Screen ";

    public static boolean isInteractive(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);

        boolean isScreenOn = false;
        try {
            isScreenOn = powerManager.isInteractive();
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
            boolean inKeyguardRestrictedInputMode = keyguardManager != null && keyguardManager.inKeyguardRestrictedInputMode();

            if (inKeyguardRestrictedInputMode) {
                isLocked = true;

            } else {
                // If password is not set in the settings, the inKeyguardRestrictedInputMode() returns false,
                // so we need to check if screen on for this case
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (powerManager != null) isLocked = !powerManager.isInteractive();
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
                int i = Objects.requireNonNull(mNotificationManager).getCurrentInterruptionFilter();
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

    public static boolean isStratos3(){
        String model = Prefs.getString(Constants.PREF_WATCH_MODEL, "-");
        boolean isStratos3 = model.contains("Amazfit Stratos 3");
        Logger.debug("DeviceUtil isStratos 3: checking if model is an Amazfit Stratos 3: " + isStratos3);
        return isStratos3;
    }

    public static boolean isVerge(){
        String model = Prefs.getString(Constants.PREF_HUAMI_MODEL, "-");
        boolean isVerge = Arrays.asList(Constants.BUILD_VERGE_MODELS).contains(model);
        Logger.debug("DeviceUtil isVerge: checking if model " + model + " is an Amazfit Verge: " + isVerge);
        return isVerge;
    }

    public static boolean isStratos(){
        String model = Prefs.getString(Constants.PREF_HUAMI_MODEL, "-");
        boolean isStratos = Arrays.asList(Constants.BUILD_STRATOS_MODELS).contains(model);
        Logger.debug("DeviceUtil isStratos: checking if model " + model + " is an Amazfit Stratos: " + isStratos);
        return isStratos;
    }

    public static boolean isPace(){
        String model = Prefs.getString(Constants.PREF_HUAMI_MODEL, "-");
        boolean isPace = Arrays.asList(Constants.BUILD_PACE_MODELS).contains(model);
        Logger.debug("DeviceUtil isPace: checking if model " + model + " is an Amazfit Pace: " + isPace);
        return isPace;
    }

    public static String getModelNoBySerialNo(String serialNo){
        // Serial is xxxx00000000 and model number is Axxxx
        return "A"+serialNo.substring(0, 4);
    }

    public static String[] getWatchInfoBySerialNo(String serialNo){
        String modelNo = getModelNoBySerialNo(serialNo);
        return new String[]{modelNo, getModelName(modelNo)};
    }

    public static String getSerialByModelNo(String modelNo){
        // Serial is xxxx00000000 and model number is Axxxx
        return modelNo.substring(1, 5)+"xxxxxxxxxx";
    }

    public static String getModelName(String model){
        if(Arrays.asList(Constants.BUILD_PACE_MODELS).contains(model)) // Pace
            return "Amazfit Pace";

        else if(Arrays.asList(Constants.BUILD_STRATOS_MODELS).contains(model)) // Stratos
            return "Amazfit Stratos";

        else if(Arrays.asList(Constants.BUILD_VERGE_MODELS).contains(model)) // Verge
            return "Amazfit Verge";

        else if(Arrays.asList(Constants.BUILD_STRATOS_3_MODELS).contains(model)) // Stratos 3
            return "Amazfit Stratos 3";

        else
            return "Unknown";
    }

    public static boolean isDrivingMode(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        boolean is_in_driving_mode = false;
        try {
            is_in_driving_mode = (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR);
        } catch (NullPointerException e){
            // empty
        }
        if(is_in_driving_mode)
            Logger.info("Is device in driving mode? : " + is_in_driving_mode);
        return is_in_driving_mode;
    }
}
