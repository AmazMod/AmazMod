package com.huami.watch.companion.ui.activity;

import android.app.Activity;
import android.view.View;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 28/01/18.
 */

/*

@DexEdit(defaultAction = DexAction.IGNORE)
public class ThirdBindActivity extends Activity {

    @DexIgnore
    private SettingItemView c;

    @DexWrap
    private void a() {
        a();

        this.c.setVisibility(View.VISIBLE);
    }
}
*/
