package com.huami.watch.companion.ui.card;

import android.app.Activity;

import com.edotasx.amazfit.R;
import com.huami.watch.dataflow.model.sport.SportSummaryManager;
import com.huami.watch.dataflow.model.sport.bean.SportSummary;
import com.huami.watch.util.Log;

import java.util.List;

import lanchon.dexpatcher.annotation.DexAdd;

/**
 * Created by edoardotassinari on 24/03/18.
 */

@DexAdd()
public class TelegramCard extends BaseCard {

    public static TelegramCard create(Activity activity) {
        return new TelegramCard(activity);
    }

    private TelegramCard(Activity activity) {
        super(activity);
    }

    @Override
    protected void clickView() {
        SportSummaryManager sportSummaryManager = SportSummaryManager.getManager();
        List<SportSummary> sportSummaryList = sportSummaryManager.getAll(System.currentTimeMillis() / 1000, 0, 1000);

        for (SportSummary sportSummary : sportSummaryList) {
            Log.d("ModSportSummary", "trackId: " + sportSummary.getTrackid() + ", source: " + sportSummary.getSource() + ", sportType: " + sportSummary.getType());
            Log.d("ModSportSummary", "==================");
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.card_telegram;
    }

    @Override
    protected void initView() {
    }

    @Override
    public String tag() {
        return "telegram";
    }
}
