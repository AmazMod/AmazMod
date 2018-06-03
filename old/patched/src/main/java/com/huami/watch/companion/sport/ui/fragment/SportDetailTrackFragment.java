package com.huami.watch.companion.sport.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexPrepend;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 24/02/18.
 */

/*
@DexEdit(defaultAction = DexAction.IGNORE)
public class SportDetailTrackFragment extends BaseSportDetailFragment
        implements View.OnClickListener{

    @DexIgnore
    private boolean x;

    @SuppressLint("MissingSuperCall")
    @DexWrap
    @Override
    public void onActivityCreated(@Nullable Bundle bundle) {
        this.x = true;
        onActivityCreated(bundle);
        this.x = false;
    }

    @DexIgnore
    @Override
    public void onClick(View view) {

    }

    @DexIgnore
    @Override
    protected int getLayout() {
        return 0;
    }

    @Override
    protected void initViews(View view) {

    }
}
*/
