package com.amazmod.service.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;

public class ConfirmationWearActivity extends Activity implements DelayedConfirmationView.DelayedConfirmationListener {

    BoxInsetLayout rootLayout;
    private DelayedConfirmationView delayedConfirmationView;

    private TextView installFinishedText, restartText;
    private String paramText, paramTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wear_confirmation);

        paramText = getIntent().getStringExtra(Constants.TEXT);
        paramTime = getIntent().getStringExtra(Constants.TIME);

        Log.d(Constants.TAG,"ConfirmationWearActivity onCreate paramText: " + paramText + " // paramTime: " + paramTime);

        if (paramTime == null || Integer.valueOf(paramTime) < 3 )
            paramTime = "3";

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
    }

    private void startDelayedConfirmationView() {
        delayedConfirmationView.setVisibility(View.VISIBLE);
        installFinishedText.setText(paramText);
        restartText.setText(String.format("Continuing in %ss…", paramTime));
        delayedConfirmationView.setPressed(false);
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);
    }

    @Override
    public void onTimerSelected(View v) {
        setResult(Activity.RESULT_CANCELED, new Intent());
        v.setPressed(true);
        delayedConfirmationView.reset();
        ((DelayedConfirmationView) v).setListener(null);
        Log.d(Constants.TAG,"ConfirmationWearActivity RESULT_CANCELED");
        finish();
    }

    @Override
    public void onTimerFinished(View v) {
        setResult(Activity.RESULT_OK, new Intent());
        ((DelayedConfirmationView) v).setListener(null);
        final Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        startActivity(intent);
        Log.d(Constants.TAG,"ConfirmationWearActivity RESULT_OK");
        finish();
    }

}
