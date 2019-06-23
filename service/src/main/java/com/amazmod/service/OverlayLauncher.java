package com.amazmod.service;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.graphics.PixelFormat;

import android.os.IBinder;
import android.os.Vibrator;
import android.view.Gravity;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;

import com.amazmod.service.springboard.LauncherWearGridActivity;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.SystemProperties;

import org.tinylog.Logger;

public class OverlayLauncher extends Service implements OnTouchListener, OnClickListener {

    private Button overlayedButton;
    private WindowManager wm;
    private Context context;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint({"RtlHardcoded", "ClickableViewAccessibility"})
    @Override
    public void onCreate() {
        super.onCreate();

        Logger.debug("OverlayLauncher onCreate");

        context = getApplicationContext();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        overlayedButton = new Button(this);
        overlayedButton.setText("                                            ");
        overlayedButton.setBackgroundColor(0x00fe4444);
        overlayedButton.setOnTouchListener(this);
        overlayedButton.setOnClickListener(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        if (DeviceUtil.isVerge()) {
            params.height = 35;
            params.width = 180;
            params.x = 180;
        }
        else {
            params.height = 20;
            params.width = 160;
            params.x = 160;
        }
        params.y = 0;
        wm.addView(overlayedButton, params);

    }

    @Override
    public void onDestroy() {
        if (overlayedButton != null) {
            wm.removeView(overlayedButton);
            overlayedButton = null;
        }
        Logger.debug("OverlayLauncher onDestroy");
        super.onDestroy();
    }

    @Override
    public void onClick(View notification) {

        final boolean inWhatFace = SystemProperties.getBoolean("prop.launcher.at_watchface", false);
        Logger.debug("OverlayLauncher onClick isWatchFace: {}", inWhatFace);

        if (inWhatFace) {

            Intent intent = new Intent(this, LauncherWearGridActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.NOTIFICATIONS);

            try {
                ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(context, R.anim.slide_in_from_right, R.anim.fade_out);
                this.startActivity(intent, activityOptions.toBundle());
                Logger.info("OverlayLauncher onClick activity started with animation");
            } catch (Exception ex){
                this.startActivity(intent);
                Logger.error(ex, "OverlayLauncher onClick - failed to get context!");
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        final boolean inWhatFace = SystemProperties.getBoolean("prop.launcher.at_watchface", false);
        Logger.debug("OverlayLauncher onTouch isWatchFace: {}", inWhatFace);

        //Vibrate when touched while watchface is shown
        if (inWhatFace) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibe != null) {
                    vibe.vibrate(10);
                }
            }
        }

        return false;
    }
}
