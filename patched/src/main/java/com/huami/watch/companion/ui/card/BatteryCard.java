package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.view.View;

import com.edotasx.amazfit.db.model.BatteryRead;
import com.huami.watch.companion.battery.bean.BatteryInfo;
import com.huami.watch.util.Log;
import com.raizlabs.android.dbflow.config.FlowManager;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 02/04/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class BatteryCard extends BaseCard {

    @DexIgnore
    public BatteryCard(Activity activity) {
        super(activity);
    }

    @DexIgnore
    public static BatteryCard create(Activity activity) {
        return null;
    }

    @DexWrap
    public void updateBatteryViews(BatteryInfo batteryInfo) {
        updateBatteryViews(batteryInfo);

        BatteryRead batteryRead = new BatteryRead();
        batteryRead.setDate(System.currentTimeMillis());
        batteryRead.setLevel(batteryInfo.getBatteryLevel());
        batteryRead.setCharging(batteryInfo.isBatteryCharging());

        FlowManager.getModelAdapter(BatteryRead.class).insert(batteryRead);

        Log.d("BatteryChart", "level: " + batteryInfo.getBatteryLevel());
    }

    @DexIgnore
    @Override
    protected void clickView() {

    }

    @DexIgnore
    @Override
    protected int getLayoutRes() {
        return 0;
    }

    @DexIgnore
    @Override
    protected void initView() {

    }

    @DexIgnore
    @Override
    public String tag() {
        return null;
    }
}
