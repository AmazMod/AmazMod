package com.edotassi.amazmod.util;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import static android.content.Context.POWER_SERVICE;

public class Screen {

    public static boolean isInteractive(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH
                ? powerManager.isInteractive()
                : powerManager.isScreenOn();
    }
}
