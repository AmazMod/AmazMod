package com.huami.watch.companion.update;

import android.content.Context;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.preference.PreferenceManager;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 10/03/18.
 */

/*
@DexEdit(defaultAction = DexAction.IGNORE)
public class RomUpgradeHelper {

    @DexIgnore
    private Context a;

    @DexWrap
    public void checkRomNeedUpgradeAsync(Observer<Boolean> observer) {
        if (PreferenceManager.getBoolean(a, Constants.PREFERENCE_ENABLE_OTA, false)) {
            checkRomNeedUpgradeAsync(observer);
        } else {
            if (!PreferenceManager.getBoolean(a, Constants.PREFERENCE_HIDE_OTA_TOAST, false)) {
                checkRomNeedUpgradeAsync(new ToastUpgradeObserver());
            }
        }
    }
}
*/