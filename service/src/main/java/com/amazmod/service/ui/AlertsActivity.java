package com.amazmod.service.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.support.ActivityFinishRunnable;
import com.amazmod.service.util.DeviceUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tinylog.Logger;

public class AlertsActivity extends Activity {

    @BindView(R.id.description)
    TextView text;
    @BindView(R.id.icon)
    ImageView icon;
    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    int vibrate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.icon_overlay);
        ButterKnife.bind(this);
        setWindowFlags(true);

        SettingsManager settingsManager = new SettingsManager(this);

        vibrate = Constants.VIBRATION_SHORT;

        // Get passed parameters
        Intent myIntent = getIntent();
        String alert_type = myIntent.getStringExtra("type");

        switch(alert_type) {
            case "phone_battery":
                icon.setImageDrawable(getDrawable(R.drawable.ic_battery_alert_black_24dp));
                text.setText(getString(R.string.phone_battery,settingsManager.getInt(Constants.PREF_BATTERY_PHONE_ALERT, 0)+"%"));
                vibrate = Constants.VIBRATION_LONG;
                break;
            case "watch_battery":
                icon.setImageDrawable(getDrawable(R.drawable.ic_battery_alert_black_24dp));
                text.setText(getString(R.string.watch_battery,settingsManager.getInt(Constants.PREF_BATTERY_PHONE_ALERT, 0)+"%"));
                vibrate = Constants.VIBRATION_LONG;
                break;
            case "phone_connection":
            default:
                // type= phone_connection
                if(android.provider.Settings.System.getString(getContentResolver(), "com.huami.watch.extra.DEVICE_CONNECTION_STATUS").equals("0")){
                    // Phone disconnected
                    icon.setImageDrawable(getDrawable(R.drawable.ic_outline_phonelink_erase));
                    text.setText(getString(R.string.phone_disconnected));
                    vibrate = Constants.VIBRATION_LONG;
                }else{
                    // Phone connected
                    icon.setImageDrawable(getDrawable(R.drawable.ic_outline_phonelink_ring));
                    text.setText(getString(R.string.phone_connected));
                }
        }

        handler = new Handler();
        activityFinishRunnable = new ActivityFinishRunnable(this);
        startTimerFinish();

        final Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                makeVibration(vibrate);
            }
        }, 1500);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startTimerFinish() {

        // Vibrate
        makeVibration(Constants.PREF_DEFAULT_NOTIFICATION_VIBRATION);

        handler.removeCallbacks(activityFinishRunnable);
        handler.postDelayed(activityFinishRunnable, 3 * 1000L /* 3s */);

    }

    @Override
    public void finish() {
        handler.removeCallbacks(activityFinishRunnable);
        setWindowFlags(false);
        super.finish();
    }

    private void makeVibration(int duration) {
        //Do not vibrate if DND is active
        if (DeviceUtil.isDNDActive(this))
            return;

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        try {
            if (vibrator != null)
                vibrator.vibrate(duration);
        } catch (Exception ex) {
            Logger.error(ex, "AlertsActivity makeVibration excepition: ", ex.getMessage());
        }
    }

    private void setWindowFlags(boolean enable) {

        final int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

        if (enable) {
            getWindow().addFlags(flags);
        } else {
            getWindow().clearFlags(flags);
        }
    }
}
