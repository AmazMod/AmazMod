package com.huami.watch.companion.otaphone.service;

import android.content.Context;
import android.os.Bundle;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.ota.cloud.RomInfo;
import com.huami.watch.transport.ConnectionResult;
import com.huami.watch.transport.HmApiClient;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 11/03/18.
 */

/*
@DexEdit(defaultAction = DexAction.IGNORE)
public class OtaTransportCmd implements HmApiClient.ConnectionCallbacks,
        HmApiClient.OnConnectionFailedListener {

    @DexIgnore
    private Context c;

    @DexWrap
    public void cmdSendRomInfo(RomInfo parcelable) {

        if (parcelable == null) {
            return;
        }

        if (PreferenceManager.getBoolean(c, Constants.PREFERENCE_ENABLE_OTA, false)) {
            cmdSendRomInfo(parcelable);
        }


        cmdSendRomInfo(parcelable);
    }

    @DexIgnore
    @Override
    public void onServicesConnected(Bundle bundle) {

    }

    @DexIgnore
    @Override
    public void onServicesDisConnected(ConnectionResult connectionResult) {

    }

    @DexIgnore
    @Override
    public void onServicesConnectionFailed(ConnectionResult connectionResult) {

    }

    @DexIgnore
    public static interface callback {
        public void actionAddEapWifi(String var1);

        public void actionAddHidenWifi();

        public void actionAddWifiPassword(String var1);

        public void actionCancelPasswordInput();

        public void actionCancelWifiAdd();

        public void actionCheckUpgrade();

        public void dataChannelChanged(boolean var1);
    }
}
*/
