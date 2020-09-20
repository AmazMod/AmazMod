package com.edotassi.amazmod.sleep;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import org.tinylog.Logger;

import java.util.Arrays;

import amazmod.com.transport.data.SleepData;
import static amazmod.com.transport.data.SleepData.actions;

public class sleepUtils {

    public static final String PACKAGE = "com.urbandroid.sleep";

    public static void broadcast(Context context, SleepData sleepData){
        Intent intent;
        switch(sleepData.getAction()){
            case actions.ACTION_DATA_UPDATE:
                intent = new Intent("com.urbandroid.sleep.watch.DATA_UPDATE");
                intent.putExtra("MAX_DATA", sleepData.getMax_data());
                intent.putExtra("MAX_RAW_DATA", sleepData.getMax_raw_data());
                sendIntent(intent, context);
                Logger.debug("sleep: Received accelerometer update. MAX_DATA: \"" + Arrays.toString(sleepData.getMax_data()) + "\", MAX_RAW_DATA: \"" + Arrays.toString(sleepData.getMax_raw_data()) + "\"");
                break;
            case actions.ACTION_HRDATA_UPDATE:
                intent = new Intent("com.urbandroid.sleep.watch.HR_DATA_UPDATE");
                intent.putExtra("DATA", sleepData.getHrdata());
                sendIntent(intent, context);
                Logger.debug("sleep: Received hr update: \"" + Arrays.toString(sleepData.getHrdata()) + "\"");
                break;
            case actions.ACTION_SNOOZE_FROM_WATCH:
                sendIntent("com.urbandroid.sleep.watch.SNOOZE_FROM_WATCH", context);
                Logger.debug("sleep: Received snooze");
                break;
            case actions.ACTION_DISMISS_FROM_WATCH:
                sendIntent("com.urbandroid.sleep.watch.DISMISS_FROM_WATCH", context);
                Logger.debug("sleep: Received dismiss");
                break;
            default:
                break;
        }
    }

    public static void sendIntent(String action, Context context){
        sendIntent(new Intent(action), context);
    }

    public static void sendIntent(Intent intent, Context context){
        intent.setPackage(PACKAGE);
        context.sendBroadcast(intent);
    }
}
