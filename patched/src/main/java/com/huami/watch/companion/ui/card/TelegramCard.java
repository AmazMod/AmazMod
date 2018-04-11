package com.huami.watch.companion.ui.card;

import android.app.Activity;
import android.os.Parcel;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.transport.TransportService;
import com.huami.watch.dataflow.model.sport.SportSummaryManager;
import com.huami.watch.dataflow.model.sport.bean.SportSummary;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;
import com.huami.watch.util.Log;

import java.util.List;

import lanchon.dexpatcher.annotation.DexAdd;

/**
 * Created by edoardotassinari on 24/03/18.
 */

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
        /*
        SportSummaryManager sportSummaryManager = SportSummaryManager.getManager();
        List<SportSummary> sportSummaryList = sportSummaryManager.getAll(System.currentTimeMillis() / 1000, 0, 1000);

        for (SportSummary sportSummary : sportSummaryList) {
            Log.d("ModSportSummary", "trackId: " + sportSummary.getTrackid() + ", source: " + sportSummary.getSource() + ", sportType: " + sportSummary.getType());
            Log.d("ModSportSummary", "==================");
        }
        */
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.card_telegram;
    }

    @Override
    protected void initView() {
        /*
        Button powerOffButton = getView().findViewById(R.id.button_poweroff);
        Button restartButton = getView().findViewById(R.id.button_restart);

        powerOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DataBundle dataBundle = new DataBundle();
                dataBundle.putString("message", "hello word");

                TransportService.sharedInstance(getContext()).send("poweroff", dataBundle);
            }
        });
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TransportService.sharedInstance(getContext()).send("restart");
            }
        });
        */
    }

    @Override
    public String tag() {
        return "telegram";
    }
}
