package com.amazmod.service.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Vibrator;

import com.amazmod.service.events.HourlyChime;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.SystemProperties;

import org.tinylog.Logger;

public class AlarmReceiver extends BroadcastReceiver {
    public static final int CHIME_CODE = 1111;
    public static final String REQUEST_CODE = "code";

    public void onReceive(Context context, Intent intent) {
        int code = intent.getIntExtra(REQUEST_CODE, 0);
        StringBuilder sb = new StringBuilder();
        sb.append("AlarmReceiver code: ");
        sb.append(code);
        Logger.debug(sb.toString());
        if (code == 1111) {
            chime(context);
        } else {
            Logger.error("AlarmReceiver unknown request code!");
        }
    }

    private void chime(Context context) {
        if (!DeviceUtil.isDNDActive(context)) {
            if (SystemProperties.isVerge()) {
                MediaPlayer mp = new MediaPlayer();
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM).build());
                Logger.debug("Set to use alarm channel");
                mp.start();
            }
            final Vibrator mVibrator = (Vibrator) context.getSystemService("vibrator");
            if (mVibrator != null) {
                mVibrator.vibrate(200);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        mVibrator.vibrate(200);
                    }
                }, 300);
            } else {
                Logger.error("AlarmReceiver null vibrator!");
            }
            HourlyChime.setHourlyChime(context, true);
        }
    }
}