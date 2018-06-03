package com.edotassi.amazmodcompanionservice.notifications;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;

import com.edotassi.amazmodcompanionservice.ui.NotificationActivity;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

/**
 * Created by edoardotassinari on 20/04/18.
 */

public class NotificationService {

    private Context context;
    private Vibrator vibrator;
    private NotificationManager notificationManager;

    public NotificationService(Context context) {
        this.context = context;
        vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

    public void post(NotificationSpec notificationSpec) {
        postWithCustomUI(notificationSpec);
    }

    private void postWithCustomUI(NotificationSpec notificationSpec) {
        Intent intent = new Intent(context, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtras(NotificationSpecFactory.toBundle(notificationSpec));

        context.startActivity(intent);
    }

    private void postWithStandardUI(NotificationSpec notificationSpec) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.InboxStyle())
                .setContentText(notificationSpec.getText())
                .setContentTitle(notificationSpec.getTitle())
                .setVibrate(new long[]{notificationSpec.getVibration()});

        Notification notification = builder.build();
        notificationManager.notify(notificationSpec.getId(), notification);
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

        /*
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
