package com.edotassi.amazmod.sleep;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.edotassi.amazmod.AmazModApplication;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import java.util.Objects;

import amazmod.com.transport.data.SleepData;
import static amazmod.com.transport.data.SleepData.actions;

public class broadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SleepData sleepData = new SleepData();
        sleepData.setAction(-1);
        switch(Objects.requireNonNull(intent.getAction())){
            case "com.urbandroid.sleep.watch.CHECK_CONNECTED":
                if(AmazModApplication.isWatchConnected() && BluetoothAdapter.getDefaultAdapter().isEnabled()){
                    Intent newIntent = new Intent();
                    newIntent.setComponent(new ComponentName(sleepUtils.PACKAGE, "com.urbandroid.sleep.watch.CONFIRM_CONNECTED"));
                    context.sendBroadcast(newIntent);
                }
                break;
            case "com.urbandroid.sleep.watch.START_TRACKING":
                sleepData.setAction(actions.ACTION_START_TRACKING);
                break;
            case "com.urbandroid.sleep.watch.STOP_TRACKING":
                sleepData.setAction(actions.ACTION_STOP_TRACKING);
                break;
            case "com.urbandroid.sleep.watch.SET_PAUSE":
                //sleepData.setAction(actions.ACTION_SET_PAUSE); Not added yet
                break;
            case "com.urbandroid.sleep.watch.SET_SUSPENDED":
                sleepData.setAction(actions.ACTION_SET_SUSPENDED);
                sleepData.setSuspended(intent.getBooleanExtra("SUSPENDED", false));
                break;
            case "com.urbandroid.sleep.watch.SET_BATCH_SIZE":
                sleepData.setAction(actions.ACTION_SET_BATCH_SIZE);
                sleepData.setBatchsize(intent.getLongExtra("SIZE", 120));
                break;
            case "com.urbandroid.sleep.watch.START_ALARM":
                sleepData.setAction(actions.ACTION_START_ALARM);
                sleepData.setDelay(intent.getIntExtra("DELAY", 10000));
                break;
            case "com.urbandroid.sleep.watch.STOP_ALARM":
                sleepData.setAction(actions.ACTION_STOP_ALARM);
                sleepData.setHour(intent.getIntExtra("HOUR", 0));
                sleepData.setMinute(intent.getIntExtra("MINUTE", 0));
                sleepData.setTimestamp(intent.getLongExtra("TIMESTAMP", 0));
                break;
            case "com.urbandroid.sleep.watch.SHOW_NOTIFICATION":
                sleepData.setAction(actions.ACTION_SHOW_NOTIFICATION);
                sleepData.setTitle(intent.getStringExtra("TITLE"));
                sleepData.setText(intent.getStringExtra("TEXT"));
                break;
            case "com.urbandroid.sleep.watch.HINT":
                sleepData.setAction(actions.ACTION_HINT);
                sleepData.setRepeat(intent.getIntExtra("REPEAT", -1));
                break;
            default:
                break;
        }
        if(sleepData.getAction() != -1) {
            sleepListener.send(sleepData.toDataBundle(new DataBundle()));
            Logger.debug("sleep: Detected intent \"" + intent.getAction() + "\", sending action " + sleepData.getAction());
        } else {
            Logger.debug("sleep: broadcastReceiver: Received unknown intent: " + intent.getAction());
        }
    }
}
