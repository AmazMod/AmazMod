package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.settings.SettingsManager;

import java.util.List;

import amazmod.com.transport.data.NotificationData;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

public class InputhMethodFragment extends Fragment {

    TextView title, time, text;
    ImageView icon, image;

    private Context mContext;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Log.i(Constants.TAG,"InputhMethodFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //notificationSpec = NotificationData.fromBundle(getArguments());
        Log.i(Constants.TAG,"InputhMethodFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(Constants.TAG,"InputhMethodFragment onCreateView");

        return inflater.inflate(R.layout.activity_radiobutton, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(Constants.TAG,"InputhMethodFragment onViewCreated");

        updateContent();

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void updateContent(){

        title = getActivity().findViewById(R.id.radio_textView01);
        title.setText(getResources().getString(R.string.input_methods));

        RadioGroup radioGroup = (RadioGroup) getActivity().findViewById(R.id.radio_group);

        InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            final List<InputMethodInfo> inputMethodInfos = inputMethodManager.getInputMethodList();

            for (InputMethodInfo inputMethodInfo : inputMethodInfos) {
                Log.i(Constants.TAG,"InputhMethodFragment clearPackage inputMethodInfo getID: " + inputMethodInfo.getId()
                        + " getPackageName: " + inputMethodInfo.getPackageName()
                        + " getServiceName: " + inputMethodInfo.getServiceName()
                        + " getSettingsActivity: " + inputMethodInfo.getSettingsActivity());

                    RadioButton radioButton = new RadioButton(getActivity());
                    radioButton.setText(inputMethodInfo.loadLabel(mContext.getPackageManager()).toString());
                    radioGroup.addView(radioButton);

                    if (inputMethodInfo.getId().equals(Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD)))
                        radioButton.setChecked(true);
            }

            List<InputMethodInfo> mInputMethodProperties = inputMethodManager.getEnabledInputMethodList();
            for (InputMethodInfo inputMethodInfo : mInputMethodProperties) {
                Log.i(Constants.TAG,"InputhMethodFragment clearPackage EnabledInputMethodInfo getID: " + inputMethodInfo.getId()
                        + " getPackageName: " + inputMethodInfo.getPackageName()
                        + " getServiceName: " + inputMethodInfo.getServiceName()
                        + " getSettingsActivity: " + inputMethodInfo.getSettingsActivity());
            }
            Log.i(Constants.TAG,"InputhMethodFragment clearPackage DEFAULT_INPUT_METHOD: "
                    + Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD));

            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    int childCount = group.getChildCount();
                    for (int x = 0; x < childCount; x++) {
                        RadioButton btn = (RadioButton) group.getChildAt(x);
                        if (btn.getId() == checkedId) {
                            Log.i(Constants.TAG,"InputhMethodFragment selected RadioButton: " + btn.getText().toString() + " x: " + x);
                            runCommand("adb shell ime enable " + inputMethodInfos.get(x).getId());
                            runCommand("adb shell ime set " + inputMethodInfos.get(x).getId());
                        }
                    }
                }
            });

        }

    }

    private void runCommand(String command) {
        Log.d(Constants.TAG, "InputhMethodFragment runCommand: " + command);
        if (!command.isEmpty()) {
            try {
                Runtime.getRuntime().exec(command);
            } catch (Exception e) {
                Log.e(Constants.TAG, "InputhMethodFragment runCommand exception: " + e.toString());
            }
        }
    }

    public static InputhMethodFragment newInstance() {
        Log.i(Constants.TAG,"InputhMethodFragment newInstance");
        return new InputhMethodFragment();
    }

}
