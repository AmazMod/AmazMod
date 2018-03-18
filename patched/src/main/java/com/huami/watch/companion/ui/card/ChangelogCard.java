package com.huami.watch.companion.ui.card;

import android.app.Activity;

import com.edotasx.amazfit.R;

import lanchon.dexpatcher.annotation.DexAdd;

/**
 * Created by edoardotassinari on 04/02/18.
 */

@DexAdd
public class ChangelogCard extends BaseCard {

    public static BaseCard create(Activity activity) {
        return new ChangelogCard(activity);
    }

    private ChangelogCard(Activity activity) {
        super(activity);
    }

    @Override
    protected void clickView() {
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.card_changelog;
    }

    @Override
    protected void initView() {
    }

    @Override
    public String tag() {
        return "changelog";
    }
}
