package com.edotassi.amazmod.log;

import com.edotassi.amazmod.Constants;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.PrettyFormatStrategy;

public class Logger {

    public static void init() {
        AndroidLogAdapter androidLogAdapter = new AndroidLogAdapter(PrettyFormatStrategy.newBuilder().tag(Constants.TAG).build());
        com.orhanobut.logger.Logger.addLogAdapter(androidLogAdapter);
    }

    public static void debug(String message, Object... args) {
        com.orhanobut.logger.Logger.d(message, args);
    }

    public static void warn(String message, Object... args) {
        com.orhanobut.logger.Logger.w(message, args);
    }

    public static void error(Throwable throwable, String message, Object... args) {
        com.orhanobut.logger.Logger.e(throwable, message, args);
    }
}
