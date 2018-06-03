package com.edotassi.amazmod.log;

public class LoggerScoped {

    private String scope;

    private LoggerScoped(String scope) {
        this.scope = scope;
    }

    public static LoggerScoped get(Class classScope) {
        return new LoggerScoped(classScope.getSimpleName());
    }

    public void debug(String message, Object... args) {
        Logger.debug(getMessageScoped(scope, message), args);
    }

    public void error(Throwable throwable, String message, Object... args) {
        Logger.error(throwable, getMessageScoped(scope, message), args);
    }

    private String getMessageScoped(String scope, String message) {
        return "[" + scope + "] " + message;
    }
}
