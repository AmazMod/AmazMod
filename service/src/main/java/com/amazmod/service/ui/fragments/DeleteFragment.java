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
import android.widget.ImageView;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.NotificationWearActivity;
import com.amazmod.service.util.FragmentUtil;

public class DeleteFragment extends Fragment implements DelayedConfirmationView.DelayedConfirmationListener {


    BoxInsetLayout rootLayout;
    private DelayedConfirmationView delayedConfirmationView;

    private TextView textView;
    private ImageView imageView;

    private FragmentUtil util;

    private String selectedSilenceTime, notificationKey, key, mode;
    private boolean enableInvertedTheme, disableDelay;
    private Context mContext;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Log.i(Constants.TAG,"DeleteFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        key = getArguments().getString(NotificationWearActivity.KEY);
        mode = getArguments().getString(NotificationWearActivity.MODE);
        notificationKey = NotificationStore.getCustomNotification(key).getKey();

        Log.d(Constants.TAG,"DeleteFragment onCreate key: " + key);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(Constants.TAG,"DeleteFragment onCreateView");
        return inflater.inflate(R.layout.fragment_delete, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(Constants.TAG,"DeleteFragment onViewCreated");
        updateContent();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void updateContent(){
        mContext = getActivity();

        util = new FragmentUtil(mContext);

        Log.d(Constants.TAG,"DeleteFragment updateContent " + notificationKey);

        rootLayout = getActivity().findViewById(R.id.fragment_delete_root_layout);
        textView = getActivity().findViewById(R.id.fragment_delete_textview);
        imageView = getActivity().findViewById(R.id.fragment_delete_imageview);
        delayedConfirmationView = getActivity().findViewById(R.id.fragment_delete_delayedView);
        delayedConfirmationView.setTotalTimeMs(3000);

        //Load preferences
        enableInvertedTheme = util.getInvertedTheme();
        disableDelay = util.getDisableDelay();

        // Set theme and font size
        //Log.d(Constants.TAG, "NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
            textView.setTextColor(getResources().getColor(R.color.black));
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.outline_delete_black_24));
        } else
            imageView.setImageDrawable(getResources().getDrawable(R.drawable.outline_delete_white_24));

        delayedConfirmationView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDeleteCommand(v);
            }
        });

    }


    private void sendDeleteCommand(View v) {

        ((NotificationWearActivity)getActivity()).stopTimerFinish();
        imageView.setVisibility(View.GONE);

        if (disableDelay) {
            Log.i(Constants.TAG, "DeleteFragment sendSilenceCommand without delay");
            onTimerFinished(v);
        } else {
            Log.d(Constants.TAG, "DeleteFragment sendSilenceCommand with delay");
            util.setParamMargins(0, 24, 0, 4);
            delayedConfirmationView.setVisibility(View.VISIBLE);
            delayedConfirmationView.setPressed(false);
            delayedConfirmationView.start();
            delayedConfirmationView.setListener(this);
            Log.i(Constants.TAG, "DeleteFragment sendSilenceCommand isPressed: " + delayedConfirmationView.isPressed());
        }
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        delayedConfirmationView.reset();

        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);

        ((NotificationWearActivity)getActivity()).startTimerFinish();

        delayedConfirmationView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        Log.i(Constants.TAG, "DeleteFragment onTimerSelected isPressed: " + v.isPressed());
    }

    @Override
    public void onTimerFinished(View v) {
        Log.i(Constants.TAG, "DeleteFragment onTimerFinished isPressed: " + v.isPressed());

        if (v instanceof DelayedConfirmationView)
            ((DelayedConfirmationView) v).setListener(null);

        Intent intent = new Intent(mContext, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Deleted!");
        startActivity(intent);

        NotificationStore.removeCustomNotification(key);

        WearNotificationsFragment.getInstance().loadNotifications();

        getActivity().finish();

    }

    public static DeleteFragment newInstance(String key, String mode) {

        Log.i(Constants.TAG,"DeleteFragment newInstance key: " + key);
        DeleteFragment myFragment = new DeleteFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NotificationWearActivity.KEY, key);
        bundle.putString(NotificationWearActivity.MODE, mode);
        myFragment.setArguments(bundle);

        return myFragment;
    }

}
