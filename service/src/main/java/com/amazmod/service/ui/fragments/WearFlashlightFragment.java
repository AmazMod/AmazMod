package com.amazmod.service.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.view.BoxInsetLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ScrollView;

import com.amazmod.service.R;

import org.tinylog.Logger;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

public class WearFlashlightFragment extends Fragment {

	BoxInsetLayout mainLayout;
	ScrollView infoLayout;

    private Context mContext;

    private static boolean screenToggle = false;
    private static int screenMode;
    private static int screenBrightness = 999989;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.debug("WearFlashlightFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.debug("WearFlashlightFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.debug("WearFlashlightFragment onCreateView");

        return inflater.inflate(R.layout.activity_wear_info, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.debug("WearFlashlightFragment onViewCreated");

        updateContent();

    }

    @Override
    public void onStart() {
        super.onStart();

    }

	@SuppressLint("ClickableViewAccessibility")
	private void updateContent() {

        mainLayout = getActivity().findViewById(R.id.wear_info_main_layout);
        infoLayout = getActivity().findViewById(R.id.wear_info_scroll_layout);

        infoLayout.setVisibility(View.GONE);

        flashlight();

	}

    @Override
    public void onDestroy() {
        if (screenToggle)
            setMaxBrightness(false);
        Logger.debug("WearFlashlightFragment flashlight off");
        setWindowFlags(false);
	    super.onDestroy();
    }


    private void flashlight() {
        Logger.debug("WearFlashlightFragment flashlight on");
        mainLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
        setMaxBrightness(true);
        setWindowFlags(true);
    }

    private void setMaxBrightness(boolean mode) {

        if (mode) {
            Logger.debug("WearFlashlightFragment setScreenModeOff mode true");
            screenMode = Settings.System.getInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, 0);
            screenBrightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        } else {
            if (screenBrightness != 999989) {
                Logger.debug("WearFlashlightFragment setScreenModeOff mode false \\ screenMode: " + screenMode);
                Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, screenMode);
                Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightness);
            }
        }
        screenToggle = mode;
    }

    public static WearFlashlightFragment newInstance() {
        Logger.debug("WearFlashlightFragment newInstance");
        return new WearFlashlightFragment();
    }

    private void setWindowFlags(boolean enable) {

        final int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

        if (enable) {
            getActivity().getWindow().addFlags(flags);
        } else {
            getActivity().getWindow().clearFlags(flags);
        }
    }
}