package com.huami.watch.companion.util;

import android.content.Context;
import android.os.Build;

import com.huami.watch.companion.CompanionApplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexAdd;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 07/03/18.
 */

/*
@DexEdit(defaultAction = DexAction.IGNORE)
public class TimeUtil {

    @DexReplace
    public static String formatDateTime(long l2, String pattern) {
        synchronized (TimeUtil.class) {
            Locale locale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                 locale = CompanionApplication.getContext().getResources().getConfiguration().getLocales().get(0);
            } else{
                //noinspection deprecation
                locale = CompanionApplication.getContext().getResources().getConfiguration().locale;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, locale);
            return simpleDateFormat.format(new Date(l2));
        }
    }

    @DexReplace
    public static String formatForDayTitle(Context object, DateDay dateDay) {
        if (object != null) return new SimpleDateFormat(object.getString(com.huami.watch.companion.R.string.date_month_day), getLocale()).format(dateDay.calendar().getTime());
        return TimeUtil.format(dateDay);
    }


    @DexReplace
    public static String formatForMonthTitle(Context object, DateDay dateDay) {
        if (object != null) return new SimpleDateFormat(object.getString(com.huami.watch.companion.R.string.date_year_month), getLocale()).format(dateDay.calendar().getTime());
        return TimeUtil.format(dateDay);
    }

    @DexAdd
    private static Locale getLocale() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            locale = CompanionApplication.getContext().getResources().getConfiguration().getLocales().get(0);
        } else{
            //noinspection deprecation
            locale = CompanionApplication.getContext().getResources().getConfiguration().locale;
        }

        return locale;
    }

    @DexIgnore
    public static String format(DateDay object) {
        return "";
    }
}
*/