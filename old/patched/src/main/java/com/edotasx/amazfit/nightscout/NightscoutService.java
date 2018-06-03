package com.edotasx.amazfit.nightscout;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edotasx.amazfit.Constants;

/**
 * Created by edoardotassinari on 09/04/18.
 */

public class NightscoutService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.TAG, "NightscoutService: onStartCommand");

        NightscoutHelper.sharedInstance(this).sync();

        return super.onStartCommand(intent, flags, startId);
    }
}
