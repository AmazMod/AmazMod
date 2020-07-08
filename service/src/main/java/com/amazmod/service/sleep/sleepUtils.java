package com.amazmod.service.sleep;

import android.content.Context;
import android.os.Vibrator;

import com.amazmod.service.notifications.NotificationService;
import com.amazmod.service.sleep.sensor.sensorsStore;

import java.util.LinkedList;

import amazmod.com.transport.data.NotificationData;

public class sleepUtils {
    public static float[] linkedToArray(LinkedList<Float> list){
        Object[] objectArray = list.toArray();
        int length = objectArray.length;;
        float[] finalArray = new float[length];
        for(int i =0; i < length; i++) {
            finalArray[i] = Float.parseFloat((String) objectArray[i]);
        }
        return finalArray;
    }

    public static void startTracking(Context context){
        sensorsStore.getAccelerometer().registerListener(context);
        sensorsStore.getHrSensor().registerListener(context);
        sleepStore.setTracking(true);
    }

    public static void stopTracking(Context context){
        sensorsStore.getAccelerometer().unregisterListener(context);
        sensorsStore.getHrSensor().unregisterListener(context);
        sleepStore.setTracking(false);
    }

    public static void postNotification(String title, String text, Context context){
        NotificationService notificationService = new NotificationService(context);
        NotificationData notificationData = new NotificationData();
        notificationData.setTitle(title);
        notificationData.setText(text);
        notificationData.setHideButtons(true);
        notificationData.setForceCustom(false);
        notificationData.setHideReplies(true);
        //notificationData.setIcon(); TODO Set sleep as android icon
        notificationService.post(notificationData);
    }

    public static void startHint(int repeat, Context context){
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = new long[]{0, 10, 10}; //TODO Create a better pattern for this hint
        v.vibrate(pattern, repeat);
    }
}
