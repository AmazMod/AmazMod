package com.amazmod.service.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.HourlyChime;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.SystemProperties;

import org.tinylog.Logger;

import java.io.IOException;

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
                Uri sound;
                sound = Uri.parse(Constants.RES_PREFIX + R.raw.hourly_chime);
                try {
                    mp.setDataSource(context, sound);
                    mp.prepare();
                    Logger.debug("Play hourly chime sound");
                } catch (IOException e) {
                    Logger.error("Can't play hourly chime sound");
                }
                mp.start();
            }
            final Vibrator mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
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
        }
        HourlyChime.setHourlyChime(context, true);

    }
}