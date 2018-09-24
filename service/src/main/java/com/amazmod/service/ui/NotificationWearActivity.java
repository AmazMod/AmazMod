package com.amazmod.service.ui;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.text.emoji.EmojiCompat;
import android.support.text.emoji.bundled.BundledEmojiCompatConfig;
import android.support.text.emoji.widget.EmojiButton;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.SwipeDismissFrameLayout;
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
import com.amazmod.service.adapters.GridViewPagerAdapter;
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

public class NotificationWearActivity extends Activity {

    @BindView(R.id.notification_wear_swipe_layout)
    SwipeDismissFrameLayout swipeLayout;
    @BindView(R.id.notification_wear_root_layout)
    BoxInsetLayout rootLayout;

    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    private static boolean screenToggle;
    private static int screenMode;
    private static int screenBrightness = 999989;
    private static boolean mustLockDevice;
    private Context mContext;

    private NotificationData notificationSpec;

    private SettingsManager settingsManager;

    private GridViewPager mGridViewPager;
    private DotsPageIndicator mPageIndicator;

    private static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationSpec = getIntent().getParcelableExtra(NotificationData.EXTRA);

        EmojiCompat.Config config = new BundledEmojiCompatConfig(this);
        config.setReplaceAll(true);
        EmojiCompat.init(config);

        this.mContext = this;

        setContentView(R.layout.activity_wear_notification);

        ButterKnife.bind(this);

        swipeLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
                                   @Override
                                   public void onDismissed(SwipeDismissFrameLayout layout) {
                                       finish();
                                   }
                               }
        );

        mGridViewPager = findViewById(R.id.pager);
        mPageIndicator = findViewById(R.id.page_indicator);
        mPageIndicator.setPager(mGridViewPager);
        GridViewPagerAdapter adapter = new GridViewPagerAdapter(this, getFragmentManager(), notificationSpec);
        mGridViewPager.setAdapter(adapter);

        settingsManager = new SettingsManager(this);

        mustLockDevice = DeviceUtil.isDeviceLocked(getBaseContext());

        //Load preferences
        boolean disableNotificationsScreenOn = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_SCREENON,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON);

        setWindowFlags(true);

        //Do not activate screen if it is disabled in settings and screen is off
        if (disableNotificationsScreenOn && mustLockDevice) {
            setScreenModeOff(true);
        } else {
            screenToggle = false;
        }

        handler = new Handler();
        activityFinishRunnable = new ActivityFinishRunnable(this);
        startTimerFinish();

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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        findViewById(R.id.notification_wear_swipe_layout).dispatchTouchEvent(event);

        if (screenToggle) {
            setScreenModeOff(false);
        }

        startTimerFinish();
        return false;
    }


    private void startTimerFinish() {
        handler.removeCallbacks(activityFinishRunnable);
        handler.postDelayed(activityFinishRunnable, notificationSpec.getTimeoutRelock());
    }

    @Override
    public void finish() {
        handler.removeCallbacks(activityFinishRunnable);
        setWindowFlags(false);
        super.finish();

        boolean flag = true;
        Log.i(Constants.TAG, "NotificationWearActivity finish screenToggle: " + screenToggle);

        if (screenToggle) {
            flag = false;
            setScreenModeOff(false);
        }

        if (mustLockDevice) {
            if (flag) {
                SystemClock.sleep(500);
            }
            lock();
        }
    }

    private void lock() {
        if (!DeviceUtil.isDeviceLocked(mContext)) {
            DevicePolicyManager mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (mDPM != null) {
                try {
                    mDPM.lockNow();
                } catch (SecurityException ex) {
                    //Toast.makeText(
                    //        this,
                    //        getResources().getText(R.string.device_owner),
                    //        Toast.LENGTH_LONG).show();
                    Log.w(Constants.TAG, getResources().getString(R.string.device_owner));
                    Log.e(Constants.TAG, "NotificationActivity SecurityException: " + ex.toString());
                }
            }
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

    private void setScreenModeOff(boolean mode) {

        WindowManager.LayoutParams params = getWindow().getAttributes();
        if (mode) {
            Log.i(Constants.TAG, "NotificationActivity setScreenModeOff1 mode: " + mode);
            screenMode = Settings.System.getInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, 0);
            screenBrightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            //Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            //Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
            getWindow().setAttributes(params);
        } else {
            if (screenBrightness != 999989) {
                Log.i(Constants.TAG, "NotificationActivity setScreenModeOff2 mode: " + mode + " / screenMode: " + screenMode);
                //Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, screenMode);
                //Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightness);
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                getWindow().setAttributes(params);
            }
        }
        screenToggle = mode;
    }
}
