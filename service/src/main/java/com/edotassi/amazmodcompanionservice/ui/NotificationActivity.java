package com.edotassi.amazmodcompanionservice.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
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

import com.edotassi.amazmodcompanionservice.Constants;
import com.edotassi.amazmodcompanionservice.R;
import com.edotassi.amazmodcompanionservice.R2;
import com.edotassi.amazmodcompanionservice.events.ReplyNotificationEvent;
import com.edotassi.amazmodcompanionservice.settings.SettingsManager;
import com.edotassi.amazmodcompanionservice.support.ActivityFinishRunnable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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
    @BindView(R2.id.notification_text)
    TextView text;
    @BindView(R2.id.notification_icon)
    ImageView icon;
    @BindView(R2.id.notification_replies_container)
    LinearLayout repliesContainer;

    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    private NotificationData notificationSpec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        setContentView(R.layout.activity_notification);

        ButterKnife.bind(this);

        notificationSpec = getIntent().getParcelableExtra(NotificationData.EXTRA);

        try {
            title.setText(notificationSpec.getTitle());
            text.setText(notificationSpec.getText());
            int[] iconData = notificationSpec.getIcon();
            int iconWidth = notificationSpec.getIconWidth();
            int iconHeight = notificationSpec.getIconHeight();
            Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

            icon.setImageBitmap(bitmap);

            if (notificationSpec.getVibration() > 0) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(notificationSpec.getVibration());
            }
        } catch (Exception ex) {
            Log.d("NotificationActivity","Exception: " + ex + " notificationSpec: " + notificationSpec);
            title.setText("AmazMod");
            text.setText("Welcome to AmazMod");
            icon.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher_background));
        }

        handler = new Handler();
        activityFinishRunnable = new ActivityFinishRunnable(this);

        startTimerFinish();

        SettingsManager settingsManager = new SettingsManager(this);
        final boolean disableNotificationReplies = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        if (!disableNotificationReplies) {

            List<Reply> repliesList = loadReplies();
            for (final Reply reply : repliesList) {
                Button button = new Button(this);
                button.setLayoutParams(param);
                button.setText(reply.getValue());
                button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HermesEventBus.getDefault().post(new ReplyNotificationEvent(notificationSpec.getKey(), reply.getValue()));
                        finish();
                    }
                });

                repliesContainer.addView(button);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        findViewById(R.id.notification_root_layout).dispatchTouchEvent(event);

        startTimerFinish();

        return false;
    }

    @OnClick(R2.id.activity_notification_button_close)
    public void clickClose() {
        finish();
    }

    @OnClick(R2.id.activity_notification_button_reply)
    public void clickReply() {
        Toast.makeText(this, "not_implented", Toast.LENGTH_SHORT).show();
    }

    private void startTimerFinish() {
        try {
            handler.removeCallbacks(activityFinishRunnable);
            handler.postDelayed(activityFinishRunnable, notificationSpec.getTimeoutRelock());
        } catch (Exception ex) {
        Log.d("NotificationActivity","Exception: " + ex + " activityFinishRunnable: " + activityFinishRunnable);
        }
    }

    @Override
    public void finish() {
        handler.removeCallbacks(activityFinishRunnable);
        super.finish();
    }

    private List<Reply> loadReplies() {
        SettingsManager settingsManager = new SettingsManager(this);
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
