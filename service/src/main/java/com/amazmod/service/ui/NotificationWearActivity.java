package com.amazmod.service.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.SwipeDismissFrameLayout;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.emoji.text.EmojiCompat;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.adapters.GridViewPagerAdapter;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.support.ActivityFinishRunnable;
import com.amazmod.service.support.HorizontalGridViewPager;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.fragments.NotificationFragment;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.SystemProperties;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationWearActivity extends Activity {

    @BindView(R.id.activity_wear_swipe_layout)
    SwipeDismissFrameLayout swipeLayout;
    @BindView(R.id.activity_wear_root_layout)
    BoxInsetLayout rootLayout;

    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    private static boolean screenToggle = false, mustLockDevice = false, wasScreenLocked = false;
    private boolean keyboardVisible = false, specialNotification = false;

    private static int screenMode;
    private static int screenBrightness = 999989;
    private Context mContext;

    private String key, mode;

    private SettingsManager settingsManager;

    private static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    public static final String KEY = "key";
    public static final String MODE = "mode";
    public static final String MODE_ADD = "add";
    public static final String MODE_VIEW = "view";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        key = getIntent().getStringExtra(KEY);
        mode = getIntent().getStringExtra(MODE);

        if (NotificationStore.getCustomNotification(key) == null){
            Logger.error("onCreate: invalid key '" + key + "' - notification will not be shown");
            finish();
            return;
        }

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
                               });

        HorizontalGridViewPager mGridViewPager = findViewById(R.id.pager);
        DotsPageIndicator mPageIndicator = findViewById(R.id.page_indicator);
        mPageIndicator.setPager(mGridViewPager);

        settingsManager = new SettingsManager(this);

        wasScreenLocked = DeviceUtil.isDeviceLocked(getBaseContext());
        if (mustLockDevice && screenToggle)
            wasScreenLocked = true;
        mustLockDevice = wasScreenLocked;

        setWindowFlags(true);

        //Load preferences
        final boolean disableNotificationsScreenOn = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_SCREENON,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON);
        boolean disableNotificationReplies = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);

        final boolean notificationHasHideReplies = NotificationStore.getHideReplies(key);
        final boolean notificationHasForceCustom = NotificationStore.getForceCustom(key);

        if (notificationHasForceCustom && notificationHasHideReplies)
            specialNotification = true;

        Logger.debug("NotificationWearActivity specialNotification: {}", specialNotification);

        //Do not activate screen if it is disabled in settings and screen was off or it was disabled previously
        if (disableNotificationsScreenOn && (wasScreenLocked || screenToggle)) {
            if (wasScreenLocked)
                mustLockDevice = true;
            if (screenToggle)
                mustLockDevice = false;
            setScreenOff();
        }

        clearBackStack();

        GridViewPagerAdapter adapter;
        List<Fragment> items = new ArrayList<Fragment>();

        items.add(NotificationFragment.newInstance(key, mode));

        adapter = new GridViewPagerAdapter(getBaseContext(), this.getFragmentManager(), items);
        mGridViewPager.setAdapter(adapter);

        handler = new Handler();
        activityFinishRunnable = new ActivityFinishRunnable(this);
        startTimerFinish();

        if (SystemProperties.isVerge()) {
            MediaPlayer mPlayer = MediaPlayer.create(this, R.raw.alerts_notification);
            mPlayer.start();
        }

        Logger.info("NotificationWearActivity onCreate key: " + key + " | mode: "+ mode
                + " | wasLckd: "+ wasScreenLocked + " | mustLck: " + mustLockDevice
                + " | scrTg: " + screenToggle);
    }

    private void clearBackStack() {
        FragmentManager manager = this.getFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            Logger.warn("NotificationWearActivity ***** clearBackStack getBackStackEntryCount: " + manager.getBackStackEntryCount());
            while (manager.getBackStackEntryCount() > 0){
                manager.popBackStackImmediate();
            }
        }
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
        findViewById(R.id.activity_wear_root_layout).dispatchTouchEvent(event);

        if (screenToggle)
            setScreenOn();

        if (!keyboardVisible) {
            startTimerFinish();
        }
        return false;
    }

    public void startTimerFinish() {
        if (!mode.equals(MODE_VIEW)) {
            Logger.debug("NotificationWearActivity startTimerFinish");
            if (activityFinishRunnable != null)
                handler.removeCallbacks(activityFinishRunnable);
            int timeOutRelock = NotificationStore.getTimeoutRelock(key);
            if (timeOutRelock == 0)
                timeOutRelock = settingsManager.getInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, Constants.PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT);
            handler.postDelayed(activityFinishRunnable, timeOutRelock);
        }
    }

    public void stopTimerFinish() {
        Logger.debug("NotificationWearActivity stopTimerFinish");
        if (activityFinishRunnable != null)
            handler.removeCallbacks(activityFinishRunnable);
    }

    public void setKeyboardVisible(boolean visible){
        keyboardVisible = visible;
    }

    @Override
    public void finish() {
        if (activityFinishRunnable != null)
            handler.removeCallbacks(activityFinishRunnable);
        setWindowFlags(false);
        super.finish();

        boolean flag = true;
        Logger.info("NotificationWearActivity finish key: " + key
                + " | scrT: " + screenToggle + " | mustLck: " + mustLockDevice);

        if (screenToggle) {
            flag = false;
            setScreenOn();
        }

        if (mustLockDevice) {
            mustLockDevice = false;
            screenToggle = false;
            if (flag) {
                final Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        lock();
                    }
                }, 500);
            } else
                lock();
        } else if (wasScreenLocked)
            mustLockDevice = true;

        if (specialNotification) {
            NotificationStore.removeCustomNotification(key);
            NotificationStore.setNotificationCount(mContext);
        }
    }

    private void lock() {
        if (!DeviceUtil.isDeviceLocked(mContext)) {
            DevicePolicyManager mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (mDPM != null) {
                try {
                    mDPM.lockNow();
                } catch (SecurityException ex) {
                    //Toast.makeText(this, getResources().getText(R.string.device_owner), Toast.LENGTH_LONG).show();
                    Logger.error("NotificationWearActivity SecurityException: " + ex.toString());
                }
            }
        }
    }

    private void setWindowFlags(boolean enable) {

        if (MODE_VIEW.equals(mode))
            return;

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

    private void setScreenOff(){
        setScreenModeOff(true);
    }

    private void setScreenOn(){
        setScreenModeOff(false);
    }

    private void setScreenModeOff(boolean mode) {

        WindowManager.LayoutParams params = getWindow().getAttributes();
        if (mode) {
            Logger.info("NotificationWearActivity setScreenModeOff true");
            screenMode = DeviceUtil.systemGetInt(mContext, SCREEN_BRIGHTNESS_MODE, 0);
            screenBrightness = DeviceUtil.systemGetInt(mContext, Settings.System.SCREEN_BRIGHTNESS, 0);
            //Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            //Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
            getWindow().setAttributes(params);
        } else {
            if (screenBrightness != 999989) {
                Logger.info("NotificationWearActivity setScreenModeOff false | screenMode: " + screenMode);
                //Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, screenMode);
                //Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightness);
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                getWindow().setAttributes(params);
            }
        }
        screenToggle = mode;
    }
}
