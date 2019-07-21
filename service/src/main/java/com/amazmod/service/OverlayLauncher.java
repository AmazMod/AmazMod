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
    private static int overlayColor;

    public static final char POSITION_RIGHT = 'R';
    public static final char POSITION_LEFT = 'L';


    private static int OVERLAY_COLOR_AMOLED = 0x40fe4444;
    private static int OVERLAY_COLOR_TRANSFLECTIVE = 0x80fe4444;
    private static int OVERLAY_COLOR_TRANSPARENT = 0x00fe4444;

    private static long OVERLAY_DELAY = 2000;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint({"RtlHardcoded", "ClickableViewAccessibility"})
    @Override
    public void onCreate() {
        super.onCreate();

        Logger.debug("OverlayLauncher onCreate");

        boolean atWatchface = isWatchface();

        context = getApplicationContext();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        initParams();

        overlayLauncher = new View(this);
        if (atWatchface && (!MainService.isNotification())) {
            overlayLauncher.setBackgroundColor(overlayColor);
        }else{
            overlayLauncher.setBackgroundColor(OVERLAY_COLOR_TRANSPARENT);
            MainService.setIsNotification(false);
        }
        overlayLauncher.setOnTouchListener(this);


        wm.addView(overlayLauncher, params);

        if (atWatchface) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    if (overlayLauncher != null)
                        overlayLauncher.setBackgroundColor(OVERLAY_COLOR_TRANSPARENT);
                }
            }, OVERLAY_DELAY);
        }
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

    private void initParams(){

        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        params.x = 0;
        params.y = 0;
        if (SystemProperties.isVerge()) {
            params.height = 30;
            params.width = 170;
            vibration = 30;
            overlayColor = OVERLAY_COLOR_AMOLED;
        }
        else {
            params.height = 15;
            params.width = 150;
            vibration = 10;
            overlayColor = OVERLAY_COLOR_TRANSFLECTIVE;
        }

        if (POSITION_LEFT == MainService.getOverlayLauncherPosition())
            setParamsLeft();
        else
            position = POSITION_RIGHT;
    }

    private void setParamsRight() {
        if (SystemProperties.isVerge())
            params.x = 0;
        else
            params.x = 0;
        position = POSITION_RIGHT;
        MainService.setOverlayLauncherPosition(position);
    }
    private void setParamsLeft() {
        if (SystemProperties.isVerge())
            params.x = 190;
        else
            params.x = 170;
        position = POSITION_LEFT;
        MainService.setOverlayLauncherPosition(position);
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
