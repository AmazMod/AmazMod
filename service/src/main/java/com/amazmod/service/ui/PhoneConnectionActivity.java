package com.amazmod.service.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
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
import xiaofei.library.hermeseventbus.HermesEventBus;

public class PhoneConnectionActivity extends Activity {

    @BindView(R.id.description)
    TextView text;
    @BindView(R.id.icon)
    ImageView icon;
    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.icon_overlay);

        ButterKnife.bind(this);

        if(android.provider.Settings.System.getString(getContentResolver(), "com.huami.watch.extra.DEVICE_CONNECTION_STATUS").equals("0")){
            // Phone disconnected
            // Wake screen and trow overlay here
            icon.setImageDrawable(getDrawable(R.drawable.ic_outline_phonelink_erase));
            text.setText(getString(R.string.phone_disconnected));
        }else{
            // Phone connected
            // Wake screen and trow overlay here
            icon.setImageDrawable(getDrawable(R.drawable.ic_outline_phonelink_ring));
            text.setText(getString(R.string.phone_connected));
        }

        handler = new Handler();
        activityFinishRunnable = new ActivityFinishRunnable(this);
        startTimerFinish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //ShakeDetector.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //ShakeDetector.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //ShakeDetector.destroy();
    }

    private void startTimerFinish() {
        // Vibrate
        try {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(1000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        handler.removeCallbacks(activityFinishRunnable);
        handler.postDelayed(activityFinishRunnable, 3*1000);
    }

    @Override
    public void finish() {
        handler.removeCallbacks(activityFinishRunnable);
        super.finish();
    }
}
