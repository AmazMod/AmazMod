package com.edotassi.amazmod.notification;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.edotassi.amazmod.R;

/**
 * Created by JJ on 22/05/15.
 */
public class NotificationIds {

    public int ICON;
    public int TITLE;
    public int BIG_TEXT;
    public int TEXT;
    public int BIG_PIC;
    public int EMAIL_0;
    public int EMAIL_1;
    public int EMAIL_2;
    public int EMAIL_3;
    public int EMAIL_4;
    public int EMAIL_5;
    public int EMAIL_6;
    public int INBOX_MORE;

    private static NotificationIds singleton;

    public static NotificationIds getInstance(Context context) {
        if (singleton == null)
            singleton = new NotificationIds(context);
        return singleton;
    }

    public NotificationIds(final Context context) {
        Resources r = context.getResources();
        ICON = r.getIdentifier("android:id/icon", null, null);
        TITLE = r.getIdentifier("android:id/title", null, null);
        BIG_TEXT = r.getIdentifier("android:id/big_text", null, null);
        TEXT = r.getIdentifier("android:id/text", null, null);
        BIG_PIC = r.getIdentifier("android:id/big_picture", null, null);
        EMAIL_0 = r.getIdentifier("android:id/inbox_text0", null, null);
        EMAIL_1 = r.getIdentifier("android:id/inbox_text1", null, null);
        EMAIL_2 = r.getIdentifier("android:id/inbox_text2", null, null);
        EMAIL_3 = r.getIdentifier("android:id/inbox_text3", null, null);
        EMAIL_4 = r.getIdentifier("android:id/inbox_text4", null, null);
        EMAIL_5 = r.getIdentifier("android:id/inbox_text5", null, null);
        EMAIL_6 = r.getIdentifier("android:id/inbox_text6", null, null);
        INBOX_MORE = r.getIdentifier("android:id/inbox_more", null, null);
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                return null;
            }
            @Override
            protected void onPostExecute(Object o) {
                detectNotificationIds(context);
            }
        }.execute();

    }

    public int notification_title_id = 0;
    public int big_notification_summary_id = 0;
    public int big_notification_content_title = 0;
    public int big_notification_content_text = 0;
    public int notification_image_id = 0;

    public int inbox_notification_title_id = 0;
    public int big_notification_title_id = 0;

    public int notification_subtext_id = 0;
    public int inbox_notification_event_1_id = 0;
    public int inbox_notification_event_2_id = 0;
    public int inbox_notification_event_3_id = 0;
    public int inbox_notification_event_4_id = 0;
    public int inbox_notification_event_5_id = 0;
    public int inbox_notification_event_6_id = 0;
    public int inbox_notification_event_7_id = 0;
    public int inbox_notification_event_8_id = 0;
    public int inbox_notification_event_9_id = 0;
    public int inbox_notification_event_10_id = 0;

    private void recursiveDetectNotificationsIds(ViewGroup v)
    {
        for(int i=0; i<v.getChildCount(); i++)
        {
            View child = v.getChildAt(i);
            if (child instanceof ViewGroup)
                recursiveDetectNotificationsIds((ViewGroup)child);
            else if (child instanceof TextView)
            {
                String text = ((TextView)child).getText().toString();
                int id = child.getId();
                if (text.equals("1")) notification_title_id = id;
                    //else if (text.equals("2")) notification_text_id = id;
                    //else if (text.equals("3")) notification_info_id = id;
                else if (text.equals("4")) notification_subtext_id = id;
                else if (text.equals("5")) big_notification_summary_id = id;
                else if (text.equals("6")) big_notification_content_title = id;
                else if (text.equals("7")) big_notification_content_text = id;
                else if (text.equals("8")) big_notification_title_id = id;
                else if (text.equals("9")) inbox_notification_title_id = id;
                else if (text.equals("10")) inbox_notification_event_1_id = id;
                else if (text.equals("11")) inbox_notification_event_2_id = id;
                else if (text.equals("12")) inbox_notification_event_3_id = id;
                else if (text.equals("13")) inbox_notification_event_4_id = id;
                else if (text.equals("14")) inbox_notification_event_5_id = id;
                else if (text.equals("15")) inbox_notification_event_6_id = id;
                else if (text.equals("16")) inbox_notification_event_7_id = id;
                else if (text.equals("17")) inbox_notification_event_8_id = id;
                else if (text.equals("18")) inbox_notification_event_9_id = id;
                else if (text.equals("19")) inbox_notification_event_10_id = id;
            }
            else if (child instanceof ImageView)
            {
                Drawable d = ((ImageView)child).getDrawable();
                if (d!=null)
                {
                    this.notification_image_id = child.getId();
                }
            }
        }
    }

    //  TODO: This is the old main method used to detect notification id's
    private void detectNotificationIds(Context context)
    {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.dummy_icon)
                .setContentTitle("1")
                .setContentText("2")
                .setContentInfo("3")
                .setSubText("4");

        android.app.Notification n = mBuilder.build();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup localView;

        // detect id's from normal view
        localView = (ViewGroup) inflater.inflate(n.contentView.getLayoutId(), null);
        n.contentView.reapply(context, localView);
        recursiveDetectNotificationsIds(localView);

        // detect id's from expanded views
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        //{
        NotificationCompat.BigTextStyle bigtextstyle = new NotificationCompat.BigTextStyle();
        bigtextstyle.setSummaryText("5");
        bigtextstyle.setBigContentTitle("6");
        bigtextstyle.bigText("7");
        mBuilder.setContentTitle("8");
        mBuilder.setStyle(bigtextstyle);
        detectExpandedNotificationsIds(mBuilder.build(), context);

        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        String[] events = {"10","11","12","13","14","15","16","17","18","19"};
        inboxStyle.setBigContentTitle("6");
        mBuilder.setContentTitle("9");
        inboxStyle.setSummaryText("5");

        for (int i=0; i < events.length; i++)
        {
            inboxStyle.addLine(events[i]);
        }
        mBuilder.setStyle(inboxStyle);

        detectExpandedNotificationsIds(mBuilder.build(), context);
        //}
    }

    @SuppressLint("NewApi")
    private void detectExpandedNotificationsIds(android.app.Notification n, Context context)
    {
        if(Build.VERSION.SDK_INT >= 16)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup localView = (ViewGroup) inflater.inflate(n.bigContentView.getLayoutId(), null);
            n.bigContentView.reapply(context, localView);
            recursiveDetectNotificationsIds(localView);
        }
    }

}