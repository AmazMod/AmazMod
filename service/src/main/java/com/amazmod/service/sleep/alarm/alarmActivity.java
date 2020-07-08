package com.amazmod.service.sleep.alarm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    private TextView time;
    private Button snooze, dismiss;

    private Handler timeHandler;
    private Vibrator vibrator;

    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(INTENT_CLOSE)){
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        init();
        long[] VIBRATION_PATTERN = new long[]{
                getIntent().getIntExtra("DELAY", 0), //Get delay from saa's extra
                200, 100, 300, 200, 400, 300, 500, 400, 600, 500, 700, 600, 1000, 500, 1000, 500, 1000, 500
                //TODO Make a good vibration pattern or extract from huami patterns, this one is random
        };
        //Create broadcast receiver to finish activity when received signal
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(INTENT_CLOSE);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
        setupTime();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATION_PATTERN, 200 /*It will get cancelled after closing activity*/);
    }

    private void setupTime(){
        Looper.prepareMainLooper();
        timeHandler = new Handler(Looper.getMainLooper());
        timeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                time.setText(new SimpleDateFormat("HH:mm", Locale.US).format(new Date()));
                timeHandler.postDelayed(this, 1000);
            }
        }, 10);
    }

    private void init(){
        time = findViewById(R.id.alarm_time);
        snooze = findViewById(R.id.alarm_snooze);
        dismiss = findViewById(R.id.alarm_dismiss);
        snooze.setOnClickListener(view -> {
            SleepData sleepData = new SleepData();
            sleepData.setAction(actions.ACTION_SNOOZE_FROM_WATCH);
            sleepService.send(sleepData.toDataBundle(new DataBundle()));
            vibrator.cancel();
            finish();
        });
        dismiss.setOnClickListener(view -> {
            SleepData sleepData = new SleepData();
            sleepData.setAction(actions.ACTION_DISMISS_FROM_WATCH);
            sleepService.send(sleepData.toDataBundle(new DataBundle()));
            vibrator.cancel();
            finish();
        });
    }

    public void onDestroy() {
        super.onDestroy();
        if (timeHandler != null)
            timeHandler.removeCallbacksAndMessages(null);
        timeHandler = null;
        vibrator.cancel();
    }
}