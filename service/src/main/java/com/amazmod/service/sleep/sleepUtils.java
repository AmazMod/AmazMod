package com.amazmod.service.sleep;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Vibrator;

import com.amazmod.service.R;
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
