package com.amazmod.service.sleep;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;

import com.amazmod.service.notifications.NotificationService;
import com.amazmod.service.sleep.sensor.sensorsStore;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;

import amazmod.com.transport.data.NotificationData;

public class sleepUtils {
    public static float[] linkedToArray(LinkedList<Float> list){
        Object[] objectArray = list.toArray();
        int length = objectArray.length;;
        float[] finalArray = new float[length];
        for(int i =0; i < length; i++) {
            finalArray[i] = (float) objectArray[i];
        }
        return finalArray;
    }

    public static void startTracking(Context context){
        sleepStore.setTracking(true, context);
    }

    public static void stopTracking(Context context){
        sleepStore.setTracking(false, context);
    }

    public static void postNotification(String title, String text, Context context){
        NotificationService notificationService = new NotificationService(context);
        NotificationData notificationData = new NotificationData();
        notificationData.setId(1834);
        notificationData.setKey("amazmod|SAA");
        notificationData.setTitle(title);
        notificationData.setText(text);
        notificationData.setTime(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Calendar.getInstance().getTime()));
        notificationData.setForceCustom(false);
        notificationData.setHideButtons(true);
        notificationData.setHideReplies(true);
        //notificationData.setIcon(); TODO Set sleep as android icon

        notificationService.post(notificationData);
    }

    public static void startHint(int repeat, Context context){
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = new long[]{0, 50, 1000};
        int cancelDelay = 0;
        for(long x : pattern){
            cancelDelay += x;
        }
        cancelDelay *= repeat;
        if(repeat > 1) {
            v.vibrate(pattern, 0);
            new Handler().postDelayed(v::cancel, cancelDelay);
        } else
            v.vibrate(pattern, -1); //If repeat == 0 or -1 don't repeat it
    }
}
