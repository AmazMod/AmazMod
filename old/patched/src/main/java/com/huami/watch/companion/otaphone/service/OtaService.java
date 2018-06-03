package com.huami.watch.companion.otaphone.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.watch.companion.update.UpdateManager;
import com.huami.watch.ota.cloud.RomInfo;

import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 04/03/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class OtaService extends Service
        implements OtaTransportCmd.callback  {

    @DexIgnore
    private Context d;
    @DexIgnore
    private RomInfo i;

    @DexReplace
    static RomInfo a(OtaService otaService, RomInfo romInfo) {
        if (PreferenceManager.getBoolean(otaService.d, Constants.PREFERENCE_ENABLE_OTA, false)) {
            otaService.i = romInfo;
        } else {
            otaService.i = null;
        }
        return romInfo;
    }


    @DexIgnore
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @DexIgnore
    @Override
    public void actionAddEapWifi(String s) {

    }

    @DexIgnore
    @Override
    public void actionAddHidenWifi() {

    }

    @DexIgnore
    @Override
    public void actionAddWifiPassword(String s) {

    }

    @DexIgnore
    @Override
    public void actionCancelPasswordInput() {

    }

    @DexIgnore
    @Override
    public void actionCancelWifiAdd() {

    }

    @DexIgnore
    @Override
    public void actionCheckUpgrade() {

    }

    @DexIgnore
    @Override
    public void dataChannelChanged(boolean b) {

    }

    /*
    @DexIgnore
    @Override
    public void downloadUpgrade(int i) {

    }

    @DexIgnore
    @Override
    public void syncFinished(int i) {

    }

    @DexIgnore
    @Override
    public void updateDownloadProgress(int i) {

    }
    */
}
