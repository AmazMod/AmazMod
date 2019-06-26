package com.amazmod.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

import com.amazmod.service.springboard.LauncherWearGridActivity;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.SystemProperties;

import org.tinylog.Logger;

public class OverlayLauncher extends Service implements OnTouchListener {

    private View overlayLauncher;
    private WindowManager wm;
    private WindowManager.LayoutParams params, paramsRight, paramsLeft;
    private Context context;

    private static char position;
    private static int originX, originY, moveX, moveY;
    private static int vibration;

    private static final char POSITION_RIGHT = 'R';
    private static final char POSITION_LEFT = 'L';

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

        overlayLauncher = new View(this);
        overlayLauncher.setBackgroundColor(0x40fe4444);
        overlayLauncher.setOnTouchListener(this);

        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        if (DeviceUtil.isVerge()) {
            params.height = 30;
            params.width = 170;
            params.x = 0;
            vibration = 30;
        }
        else {
            params.height = 15;
            params.width = 150;
            params.x = 0;
            vibration = 10;
        }
        params.y = 0;

        position = POSITION_RIGHT;
        wm.addView(overlayLauncher, params);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                overlayLauncher.setBackgroundColor(0x00fe4444);
            }
        }, 3000);
    }

    @Override
    public void onDestroy() {
        if (overlayLauncher != null) {
            wm.removeView(overlayLauncher);
            overlayLauncher = null;
        }
        Logger.debug("OverlayLauncher onDestroy");
        super.onDestroy();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        Logger.debug("OverlayLauncher onTouch action: {}", event.getAction());

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                originX = (int) event.getX();
                originY = (int) event.getY();
                Logger.debug("OverlayLauncher onTouch ACTION_DOWN x: {} y: {}", originX, originY);
                if (isWatchface())
                    vibrate();
                break;

            case MotionEvent.ACTION_MOVE:
                moveX = (int) event.getX();
                moveY = (int) event.getY();
                Logger.debug("OverlayLauncher onTouch ACTION_MOVE x: {} y: {}", moveX, moveY);
                break;

            case MotionEvent.ACTION_UP:
                Logger.debug("OverlayLauncher onTouch ACTION_UP originX: {} moveX: {}", originX, moveX);

                if (((originX - moveX) > 80) && position == POSITION_RIGHT) {
                    setParamsLeft();
                    wm.updateViewLayout(overlayLauncher, params);
                } else if (((moveX - originX) > 80) && position == POSITION_LEFT) {
                    setParamsRight();
                    wm.updateViewLayout(overlayLauncher, params);
                } else {
                    if (isWatchface())
                        startIntent();
                }
                originX = originY = moveX = moveY = 0;
                break;

            case MotionEvent.ACTION_OUTSIDE: //Touches outside the layer are detected
                moveX = (int) event.getX();  // but without coordinates when there isn't any other
                moveY = (int) event.getY();  // open activity from this app
                Logger.debug("OverlayLauncher onTouch ACTION_OUTSIDE x: {} y: {}", moveX, moveY);
                originX = originY = moveX = moveY = 0;
                break;
        }

        return false;
    }

    private void setParamsRight() {
        if (DeviceUtil.isVerge())
            params.x = 0;
        else
            params.x = 0;
        position = POSITION_RIGHT;
    }
    private void setParamsLeft() {
        if (DeviceUtil.isVerge())
            params.x = 190;
        else
            params.x = 170;
        position = POSITION_LEFT;
    }

    public static boolean isWatchface() {
        final boolean isWhatFace = SystemProperties.getBoolean("prop.launcher.at_watchface", false);
        Logger.debug("OverlayLauncher isWatchFace: {}", isWhatFace);
        return isWhatFace;
    }

    private void startIntent(){
        Intent intent = new Intent(context, LauncherWearGridActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.NOTIFICATIONS_FROM_WATCHFACE);
        startActivity(intent);
    }

    private void vibrate(){
        Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibe != null) {
            vibe.vibrate(vibration);
        }
    }

}
