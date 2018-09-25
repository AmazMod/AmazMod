package com.amazmod.service.ui.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.text.emoji.widget.EmojiButton;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.settings.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;
import xiaofei.library.hermeseventbus.HermesEventBus;

import static android.content.Context.VIBRATOR_SERVICE;

public class CustomFragment extends Fragment {

    TextView title, time, text;
    ImageView icon;
    LinearLayout repliesContainer, buttonsLayout;
    BoxInsetLayout rootLayout;
    Button closeButton, replyButton;

    private static float fontSizeSP;
    private int id;
    private static String defaultLocale;
    private boolean enableInvertedTheme;
    private Context mContext;
    private SettingsManager settingsManager;

    private static final float FONT_SIZE_NORMAL = 14.0f;
    private static final float FONT_SIZE_LARGE = 18.0f;
    private static final float FONT_SIZE_HUGE = 22.0f;
    private static final String ID = "id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_custom, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateContent();

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void updateContent(){
        mContext = getActivity();

        id = getArguments().getInt(ID, 1);
        Log.i(Constants.TAG, "CustomFragment id: " + id);
        NotificationData notificationSpec = getArguments().getParcelable(NotificationData.EXTRA);

        settingsManager = new SettingsManager(mContext);

        title = getActivity().findViewById(R.id.notification_title);
        time = getActivity().findViewById(R.id.notification_time);
        text = getActivity().findViewById(R.id.notification_text);
        icon = getActivity().findViewById(R.id.notification_icon);
        repliesContainer = getActivity().findViewById(R.id.notification_replies_container);
        rootLayout = getActivity().findViewById(R.id.notification_root_layout);
        buttonsLayout = getActivity().findViewById(R.id.activity_buttons);
        replyButton = getActivity().findViewById(R.id.activity_notification_button_reply);
        closeButton = getActivity().findViewById(R.id.activity_notification_button_close);

        boolean hideReplies;

        //Load preferences
        boolean disableNotificationReplies = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);
        enableInvertedTheme = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_INVERTED_THEME,
                Constants.PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME);
        defaultLocale = settingsManager.getString(Constants.PREF_DEFAULT_LOCALE, "");
        Log.i(Constants.TAG, "CustomFragment defaultLocale: " + defaultLocale);


        // Set theme and font size
        //Log.d(Constants.TAG, "NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            icon.setBackgroundColor(getResources().getColor(R.color.darker_gray));
        }

        setFontSizeSP();
        time.setTextSize(fontSizeSP);
        title.setTextSize(fontSizeSP);
        text.setTextSize(fontSizeSP);

        try {

            hideReplies = notificationSpec.getHideReplies();

            int[] iconData = notificationSpec.getIcon();
            int iconWidth = notificationSpec.getIconWidth();
            int iconHeight = notificationSpec.getIconHeight();
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
            Log.e(Constants.TAG, "NotificationActivity onCreate - Exception: " + ex.toString()
                    + " notificationSpec: " + notificationSpec);
            title.setText("AmazMod");
            text.setText("Welcome to AmazMod");
            time.setText("00:00");
            icon.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.amazmod));
            hideReplies = true;
        }

        buttonsLayout.setVisibility(View.GONE);

        if (id == 2) {
            text.setVisibility(View.GONE);
            time.setVisibility(View.GONE);
            addReplies(notificationSpec);
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

    private void setFontLocale(Button b, String locale) {
        Log.i(Constants.TAG, "NotificationActivity setFontLocale Button: " + locale);
        if (locale.contains("iw")) {
            Typeface face = Typeface.createFromAsset(mContext.getAssets(),"fonts/DroidSansFallback.ttf");
            b.setTypeface(face);
        }
    }

    private void addReplies(final NotificationData notificationData) {
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        param.setMargins(20,8,20,8);

        List<Reply> repliesList = loadReplies();
        for (final Reply reply : repliesList) {
            EmojiButton button = new EmojiButton(mContext);
            button.setLayoutParams(param);
            button.setPadding(0,8,0,8);
            setFontLocale(button, defaultLocale);
            button.setText(reply.getValue());
            button.setAllCaps(false);
            button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);
            setButtonTheme(button, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setBackground(mContext.getDrawable(R.drawable.reply_dark_grey));
                    final NotificationData notificationSpec = notificationData;
                    HermesEventBus.getDefault().post(new ReplyNotificationEvent(notificationSpec.getKey(), reply.getValue()));
                }
            });
            repliesContainer.addView(button);
        }

    }

    private List<Reply> loadReplies() {
        final String replies = settingsManager.getString(Constants.PREF_NOTIFICATION_CUSTOM_REPLIES, "[]");

        try {
            Type listType = new TypeToken<List<Reply>>() {
            }.getType();
            return new Gson().fromJson(replies, listType);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private void setButtonTheme(Button button, String color){
        switch (color) {
            case ("red"): {
                button.setTextColor(Color.parseColor("#ffffff"));
                button.setBackground(mContext.getDrawable(R.drawable.close_red));
                break;
            }
            case ("blue"): {
                button.setTextColor(Color.parseColor("#ffffff"));
                button.setBackground(mContext.getDrawable(R.drawable.reply_blue));
                break;
            }
            case ("grey"): {
                button.setTextColor(Color.parseColor("#000000"));
                button.setBackground(mContext.getDrawable(R.drawable.reply_grey));
                break;
            }
            default: {
                button.setTextColor(Color.parseColor("#000000"));
                button.setBackground(mContext.getDrawable(R.drawable.reply_grey));
            }
        }
    }

    public static CustomFragment newInstance(int id, NotificationData notificationData) {
        CustomFragment myFragment = new CustomFragment();

        Bundle args = new Bundle();
        args.putInt(ID, id);
        args.putParcelable(NotificationData.EXTRA, notificationData);
        myFragment.setArguments(args);

        return myFragment;
    }

    public String getFragmentId() {
        return ("frag"+String.valueOf(getArguments().getInt(ID, 1)));
    }

}
