package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.os.Parcel;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.transport.TransportService;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.huami.watch.util.Log;

import java.util.List;

import lanchon.dexpatcher.annotation.DexAdd;

/**
 * Created by edoardotassinari on 24/03/18.
 */

/*
@DexAdd()
public class TelegramCard extends BaseCard {

    private Transporter transporter;

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
*/
