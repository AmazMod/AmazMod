package com.huami.watch.companion.cloud;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 16/03/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class Cloud {

    @DexWrap
    public void updateHosts(boolean bl2, boolean bl3) {
        updateHosts(bl2, bl3);
    }
}
