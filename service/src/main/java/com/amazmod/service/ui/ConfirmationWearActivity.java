package com.amazmod.service.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.view.View;
import android.view.WindowManager;

import android.widget.ImageView;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.ExecCommand;

import org.tinylog.Logger;

import java.io.File;

public class ConfirmationWearActivity extends Activity implements DelayedConfirmationView.DelayedConfirmationListener {

    BoxInsetLayout rootLayout;
    private DelayedConfirmationView delayedConfirmationView;

    private TextView installFinishedText, restartText;
    private ImageView closeButton;

    private String paramText, paramTime, paramMode, paramPkg;
    private int time;

    private static boolean isRunning = false;

    private static final Handler mHandler = new Handler();

    private static final String DENSITY_HIGH = "wm density 148;exit";
    private static final String DENSITY_RESET = "wm density reset;exit";
    private static final String INSTALL_NON_MARKET_APPS = "settings put secure install_non_market_apps 1;exit";
    private static final String KILL_LAUNCHER = "am force-stop com.huami.watch.launcher;exit";
    private static final int INSTALL_REQUEST_CODE = 1;

    private final Runnable delayedDensity = new Runnable() {
        public void run() {
            //runCommand(KILL_LAUNCHER);
            runCommand(DENSITY_HIGH);
        }
    };

    private final Runnable delayedFinish = new Runnable() {
        public void run() {
            isRunning = false;
            Logger.debug("ConfirmationWearActivity finishConfirmationActivity delayedFinish");
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.huami.watch.launcher");
            if (launchIntent != null) {
                startActivity(launchIntent);
            }
            finish();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wear_confirmation);

        paramText = getIntent().getStringExtra(Constants.TEXT);
        paramTime = getIntent().getStringExtra(Constants.TIME);
        paramMode = getIntent().getStringExtra(Constants.MODE);
        paramPkg = getIntent().getStringExtra(Constants.PKG);

        Logger.debug("ConfirmationWearActivity onCreate paramText: " + paramText + " | paramTime: " + paramTime);
        Logger.debug("ConfirmationWearActivity onCreate isRunning: " + isRunning);

        if (paramTime == null || (Integer.valueOf(paramTime) >= 0 && Integer.valueOf(paramTime) < 3) )
            paramTime = "3";

        if (paramMode == null)
            paramMode = Constants.NORMAL;

        if (paramPkg == null)
            paramPkg = "";

        time = Integer.valueOf(paramTime);

        rootLayout = findViewById(R.id.install_root_layout);
        installFinishedText = findViewById(R.id.install_finished_text);
        restartText = findViewById(R.id.restart_text);
        closeButton = findViewById(R.id.close_button);
        delayedConfirmationView = findViewById(R.id.install_delayed_view);
        delayedConfirmationView.setTotalTimeMs(time * 1000);

        closeButton.setBackground(this.getDrawable(R.drawable.round_grey_bg));
        closeButton.setImageDrawable(this.getDrawable(R.drawable.ic_full_cancel));
        installFinishedText.setText(paramText);

        final int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

        getWindow().addFlags(flags);

        if (isRunning) {

            restartText.setText("Tap to Close");
            hideConfirm();

        } else {

            closeButton.setVisibility(View.GONE);
            delayedConfirmationView.setVisibility(View.VISIBLE);
            showConfirm();
        }

    }

    @Override
    public void onTimerSelected(View v) {
        setResult(Activity.RESULT_CANCELED, new Intent());
        v.setPressed(true);
        delayedConfirmationView.reset();
        ((DelayedConfirmationView) v).setListener(null);
        Logger.debug("ConfirmationWearActivity RESULT_CANCELED");
        finishConfirmationActivity();
    }

    @Override
    public void onTimerFinished(View v) {
        setResult(Activity.RESULT_OK, new Intent());
        v.setPressed(false);
        delayedConfirmationView.reset();
        ((DelayedConfirmationView) v).setListener(null);

        Logger.debug("ConfirmationWearActivity onTimerFinished isRunning: " + isRunning);

        if (Constants.INSTALL.equals(paramMode) || Constants.DELETE.equals(paramMode)) {
            Logger.debug("ConfirmationWearActivity onTimerFinished paramMode: " + paramMode + " | paramPkg: " + paramPkg);

            isRunning = true;
            restartText.setText("Please wait…");
            hideConfirm();

            runCommand(INSTALL_NON_MARKET_APPS);
            //DeviceUtil.killBackgroundTasks(this, false);
            runCommand(KILL_LAUNCHER);

            startInstall(paramPkg);

        } else {

            final Intent intent = new Intent(this, ConfirmationActivity.class);
            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
            startActivity(intent);
            Logger.debug("ConfirmationWearActivity RESULT_OK");
            finishConfirmationActivity();
        }
    }

    @Override
    public void finish() {
        Logger.debug("ConfirmationWearActivity finish isRunning: " + isRunning);
        super.finish();
    }

    @Override
    public void onDestroy() {

        Logger.debug("ConfirmationWearActivity onDestroy isRunning: " + isRunning);

        super.onDestroy();
    }

    private void runCommand(String command) {

        Logger.debug("ConfirmationWearActivity runCommand: " + command);
        if (!command.isEmpty()) {

            /* Deprecated, replaced with new ExecCommand class
            try {
                Runtime.getRuntime().exec(new String[]{"adb", "shell", command},
                        null, Environment.getExternalStorageDirectory());
            } catch (Exception e) {
                Logger.error("ConfirmationWearActivity runCommand exception: " + e.toString());
            }
            */

            new ExecCommand(ExecCommand.ADB, String.format("adb shell %s", command));
        }
    }

    private void startInstall(String apkPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        this.startActivityForResult(intent, INSTALL_REQUEST_CODE);
    }

    private void finishConfirmationActivity(){
        Logger.debug("ConfirmationWearActivity finishConfirmationActivity paramMode: " + paramMode);

        restartText.setText("Please wait…");

        if (isRunning && (Constants.INSTALL.equals(paramMode) || Constants.DELETE.equals(paramMode))) {

            Logger.debug("ConfirmationWearActivity finishConfirmationActivity isRunning: " + isRunning);

            runCommand(DENSITY_RESET);
            if (Constants.DELETE.equals(paramMode)) {
                final File apkFile = new File(paramPkg);
                if (apkFile.exists())
                    Logger.debug("ConfirmationWearActivity finishConfirmationActivity deleting: " + paramPkg);
                    apkFile.delete();
            }

            mHandler.postDelayed(delayedFinish, 2000);

        } else {
            if (!isRunning)
                mHandler.removeCallbacks(delayedDensity);
            isRunning = false;
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INSTALL_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Logger.debug("ConfirmationWearActivity onActivityResult RESULT_OK");
                finishConfirmationActivity();
            } else if (resultCode == RESULT_CANCELED) {
                Logger.debug("ConfirmationWearActivity onActivityResult RESULT_CANCELED");
                finishConfirmationActivity();
            } else if (resultCode == RESULT_FIRST_USER) {
                Logger.debug("ConfirmationWearActivity onActivityResult RESULT_FIRST_USER");
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private void hideConfirm() {

        Logger.debug("ConfirmationWearActivity hideConfirm isRunning: " + isRunning);

        delayedConfirmationView.setVisibility(View.GONE);
        delayedConfirmationView.setClickable(false);
        delayedConfirmationView.clearAnimation();
        closeButton.requestFocus();
        closeButton.setClickable(true);

        closeButton.setVisibility(View.VISIBLE);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishConfirmationActivity();
            }
        });

    }

    private void showConfirm() {

        Logger.debug("ConfirmationWearActivity showConfirm isRunning: " + isRunning);

        closeButton.setVisibility(View.GONE);
        closeButton.setClickable(false);
        closeButton.clearAnimation();

        delayedConfirmationView.setVisibility(View.VISIBLE);
        delayedConfirmationView.requestFocus();
        delayedConfirmationView.setClickable(true);

        if (Integer.valueOf(paramTime) > 0) {

            if (Constants.INSTALL.equals(paramMode) || Constants.DELETE.equals(paramMode)) {
                mHandler.postDelayed(delayedDensity, 3500);
            }

            restartText.setText(String.format("Continuing in %ss…", paramTime));
            delayedConfirmationView.setPressed(false);
            delayedConfirmationView.start();
            delayedConfirmationView.setListener(this);

        } else
            restartText.setText("Please wait, it may\ntake a while…");

    }

}
