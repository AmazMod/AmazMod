package com.edotasx.amazfit.nightscout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by edoardotassinari on 08/04/18.
 */

public class NightscoutReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent nightscoutServiceIntent = new Intent(context, NightscoutService.class);
        context.startService(nightscoutServiceIntent);
    }
}
