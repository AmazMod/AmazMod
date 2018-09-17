package com.amazmod.service.ui;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.support.ActivityFinishRunnable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;
import butterknife.BindView;
import butterknife.ButterKnife;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class RepliesActivity extends Activity {

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

    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    private static boolean nullError = false;
    private static boolean mustLockDevice;
    private static float fontSizeSP;

    private NotificationData notificationSpec;

    private SettingsManager settingsManager;

    private static final float FONT_SIZE_NORMAL = 14.0f;
    private static final float FONT_SIZE_LARGE = 18.0f;
    private static final float FONT_SIZE_HUGE = 22.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notification);

        ButterKnife.bind(this);

        settingsManager = new SettingsManager(this);

        notificationSpec = getIntent().getParcelableExtra(NotificationData.EXTRA);

        mustLockDevice = getIntent().getBooleanExtra("MUSTLOCKDEVICE", true);

        //Load preferences
        boolean enableInvertedTheme = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_INVERTED_THEME,
                Constants.PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME);

        //Make sure background is transparent
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.transparent));
        rootLayout.getRootView().setBackgroundColor(getResources().getColor(R.color.transparent));

        //Set theme and font size
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            icon.setBackgroundColor(getResources().getColor(R.color.darker_gray));
        } else {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.black));
            time.setTextColor(getResources().getColor(R.color.text));
            title.setTextColor(getResources().getColor(R.color.text));
            text.setTextColor(getResources().getColor(R.color.text));
            icon.setBackgroundColor(getResources().getColor(R.color.black));
        }

        setFontSizeSP();
        time.setTextSize(fontSizeSP);
        title.setTextSize(fontSizeSP);
        text.setTextSize(fontSizeSP);

        try {
            int[] iconData = notificationSpec.getIcon();
            int iconWidth = notificationSpec.getIconWidth();
            int iconHeight = notificationSpec.getIconHeight();
            Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);
            icon.setImageBitmap(bitmap);
            title.setText(notificationSpec.getTitle());
            text.setVisibility(View.GONE);
            time.setVisibility(View.GONE);

        } catch (NullPointerException ex) {
            Log.e(Constants.TAG, "NotificationActivity onCreate - Exception: " + ex.toString()
                    + " notificationSpec: " + notificationSpec);
            title.setText("AmazMod");
            text.setText("Welcome to AmazMod");
            time.setText("00:00");
            icon.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.amazmod));
            nullError = true;
        }

        buttonsLayout.setVisibility(View.GONE);
        addReplies();

        handler = new Handler();
        activityFinishRunnable = new ActivityFinishRunnable(this);
        startTimerFinish();

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        findViewById(R.id.notification_root_layout).dispatchTouchEvent(event);

        startTimerFinish();

        return false;
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
        super.finish();
        if (mustLockDevice) {
            DevicePolicyManager mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (mDPM != null) {
                SystemClock.sleep(500);
                mDPM.lockNow();
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


}