package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.settings.SettingsManager;
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
    Button deleteButton;
    BoxInsetLayout rootLayout;
    LinearLayout repliesLayout;
    NotificationData notificationData;

    private String key, mode;
    private boolean enableInvertedTheme;
    private Context mContext;
    private FragmentUtil util;
    private SettingsManager settingsManager;

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

        key = getArguments().getString(NotificationWearActivity.KEY);
        mode = getArguments().getString(NotificationWearActivity.MODE);
        settingsManager = new SettingsManager(this.mContext);
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
        final String mode = getArguments().getString(NotificationWearActivity.MODE);
        notificationData = NotificationStore.getCustomNotification(key);

        Log.i(Constants.TAG, "NotificationFragment updateContent context: " + mContext + " | key: " + key);

        util = new FragmentUtil(mContext);

        title = getActivity().findViewById(R.id.fragment_custom_notification_title);
        time = getActivity().findViewById(R.id.fragment_custom_notification_time);
        text = getActivity().findViewById(R.id.fragment_custom_notification_text);
        icon = getActivity().findViewById(R.id.fragment_custom_notification_icon);
        picture = getActivity().findViewById(R.id.fragment_custom_notification_picture);
        rootLayout = getActivity().findViewById(R.id.fragment_custom_root_layout);
        repliesLayout = getActivity().findViewById(R.id.fragment_custom_notification_replies_layout);
        image = getActivity().findViewById(R.id.fragment_custom_notification_replies_image);
        deleteButton = getActivity().findViewById(R.id.fragment_delete_button);
        if (settingsManager.getBoolean(Constants.PREF_NOTIFICATION_DELETE_BUTTON, false)) {
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendDeleteCommand(v);
                }
            });
        }else {
            deleteButton.setVisibility(View.GONE);
        }

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
            //deleteButton.setTextColor(getResources().getColor(R.color.black));
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

            if (notificationData.getVibration() > 0 && NotificationWearActivity.MODE_ADD.equals(mode)) {
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
            picture.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            if (enableInvertedTheme)
                image.setImageDrawable(getResources().getDrawable(R.drawable.outline_screen_lock_portrait_black_48));
            else
                image.setImageDrawable(getResources().getDrawable(R.drawable.outline_screen_lock_portrait_white_48));
        }
    }

    public static NotificationFragment newInstance(String key, String mode) {

        Log.i(Constants.TAG, "NotificationFragment newInstance key: " + key);
        NotificationFragment myFragment = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NotificationWearActivity.KEY, key);
        bundle.putString(NotificationWearActivity.MODE, mode);
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
                Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);

                //Invert color (works if the bitmap is in ARGB_8888 format)
                if (enableInvertedTheme) {
                    int[] invertedIconData = new int[iconData.length];
                    for (int i = 0; i < iconData.length; i++) {
                        if (iconData[i] == 0xffffffff)
                            invertedIconData[i] = 0xff000000;
                        else
                            invertedIconData[i] = iconData[i];
                    }
                    bitmap.setPixels(invertedIconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

                } else
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


    private void sendDeleteCommand(View v) {

        Log.i(Constants.TAG, "NotificationFragment sendDeleteCommand");

        Intent intent = new Intent(mContext, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Deleted!");
        startActivity(intent);

        NotificationStore.removeCustomNotification(key);

        if (NotificationWearActivity.MODE_VIEW.equals(mode))
            WearNotificationsFragment.getInstance().loadNotifications();

        getActivity().finish();
    }

}
