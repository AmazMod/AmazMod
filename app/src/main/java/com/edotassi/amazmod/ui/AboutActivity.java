package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.edotassi.amazmod.BuildConfig;
import com.edotassi.amazmod.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AboutActivity extends AppCompatActivity {

    @BindView(R.id.activity_about_version)
    TextView version;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);

        version.setText(BuildConfig.VERSION_NAME);
    }
}
