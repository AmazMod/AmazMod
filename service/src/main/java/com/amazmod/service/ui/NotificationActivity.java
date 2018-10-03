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
import android.support.wearable.view.SwipeDismissFrameLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
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
    BoxInsetLayout rootLayout;
    @BindView(R.id.notification_swipe_layout)
    SwipeDismissFrameLayout swipeLayout;

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
    private static String defaultLocale;
    private boolean enableInvertedTheme;
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

        EmojiCompat.Config config = new BundledEmojiCompatConfig(this);
        config.setReplaceAll(true);
        EmojiCompat.init(config);

        this.mContext = this;

        setContentView(R.layout.activity_notification);

        ButterKnife.bind(this);

        swipeLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
                                    @Override
                                    public void onDismissed(SwipeDismissFrameLayout layout) {
                                        finish();
                                    }
                                }
        );

        settingsManager = new SettingsManager(this);

        notificationSpec = getIntent().getParcelableExtra(NotificationData.EXTRA);

        mustLockDevice = DeviceUtil.isDeviceLocked(getBaseContext());

        boolean hideReplies;

        //Load preferences
        boolean disableNotificationsScreenOn = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_SCREENON,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON);
        boolean disableNotificationReplies = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);
        enableInvertedTheme = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_INVERTED_THEME,
                Constants.PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME);
        defaultLocale = settingsManager.getString(Constants.PREF_DEFAULT_LOCALE, "");
        Log.i(Constants.TAG, "NotificationActivity defaultLocale: " + defaultLocale);

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
            swipeLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            icon.setBackgroundColor(getResources().getColor(R.color.darker_gray));
        } else
            swipeLayout.setBackgroundColor(getResources().getColor(R.color.black));

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
            setFontLocale(text, defaultLocale);
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
                setFontLocale(replyButton, defaultLocale);
                replyButton.setText(R.string.reply);
                setButtonTheme(replyButton, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
            } else {
                replyButton.setVisibility(View.GONE);
            }
            /* Disabled when using swipe to close
            closeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);
            closeButton.setAllCaps(true);
            setFontLocale(closeButton, defaultLocale);
            closeButton.setText(R.string.close);
            setButtonTheme(closeButton, Constants.RED); */
            closeButton.setVisibility(View.GONE);
            buttonsLayout.setOrientation(LinearLayout.VERTICAL);
            replyButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            replyButton.setGravity(Gravity.CENTER);
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
    public void clickClose(Button b) {
        b.setBackground(getDrawable(R.drawable.reply_dark_grey));
        //mustLockDevice = true;
        finish();
    }

    @OnClick(R.id.activity_notification_button_reply)
    public void clickReply(Button b) {
        b.setBackground(getDrawable(R.drawable.reply_dark_grey));
        text.setVisibility(View.GONE);
        time.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.GONE);
        addReplies();
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

        boolean flag = true;
        Log.i(Constants.TAG, "NotificationActivity finish screenToggle: " + screenToggle);

        if (screenToggle) {
            flag = false;
            setScreenModeOff(false);
        }

        if (mustLockDevice) {
            if (flag) {
                final Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        lock();
                    }
                }, 500);
            } else
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

    private void setFontLocale(TextView tv, String locale) {
        Log.i(Constants.TAG, "NotificationActivity setFontLocale TextView: " + locale);
        if (locale.contains("iw")) {
            Typeface face = Typeface.createFromAsset(getAssets(),"fonts/DroidSansFallback.ttf");
            tv.setTypeface(face);
        }
    }

    private void setFontLocale(Button b, String locale) {
        Log.i(Constants.TAG, "NotificationActivity setFontLocale Button: " + locale);
        if (locale.contains("iw")) {
            Typeface face = Typeface.createFromAsset(getAssets(),"fonts/DroidSansFallback.ttf");
            b.setTypeface(face);
        }
    }

    private void addReplies() {
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        param.setMargins(20,8,20,8);

        List<Reply> repliesList = loadReplies();
        for (final Reply reply : repliesList) {
            EmojiButton button = new EmojiButton(this);
            button.setLayoutParams(param);
            button.setPadding(0,8,0,8);
            setFontLocale(button, defaultLocale);
            button.setText(reply.getValue());
            button.setAllCaps(false);
            button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);
            setButtonTheme(button, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setBackground(getDrawable(R.drawable.reply_dark_grey));
                    HermesEventBus.getDefault().post(new ReplyNotificationEvent(notificationSpec.getKey(), reply.getValue()));
                    finish();
                }
            });
            repliesContainer.addView(button);
        }
        /* Disabled when using swipe to close
        //Add Close button
        Button button = new Button(this);
        button.setPadding(0,8,0,8);
        button.setLayoutParams(param);
        setFontLocale(button, defaultLocale);
        button.setText(R.string.close);
        button.setAllCaps(true);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);
        setButtonTheme(button, Constants.RED);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setBackground(getDrawable(R.drawable.reply_dark_grey));
                finish();
            }
        });
        repliesContainer.addView(button); */

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

    private void setButtonTheme(Button button, String color){
        switch (color) {
            case ("red"): {
                button.setTextColor(Color.parseColor("#ffffff"));
                button.setBackground(getDrawable(R.drawable.close_red));
                break;
            }
            case ("blue"): {
                button.setTextColor(Color.parseColor("#ffffff"));
                button.setBackground(getDrawable(R.drawable.reply_blue));
                break;
            }
            case ("grey"): {
                button.setTextColor(Color.parseColor("#000000"));
                button.setBackground(getDrawable(R.drawable.reply_grey));
                break;
            }
            default: {
                button.setTextColor(Color.parseColor("#000000"));
                button.setBackground(getDrawable(R.drawable.reply_grey));
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
