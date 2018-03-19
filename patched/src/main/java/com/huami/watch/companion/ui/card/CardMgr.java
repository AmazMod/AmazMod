package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import com.edotasx.amazfit.boot.Boot;
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

@DexEdit(defaultAction = DexAction.IGNORE)
public class CardMgr {

    @DexIgnore
    private Activity b;
    @DexIgnore
    private List<ICard> c = new ArrayList<ICard>();

    @DexReplace
    private List<ICard> a() {
        if (this.c.isEmpty()) {
            this.c.add(StepCard.create(this.b));
            this.c.add(EverestHelpCard.create(this.b, CardMgr.showEverestHelpCard(this.b)));
            this.c.add(BatteryCard.create(this.b));
            this.c.add(SleepCard.create(this.b));
            this.c.add(HeartCard.create(this.b));
            this.c.add(ModSettingsCard.create(this.b));
            this.c.add(FacebookCard.create(this.b));
            this.c.add(BugsCard.create(this.b));
            this.c.add(BuyMeACoffeCard.create(this.b));
            this.c.add(ChangelogCard.create(this.b));
        }
        return this.c;
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
