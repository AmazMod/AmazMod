package com.edotasx.amazfit.preference;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by edoardotassinari on 03/03/18.
 */

public class PreferenceManager {

    public static String getString(Context context, String key, String defaultValue) {
        SharedPreferences sharedPref = getSharedPreferences(context);
        return sharedPref.getString(key, defaultValue);
    }

    public static Boolean getBoolean(Context context, String key, Boolean defaultValue){
        return getSharedPreferences(context).getBoolean(key, defaultValue);
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }
}
