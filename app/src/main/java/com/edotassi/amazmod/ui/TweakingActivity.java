package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;
import android.widget.Toast;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.Brightness;

import amazmod.com.transport.data.BrightnessData;
import butterknife.BindView;
import butterknife.ButterKnife;
import xiaofei.library.hermeseventbus.HermesEventBus;

public class TweakingActivity extends AppCompatActivity {

    @BindView(R.id.activity_tweaking_seekbar)
    SeekBar brightnessSeekbar;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweaking);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.tweaking);

        ButterKnife.bind(this);

        brightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                BrightnessData brightnessData = new BrightnessData();
                brightnessData.setLevel(seekBar.getProgress());

                HermesEventBus.getDefault().post(new Brightness(brightnessData));

                Toast.makeText(TweakingActivity.this, "Brightness set to " + seekBar.getProgress(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
