package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;

import lanchon.dexpatcher.annotation.DexAdd;

/**
 * Created by edoardotassinari on 04/02/18.
 */

@DexAdd
public class FacebookCard extends BaseCard {

    public static BaseCard create(Activity activity) {
        return new FacebookCard(activity);
    }

    private FacebookCard(Activity activity) {
        super(activity);
    }

    @Override
    protected void clickView() {
        Intent facebookIntent = getOpenFacebookIntent(getContext());
        getContext().startActivity(facebookIntent);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.card_facebook;
    }

    @Override
    protected void initView() {
    }

    @Override
    public String tag() {
        return "facebook";
    }

    private Intent getOpenFacebookIntent(Context context) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FACEBOOK_GROUP_URL));
    }
}
