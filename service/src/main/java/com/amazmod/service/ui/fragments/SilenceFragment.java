package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.SilenceApplicationEvent;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.NotificationWearActivity;
import com.amazmod.service.util.FragmentUtil;

import java.util.ArrayList;
import java.util.List;

import xiaofei.library.hermeseventbus.HermesEventBus;

public class SilenceFragment extends Fragment implements DelayedConfirmationView.DelayedConfirmationListener {

    LinearLayout silenceContainer;
    BoxInsetLayout rootLayout;
    ScrollView scrollView;
    TextView textView;
    private DelayedConfirmationView delayedConfirmationView;

    private String selectedSilenceTime, notificationKey, key, mode;
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

        key = getArguments().getString(NotificationWearActivity.KEY);
        mode = getArguments().getString(NotificationWearActivity.MODE);
        notificationKey = NotificationStore.getCustomNotification(key).getKey();

        Log.d(Constants.TAG,"SilenceFragment onCreate key: " + key);

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

        Log.d(Constants.TAG,"SilenceFragment updateContent " + notificationKey);

        util = new FragmentUtil(mContext);

        rootLayout = getActivity().findViewById(R.id.fragment_silence_root_layout);
        scrollView = getActivity().findViewById(R.id.fragment_silence_scrollview);
        textView = getActivity().findViewById(R.id.fragment_silence_textview);
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
            textView.setTextColor(getResources().getColor(R.color.black));
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
            Button button = new Button(mContext);
            //Button button = new Button(mContext);
            util.setButtonParams(button, silence.toString() + " minutes", true);
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

        Button dayButton = new Button(mContext);
        util.setButtonParams(dayButton, "One Day", true);
        util.setButtonTheme(dayButton, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
        dayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedSilenceTime = "1440";
                sendSilenceCommand(v);
            }
        });
        silenceContainer.addView(dayButton);

        Button blockButton = new Button(mContext);
        util.setButtonParams(blockButton, "BLOCK APP", true);
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

        NotificationStore.removeCustomNotification(key);
        if (NotificationWearActivity.MODE_VIEW.equals(mode))
            WearNotificationsFragment.getInstance().loadNotifications();
        HermesEventBus.getDefault().post(new SilenceApplicationEvent(notificationKey, selectedSilenceTime));
        getActivity().finish();

    }

    public static SilenceFragment newInstance(String key, String mode) {

        Log.i(Constants.TAG,"SilenceFragment newInstance key: " + key);
        SilenceFragment myFragment = new SilenceFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NotificationWearActivity.KEY, key);
        bundle.putString(NotificationWearActivity.MODE, mode);
        myFragment.setArguments(bundle);

        return myFragment;
    }

}
