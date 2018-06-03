package com.huami.watch.util;

import com.edotasx.amazfit.Constants;

import java.io.File;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;

/**
 * Created by edoardotassinari on 30/01/18.
 */

/*

@DexEdit(defaultAction = DexAction.IGNORE)
public class Log {

    @DexIgnore
    public static Log.Settings init() {
        return null;
    }

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

    @DexIgnore
    public static enum LogLevel {
        NONE,
        CONSOLE_ONLY,
        FILE_ONLY,
        FULL;


        private LogLevel() {
        }
    }

    @DexIgnore
    public static class Settings {
        private String a = "LOG";
        private boolean b = true;
        private int c = 2;
        private int d = 0;
        private Log.LogLevel e = Log.LogLevel.CONSOLE_ONLY;
        private File f;
        private long g = 25165824;

        String a() {
            return this.a;
        }

        boolean b() {
            return this.b;
        }

        int c() {
            return this.c;
        }

        int d() {
            return this.d;
        }

        File e() {
            return this.f;
        }

        boolean f() {
            if (this.e == Log.LogLevel.NONE) return false;
            return true;
        }

        boolean g() {
            if (this.e == Log.LogLevel.FILE_ONLY) return true;
            if (this.e != Log.LogLevel.FULL) return false;
            return true;
        }

        public Log.LogLevel getLogLevel() {
            return this.e;
        }

        boolean h() {
            if (this.e == Log.LogLevel.CONSOLE_ONLY) return true;
            if (this.e != Log.LogLevel.FULL) return false;
            return true;
        }

        public Settings hideThreadInfo() {
            this.b = false;
            return this;
        }

        long i() {
            return this.g;
        }

        public Settings setLogFile(File file) {
            this.f = file;
            return this;
        }

        public void setLogFileMaxSize(long l2) {
            this.g = l2;
        }

        public Settings setLogLevel(Log.LogLevel logLevel) {
            this.e = logLevel;
            return this;
        }

        public Settings setMethodCount(int n2) {
            this.c = n2;
            return this;
        }

        public Settings setMethodOffset(int n2) {
            this.d = n2;
            return this;
        }

        public Settings setTag(String string2) {
            this.a = string2;
            return this;
        }
    }
}
*/