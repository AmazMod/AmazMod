package com.amazmod.service.sleep.alarm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amazmod.service.R;
import com.amazmod.service.sleep.sleepService;
import com.huami.watch.transport.DataBundle;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import amazmod.com.transport.data.SleepData;

import static amazmod.com.transport.data.SleepData.actions;

public class alarmActivity extends Activity {

    public static final String INTENT_CLOSE = "com.amazmod.alarm.action.close";

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build();
    private static final long[] VIBRATION_PATTERN = new long[]{0, 200, 100, 200, 100, 200, 100, 0, 400};

    private TextView time;
    private Button snooze, dismiss;

    private Handler timeHandler;
    private Vibrator vibrator;

    PowerManager.WakeLock wakeLock = null;

    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_CLOSE))
                stop();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        init();
        setWakeLock();
        //Create broadcast receiver to finish activity when received signal
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(INTENT_CLOSE);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
        setupTime();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        timeHandler.postDelayed(() -> vibrator.vibrate(VIBRATION_PATTERN, 0, VIBRATION_ATTRIBUTES),
                getIntent().getIntExtra("DELAY", 0));
    }

    private void setupTime() {
        timeHandler = new Handler(Looper.getMainLooper());
        timeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                time.setText(new SimpleDateFormat("HH:mm", Locale.US).format(new Date()));
                timeHandler.postDelayed(this, 1000);
            }
        }, 10);
    }

    private void init() {
        time = findViewById(R.id.alarm_time);
        snooze = findViewById(R.id.alarm_snooze);
        dismiss = findViewById(R.id.alarm_dismiss);
        snooze.setOnClickListener(view -> stop(actions.ACTION_SNOOZE_FROM_WATCH));
        dismiss.setOnClickListener(view -> stop(actions.ACTION_DISMISS_FROM_WATCH));
    }

    public void onDestroy() {
        super.onDestroy();
        vibrator.cancel();
        if (timeHandler != null)
            timeHandler.removeCallbacksAndMessages(null);
        timeHandler = null;
        releaseWakeLock();
    }

    private void stop(int action) {
        SleepData sleepData = new SleepData();
        sleepData.setAction(action);
        sleepService.send(sleepData.toDataBundle(new DataBundle()));
        stop();
    }

    private void stop() {
        vibrator.cancel();
        finish();
    }

    private void setWakeLock() {
        //Wake the screen
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "AmazMod:sleepasandroid_alarm");

        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }
}