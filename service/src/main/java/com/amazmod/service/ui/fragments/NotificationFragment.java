package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
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
import com.amazmod.service.settings.SettingsManager;

import amazmod.com.transport.data.NotificationData;

import static android.content.Context.VIBRATOR_SERVICE;

public class NotificationFragment extends Fragment {

    TextView title, time, text;
    ImageView icon, image;
    BoxInsetLayout rootLayout;
    LinearLayout repliesLayout;
    NotificationData notificationSpec;

    private float fontSizeSP;
    private String defaultLocale;
    private boolean enableInvertedTheme;
    private Context mContext;
    private SettingsManager settingsManager;

    private static final float FONT_SIZE_NORMAL = 14.0f;
    private static final float FONT_SIZE_LARGE = 18.0f;
    private static final float FONT_SIZE_HUGE = 22.0f;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Log.i(Constants.TAG,"NotificationFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //notificationSpec = NotificationData.fromBundle(getArguments());
        Log.i(Constants.TAG,"NotificationFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(Constants.TAG,"NotificationFragment onCreateView");

        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(Constants.TAG,"NotificationFragment onViewCreated");

        updateContent();

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void updateContent(){
        //mContext = getActivity();

        notificationSpec = getArguments().getParcelable(NotificationData.EXTRA);

        Log.i(Constants.TAG,"NotificationFragment updateContent context: " + mContext);

        settingsManager = new SettingsManager(mContext);

        title = getActivity().findViewById(R.id.fragment_custom_notification_title);
        time = getActivity().findViewById(R.id.fragment_custom_notification_time);
        text = getActivity().findViewById(R.id.fragment_custom_notification_text);
        icon = getActivity().findViewById(R.id.fragment_custom_notification_icon);
        rootLayout = getActivity().findViewById(R.id.fragment_custom_root_layout);
        repliesLayout = getActivity().findViewById(R.id.fragment_custom_notification_replies_layout);
        image = getActivity().findViewById(R.id.fragment_custom_notification_replies_image);

        boolean hideReplies;

        //Load preferences
        boolean disableNotificationText = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);
        enableInvertedTheme = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_INVERTED_THEME,
                Constants.PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME);
        defaultLocale = settingsManager.getString(Constants.PREF_DEFAULT_LOCALE, "");
        Log.i(Constants.TAG, "NotificationFragment defaultLocale: " + defaultLocale + " / enableInvertedTheme: " + enableInvertedTheme);


        // Set theme and font size
        //Log.d(Constants.TAG, "NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            //icon.setBackgroundColor(getResources().getColor(R.color.darker_gray));
        }

        setFontSizeSP();
        time.setTextSize(fontSizeSP);
        title.setTextSize(fontSizeSP);
        text.setTextSize(fontSizeSP);

        try {

            Log.i(Constants.TAG,"NotificationFragment updateContent try");

            hideReplies = notificationSpec.getHideReplies();

            int[] iconData = notificationSpec.getIcon();
            int iconWidth = notificationSpec.getIconWidth();
            int iconHeight = notificationSpec.getIconHeight();

            //Invert color (works if the bitmap is in ARGB_8888 format)
            if (enableInvertedTheme) {
                for (int i = 0; i < iconData.length; i++){
                    if (iconData[i] == 0xffffffff){
                        iconData[i] = 0xff000000;
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(iconData, 0, iconWidth, 0, 0, iconWidth, iconHeight);

            icon.setImageBitmap(bitmap);
            title.setText(notificationSpec.getTitle());
            setFontLocale(text, defaultLocale);
            text.setText(notificationSpec.getText());
            time.setText(notificationSpec.getTime());

            if (notificationSpec.getVibration() > 0) {
                Vibrator vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(notificationSpec.getVibration());
                }
            }

        } catch (NullPointerException ex) {
            Log.e(Constants.TAG, "NotificationFragment updateContent - Exception: " + ex.toString()
                    + " notificationSpec: " + notificationSpec);
            title.setText("AmazMod");
            text.setText("Welcome to AmazMod");
            time.setText("00:00");
            icon.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.amazmod));
            hideReplies = true;
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


    private void setFontSizeSP(){
        String fontSize = settingsManager.getString(Constants.PREF_NOTIFICATIONS_FONT_SIZE,
                Constants.PREF_DEFAULT_NOTIFICATIONS_FONT_SIZE);
        switch (fontSize) {
            case "l":
                fontSizeSP = FONT_SIZE_LARGE;
                break;
            case "h":
                fontSizeSP = FONT_SIZE_HUGE;
                break;
            default:
                fontSizeSP = FONT_SIZE_NORMAL;
        }
    }

    private void setFontLocale(TextView tv, String locale) {
        Log.i(Constants.TAG, "NotificationActivity setFontLocale TextView: " + locale);
        if (locale.contains("iw")) {
            Typeface face = Typeface.createFromAsset(mContext.getAssets(),"fonts/DroidSansFallback.ttf");
            tv.setTypeface(face);
        }
    }

    public static NotificationFragment newInstance(Bundle b) {

        Log.i(Constants.TAG,"NotificationFragment newInstance");
        NotificationFragment myFragment = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(NotificationData.EXTRA, NotificationData.fromBundle(b));
        myFragment.setArguments(bundle);

        return myFragment;
    }

}
