package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.boot.Boot;
import com.edotasx.amazfit.permission.PermissionManager;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.companion.device.Device;
import com.huami.watch.companion.device.DeviceManager;
import com.huami.watch.companion.device.DeviceUtil;
import com.huami.watch.companion.util.Box;

import java.util.ArrayList;
import java.util.List;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;

/**
 * Created by edoardotassinari on 04/02/18.
 */

/*
@DexEdit(defaultAction = DexAction.IGNORE)
public class CardMgr {

    @DexIgnore
    private Activity b;
    @DexIgnore
    private List<ICard> c = new ArrayList<ICard>();

    @DexReplace
    private List<ICard> a() {
        if (c.isEmpty()) {
            PermissionManager.sharedInstance().requestPermissions(b);

            c.add(StepCard.create(b));
            c.add(EverestHelpCard.create(b, CardMgr.showEverestHelpCard(b)));
            c.add(BatteryCard.create(b));

            if (!PreferenceManager.getBoolean(b, Constants.PREFERENCE_DISABLE_BATTERY_CHART, false)) {
                c.add(BatteryChartCard.create(b));
            }

            c.add(SleepCard.create(b));
            c.add(HeartCard.create(b));
            c.add(ModSettingsCard.create(b));
            c.add(TelegramCard.create(b));
            c.add(FacebookCard.create(b));
            c.add(GithubCard.create(b));
            c.add(BugsCard.create(b));
            //c.add(BuyMeACoffeCard.create(b));
            c.add(ChangelogCard.create(b));
        }
        return c;
    }

    @DexIgnore
    public static CardMgr create(Activity object) {
        return null;
    }

    @DexIgnore
    public List<ICard> cards() {
        return null;
    }

    @DexIgnore
    public ICard findCardByTag(@NonNull String object) {
        return null;
    }

    @DexIgnore
    public void dispatchCardLoadEvent() {
    }

    @DexIgnore
    public static boolean showEverestHelpCard(Context object) {
        return false;
    }
}
*/