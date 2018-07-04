package com.edotassi.amazmodcompanionservice.notifications;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.edotassi.amazmodcompanionservice.Constants;
import com.edotassi.amazmodcompanionservice.R;
import com.edotassi.amazmodcompanionservice.settings.SettingsManager;
import com.edotassi.amazmodcompanionservice.ui.NotificationActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

/**
 * Created by edoardotassinari on 20/04/18.
 */

public class NotificationService {

    private Context context;
//    private Vibrator vibrator;
    private NotificationManager notificationManager;

    public NotificationService(Context context) {
        this.context = context;
//        vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_ACTION_REPLY);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int notificationId = intent.getIntExtra(Constants.EXTRA_NOTIFICATION_ID, -1);
                notificationManager.cancel(notificationId);
            }
        }, filter);
    }

    public void post(NotificationData notificationSpec) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean enableCustomUI = sharedPreferences.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI,
                Constants.PREF_DEFAULT_NOTIFICATIONS_ENABLE_CUSTOM_UI);

        boolean disableNotificationReplies = sharedPreferences.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);

        if (enableCustomUI) {
            postWithCustomUI(notificationSpec);
        } else {
            postWithStandardUI(notificationSpec, disableNotificationReplies);
        }
    }


    private void postWithStandardUI(NotificationData notificationData, boolean disableNotificationReplies) {

        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 , intent,PendingIntent.FLAG_ONE_SHOT);

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_test);
        contentView.setTextViewText(R.id.notification_title, notificationData.getTitle());
        contentView.setTextViewText(R.id.notification_text, notificationData.getText());

        int[] iconData = notificationData.getIcon();
        int iconWidth = notificationData.getIconWidth();
        int iconHeight = notificationData.getIconHeight();
        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);
        Log.d("NotifIcon", "1");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "")
                .setLargeIcon(bitmap)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setStyle(new NotificationCompat.InboxStyle())
                .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(notificationData.getText()))
                .setContent(contentView)
                .setContentIntent(pendingIntent)
                .setContentText(notificationData.getText())
                .setContentTitle(notificationData.getTitle())
                .setVibrate(new long[]{notificationData.getVibration()});

        if (!disableNotificationReplies) {

            List<Reply> repliesList = loadReplies();

            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
            for (Reply reply : repliesList) {
//                Intent intent = new Intent();
                intent.setPackage(context.getPackageName());
                intent.setAction(Constants.INTENT_ACTION_REPLY);
                intent.putExtra(Constants.EXTRA_REPLY, reply.getValue());
                intent.putExtra(Constants.EXTRA_NOTIFICATION_KEY, notificationData.getKey());
                intent.putExtra(Constants.EXTRA_NOTIFICATION_ID, notificationData.getId());
                PendingIntent replyIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT);

                NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, reply.getValue(), replyIntent).build();
                wearableExtender.addAction(replyAction);
            }

            builder.extend(wearableExtender);
        }

        Notification notification = builder.build();

//        ApplicationInfo applicationInfo = notification.extras.getParcelable("android.rebuild.applicationInfo");

        notificationManager.notify(notificationData.getId(), notification);

        //Log.d("Notifiche", "postWithStandardUI: " + notificationSpec.getKey() + " " + notificationSpec.getId() + " " + notificationSpec.getPkg());

        //  Utils.BitmapExtender bitmapExtender = Utils.retrieveAppIcon(context, notificationSpec.getPkg());
        // if (bitmapExtender != null) {
      /*  int[] iconData = notificationSpec.getIcon();
        int iconWidth = notificationSpec.getIconWidth();
        int iconHeight = notificationSpec.getIconHeight();
        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);
        builder.setLargeIcon(bitmap);*/
        //builder.setSmallIcon(Icon.createWithBitmap(bitmapExtender.bitmap));
        //data.mCanRecycleBitmap = bitmapExtender.canRecycle;
        //}
        /*
        String replyLabel = "Replay";
        String[] replyChoices = new String[]{"ok", "no", "forse domani"};

        RemoteInput remoteInput = new RemoteInput.Builder(KEY_QUICK_REPLY_TEXT)
                .setLabel(replyLabel)
                .setChoices(replyChoices)
                .build();
        */



        /*
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if(km.isKeyguardLocked()) {
            PowerManager pm = (PowerManager) context.ge tSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
            wakeLock.acquire();
            wakeLock.release();
        }
        */
    }

    private void postWithCustomUI(NotificationData notificationSpec) {
        Intent intent = new Intent(context, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtras(notificationSpec.toBundle());

        context.startActivity(intent);
    }

    private List<Reply> loadReplies() {
        SettingsManager settingsManager = new SettingsManager(context);
        final String replies = settingsManager.getString(Constants.PREF_NOTIFICATION_CUSTOM_REPLIES, "[]");

        try {
            Type listType = new TypeToken<List<Reply>>() {
            }.getType();
            return new Gson().fromJson(replies, listType);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

/*
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public void add(TransportDataItem transportDataItem) {
        DataBundle dataBundle = transportDataItem.getData();
        StatusBarNotificationData statusBarNotificationData = dataBundle.getParcelable("data");

        if (statusBarNotificationData == null) {
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "statsBarNotificationData == null");
        } else {
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "statusBarNotificationData:");
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "pkg: " + statusBarNotificationData.pkg);
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "id: " + statusBarNotificationData.id);
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "groupKey: " + statusBarNotificationData.groupKey);
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "key: " + statusBarNotificationData.key);
            Log.d(Constants.TAG_NOTIFICATION_MANAGER, "tag: " + statusBarNotificationData.tag);
        }


        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_test);
        contentView.setTextViewText(R.id.notification_title, statusBarNotificationData.notification.title);
        contentView.setTextViewText(R.id.notification_text, statusBarNotificationData.notification.text);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "")
                .setLargeIcon(statusBarNotificationData.notification.smallIcon)
                .setSmallIcon(xiaofei.library.hermes.R.drawable.abc_btn_check_material)
                .setContent(contentView)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.InboxStyle())
                .setContentText(statusBarNotificationData.notification.text)
                .setContentTitle(statusBarNotificationData.notification.title)
                .setVibrate(new long[]{500})
                .addAction(android.R.drawable.ic_dialog_info, "Demo", pendingIntent);

        vibrator.vibrate(500);

        notificationManager.notify(statusBarNotificationData.id, mBuilder.build());
        */



        /*
        Bitmap background = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.lau_notify_icon_upgrade_bg)).getBitmap();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "");

        builder.setContentTitle(title).setContentText(content);

        if (statusBarNotificationData.notification.smallIcon != null) {
            builder.setLargeIcon(statusBarNotificationData.notification.smallIcon);
        }

        builder.setSmallIcon(android.R.drawable.ic_dialog_email);


        Intent displayIntent = new Intent();
        displayIntent.setAction("com.saglia.notify.culo");
        displayIntent.setPackage("com.saglia.notify");
        displayIntent.putExtra("saglia_app", "Notifica");
        builder1.setContentIntent(PendingIntent.getBroadcast(context, 1, displayIntent, PendingIntent.FLAG_ONE_SHOT));

        NotificationData.ActionData[] actionDataList = statusBarNotificationData.notification.wearableExtras.actions;

        Intent intent = new Intent(context, NotificationsReceiver.class);
        intent.setPackage(context.getPackageName());
        intent.setAction("com.amazmod.intent.notification.reply");
        intent.putExtra("reply", "hello world!");
        intent.putExtra("id", statusBarNotificationData.id);
        PendingIntent installPendingIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Action installAction = new NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, "Reply", installPendingIntent).build();
        builder.extend(new NotificationCompat.WearableExtender().addAction(installAction));

        Notification notification1 = builder.build();
        notification1.flags = 32;

        notificationManager.notify(statusBarNotificationData.id, notification1);

    }
    */
}
