package com.huami.watch.companion;

import android.annotation.SuppressLint;
import android.view.View;

import com.huami.watch.connect.PhoneConnectionApplication;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 12/02/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class CompanionApplication extends PhoneConnectionApplication {

    @SuppressLint("MissingSuperCall")
    @DexWrap()
    public void onCreate() {
        //getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);


        onCreate();
    }
}
