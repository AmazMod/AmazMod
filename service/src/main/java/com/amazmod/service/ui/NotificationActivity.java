package com.amazmod.service.ui;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
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
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.support.ActivityFinishRunnable;
import com.amazmod.service.AdminReceiver;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.SystemProperties;
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

public class NotificationActivity extends Activity {

    @BindView(R.id.notification_title)
    TextView title;
    @BindView(R.id.notification_time)
    TextView time;
    @BindView(R.id.notification_text)
    TextView text;
    @BindView(R.id.notification_icon)
    ImageView icon;
    @BindView(R.id.notification_replies_container)
    LinearLayout repliesContainer;
    @BindView(R.id.notification_root_layout)
    LinearLayout rootLayout;

    @BindView(R.id.activity_buttons)
    LinearLayout buttonsLayout;
    @BindView(R.id.activity_notification_button_reply)
    Button replyButton;
    @BindView(R.id.activity_notification_button_close)
    Button closeButton;

    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    private static boolean nullError = false;
    private static boolean screenToggle;
    private static float fontSizeSP;
    private static int screenMode;
    private static int screenBrightness = 999989;
    private static boolean mustLockDevice;
    private Context mContext;

    private NotificationData notificationSpec;

    private SettingsManager settingsManager;

    private static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;
    private static final float FONT_SIZE_NORMAL = 14.0f;
    private static final float FONT_SIZE_LARGE = 18.0f;
    private static final float FONT_SIZE_HUGE = 22.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mContext = this;

        setContentView(R.layout.activity_notification);

        ButterKnife.bind(this);

        settingsManager = new SettingsManager(this);

        notificationSpec = getIntent().getParcelableExtra(NotificationData.EXTRA);

        mustLockDevice = DeviceUtil.isDeviceLocked(getBaseContext());

        boolean hideReplies;

        //Load preferences
        boolean disableNotificationsScreenOn = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_SCREENON,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON);
        boolean disableNotificationReplies = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);
        boolean enableInvertedTheme = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_INVERTED_THEME,
                Constants.PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME);

        setWindowFlags(true);

        //Do not activate screen if it is disabled in settings and screen is off
        if (disableNotificationsScreenOn && mustLockDevice) {
            setScreenModeOff(true);
        } else {
            screenToggle = false;
        }

        // Set theme and font size
        //Log.d(Constants.TAG, "NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            icon.setBackgroundColor(getResources().getColor(R.color.darker_gray));
        }

        setFontSizeSP();
        time.setTextSize(fontSizeSP);
        title.setTextSize(fontSizeSP);
        text.setTextSize(fontSizeSP);

        try {

            hideReplies = notificationSpec.getHideReplies();

            int[] iconData = notificationSpec.getIcon();
            int iconWidth = notificationSpec.getIconWidth();
            int iconHeight = notificationSpec.getIconHeight();
            Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

            icon.setImageBitmap(bitmap);
            title.setText(notificationSpec.getTitle());
            text.setText(notificationSpec.getText());
            time.setText(notificationSpec.getTime());

            if (notificationSpec.getVibration() > 0) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(notificationSpec.getVibration());
                }
            }

        } catch (NullPointerException ex) {
            Log.e(Constants.TAG, "NotificationActivity onCreate - Exception: " + ex.toString()
                    + " notificationSpec: " + notificationSpec);
            title.setText("AmazMod");
            text.setText("Welcome to AmazMod");
            time.setText("00:00");
            icon.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.amazmod));
            hideReplies = true;
            nullError = true;
        }

        //Probably it is not needed anymore
        //if (screenToggle && nullError) {
        //    setScreenModeOff(false);
        //}

        if (!hideReplies && !disableNotificationReplies) {
            buttonsLayout.setVisibility(View.GONE);
            addReplies();
        } else {
            buttonsLayout.setVisibility(View.VISIBLE);
            if (!hideReplies) {
                replyButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);
                replyButton.setAllCaps(true);
                replyButton.setText(R.string.replies);
            } else {
                replyButton.setVisibility(View.GONE);
            }
            closeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);
            closeButton.setAllCaps(true);
            closeButton.setText(R.string.close);
        }

        handler = new Handler();
        activityFinishRunnable = new ActivityFinishRunnable(this);
        startTimerFinish();

        /* ShakeDetector.create(this, new ShakeDetector.OnShakeListener() {
            @Override
            public void OnShake() {
                NotificationActivity.this.finish();
                Toast.makeText(getApplicationContext(), "Shaken!", Toast.LENGTH_SHORT).show();
            }
        });

        float gravity = settingsManager.getInt(Constants.PREF_SHAKE_TO_DISMISS_GRAVITY, 1000) / 1000f;
        int numOfShakes = settingsManager.getInt(Constants.PREF_SHAKE_TO_DISMISS_NUM_OF_SHAKES, 2);

        ShakeDetector.updateConfiguration(gravity, numOfShakes); */
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        findViewById(R.id.notification_root_layout).dispatchTouchEvent(event);

        if (screenToggle) {
            setScreenModeOff(false);
        }

        startTimerFinish();

        return false;
    }

    @OnClick(R.id.activity_notification_button_close)
    public void clickClose() {
        //mustLockDevice = true;
        finish();
    }

    @OnClick(R.id.activity_notification_button_reply)
    public void clickReply() {
        Intent intent = new Intent(this, RepliesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtras(notificationSpec.toBundle());
        intent.putExtra("MUSTLOCKDEVICE", mustLockDevice);
        this.startActivity(intent);
        mustLockDevice = false;
        finish();
    }

    private void startTimerFinish() {
        handler.removeCallbacks(activityFinishRunnable);
        if (!nullError) {
            handler.postDelayed(activityFinishRunnable, notificationSpec.getTimeoutRelock());
        }
    }

    @Override
    public void finish() {
        handler.removeCallbacks(activityFinishRunnable);
        setWindowFlags(false);
        super.finish();

        if (mustLockDevice) {
            SystemClock.sleep(100);
            lock();
        }

        Log.i(Constants.TAG, "NotificationActivity finish screenToggle: " + screenToggle);
        if (screenToggle) {
            //SystemProperties.goToSleep(mContext);
            setScreenModeOff(false);
            /* Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    try {
                        //Toast.makeText(getApplicationContext(), "delayed", Toast.LENGTH_SHORT).show();
                        setScreenModeOff(false);
                        Log.i(Constants.TAG, "NotificationActivity delayed finish");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(Constants.TAG, "NotificationActivity finish exception: " + e.toString());
                    }
                }
            }, 10000 - notificationSpec.getTimeoutRelock() + 600); */
        }

    }

    private void lock() {
        if (!DeviceUtil.isDeviceLocked(mContext)) {
            DevicePolicyManager mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (mDPM != null) {
                try {
                    mDPM.lockNow();
                } catch (SecurityException ex) {
                    Toast.makeText(
                            this,
                            getResources().getText(R.string.device_owner),
                            Toast.LENGTH_LONG).show();
                    Log.e(Constants.TAG, "NotificationActivity SecurityException: " + ex.toString());
                /* ComponentName admin = new ComponentName(mContext, AdminReceiver.class);
                Intent intent = new Intent(
                        DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(
                        DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                mContext.startActivity(intent); */
                }
            }
        }
    }

    private void setFontSizeSP(){
        String fontSize = settingsManager.getString(Constants.PREF_NOTIFICATIONS_FONT_SIZE,
                Constants.PREF_DEFAULT_NOTIFICATIONS_FONT_SIZE);
        switch (fontSize) {
            case "l":
                fontSizeSP = FONT_SIZE_LARGE;
                break;
            case "h":
                fontSizeSP = FONT_SIZE_HUGE;
                break;
            default:
                fontSizeSP = FONT_SIZE_NORMAL;
        }
    }

    private void addReplies() {

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        List<Reply> repliesList = loadReplies();
        for (final Reply reply : repliesList) {
            Button button = new Button(this);
            button.setLayoutParams(param);
            button.setText(reply.getValue());
            button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HermesEventBus.getDefault().post(new ReplyNotificationEvent(notificationSpec.getKey(), reply.getValue()));
                    finish();
                }
            });
            repliesContainer.addView(button);
        }
        //Add Close button
        Button button = new Button(this);
        button.setLayoutParams(param);
        button.setText(R.string.close);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        repliesContainer.addView(button);

    }

    private List<Reply> loadReplies() {
        final String replies = settingsManager.getString(Constants.PREF_NOTIFICATION_CUSTOM_REPLIES, "[]");

        try {
            Type listType = new TypeToken<List<Reply>>() {
            }.getType();
            return new Gson().fromJson(replies, listType);
        } catch (Exception ex) {
            return new ArrayList<>();
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
        if (mode) {
            screenMode = Settings.System.getInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, 0);
            screenBrightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        } else {
            if (screenBrightness != 999989) {
                Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, screenMode);
                Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightness);
            }
        }
        screenToggle = mode;
    }
}
