package com.amazmod.service.ui;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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
import com.amazmod.service.R2;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.support.ActivityFinishRunnable;
import com.amazmod.service.util.SystemProperties;
import com.github.tbouron.shakedetector.library.ShakeDetector;
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

    @BindView(R2.id.notification_title)
    TextView title;
    @BindView(R2.id.notification_time)
    TextView time;
    @BindView(R2.id.notification_text)
    TextView text;
    @BindView(R2.id.notification_icon)
    ImageView icon;
    @BindView(R2.id.notification_replies_container)
    LinearLayout repliesContainer;
    @BindView(R2.id.notification_root_layout)
    LinearLayout rootLayout;

    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    private Boolean nullError = false;

    private NotificationData notificationSpec;

    private SettingsManager settingsManager;

    private static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    private static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    private static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;
    private static int screenMode;
    private static int screenBrightness;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mContext = this;

        final float FONT_SIZE_NORMAL = 14.0f;
        final float FONT_SIZE_LARGE = 18.0f;
        final float FONT_SIZE_HUGE = 22.0f;

        setContentView(R.layout.activity_notification);

        ButterKnife.bind(this);

        settingsManager = new SettingsManager(this);

        // Do not activate screen if it is disabled in settings
        boolean isDisableNotificationsScreenOn = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_SCREENON,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (isDisableNotificationsScreenOn) {
            screenMode = Settings.System.getInt(this.getContentResolver(), SCREEN_BRIGHTNESS_MODE,0);
            screenBrightness = Settings.System.getInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,0);
            Settings.System.putInt(this.getContentResolver(), SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
        }

        // Set theme and font size
        boolean enableInvertedTheme = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_INVERTED_THEME,
                Constants.PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME);
        String fontSize = settingsManager.getString(Constants.PREF_NOTIFICATIONS_FONT_SIZE,
                Constants.PREF_DEFAULT_NOTIFICATIONS_FONT_SIZE);
        Log.d(Constants.TAG, "NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        float fontSizeSP;

        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            icon.setBackgroundColor(getResources().getColor(R.color.darker_gray));
        }
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
        time.setTextSize(fontSizeSP);
        title.setTextSize(fontSizeSP);
        text.setTextSize(fontSizeSP);

        notificationSpec = getIntent().getParcelableExtra(NotificationData.EXTRA);

        boolean hideReplies;

        try {
            title.setText(notificationSpec.getTitle());
            text.setText(notificationSpec.getText());
            time.setText(notificationSpec.getTime());
            int[] iconData = notificationSpec.getIcon();
            int iconWidth = notificationSpec.getIconWidth();
            int iconHeight = notificationSpec.getIconHeight();
            Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

            hideReplies = notificationSpec.getHideReplies();

            //Modified for RC1
            //if (notificationSpec.getHideButtons()) {
            findViewById(R.id.activity_buttons).setVisibility(View.GONE);
            //}

            icon.setImageBitmap(bitmap);

            //Changed for RC1 - vibrates only for voice & maps
            //if (!notificationSpec.getHideButtons() && hideReplies) {
                if (notificationSpec.getVibration() > 0) {
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        vibrator.vibrate(notificationSpec.getVibration());
                    }
                }
            //}

        } catch (NullPointerException ex) {
            Log.e(Constants.TAG, "NotificationActivity onCreate - Exception: " + ex.toString() + " notificationSpec: " + notificationSpec);
            title.setText("AmazMod");
            text.setText("Welcome to AmazMod");
            time.setText("00:00");
            icon.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.amazmod));
            hideReplies = true;
            nullError = true;
        }

        boolean disableNotificationReplies = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);

        if (disableNotificationReplies || hideReplies) {
            disableNotificationReplies = true;
        }

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        if (!disableNotificationReplies) {
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
            //Added for RC1
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

        //Added for RC1: it adds close button for voice/maps/test notification
        if (!nullError) {
            if (!notificationSpec.getHideButtons() && hideReplies) {
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
        } else {
            findViewById(R.id.activity_buttons).setVisibility(View.VISIBLE);
        }

        handler = new Handler();
        activityFinishRunnable = new ActivityFinishRunnable(this);
        startTimerFinish();

        ShakeDetector.create(this, new ShakeDetector.OnShakeListener() {
            @Override
            public void OnShake() {
                NotificationActivity.this.finish();
                Toast.makeText(getApplicationContext(), "Shaken!", Toast.LENGTH_SHORT).show();
            }
        });

        float gravity = settingsManager.getInt(Constants.PREF_SHAKE_TO_DISMISS_GRAVITY, 1000) / 1000f;
        int numOfShakes = settingsManager.getInt(Constants.PREF_SHAKE_TO_DISMISS_NUM_OF_SHAKES, 2);

        ShakeDetector.updateConfiguration(gravity, numOfShakes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ShakeDetector.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ShakeDetector.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShakeDetector.destroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        findViewById(R.id.notification_root_layout).dispatchTouchEvent(event);

        Settings.System.putInt(this.getContentResolver(), SCREEN_BRIGHTNESS_MODE, screenMode);
        Settings.System.putInt(this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightness);
        startTimerFinish();

        return false;
    }

    @OnClick(R2.id.activity_notification_button_close)
    public void clickClose() {
        finish();
    }

    @OnClick(R2.id.activity_notification_button_reply)
    public void clickReply() {
        if (nullError) {
            finish();
        } else {
            //Added the code here because there is an error of the BroadcastReceiver being leaked otherwise #1
            postWithStandardUI(notificationSpec);
            finish();
        }
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
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        super.finish();

        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                try {
                    Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, screenMode);
                    Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightness);
                    SystemProperties.goToSleep(mContext);
                    Log.i(Constants.TAG,"NotificationActivity delayed finish");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(Constants.TAG, "Notificationctivity finish exception: " + e.toString());
                }
            }
        }, 1000);

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

    //Added the code here because there is an error of the BroadcastReceiver being leaked otherwise #2
    private void postWithStandardUI(NotificationData notificationData) {

        Context context;

        context = getBaseContext();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        //notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        Log.d(Constants.TAG, "NotificationActivity postWithStandardUI");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setStyle(new NotificationCompat.InboxStyle())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationData.getText()))
                .setContentText(notificationData.getText())
                .setContentTitle(notificationData.getTitle())
                .setVibrate(new long[]{0});

        notificationManager.notify(notificationData.getId(), builder.build());
        //Notification notification = builder.build();
        //try {
        //    notificationManager.notify(notificationData.getId(), builder.build());
        //} catch (NullPointerException ex) {
        //    Log.d(Constants.TAG, "NotificationActivity postWithStandarUI - Exception: " + ex.toString() + " - Notification: " + notification.toString());
        //}
    }
}
