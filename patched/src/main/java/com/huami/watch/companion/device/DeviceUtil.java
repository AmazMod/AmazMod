package com.huami.watch.companion.device;

import android.content.Context;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexReplace;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 16/03/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class DeviceUtil {

    @DexWrap
    public static boolean hasEmptyInfo(Device device) {
        return hasEmptyInfo(device);
    }

    @DexReplace
    public static boolean isRomSupportWatchFaceStore(Context context, Device device) {
        return true;
    }

    @DexReplace
    public boolean isRomSupportShowQR(Context context, Device device) {
        return true;
    }
}
