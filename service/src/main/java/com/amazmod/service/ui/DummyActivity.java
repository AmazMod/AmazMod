package com.amazmod.service.ui;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.amazmod.service.AdminReceiver;
import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.util.DeviceUtil;

import java.io.IOException;
import java.util.List;

public class DummyActivity extends Activity implements DelayedConfirmationView.DelayedConfirmationListener {

    BoxInsetLayout rootLayout;
    private DelayedConfirmationView delayedConfirmationView;

    private TextView installFinishedText, restartText;
    private String paramText, appTag, continueText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wear_confirmation);

        appTag = getIntent().getStringExtra(Constants.APP_TAG);

        Log.d(Constants.TAG,"DummyActivity onCreate appTag: " + appTag);

        listPackageInstallerSessions();

        String paramTime = "5";

        if (appTag != null) {

            switch (appTag) {
                case Constants.MY_APP:
                    paramText = "AmazMod Updated";
                    continueText = String.format("Continuing in %ss…", paramTime);
                    break;

                case Constants.OTHER_APP:
                    paramText = "Install finished";
                    continueText = String.format("Restarting in %ss…", paramTime);
                    break;

                default:
                    if (appTag.contains(".apk")) {
                        paramText = "Installing APK";
                        continueText = String.format("Continuing in %ss…", paramTime);
                    } else
                        finish();
            }

            rootLayout = findViewById(R.id.install_root_layout);
            installFinishedText = findViewById(R.id.install_finished_text);
            restartText = findViewById(R.id.restart_text);
            delayedConfirmationView = findViewById(R.id.install_delayed_view);
            delayedConfirmationView.setTotalTimeMs(Integer.valueOf(paramTime) * 1000);

            final int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

            getWindow().addFlags(flags);

            delayedConfirmationView.setVisibility(View.GONE);
            startDelayedConfirmationView();
        } else
            finish();

    }

    private void startDelayedConfirmationView() {
        delayedConfirmationView.setVisibility(View.VISIBLE);
        installFinishedText.setText(paramText);
        restartText.setText(continueText);
        delayedConfirmationView.setPressed(false);
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);
    }

    @Override
    public void onTimerSelected(View v) {
        Log.d(Constants.TAG,"DummyActivity RESULT_CANCELED");
        v.setPressed(true);
        delayedConfirmationView.reset();
        ((DelayedConfirmationView) v).setListener(null);
        finish();
    }

    @Override
    public void onTimerFinished(View v) {
        Log.d(Constants.TAG,"DummyActivity RESULT_OK");
        ((DelayedConfirmationView) v).setListener(null);
        final Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        startActivity(intent);
        try {
            if (appTag.equals(Constants.MY_APP) || appTag.equals(Constants.OTHER_APP)) {
                Log.d(Constants.TAG, "DummyActivity onActivityResult restart launcher");
                Runtime.getRuntime().exec("adb shell am force-stop com.huami.watch.launcher;exit");

                if (Constants.MY_APP.equals(appTag)) {
                    DevicePolicyManager mDPM = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
                    //Log.i(Constants.TAG, "PackageReceiver onReceive isDeviceOwnerApp: " + mDPM.isDeviceOwnerApp(context.getPackageName())
                    //        + " // getActiveAdmins: " + mDPM.getActiveAdmins());
                    if (!(mDPM != null && mDPM.isAdminActive(new ComponentName(this, AdminReceiver.class)))) {
                        Log.d(Constants.TAG, "DummyActivity onActivityResult set-active-admin");
                        Runtime.getRuntime().exec("adb shell dpm set-active-admin com.amazmod.service/.AdminReceiver;exit");
                    }
                }
            } else if (appTag.contains(".apk")) {
                DeviceUtil.installPackage(this, getPackageName(), appTag);
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "DummyActivity onActivityResult IOException: " + e.toString());
        }

        finish();
    }

    private void listPackageInstallerSessions(){
        PackageInstaller packageInstaller = this.getPackageManager().getPackageInstaller();
        List<PackageInstaller.SessionInfo> allSessions = packageInstaller.getAllSessions();
        Log.i(Constants.TAG, "DummyActivity listPackageInstallerSessions AllSessions *****");
        int count = 0;
        for (PackageInstaller.SessionInfo se: allSessions) {
            count++;
            Log.i(Constants.TAG, "DummyActivity listPackageInstallerSessions getAppPackageName: " + se.getAppPackageName()
                    + " - getInstallerPackageName: " + se.getInstallerPackageName() + " - getSessionId: " + se.getSessionId());
        }
        Log.i(Constants.TAG, "DummyActivity listPackageInstallerSessions  (" + count + ") AllSessions *****");

        List<PackageInstaller.SessionInfo> mySessions = packageInstaller.getMySessions();
        Log.i(Constants.TAG, "DummyActivity listPackageInstallerSessions MySessions *****");
        count = 0;
        for (PackageInstaller.SessionInfo se: mySessions) {
            count++;
            Log.i(Constants.TAG, "DummyActivity listPackageInstallerSessions getAppPackageName: " + se.getAppPackageName()
                    + " - getInstallerPackageName: " + se.getInstallerPackageName() + " - getSessionId: " + se.getSessionId());
        }
        Log.i(Constants.TAG, "DummyActivity listPackageInstallerSessions PackageManager (" + count + ") MySessions *****");
    }

}
