package com.edotassi.amazmodcompanionservice.springboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.edotassi.amazmodcompanionservice.BuildConfig;
import com.edotassi.amazmodcompanionservice.R;
import com.edotassi.amazmodcompanionservice.R2;

import butterknife.BindView;
import butterknife.ButterKnife;
import clc.sliteplugin.flowboard.AbstractPlugin;
import clc.sliteplugin.flowboard.ISpringBoardHostStub;

public class AmazModPage extends AbstractPlugin {

    View view;
    @BindView(R2.id.amazmod_page_version)
    TextView version;

    @Override
    public View getView(Context context) {
        view = LayoutInflater.from(context).inflate(R.layout.amazmod_page, null);

        try {
            ButterKnife.bind(this, view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        version.setText(BuildConfig.VERSION_NAME);

        return view;
    }

    @Override
    public void onBindHost(ISpringBoardHostStub iSpringBoardHostStub) {

    }
}
