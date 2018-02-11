package com.huami.watch.util;

import com.edotasx.amazfit.Constants;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexReplace;

/**
 * Created by edoardotassinari on 30/01/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class Log {

    @DexReplace
    public static void d(String string2, String string3, Object... arrobject) {
        android.util.Log.d(Constants.TAG, "[D]: " + string2 + " -> " + string3);
    }

    @DexReplace
    public static void e(String string2, String string3, Throwable throwable, Object... arrobject) {
        android.util.Log.d(Constants.TAG, "[E]: " + string2 + " -> " + string3 + " " + throwable.getMessage());
        StackTraceElement[] throwables = throwable.getStackTrace();
        for (StackTraceElement element : throwables) {
            android.util.Log.d(Constants.TAG, "[E]: " + element.getClassName() + ":" + element.getMethodName() + ":" + element.getLineNumber());
        }
    }

    @DexReplace
    public static void e(String string2, String string3, Object... arrobject) {
        android.util.Log.d(Constants.TAG, "[E]: " + string2 + " -> " + string3);
    }

    @DexReplace
    public static void i(String string2, String string3, Object... arrobject) {
        android.util.Log.d(Constants.TAG, "[I]: " + string2 + " -> " + string3);
    }

    @DexReplace
    public static void v(String string2, String string3, Object... arrobject) {
        android.util.Log.d(Constants.TAG, "[V]: " + string2 + " -> " + string3);

    }

    @DexReplace
    public static void w(String string2, String string3, Throwable throwable, Object... arrobject) {
        android.util.Log.d(Constants.TAG, "[W]: " + string2 + " -> " + string3);

    }

    @DexReplace
    public static void w(String string2, String string3, Object... arrobject) {
        android.util.Log.d(Constants.TAG, "[W]: " + string2 + " -> " + string3);
    }

    @DexReplace
    public static void wtf(String string2, String string3, Object... arrobject) {
        android.util.Log.d(Constants.TAG, "[WTF]: " + string2 + " -> " + string3);
    }
}
