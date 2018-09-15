package com.edotassi.amazmod.ui.fragment;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.ui.MainActivity;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.pixplicity.easyprefs.library.Prefs;

import org.greenrobot.eventbus.EventBus;

import amazmod.com.transport.data.WatchStatusData;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class WatchInfoFragment extends Card {

    @BindView(R.id.card_amazmodservice)
    TextView amazModService;
    @BindView(R.id.card_product_device)
    TextView productDevice;
    @BindView(R.id.card_product_manufacter)
    TextView productManufacter;
    @BindView(R.id.card_product_model)
    TextView productModel;
    @BindView(R.id.card_product_name)
    TextView productName;
    @BindView(R.id.card_revision)
    TextView revision;
    @BindView(R.id.card_serialno)
    TextView serialNo;
    @BindView(R.id.card_build_date)
    TextView buildDate;
    @BindView(R.id.card_build_description)
    TextView buildDescription;
    @BindView(R.id.card_display_id)
    TextView displayId;
    @BindView(R.id.card_huami_model)
    TextView huamiModel;
    @BindView(R.id.card_huami_number)
    TextView huamiNumber;
    @BindView(R.id.card_build_fingerprint)
    TextView fingerprint;

    @BindView(R.id.isConnectedTV)
    TextView isConnectedTV;
    @BindView(R.id.card_watch_detail)
    LinearLayout watchDetail;
    @BindView(R.id.card_watch_progress)
    MaterialProgressBar watchProgress;

    private long timeLastSync = 0L;
    private static WatchStatus watchStatus;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_watch_info, container, false);

        ButterKnife.bind(this, view);

        Log.d(Constants.TAG, "WatchInfoFragment onCreateView");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(Constants.TAG, "WatchInfoFragment onResume");

        connecting();

        int syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_BATTERY_BACKGROUND_SYNC_INTERVAL, "60"));
        if (System.currentTimeMillis() - timeLastSync > (syncInterval * 30000L)) {
            timeLastSync = System.currentTimeMillis();
            Watch.get().getStatus().continueWith(new Continuation<WatchStatus, Object>() {
                @Override
                public Object then(@NonNull Task<WatchStatus> task) throws Exception {
                    if (task.isSuccessful()) {
                        AmazModApplication.isWatchConnected = true;
                        isConnected();
                        watchStatus = task.getResult();
                        refresh(watchStatus);
                    } else {
                        AmazModApplication.isWatchConnected = false;
                        try {
                            Snacky
                                    .builder()
                                    .setActivity(getActivity())
                                    .setText(R.string.failed_load_watch_status)
                                    .setDuration(Snacky.LENGTH_SHORT)
                                    .build()
                                    .show();
                        } catch (Exception e) {
                            Log.e(Constants.TAG, "WatchInfoFragment onResume exception: " + e.toString());
                        }
                        disconnected();
                    }
                    return null;
                }
            });
        } else if (watchStatus != null) {
            isConnected();
            refresh(watchStatus);
        } else disconnected();
    }

    @Override
    public String getName() {
        return "watch-info";
    }

    public void refresh(WatchStatus watchStatus) {
        TransportService.model = watchStatus.getWatchStatusData().getRoProductModel();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putString(Constants.PREF_WATCH_MODEL, TransportService.model)
                .apply();
        try {
            onWatchStatus(watchStatus);

        } catch (NullPointerException e) {
            Log.e(Constants.TAG, "WatchInfoFragment refresh exception: " + e.toString());
        }
    }

    public void onWatchStatus(WatchStatus watchStatus) {
        WatchStatusData watchStatusData = watchStatus.getWatchStatusData();

        amazModService.setText(watchStatusData.getAmazModServiceVersion());
        productDevice.setText(watchStatusData.getRoProductDevice());
        productManufacter.setText(watchStatusData.getRoProductManufacter());
        productModel.setText(watchStatusData.getRoProductModel());
        productName.setText(watchStatusData.getRoProductName());
        revision.setText(watchStatusData.getRoRevision());
        serialNo.setText(watchStatusData.getRoSerialno());
        buildDate.setText(watchStatusData.getRoBuildDate());
        buildDescription.setText(watchStatusData.getRoBuildDescription());
        displayId.setText(watchStatusData.getRoBuildDisplayId());
        huamiModel.setText(watchStatusData.getRoBuildHuamiModel());
        huamiNumber.setText(watchStatusData.getRoBuildHuamiNumber());
        fingerprint.setText(watchStatusData.getRoBuildFingerprint());

    }

    private void isConnected() {
        isConnectedTV.setTextColor(getResources().getColor(R.color.colorCharging));
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_is_connected)).toUpperCase());
        watchProgress.setVisibility(View.GONE);
        watchDetail.setVisibility(View.VISIBLE);
    }

    private void disconnected() {
        isConnectedTV.setTextColor(getResources().getColor(R.color.colorAccent));
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_disconnected)).toUpperCase());
        watchProgress.setVisibility(View.GONE);
        watchDetail.setVisibility(View.GONE);
    }

    private void connecting() {
        isConnectedTV.setTextColor(getResources().getColor(R.color.mi_text_color_secondary_light));
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_connecting)).toUpperCase());
        watchDetail.setVisibility(View.GONE);
        watchProgress.setVisibility(View.VISIBLE);
    }

}