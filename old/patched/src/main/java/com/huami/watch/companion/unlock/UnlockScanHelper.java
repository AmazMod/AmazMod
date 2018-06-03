package com.huami.watch.companion.unlock;

import android.util.Log;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 07/03/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class UnlockScanHelper {

    @DexWrap
    public void startLeScan() {
        Log.d("LE-SCAN", "start le scan");

        startLeScan();
    }
}
