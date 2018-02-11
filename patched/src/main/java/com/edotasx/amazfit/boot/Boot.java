package com.edotasx.amazfit.boot;

import android.content.Context;

import com.edotasx.amazfit.Constants;
import com.edotasx.amazfit.R;
import com.edotasx.amazfit.permission.PermissionManager;
import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.AppUpdaterUtils;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

/**
 * Created by edoardotassinari on 11/02/18.
 */

public class Boot {

    private static Boot mInstance;
    private Context mContext;

    private boolean mInitiated;

    public static Boot sharedInstance(Context pContext) {
        if (mInstance == null) {
            mInstance = new Boot(pContext);
        }

        return mInstance;
    }

    private Boot(Context pContext) {
        mContext = pContext;
    }

    public void run() {
        if (mInitiated) {
            return;
        }

        mInitiated = true;

        initiate(mContext);
    }

    private void initiate(Context pContext) {
        PermissionManager.sharedInstance().requestPermissions(pContext);

        new AppUpdater(pContext)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo(Constants.GITHUB_USERNAME, Constants.GITHUB_REPOSITORY)
                .setButtonUpdate(null)
                .setContentOnUpdateAvailable(R.string.content_update_available)
                .start();
    }
}