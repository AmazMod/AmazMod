package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;

import lanchon.dexpatcher.annotation.DexAdd;

/**
 * Created by edoardotassinari on 09/02/18.
 */

@DexAdd()
public class BuyMeACoffeCard extends BaseCard {

    public static BuyMeACoffeCard create(Activity activity) {
        return new BuyMeACoffeCard(activity);
    }

    private BuyMeACoffeCard(Activity activity) {
        super(activity);
    }

    @Override
    protected void clickView() {
        /*
        Intent paypalMeUrl = getPaypalMeUrl(getContext());
        getContext().startActivity(paypalMeUrl);
        */
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.card_buymeacoffe;
    }

    @Override
    protected void initView() {

    }

    @Override
    public String tag() {
        return "buymeacoffe";
    }

    private Intent getPaypalMeUrl(Context context) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PAYPAL_ME_URL));
    }
}
