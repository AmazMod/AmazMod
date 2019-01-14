package com.edotassi.amazmod.support;

import amazmod.com.transport.Constants;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.PrettyFormatStrategy;

public class Logger {

    public static void init() {
        PrettyFormatStrategy strategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)
                .methodCount(0)
                .methodOffset(0)
                .tag(Constants.TAG)
                .build();

        AndroidLogAdapter androidLogAdapter = new AndroidLogAdapter(strategy);
        com.orhanobut.logger.Logger.addLogAdapter(androidLogAdapter);
    }

    public static Logger get(Class classScope) {
        return new Logger(classScope.getSimpleName());
    }

    private String scope;

    public Logger() {
        this("");
    }

    public Logger(String scope) {
        this.scope = scope;
    }

    public void d(String message, Object... args) {
        com.orhanobut.logger.Logger.d(getMessage(scope, message), args);
    }

    public void i(String message, Object... args) {
        com.orhanobut.logger.Logger.d(getMessage(scope, message), args);
    }

    public void w(String message, Object... args) {
        com.orhanobut.logger.Logger.w(getMessage(scope, message), args);
    }

    public void e(Throwable throwable, String message, Object... args) {
        com.orhanobut.logger.Logger.e(throwable, getMessage(scope, message), args);
    }

    private String getMessage(String scope, String message) {
        if ((scope != null) && (!scope.trim().equals(""))) {
            return "[" + scope + "] " + message;
        } else {
            return message;
        }
    }
}
