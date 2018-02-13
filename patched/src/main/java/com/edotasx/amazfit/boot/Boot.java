package com.edotasx.amazfit.boot;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;

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
        Activity activity = (Activity) pContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activity.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }


        PermissionManager.sharedInstance().requestPermissions(pContext);

        new AppUpdater(pContext)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo(Constants.GITHUB_USERNAME, Constants.GITHUB_REPOSITORY)
                .setButtonUpdate(null)
                .setContentOnUpdateAvailable(R.string.content_update_available)
                .start();
    }
}