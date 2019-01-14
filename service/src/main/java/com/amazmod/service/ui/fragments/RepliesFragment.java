package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.text.emoji.widget.EmojiButton;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.NotificationWearActivity;
import com.amazmod.service.util.FragmentUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class RepliesFragment extends Fragment implements DelayedConfirmationView.DelayedConfirmationListener {

    LinearLayout repliesContainer, editTextContainer, buttonsContainer;
    BoxInsetLayout rootLayout;
    ScrollView scrollView;
    private DelayedConfirmationView delayedConfirmationView;

    private TextView textView;
    private EditText editText;
    private Button reply, close;

    private String selectedReply, notificationKey, key, mode;
    private boolean enableInvertedTheme, disableDelay;
    private Context mContext;
    private FragmentUtil util;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Log.i(Constants.TAG,"RepliesFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        key = getArguments().getString(NotificationWearActivity.KEY);
        mode = getArguments().getString(NotificationWearActivity.MODE);
        notificationKey = NotificationStore.getCustomNotification(key).getKey();

        Log.d(Constants.TAG,"RepliesFragment onCreate key: " + key);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(Constants.TAG,"RepliesFragment onCreateView");

        return inflater.inflate(R.layout.fragment_replies, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(Constants.TAG,"RepliesFragment onViewCreated");

        updateContent();

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void updateContent(){
        mContext = getActivity();

        Log.d(Constants.TAG,"RepliesFragment updateContent " + notificationKey);

        util = new FragmentUtil(mContext);

        rootLayout = getActivity().findViewById(R.id.fragment_replies_root_layout);
        scrollView = getActivity().findViewById(R.id.fragment_replies_scrollview);
        repliesContainer = getActivity().findViewById(R.id.fragment_replies_replies_container);
        editTextContainer = getActivity().findViewById(R.id.fragment_replies_edittext_container);
        buttonsContainer = getActivity().findViewById(R.id.fragment_replies_buttons_container);
        reply = getActivity().findViewById(R.id.fragment_replies_button_reply);
        close = getActivity().findViewById(R.id.fragment_replies_button_close);
        textView = getActivity().findViewById(R.id.fragment_replies_textview);
        editText = getActivity().findViewById(R.id.fragment_replies_edittext);
        delayedConfirmationView = getActivity().findViewById(R.id.delayedView);
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
        editTextContainer.setVisibility(View.GONE);
        addReplies();

    }

    private void addReplies() {

        List<Reply> repliesList = loadReplies();
        for (final Reply reply : repliesList) {
            EmojiButton button = new EmojiButton(mContext);
            util.setButtonParams(button, reply.getValue());
            util.setButtonTheme(button, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedReply = reply.getValue();
                    sendReply(v);
                }
            });
            repliesContainer.addView(button);
        }
        //Add keyboard button
        EmojiButton buttonkb = new EmojiButton(mContext);
        util.setButtonParams(buttonkb, getResources().getString(R.string.keyboard));
        util.setButtonTheme(buttonkb, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
        buttonkb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repliesContainer.setVisibility(View.GONE);
                editTextContainer.setVisibility(View.VISIBLE);
                ((NotificationWearActivity)getActivity()).stopTimerFinish();

                //Send buttons
                util.setButtonParams(reply, getResources().getString(R.string.send_button), false);
                util.setButtonTheme(reply, enableInvertedTheme ? Constants.BLUE : Constants.GREY);
                reply.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedReply = editText.getText().toString();
                        sendReply(v);
                    }
                });
                //Cancel button
                util.setButtonParams(close, getResources().getString(R.string.close_button), false);
                util.setButtonTheme(close, Constants.RED);
                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editTextContainer.setVisibility(View.GONE);
                        repliesContainer.setVisibility(View.VISIBLE);
                        ((NotificationWearActivity)getActivity()).startTimerFinish();
                    }
                });
            }
        });
        repliesContainer.addView(buttonkb);

    }

    private List<Reply> loadReplies() {
        final String replies = util.listReplies();

        try {
            Type listType = new TypeToken<List<Reply>>() {
            }.getType();
            return new Gson().fromJson(replies, listType);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }


    private void sendReply(View v) {
        ((NotificationWearActivity)getActivity()).stopTimerFinish();
        repliesContainer.setVisibility(View.GONE);
        editTextContainer.setVisibility(View.GONE);

        if (disableDelay) {
            Log.i(Constants.TAG, "RepliesFragment sendReply without delay");
            onTimerFinished(v);

        } else {
            Log.d(Constants.TAG, "RepliesFragment sendReply with delay");
            util.setParamMargins(0, 24, 0, 4);
            textView.setLayoutParams(util.getParams());
            delayedConfirmationView.setVisibility(View.VISIBLE);
            textView.setText(getResources().getString(R.string.sending));
            delayedConfirmationView.setPressed(false);
            delayedConfirmationView.start();
            delayedConfirmationView.setListener(this);
            Log.i(Constants.TAG, "RepliesFragment sendReply isPressed: " + delayedConfirmationView.isPressed());
        }
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        delayedConfirmationView.reset();

        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);

        ((NotificationWearActivity)getActivity()).startTimerFinish();

        textView.setLayoutParams(util.getParams());
        delayedConfirmationView.setVisibility(View.GONE);
        repliesContainer.setVisibility(View.VISIBLE);
        textView.setText(getResources().getString(R.string.reply));
        Log.i(Constants.TAG, "RepliesFragment onTimerSelected isPressed: " + v.isPressed());
    }

    @Override
    public void onTimerFinished(View v) {
        Log.i(Constants.TAG, "RepliesFragment onTimerFinished isPressed: " + v.isPressed());

        if (v instanceof DelayedConfirmationView)
            ((DelayedConfirmationView) v).setListener(null);

        Intent intent = new Intent(mContext, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Reply Sent!");
        startActivity(intent);

        NotificationStore.removeCustomNotification(key);

        if (NotificationWearActivity.MODE_VIEW.equals(mode))
            WearNotificationsFragment.getInstance().loadNotifications();

        HermesEventBus.getDefault().post(new ReplyNotificationEvent(notificationKey, selectedReply));
        getActivity().finish();

    }

    public static RepliesFragment newInstance(String key, String mode) {

        Log.i(Constants.TAG,"RepliesFragment newInstance key: " + key);
        RepliesFragment myFragment = new RepliesFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NotificationWearActivity.KEY, key);
        bundle.putString(NotificationWearActivity.MODE, mode);
        myFragment.setArguments(bundle);

        return myFragment;
    }

}
