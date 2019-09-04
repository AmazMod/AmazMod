package com.amazmod.service.events;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.amazmod.service.receiver.AlarmReceiver;

import org.tinylog.Logger;

import java.util.Calendar;

public class HourlyChime {

    public static void setHourlyChime(Context context, boolean enable) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.REQUEST_CODE, AlarmReceiver.CHIME_CODE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, AlarmReceiver.CHIME_CODE, intent, 0);
        if (alarmIntent == null) {
            Logger.error("setHourlyChime null intent!");
        } else if (alarmMgr == null) {
            Logger.error("setHourlyChime null alarmMgr!");
        } else if (enable) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(12, 0);
            calendar.set(13, 0);
            calendar.add(11, 1);
            alarmMgr.setExact(0, calendar.getTimeInMillis(), alarmIntent);
            Logger.info(String.format("setHourlyChime: %02d:%02d:%02d", new Object[]{Integer.valueOf(calendar.get(11)), Integer.valueOf(calendar.get(12)), Integer.valueOf(calendar.get(13))}));
        } else {
            alarmMgr.cancel(alarmIntent);
            Logger.info("setHourlyChime canceled");
        }
    }
}
