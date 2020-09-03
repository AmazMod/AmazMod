package com.amazmod.service.sleep;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import com.amazmod.service.R;
import com.amazmod.service.notifications.NotificationService;
import com.amazmod.service.sleep.sensor.sensorsStore;

import org.tinylog.Logger;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;

import amazmod.com.transport.Constants;
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
        notificationData.setId(sleepConstants.NOTIFICATION_ID);
        notificationData.setKey(sleepConstants.NOTIFICATION_KEY);
        notificationData.setTitle(title);
        notificationData.setText(text);
        notificationData.setTime(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Calendar.getInstance().getTime()));
        notificationData.setForceCustom(false);
        notificationData.setHideButtons(true);
        notificationData.setHideReplies(true);

        // Get and set icon
        Drawable drawable = context.getResources().getDrawable(R.drawable.ic_sleepasandroid);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        if (bitmap.getWidth() > 48) //This is not necessary but added in case that we edit icon
            bitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] intArray = new int[width * height];
        notificationData.setIcon(intArray);
        notificationData.setIconWidth(width);
        notificationData.setIconHeight(height);

        notificationService.post(notificationData);
    }

    public static void startHint(int repeat, Context context){
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = new long[repeat * 2 + 1];//new long[]{0, 50, 1000};
        pattern[0] = 0;
        if(repeat > 0) {
            for (int i = 1; i < repeat * 2 + 1; i++)
                pattern[i] = i % 2 == 0 ? 50 : 1000;
        }
        v.vibrate(pattern, -1);
    }

    public static void setSensorsState(boolean enabled, Context context){
        boolean hrEnabled = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.PREF_ENABLE_SAA_HEARTRATE, false);
        if(enabled){
            sensorsStore.getAccelerometer().registerListener(context);
            if(hrEnabled) sensorsStore.getHrSensor().registerListener(context);
        } else {
            sensorsStore.getAccelerometer().unregisterListener();
            if(hrEnabled) sensorsStore.getHrSensor().unregisterListener(context);
        }
    }
}
