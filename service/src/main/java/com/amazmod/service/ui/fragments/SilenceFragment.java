package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.text.emoji.widget.EmojiButton;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.SilenceApplicationEvent;
import com.amazmod.service.ui.NotificationWearActivity;
import com.amazmod.service.util.FragmentUtil;

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

    private String selectedSilenceTime;
    private boolean enableInvertedTheme, disableDelay;
    private Context mContext;

    private FragmentUtil util;

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

        util = new FragmentUtil(mContext);

        rootLayout = getActivity().findViewById(R.id.fragment_silence_root_layout);
        scrollView = getActivity().findViewById(R.id.fragment_silence_scrollview);
        silenceContainer = getActivity().findViewById(R.id.fragment_silence_container);
        delayedConfirmationView = getActivity().findViewById(R.id.fragment_silence_delayedView);
        delayedConfirmationView.setTotalTimeMs(3000);

        //Load preferences
        enableInvertedTheme = util.getInvertedTheme();
        disableDelay = util.getDisableDelay();

        // Set theme and font size
        //Log.d(Constants.TAG, "NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
        }

        delayedConfirmationView.setVisibility(View.GONE);
        //editTextContainer.setVisibility(View.GONE);

        addSilenceOptions();

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
            util.setButtonParams(button, silence.toString() + " minutes");
            util.setButtonTheme(button, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedSilenceTime = silence.toString();
                    sendSilenceCommand(v);
                }
            });
            silenceContainer.addView(button);
        }

        EmojiButton dayButton = new EmojiButton(mContext);
        util.setButtonParams(dayButton, "One Day");
        util.setButtonTheme(dayButton, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
        dayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSilenceTime = "1440";
                sendSilenceCommand(v);
            }
        });
        silenceContainer.addView(dayButton);

        EmojiButton blockButton = new EmojiButton(mContext);
        util.setButtonParams(blockButton, "BLOCK APP");
        util.setButtonTheme(blockButton, Constants.RED);
        blockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSilenceTime = Constants.BLOCK_APP;
                sendSilenceCommand(v);
            }
        });
        silenceContainer.addView(blockButton);
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
            util.setParamMargins(0, 24, 0, 4);
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

        util.setParamMargins(0,8,0,4);
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
