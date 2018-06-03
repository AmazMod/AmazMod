package com.huami.watch.companion.usersettings;

import android.content.Context;

import com.edotasx.amazfit.boot.Boot;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 06/02/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class UserSettingsManager {

    @DexWrap
    public static UserSettingsManager getManager(Context context) {
        //Boot.sharedInstance(context).run();

        return getManager(context);
    }
}
