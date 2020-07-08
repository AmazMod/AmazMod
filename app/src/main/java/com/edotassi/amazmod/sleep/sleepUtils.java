package com.edotassi.amazmod.sleep;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import amazmod.com.transport.data.SleepData;
import static amazmod.com.transport.data.SleepData.actions;

public class sleepUtils {

    public static final String PACKAGE = "com.urbandroid.sleep";

    public static void broadcast(Context context, SleepData sleepData){
        Intent intent = new Intent();
        switch(sleepData.getAction()){
            case actions.ACTION_DATA_UPDATE:
                intent.putExtra("MAX_DATA", sleepData.getMax_data());
                intent.putExtra("MAX_RAW_DATA", sleepData.getMax_raw_data());
                intent.setComponent(new ComponentName(PACKAGE, "com.urbandroid.sleep.watch.DATA_UPDATE"));
            case actions.ACTION_HRDATA_UPDATE:
                intent.putExtra("DATA", sleepData.getHrdata());
                intent.setComponent(new ComponentName(PACKAGE, "com.urbandroid.sleep.watch.HR_DATA_UPDATE"));
                break;
            case actions.ACTION_SNOOZE_FROM_WATCH:
                intent.setComponent(new ComponentName(PACKAGE, "com.urbandroid.sleep.watch.SNOOZE_FROM_WATCH"));
                break;
            case actions.ACTION_DISMISS_FROM_WATCH:
                intent.setComponent(new ComponentName(PACKAGE, "com.urbandroid.sleep.watch.DISMISS_FROM_WATCH"));
                break;
            default:
                break;
        }
        context.sendBroadcast(intent);
    }
}