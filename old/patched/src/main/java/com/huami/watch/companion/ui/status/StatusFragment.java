package com.huami.watch.companion.ui.status;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.LinearLayout;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;

/**
 * Created by edoardotassinari on 19/03/18.
 */

/*
@DexEdit(defaultAction = DexAction.IGNORE)
public class StatusFragment extends Fragment
        implements SyncCenter.SyncListener {

    @DexIgnore
    private LinearLayout c;
    @DexIgnore
    private CardMgr f;
    @DexIgnore
    private BatteryCard g;

    @SuppressLint("ResourceType")
    @DexReplace
    private void a() {
        this.c = this.getView().findViewById(2131755652);
        this.f = CardMgr.create(this.getActivity());
        for (ICard iCard : this.f.cards()) {
            View cardView = iCard.getView();
            if (cardView != null) {
                this.c.addView(cardView);
            } else {
                Bundle bundle = new Bundle();
                bundle.putString("card", iCard.tag());
                //FirebaseAnalytics.getInstance(getContext()).logEvent("CARD_VIEW_NULL", bundle);
            }
        }
        this.g = (BatteryCard)this.f.findCardByTag("battery");
        this.f.dispatchCardLoadEvent();
    }

    @DexIgnore
    @Override
    public void onSyncStateChanged(int i, int i1, String s) {
    }
}
*/