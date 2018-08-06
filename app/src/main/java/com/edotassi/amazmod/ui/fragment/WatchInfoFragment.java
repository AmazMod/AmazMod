package com.edotassi.amazmod.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.RequestWatchStatus;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.event.local.Connected;
import com.edotassi.amazmod.event.local.Disconnected;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.TimeUnit;

import amazmod.com.transport.data.WatchStatusData;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class WatchInfoFragment extends Fragment {

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

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_watch_info, container, false);

        ButterKnife.bind(this, view);

        HermesEventBus.getDefault().register(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        watchDetail.setVisibility(View.GONE);
        watchProgress.setVisibility(View.VISIBLE);

        Flowable
                .timer(2000, TimeUnit.MILLISECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        HermesEventBus.getDefault().post(new RequestWatchStatus());
                    }
                });

        //isConnectedTV.setTextColor(batteryTv.getCurrentTextColor());
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_connecting)).toUpperCase());
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

        watchProgress.setVisibility(View.GONE);
        watchDetail.setVisibility(View.VISIBLE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    private void onConnected(Connected connected) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    private void onDisconnected(Disconnected disconnected) {
        isConnectedTV.setTextColor(getResources().getColor(R.color.colorAccent));
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_disconnected)).toUpperCase());
        watchProgress.setVisibility(View.GONE);

        watchDetail.setVisibility(View.GONE);
        Log.d(Constants.TAG, "MainActivity getTransportStatus isTransportConnected: false");
    }
}