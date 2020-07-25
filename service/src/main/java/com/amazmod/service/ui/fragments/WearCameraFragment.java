package com.amazmod.service.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.amazmod.service.AmazModService;
import com.amazmod.service.MainService;
import com.amazmod.service.R;
import com.amazmod.service.util.ButtonListener;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import org.tinylog.Logger;

import java.util.ArrayList;

import amazmod.com.transport.Transport;

import static com.amazmod.service.util.SystemProperties.isPace;
import static com.amazmod.service.util.SystemProperties.isStratos;
import static com.amazmod.service.util.SystemProperties.isStratos3;
import static com.amazmod.service.util.SystemProperties.isVerge;

public class WearCameraFragment extends Fragment {

    private static final ArrayList<Integer> delays = new ArrayList<Integer>(){{
        add(0);
        add(3);
        add(10);
    }};

    private Transporter transporter;
    private Context mContext;
    private View mView;
    private Button takepict, changedelay;
    private int currDelay = -1;
    private boolean onForeground;
    private ButtonListener btnListener = new ButtonListener();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.info("WearAppsFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupBtnListener();
        onForeground = true;
        Logger.info("WearAppsFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.info("WearAppsFragment onCreateView");
        mView = inflater.inflate(R.layout.fragment_wear_camera, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.info("WearCameraFragment onViewCreated");
        init(); //initialize
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onForeground = false;
        btnListener.stop();
    }

    private void init(){
        takepict = mView.findViewById(R.id.camera_takepict);
        changedelay = mView.findViewById(R.id.camera_changedelay);
        transporter = TransporterClassic.get(mContext, Transport.NAME);
        updateDelay();

        takepict.setOnClickListener(v -> takePicture());
        changedelay.setOnClickListener(v -> updateDelay());
    }

    private void takePicture(){
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        int finalDelay = currDelay * 1000;
        if(isStratos3() && finalDelay >= 1000)
            finalDelay -= 1000; //Push 1s before on S3 due to BT delay
        new Handler().postDelayed(() -> transporter.send(Transport.TAKE_PICTURE), finalDelay + 10);
    }

    private void updateDelay(){
        //If -1, set to -1 and get increased to 0s, else set current value and increase to next/first value
        int currIndex = (currDelay == -1) ? currDelay : delays.indexOf(currDelay);
        int newIndex = currIndex == (delays.size() - 1) ? 0 : currIndex + 1;
        currDelay = delays.get(newIndex);
        if (onForeground) {
            changedelay.setText(getResources().getString(R.string.camera_delay) + ": " + currDelay + "s");
        }
    }

    private void setupBtnListener(){
        Handler btnHandler = new Handler();
        btnListener.start(mContext, keyEvent -> {
            if((isPace() || isVerge() || isStratos()) && keyEvent.getCode() == ButtonListener.KEY_CENTER) {
                if (keyEvent.isLongPress() && onForeground)
                    btnHandler.post(this::updateDelay);
                else
                    btnHandler.post(this::takePicture);
            } else if(isStratos3())
                if(keyEvent.getCode() == ButtonListener.S3_KEY_UP && onForeground)
                    btnHandler.post(this::takePicture);
                else if((keyEvent.getCode() == ButtonListener.S3_KEY_MIDDLE_UP
                        || keyEvent.getCode() == ButtonListener.S3_KEY_MIDDLE_DOWN) && onForeground)
                    btnHandler.post(this::updateDelay);
        });
    }

    public static WearCameraFragment newInstance() {
        Logger.info("WearCameraFragment newInstance");
        return new WearCameraFragment();
    }
}
