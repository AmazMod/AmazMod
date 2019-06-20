package com.edotassi.amazmod.support;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;


public class ThemeHelper {

    //Fallback values
    private static final String ACCENT_COLOR = "#FFFF4081";
    private static final String FOREGROUND_COLOR = "#FFFFFFFF";

    private static int getFallBackAccentColor() {
        return Color.parseColor(ACCENT_COLOR);
    }

    private static int getFallBackForegroundColor() {
        return Color.parseColor(FOREGROUND_COLOR);
    }

    public static int getThemeColorAccent(Context context) {
        if (context == null)
            return getFallBackAccentColor();
        else {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.colorAccent, outValue, true);
            return outValue.data;
        }
    }

    public static int getThemeColorAccentId(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorAccent, outValue, true);
        return outValue.resourceId;
    }

    public static int getThemeForegroundColor(Context context) {
        if (context == null)
            return getFallBackForegroundColor();
        else {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.colorForeground, outValue, true);
            return outValue.data;
        }
    }

    public static int getThemeForegroundColorId(Context context) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorForeground, outValue, true);
        return outValue.resourceId;
    }

}
