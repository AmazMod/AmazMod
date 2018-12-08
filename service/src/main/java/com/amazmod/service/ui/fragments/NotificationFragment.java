package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.NotificationWearActivity;
import com.amazmod.service.util.FragmentUtil;

import amazmod.com.transport.data.NotificationData;

import static android.content.Context.VIBRATOR_SERVICE;

public class NotificationFragment extends Fragment {

    TextView title;
    TextView time;
    TextView text;
    ImageView icon;
    ImageView image;
    ImageView picture;
    BoxInsetLayout rootLayout;
    LinearLayout repliesLayout;
    NotificationData notificationData;

    private boolean enableInvertedTheme;
    private Context mContext;
    private FragmentUtil util;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Log.i(Constants.TAG, "NotificationFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(Constants.TAG, "NotificationFragment onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(Constants.TAG, "NotificationFragment onCreateView");

        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(Constants.TAG, "NotificationFragment onViewCreated");

        updateContent();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void updateContent() {

        final String key = getArguments().getString(NotificationWearActivity.KEY);
        notificationData = NotificationStore.getCustomNotification(key);

        Log.i(Constants.TAG, "NotificationFragment updateContent context: " + mContext + " | key: " + key);

        util = new FragmentUtil(mContext);

        title = getActivity().findViewById(R.id.fragment_custom_notification_title);
        time = getActivity().findViewById(R.id.fragment_custom_notification_time);
        text = getActivity().findViewById(R.id.fragment_custom_notification_text);
        icon = getActivity().findViewById(R.id.fragment_custom_notification_icon);
        picture = getActivity().findViewById(R.id.fragment_custom_notificstion_picture);
        rootLayout = getActivity().findViewById(R.id.fragment_custom_root_layout);
        repliesLayout = getActivity().findViewById(R.id.fragment_custom_notification_replies_layout);
        image = getActivity().findViewById(R.id.fragment_custom_notification_replies_image);

        //Load preferences
        boolean disableNotificationText = util.getDisableNotificationText();
        enableInvertedTheme = util.getInvertedTheme();

        // Set theme and font size
        //Log.d(Constants.TAG, "NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            //icon.setBackgroundColor(getResources().getColor(R.color.darker_gray));
        }

        time.setTextSize(util.getFontSizeSP());
        title.setTextSize(util.getFontSizeSP());
        text.setTextSize(util.getFontSizeSP());

        try {
            Log.i(Constants.TAG, "NotificationFragment updateContent try");

            //hideReplies = notificationData.getHideReplies();

            populateNotificationIcon(icon, notificationData);
            populateNotificationPicture(picture, notificationData);

            if (!hasPicture(notificationData)) {
                title.setText(notificationData.getTitle());
                time.setText(notificationData.getTime());

                util.setFontLocale(text, util.getDefaultLocale());
                text.setText(notificationData.getText());
            } else {
                title.setText(notificationData.getTitle() + " - " + notificationData.getTime());
                time.setVisibility(View.GONE);
                text.setVisibility(View.GONE);
            }

            if (notificationData.getVibration() > 0) {
                Vibrator vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(notificationData.getVibration());
                }
            }
        } catch (NullPointerException ex) {
            Log.e(Constants.TAG, "NotificationFragment updateContent - Exception: " + ex.toString()
                    + " notificationData: " + notificationData);
            title.setText("AmazMod");
            text.setText("Welcome to AmazMod");
            time.setText("00:00");
            icon.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.amazmod));
            //hideReplies = true;
        }

        if (disableNotificationText) {
            text.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            if (enableInvertedTheme)
                image.setImageDrawable(getResources().getDrawable(R.drawable.outline_screen_lock_portrait_black_48));
            else
                image.setImageDrawable(getResources().getDrawable(R.drawable.outline_screen_lock_portrait_white_48));
        }
    }

    public static NotificationFragment newInstance(String key) {

        Log.i(Constants.TAG, "NotificationFragment newInstance key: " + key);
        NotificationFragment myFragment = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NotificationWearActivity.KEY, key);
        myFragment.setArguments(bundle);

        return myFragment;
    }

    private void populateNotificationIcon(ImageView iconView, NotificationData notificationData) {
        try {
            byte[] largeIconData = notificationData.getLargeIcon();
            if ((largeIconData != null) && (largeIconData.length > 0)) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(largeIconData, 0, largeIconData.length);

                RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);

                roundedBitmapDrawable.setCircular(true);
                roundedBitmapDrawable.setAntiAlias(true);

                iconView.setImageDrawable(roundedBitmapDrawable);
            } else {
                int[] iconData = notificationData.getIcon();
                int iconWidth = notificationData.getIconWidth();
                int iconHeight = notificationData.getIconHeight();

                //Invert color (works if the bitmap is in ARGB_8888 format)
                if (enableInvertedTheme) {
                    for (int i = 0; i < iconData.length; i++) {
                        if (iconData[i] == 0xffffffff) {
                            iconData[i] = 0xff000000;
                        }
                    }
                }

                Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
                bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);
                iconView.setImageBitmap(bitmap);
            }
        } catch (Exception exception) {
            Log.d(Constants.TAG, exception.getMessage(), exception);
        }
    }

    private boolean hasPicture(NotificationData notificationData) {
        byte[] pictureData = notificationData.getPicture();
        return (pictureData != null) && (pictureData.length > 0);
    }

    private void populateNotificationPicture(ImageView pictureView, NotificationData notificationData) {
        try {
            if (hasPicture(notificationData)) {
                byte[] pictureData = notificationData.getPicture();
                Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                pictureView.setImageBitmap(bitmap);
                pictureView.setVisibility(View.VISIBLE);

            }
        } catch (Exception exception) {
            Log.d(Constants.TAG, exception.getMessage(), exception);
        }
    }
}
