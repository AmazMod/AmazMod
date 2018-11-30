package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.text.emoji.widget.EmojiButton;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.events.SilenceApplicationEvent;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.ui.NotificationWearActivity;

import java.util.ArrayList;
import java.util.List;

import amazmod.com.transport.data.NotificationData;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class SilenceFragment extends Fragment implements DelayedConfirmationView.DelayedConfirmationListener {

    LinearLayout silenceContainer;
    BoxInsetLayout rootLayout;
    ScrollView scrollView;
    NotificationData notificationSpec;
    private DelayedConfirmationView delayedConfirmationView;

    private float fontSizeSP;
    private String defaultLocale, selectedSilenceTime;
    private boolean enableInvertedTheme, disableDelay;
    private Context mContext;
    private LinearLayout.LayoutParams params;
    private SettingsManager settingsManager;

    private static final float FONT_SIZE_NORMAL = 14.0f;
    private static final float FONT_SIZE_LARGE = 18.0f;
    private static final float FONT_SIZE_HUGE = 22.0f;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Log.i(Constants.TAG,"SilenceFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationSpec = NotificationData.fromBundle(getArguments());

        Log.d(Constants.TAG,"SilenceFragment onCreate " + notificationSpec);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(Constants.TAG,"SilenceFragment onCreateView");
        return inflater.inflate(R.layout.fragment_silence, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(Constants.TAG,"SilenceFragment onViewCreated");
        updateContent();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void updateContent(){
        mContext = getActivity();

        Log.d(Constants.TAG,"SilenceFragment updateContent " + notificationSpec);

        settingsManager = new SettingsManager(mContext);

        rootLayout = getActivity().findViewById(R.id.fragment_silence_root_layout);
        scrollView = getActivity().findViewById(R.id.fragment_silence_scrollview);
        silenceContainer = getActivity().findViewById(R.id.fragment_silence_container);
        delayedConfirmationView = getActivity().findViewById(R.id.fragment_silence_delayedView);
        delayedConfirmationView.setTotalTimeMs(3000);


        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        //Load preferences
        enableInvertedTheme = settingsManager.getBoolean(Constants.PREF_NOTIFICATIONS_INVERTED_THEME,
                Constants.PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME);
        disableDelay = settingsManager.getBoolean(Constants.PREF_DISABLE_DELAY,
                Constants.PREF_DEFAULT_DISABLE_DELAY);
        defaultLocale = settingsManager.getString(Constants.PREF_DEFAULT_LOCALE, "");
        Log.i(Constants.TAG, "SilenceFragment defaultLocale: " + defaultLocale);


        // Set theme and font size
        //Log.d(Constants.TAG, "NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
        }

        delayedConfirmationView.setVisibility(View.GONE);
        //editTextContainer.setVisibility(View.GONE);
        setFontSizeSP();
        addSilenceOptions();

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

    private void setFontLocale(Button b, String locale) {
        Log.i(Constants.TAG, "SilenceFragment setFontLocale Button: " + locale);
        if (locale.contains("iw")) {
            Typeface face = Typeface.createFromAsset(mContext.getAssets(),"fonts/DroidSansFallback.ttf");
            b.setTypeface(face);
        }
    }

    private void addSilenceOptions() {

        List<Integer> silenceList = new ArrayList<>();
        silenceList.add(5);
        silenceList.add(15);
        silenceList.add(30);
        silenceList.add(60);

        for (final Integer silence : silenceList) {
            EmojiButton button = new EmojiButton(mContext);
            //Button button = new Button(mContext);
            setButtonParams(button, silence.toString() + " minutes");
            setButtonTheme(button, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedSilenceTime = silence.toString();
                    sendSilenceCommand(v);
                }
            });
            silenceContainer.addView(button);
        }

        EmojiButton foreverButton = new EmojiButton(mContext);
        setButtonParams(foreverButton, "Forever");
        setButtonTheme(foreverButton, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
        foreverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSilenceTime = "999999999999";
                sendSilenceCommand(v);
            }
        });
        silenceContainer.addView(foreverButton);
    }

    private void setButtonParams(EmojiButton button, String text) {
        params.setMargins(20, 12, 20, 12);
        button.setLayoutParams(params);
        button.setPadding(0,10,0,10);
        button.setIncludeFontPadding(false);
        button.setMinHeight(24);
        setFontLocale(button, defaultLocale);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);

    }

    private void setButtonParams(Button button, String text) {
        button.setPadding(0,10,0,10);
        button.setIncludeFontPadding(false);
        button.setMinHeight(24);
        button.setText(text);
        button.setAllCaps(true);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSizeSP);

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

    private void sendSilenceCommand(View v) {

        ((NotificationWearActivity)getActivity()).stopTimerFinish();
        silenceContainer.setVisibility(View.GONE);
        //editTextContainer.setVisibility(View.GONE);

        if (disableDelay) {
            Log.i(Constants.TAG, "SilenceFragment sendSilenceCommand without delay");
            onTimerFinished(v);
        } else {
            Log.d(Constants.TAG, "SilenceFragment sendSilenceCommand with delay");
            params.setMargins(0, 24, 0, 4);
            delayedConfirmationView.setVisibility(View.VISIBLE);
            //textView.setText(getResources().getString(R.string.sending));
            delayedConfirmationView.setPressed(false);
            delayedConfirmationView.start();
            delayedConfirmationView.setListener(this);
            Log.i(Constants.TAG, "SilenceFragment sendSilenceCommand isPressed: " + delayedConfirmationView.isPressed());
        }
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        delayedConfirmationView.reset();

        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);

        ((NotificationWearActivity)getActivity()).startTimerFinish();

        params.setMargins(0,8,0,4);
        //textView.setLayoutParams(params);
        delayedConfirmationView.setVisibility(View.GONE);
        silenceContainer.setVisibility(View.VISIBLE);
        Log.i(Constants.TAG, "SilenceFragment onTimerSelected isPressed: " + v.isPressed());
    }

    @Override
    public void onTimerFinished(View v) {
        Log.i(Constants.TAG, "SilenceFragment onTimerFinished isPressed: " + v.isPressed());

        if (v instanceof DelayedConfirmationView)
            ((DelayedConfirmationView) v).setListener(null);

        Intent intent = new Intent(mContext, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Silenced!");
        startActivity(intent);

        HermesEventBus.getDefault().post(new SilenceApplicationEvent(notificationSpec.getKey(), selectedSilenceTime));
        getActivity().finish();

    }

    public static SilenceFragment newInstance(Bundle b) {

        Log.i(Constants.TAG,"SilenceFragment newInstance");
        SilenceFragment myFragment = new SilenceFragment();
        myFragment.setArguments(b);

        return myFragment;
    }

}
