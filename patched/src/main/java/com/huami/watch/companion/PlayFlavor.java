package com.huami.watch.companion;

import android.app.Activity;
import android.content.Context;

import com.huami.watch.companion.thirdparty.strava.StravaAuthHelper;
import com.huami.watch.companion.ui.activity.SportsHealthOverseaActivity;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexReplace;

/**
 * Created by edoardotassinari on 28/01/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class PlayFlavor {

    @DexReplace
    public static void checkStravaAuthStatus(Context paramContext)
    {
        StravaAuthHelper.getHelper(paramContext).checkAuthStatus();
    }

    @DexReplace
    public static Class<? extends Activity> toSportsHealth() {
        return SportsHealthOverseaActivity.class;
    }
}
