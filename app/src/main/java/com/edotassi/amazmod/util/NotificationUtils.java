package com.edotassi.amazmod.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.notification.Action;
import com.edotassi.amazmod.notification.NotificationIds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import amazmod.com.transport.Constants;

public class NotificationUtils {

    private static final String[] REPLY_KEYWORDS = {"reply", "android.intent.extra.text"};
    private static final CharSequence REPLY_KEYWORD = "reply";
    private static final CharSequence INPUT_KEYWORD = "input";


    /**
     * Moved From AmazMod NotificationService.java *
     */

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public NotificationWear getNotificationWear(StatusBarNotification statusBarNotification) {
        NotificationWear notificationWear = new NotificationWear();
        Bundle bundle = statusBarNotification.getNotification().extras;
        notificationWear.bundle = bundle;
        for (String key : bundle.keySet()) {
            Log.d(Constants.TAG, "NotificationUtils getNotificationWear key: " + key);
            Object value = bundle.get(key);

            if ("android.wearable.EXTENSIONS".equals(key)) {
                Bundle wearBundle = ((Bundle) value);
                for (String keyInner : wearBundle.keySet()) {
                    Object valueInner = wearBundle.get(keyInner);

                    if (keyInner != null && valueInner != null) {
                        if ("actions".equals(keyInner) && valueInner instanceof ArrayList) {
                            ArrayList<Notification.Action> actions = new ArrayList<>();
                            actions.addAll((ArrayList) valueInner);
                            for (Notification.Action act : actions) {
                                if (act.getRemoteInputs() != null) {//API > 20 needed
                                    notificationWear.actionIntent = act.actionIntent;
                                    notificationWear.remoteInputs = act.getRemoteInputs();
                                    return notificationWear;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public void reply(Context context, NotificationWear notificationWear, String key, String reply) {

        Log.d(Constants.TAG, "NotificationUtils reply key: " + key);

        android.app.RemoteInput[] remoteInputs = notificationWear.remoteInputs;
        Bundle bundle = notificationWear.bundle;
        PendingIntent pendingIntent = notificationWear.actionIntent;

        if (remoteInputs == null || bundle == null || pendingIntent == null)
            return;

        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        for (android.app.RemoteInput remoteInput : remoteInputs) {
            bundle.putCharSequence(remoteInput.getResultKey(), reply);
        }
        android.app.RemoteInput.addResultsToIntent(remoteInputs, localIntent, bundle);
        try {
            pendingIntent.send(context, 0, localIntent);
        } catch (PendingIntent.CanceledException e) {
            Log.e(Constants.TAG, "NotificationUtils reply error: " + e.getLocalizedMessage());
        }
    }

    public class NotificationWear {
        android.app.RemoteInput[] remoteInputs;
        PendingIntent actionIntent;
        Bundle bundle;
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void replyToNotification(Context context, StatusBarNotification statusBarNotification, String message) {
        //NotificationWear notificationWear = new NotificationWear();
        //notificationWear.packageName = statusBarNotification.getPackageName();

        Log.d(Constants.TAG, "NotificationUtils replyToNotification key: " + statusBarNotification.getKey());

        Bundle localBundle = statusBarNotification.getNotification().extras;

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(statusBarNotification.getNotification());
        List<NotificationCompat.Action> actions = wearableExtender.getActions();
        Log.d(Constants.TAG, "NotificationUtils replyToNotification actions.size: " + actions.size() + " \\ isEmpty: " + actions.isEmpty());
        for (NotificationCompat.Action act : actions) {
            Log.d(Constants.TAG, "NotificationUtils replyToNotification act: " + act.toString());
            if (act != null && act.getRemoteInputs() != null) {
                Log.d(Constants.TAG, "NotificationUtils replyToNotification act.getTitle: " + act.getTitle());
                for (RemoteInput remoteInput : act.getRemoteInputs()) {
                    Log.d(Constants.TAG, "NotificationUtils replyToNotification remoteInput.getLabel: " + remoteInput.getLabel());
                    localBundle.putCharSequence(remoteInput.getResultKey(), message);
                }

                Intent localIntent = new Intent();
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                RemoteInput.addResultsToIntent(act.getRemoteInputs(), localIntent, localBundle);
                try {
                    act.actionIntent.send(context, 0, localIntent);
                } catch (PendingIntent.CanceledException e) {
                    Log.e(Constants.TAG, "NotificationUtils replyToNotification error: " + e.getLocalizedMessage());
                }
            }
        }

        //List<Notification> pages = wearableExtender.getPages();
        //notificationWear.pages.addAll(pages);

        //notificationWear.bundle = statusBarNotification.getNotification().extras;
        //notificationWear.tag = statusBarNotification.getTag();//TODO find how to pass Tag with sending PendingIntent, might fix Hangout problem

        //notificationWear.pendingIntent = statusBarNotification.getNotification().contentIntent;

        //RemoteInput[] remoteInputs = new RemoteInput[notificationWear.remoteInputs.size()];

        /*
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle localBundle = notificationWear.bundle;
        int i = 0;
        for (RemoteInput remoteIn : notificationWear.remoteInputs) {
            //getDetailsOfNotification(remoteIn);
            remoteInputs[i] = remoteIn;
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), message);//This work, apart from Hangouts as probably they need additional parameter (notification_tag?)
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            notificationWear.pendingIntent.send(context, 0, localIntent);
        } catch (PendingIntent.CanceledException e) {
        }
        */
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean isRecent(StatusBarNotification sbn, long recentTimeframeInSecs) {
        return sbn.getNotification().when > 0 &&  //Checks against real time to make sure its new
                System.currentTimeMillis() - sbn.getNotification().when <= TimeUnit.SECONDS.toMillis(recentTimeframeInSecs);
    }

    /**
     * http://stackoverflow.com/questions/9292032/extract-notification-text-from-parcelable-contentview-or-contentintent *
     */

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static boolean notificationMatchesFilter(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap) {
        NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
        return rankingMap.getRanking(sbn.getKey(), ranking) && ranking.matchesInterruptionFilter();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getMessage(Bundle extras) {
        Log.d(Constants.TAG, "NotificationUtils Getting message from extras..");
        Log.d(Constants.TAG, "NotificationUtils Text" + extras.getCharSequence(Notification.EXTRA_TEXT));
        Log.d(Constants.TAG, "NotificationUtils Big Text" + extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        Log.d(Constants.TAG, "NotificationUtils Title Big" + extras.getCharSequence(Notification.EXTRA_TITLE_BIG));
        Log.d(Constants.TAG, "NotificationUtils Text lines" + extras.getCharSequence(Notification.EXTRA_TEXT_LINES));
        Log.d(Constants.TAG, "NotificationUtils Info text" + extras.getCharSequence(Notification.EXTRA_INFO_TEXT));
        Log.d(Constants.TAG, "NotificationUtils Subtext" + extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        Log.d(Constants.TAG, "NotificationUtils Summary" + extras.getString(Notification.EXTRA_SUMMARY_TEXT));
        CharSequence chars = extras.getCharSequence(Notification.EXTRA_TEXT);
        if(!TextUtils.isEmpty(chars))
            return chars.toString();
        else if(!TextUtils.isEmpty((chars = extras.getString(Notification.EXTRA_SUMMARY_TEXT))))
            return chars.toString();
        else
            return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getExtended(Bundle extras, ViewGroup v) {
        Log.d(Constants.TAG, "NotificationUtils Getting message from extras..");

        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if(lines != null && lines.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence msg : lines)
//                msg = msg.toString();//.replaceAll("(\\s+$|^\\s+)", "").replaceAll("\n+", "\n");
                if (!TextUtils.isEmpty(msg)) {
                    sb.append(msg.toString());
                    sb.append('\n');
                }
            return sb.toString().trim();
        }
        CharSequence chars = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if(!TextUtils.isEmpty(chars))
            return chars.toString();
        else if(!VersionUtils.isJellyBeanMR2())
            return getExtended(v);
        else
            return getMessage(extras);
    }

    @SuppressLint("NewApi")
    public static ViewGroup getMessageView(Context context, Notification n) {
        Log.d(Constants.TAG, "NotificationUtils Getting message view..");
        RemoteViews views = null;
        if (Build.VERSION.SDK_INT >= 16)
            views = n.bigContentView;
        if (views == null)
            views = n.contentView;
        if (views == null)
            return null;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView = null;
        try {
            localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
            //ViewGroup localView = (ViewGroup) inflater.inflate(R.layout.nav_layout, null);
            views.reapply(context.getApplicationContext(), localView);
        } catch (Exception exp) {
            Log.e(Constants.TAG, "NotificationUtils getMessageView exception: " + exp.toString());
        }
        return localView;

    }

    public static String getTitle(ViewGroup localView) {
        Log.d(Constants.TAG, "NotificationUtils Getting title..");
        String msg = null;
        if (localView != null) {
            Context context = localView.getContext();
            TextView tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).TITLE);
            if (tv != null)
                msg = tv.getText().toString();
        }
        return msg;
    }

    public static String getMessage(ViewGroup localView) {
        Log.d(Constants.TAG, "NotificationUtils Getting message..");
        String msg = null;
        if (localView != null) {
            Context context = localView.getContext();
            TextView tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).BIG_TEXT);
            if (tv != null && !TextUtils.isEmpty(tv.getText()))
                msg = tv.getText().toString();
            if (TextUtils.isEmpty(msg)) {
                tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).TEXT);
                if (tv != null)
                    msg = tv.getText().toString();
            }
        }
        return msg;
    }

    public static String getExtended(ViewGroup localView) {
        Log.d(Constants.TAG, "NotificationUtils Getting extended message..");
        String msg = "";
        if (localView != null) {
            Context context = localView.getContext();
            TextView tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).EMAIL_0);
            if (tv != null && !TextUtils.isEmpty(tv.getText()))
                msg += tv.getText().toString() + '\n';
            tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).EMAIL_1);
            if (tv != null && !TextUtils.isEmpty(tv.getText()))
                msg += tv.getText().toString() + '\n';
            tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).EMAIL_2);
            if (tv != null && !TextUtils.isEmpty(tv.getText()))
                msg += tv.getText().toString() + '\n';
            tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).EMAIL_3);
            if (tv != null && !TextUtils.isEmpty(tv.getText()))
                msg += tv.getText().toString() + '\n';
            tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).EMAIL_4);
            if (tv != null && !TextUtils.isEmpty(tv.getText()))
                msg += tv.getText().toString() + '\n';
            tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).EMAIL_5);
            if (tv != null && !TextUtils.isEmpty(tv.getText()))
                msg += tv.getText().toString() + '\n';
            tv = (TextView) localView.findViewById(NotificationIds.getInstance(context).EMAIL_6);
            if (tv != null && !TextUtils.isEmpty(tv.getText()))
                msg += tv.getText().toString() + '\n';
//        tv = (TextView) localView.findViewById(NotificationIds.getInstance().INBOX_MORE);
//        if (tv != null && !TextUtils.isEmpty(tv.getText()))
//            msg += tv.getText().toString() + '\n';
        }
        if (msg.isEmpty())
            msg = getExpandedText(localView);
        if (msg.isEmpty())
            msg = getMessage(localView);
        if (msg != null)
            return msg.trim();
        else return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getTitle(Bundle extras) {
        Log.d(Constants.TAG, "NotificationUtils Getting title from extras..");
        String msg = extras.getString(Notification.EXTRA_TITLE);
        Log.d(Constants.TAG, "NotificationUtils Title Big" + extras.getString(Notification.EXTRA_TITLE_BIG));
        return msg;
    }

    /** OLD/CURRENT METHODS **/

    public static ViewGroup getView(Context context, RemoteViews view)
    {
        ViewGroup localView = null;
        try {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            localView = (ViewGroup) inflater.inflate(view.getLayoutId(), null);
            view.reapply(context, localView);
        }
        catch (Exception exp) {
            Log.e(Constants.TAG, "NotificationUtils getView exception: " + exp.toString());
        }
        return localView;
    }

    @SuppressLint("NewApi")
    public static ViewGroup getLocalView(Context context, Notification n)
    {
        RemoteViews view = null;

        if(Build.VERSION.SDK_INT >= 16) {
            view = n.bigContentView;
        }
        if (view == null) {
            view = n.contentView;
        }

        ViewGroup localView = null;

        try {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            localView = (ViewGroup) inflater.inflate(view.getLayoutId(), null);
            view.reapply(context, localView);
        } catch (Exception exp) {
            Log.e(Constants.TAG, "NotificationUtils getLovalView exception: " + exp.toString());
        }

        return localView;
    }

    public static ArrayList<Action> getActions(Notification n, String packageName, ArrayList<Action> actions) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(n);
        if (wearableExtender.getActions().size() > 0) {
            for (NotificationCompat.Action action : wearableExtender.getActions()) {
                Log.d(Constants.TAG, "NotificationUtils getActions action: " + action.getTitle().toString());
                actions.add(new Action(action, packageName, action.title.toString().toLowerCase().contains(REPLY_KEYWORD)));
            }
        }
        return actions;
    }

    public static Action getQuickReplyAction(Notification n, String packageName) {
        NotificationCompat.Action action = null;
        if(Build.VERSION.SDK_INT >= 24)
            action = getQuickReplyAction(n);
        if(action == null)
            action = getWearReplyAction(n);
        if(action == null)
            return null;
        return new Action(action, packageName, true);
    }

    private static NotificationCompat.Action getQuickReplyAction(Notification n) {
        for(int i = 0; i < NotificationCompat.getActionCount(n); i++) {
            NotificationCompat.Action action = NotificationCompat.getAction(n, i);
            if(action.getRemoteInputs() != null) {
                for (int x = 0; x < action.getRemoteInputs().length; x++) {
                    RemoteInput remoteInput = action.getRemoteInputs()[x];
                    if (isKnownReplyKey(remoteInput.getResultKey()))
                        return action;
                }
            }
        }
        return null;
    }

    private static NotificationCompat.Action getWearReplyAction(Notification n) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(n);
        for (NotificationCompat.Action action : wearableExtender.getActions()) {
            if(action.getRemoteInputs() != null) {
                for (int x = 0; x < action.getRemoteInputs().length; x++) {
                    RemoteInput remoteInput = action.getRemoteInputs()[x];
                    if (isKnownReplyKey(remoteInput.getResultKey()))
                        return action;
                    else if (remoteInput.getResultKey().toLowerCase().contains(INPUT_KEYWORD))
                        return action;
                }
            }
        }
        return null;
    }

    private static boolean isKnownReplyKey(String resultKey) {
        if(TextUtils.isEmpty(resultKey))
            return false;

        resultKey = resultKey.toLowerCase();
        for(String keyword : REPLY_KEYWORDS)
            if(resultKey.contains(keyword))
                return true;

        return false;
    }

    //OLD METHOD
    public static String getExpandedText(ViewGroup localView)
    {
        String text = "";
        if (localView != null)
        {
            Context context = localView.getContext();
            View v;
            // try to get big text
            v = localView.findViewById(NotificationIds.getInstance(context).big_notification_content_text);
            if (v != null && v instanceof TextView)
            {
                String s = ((TextView)v).getText().toString();
                if (!s.equals(""))
                {
                    // add title string if available
                    View titleView = localView.findViewById(android.R.id.title);
                    if (v != null && v instanceof TextView)
                    {
                        String title = ((TextView)titleView).getText().toString();
                        if (!title.equals(""))
                            text = title + " " + s;
                        else
                            text = s;
                    }
                    else
                        text = s;
                }
            }

            // try to extract details lines
            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_10_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    if (!s.equals(""))
                        text += s.toString();
            }

            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_9_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    text += "\n" + s.toString();
            }

            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_8_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    text += "\n" + s.toString();
            }

            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_7_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    text += "\n" + s.toString();
            }

            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_6_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    text += "\n" + s.toString();
            }

            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_5_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    text += "\n" + s.toString();
            }

            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_4_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    text += "\n" + s.toString();
            }

            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_3_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    text += "\n" + s.toString();
            }

            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_2_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    text += "\n" + s.toString();
            }

            v = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_event_1_id);
            if (v != null && v instanceof TextView)
            {
                CharSequence s = ((TextView)v).getText();
                if (!s.equals(""))
                    text += "\n" + s.toString();
            }

            if (text.equals("")) //Last resort for Kik
            {
                // get title string if available
                View titleView = localView.findViewById(NotificationIds.getInstance(context).notification_title_id );
                View bigTitleView = localView.findViewById(NotificationIds.getInstance(context).big_notification_title_id );
                View inboxTitleView = localView.findViewById(NotificationIds.getInstance(context).inbox_notification_title_id );
                try {
                    if (titleView != null && titleView instanceof TextView) {
                        text += ((TextView) titleView).getText() + " - ";
                    } else if (bigTitleView != null && bigTitleView instanceof TextView) {
                        text += ((TextView) titleView).getText();
                    } else if (inboxTitleView != null && inboxTitleView instanceof TextView) {
                        text += ((TextView) titleView).getText();
                    }
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG, "NotificationService getExpandedText NullPointerException: " + e.toString());
                } catch (ClassCastException e) {
                    Log.e(Constants.TAG, "NotificationService getExpandedText ClassCastException: " + e.toString());
                }

                v = localView.findViewById(NotificationIds.getInstance(context).notification_subtext_id);
                if (v != null && v instanceof TextView)
                {
                    CharSequence s = ((TextView)v).getText();
                    if (!s.equals(""))
                    {
                        text += s.toString();
                    }
                }
            }

        }
        return text.trim();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isAPriorityMode(int interruptionFilter) {
        return interruptionFilter != NotificationListenerService.INTERRUPTION_FILTER_NONE &&
                interruptionFilter != NotificationListenerService.INTERRUPTION_FILTER_UNKNOWN;
    }

}