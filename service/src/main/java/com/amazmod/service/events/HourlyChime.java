package com.amazmod.service.events;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.amazmod.service.receiver.AlarmReceiver;

import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

public class HourlyChime {

    public static void setHourlyChime(Context context) {
        // Automatic enable/disable
        boolean status = !HourlyChime.checkIfSet();
        setHourlyChime(context, status);
    }

    public static void setHourlyChime(Context context, Boolean status) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.REQUEST_CODE, AlarmReceiver.CHIME_CODE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, AlarmReceiver.CHIME_CODE, intent, 0);

        if (alarmIntent == null) {
            Logger.error("setHourlyChime null intent!");
        } else if (alarmMgr == null) {
            Logger.error("setHourlyChime null alarmMgr!");
        } else if (status) {
            // Enable
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.MINUTE, 0);//12
            calendar.set(Calendar.SECOND, 0);//13
            calendar.add(Calendar.HOUR_OF_DAY, 1);//11
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), alarmIntent);
            Logger.info(String.format("setHourlyChime: %02d:%02d:%02d", new Object[]{Integer.valueOf(calendar.get(11)), Integer.valueOf(calendar.get(12)), Integer.valueOf(calendar.get(13))}));
        } else {
            // Disable
            alarmMgr.cancel(alarmIntent);
            Logger.info("setHourlyChime canceled");
        }
    }

    public static boolean checkIfSet(){
        try {
            Process process = Runtime.getRuntime().exec("adb shell dumpsys alarm");
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;

            while ((line = in.readLine()) != null) {
                if (line.contains("com.amazmod.service/.receiver.AlarmReceiver")) {
                    Logger.info("Alarm is set");
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger.info("Alarm is not set");
        return false;
    }
}
