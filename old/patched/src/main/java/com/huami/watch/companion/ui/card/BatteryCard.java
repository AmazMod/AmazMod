package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.view.View;

import com.edotasx.amazfit.db.model.BatteryRead;
import com.edotasx.amazfit.db.model.BatteryRead_Table;
import com.edotasx.amazfit.events.BatteryHistoryUpdatedEvent;
import com.huami.watch.companion.battery.bean.BatteryInfo;
import com.huami.watch.companion.util.RxBus;
import com.huami.watch.util.Log;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 02/04/18.
 */

/*
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

        long date = System.currentTimeMillis();

        BatteryRead batteryRead = new BatteryRead();
        batteryRead.setDate(date);
        batteryRead.setLevel(batteryInfo.getBatteryLevel());
        batteryRead.setCharging(batteryInfo.isBatteryCharging());

        try {
            BatteryRead batteryReadStored = SQLite
                    .select()
                    .from(BatteryRead.class)
                    .where(BatteryRead_Table.date.is(date))
                    .querySingle();

            if (batteryReadStored == null) {
                FlowManager.getModelAdapter(BatteryRead.class).insert(batteryRead);
            }
        } catch (Exception ex) {
            //Crashlytics.logException(ex);
        }
        Log.d("BatteryChart", "level: " + batteryInfo.getBatteryLevel());

        RxBus.get().post(new BatteryHistoryUpdatedEvent());
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
*/
