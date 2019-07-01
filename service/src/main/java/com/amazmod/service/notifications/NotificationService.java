package com.amazmod.service.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.NotificationWearActivity;
import com.amazmod.service.util.DeviceUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.tinylog.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by edoardotassinari on 20/04/18.
 */

public class NotificationService {

    private Context context;
    //    private Vibrator vibrator;
    private NotificationManager notificationManager;
    private SettingsManager settingsManager;

    public NotificationService(Context context) {
        this.context = context;
//        vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        settingsManager = new SettingsManager(context);

        //NotificationStore notificationStore = new NotificationStore();

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

    public void post(final NotificationData notificationSpec) {

        if (!DeviceUtil.isDNDActive(context)) {

            boolean enableCustomUI = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI,
                    Constants.PREF_DEFAULT_NOTIFICATIONS_ENABLE_CUSTOM_UI);

            boolean disableNotificationReplies = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                    Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);

            boolean forceCustom = notificationSpec.getForceCustom();
            boolean hideReplies = notificationSpec.getHideReplies();

            if (disableNotificationReplies || hideReplies) {
                disableNotificationReplies = true;
            }

            final String key = notificationSpec.getKey();
            final String notificationStoreKey = key + "|" + String.valueOf(System.currentTimeMillis());

            Logger.debug("NotificationService notificationSpec.getKey(): " + key);
            //Handles test notifications
            if (key.contains("amazmod|test|99")) {
                if (notificationSpec.getText().equals("Test Notification")) {
                    if (forceCustom) {
                        Logger.debug("NotificationService1 notificationSpec.getKey(): " + key);
                        NotificationStore.addCustomNotification(notificationStoreKey, notificationSpec);
                        postWithCustomUI(notificationStoreKey);
                    } else {
                        Logger.debug("NotificationService2 notificationSpec.getKey(): " + key);
                        postWithStandardUI(notificationSpec, hideReplies);
                    }
                } else if (key.contains("amazmod|test|9979")) {
                    Logger.debug("NotificationService3 notificationSpec.getKey(): " + key);
                    postWithStandardUI(notificationSpec, hideReplies);
                }
                //Handles normal notifications
            } else {
                Logger.debug("NotificationService6 notificationSpec.getKey(): " + key);
                if (enableCustomUI || forceCustom) {
                    if (!forceCustom)
                        NotificationStore.addCustomNotification(notificationStoreKey , notificationSpec);
                    postWithCustomUI(notificationStoreKey);
                } else {
                    postWithStandardUI(notificationSpec, disableNotificationReplies);
                }
            }
        }
    }


    private void postWithStandardUI(NotificationData notificationData, boolean disableNotificationReplies) {
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        /*
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_test);
        contentView.setTextViewText(R.id.notification_title, notificationData.getTitle());
        contentView.setTextViewText(R.id.notification_text, notificationData.getText());

        contentView.setImageViewBitmap(R.id.notification_icon, bitmap);
        contentView.setBitmap(R.id.notification_icon, "setImageBitmap", bitmap);
        */

        Logger.debug("NotificationService postWithStandardUI notificationData: " +
                notificationData.toString() + " / disableNotificationReplies: " + disableNotificationReplies);

        int[] iconData = notificationData.getIcon();
        int iconWidth = notificationData.getIconWidth();
        int iconHeight = notificationData.getIconHeight();
        Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

        Bundle bundle = new Bundle();
        bundle.putParcelable(Notification.EXTRA_LARGE_ICON_BIG, bitmap);
        bundle.putParcelable(Notification.EXTRA_LARGE_ICON, bitmap);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setContentText(notificationData.getText())
                .setContentTitle(notificationData.getTitle())
                .setExtras(bundle)
                .setVibrate(new long[]{notificationData.getVibration()});

        if (notificationData.getKey().contains("amazmod|test|9979")) {
            if (notificationData.getText().equals(context.getResources().getString(R.string.phone_disconnected))) {
                builder.setSmallIcon(R.drawable.ic_outline_phonelink_erase_inverted);
            } else if (notificationData.getText().equals(context.getResources().getString(R.string.phone_connected))) {
                builder.setSmallIcon(R.drawable.ic_outline_phonelink_ring_inverted);
            }
        }

        if (!disableNotificationReplies) {

            List<Reply> repliesList = loadReplies();

            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
            for (Reply reply : repliesList) {
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

        /*
        try {
            int[] iconData = notificationData.getIcon();
            int iconWidth = notificationData.getIconWidth();
            int iconHeight = notificationData.getIconHeight();
            Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

            Icon icon = Icon.CREATOR.createFromParcel(null);
            //Icon ic = new Icon(Icon.T);
            FieldUtils.writeField(notification, "mSmallIcon", null, true);
        } catch (Exception ex) {
            Log.d(Constants.TAG, "write field failed");
        }
        */

        notificationManager.notify(notificationData.getId(), notification);

        //Log.d("Notifiche", "postWithStandardUI: " + notificationSpec.getKey() + " " + notificationSpec.getId() + " " + notificationSpec.getPkg());

        //  Utils.BitmapExtender bitmapExtender = Utils.retrieveAppIcon(context, notificationSpec.getPkg());
        // if (bitmapExtender != null) {
        /* int[] iconData = notificationSpec.getIcon();
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
        // Keyguard does not work correctly, use "DeviceUtil.isDeviceLocked(context)"
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

    private void postWithCustomUI(String key) {

        Logger.debug("NotificationService postWithCustomUI: " + NotificationStore.getCustomNotificationCount());

        NotificationStore.setNotificationCount(context);

        Intent intent = new Intent(context, NotificationWearActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(NotificationWearActivity.KEY, key);
        intent.putExtra(NotificationWearActivity.MODE, NotificationWearActivity.MODE_ADD);

        context.startActivity(intent);

    }

    private List<Reply> loadReplies() {
        //SettingsManager settingsManager = new SettingsManager(context);
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

        Intent intent = new Intent(context, NotificationReplyReceiver.class);
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
