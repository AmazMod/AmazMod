package com.huami.watch.companion.ui.card;

import android.app.Activity;

import com.edotasx.amazfit.R;

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
