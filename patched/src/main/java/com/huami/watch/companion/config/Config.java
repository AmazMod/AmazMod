package com.huami.watch.companion.config;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 09/04/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public final class Config {

    @DexWrap
    public static boolean isDebug() {
        //return true;
        return isDebug();
    }

    @DexWrap
    public static boolean isTestMode() {
        //return true;
        return isTestMode();
    }

    @DexWrap
    public static boolean isTestModeBackDoor() {
        //return true;
        return isTestModeBackDoor();
    }

    @DexIgnore
    public static boolean isOversea() {
        return false;
    }

    @DexIgnore
    public static boolean isTestHosts() {
        return false;
    }
}
