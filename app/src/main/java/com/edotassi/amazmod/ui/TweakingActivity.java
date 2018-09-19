package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.greenrobot.eventbus.EventBus;

import amazmod.com.transport.data.BrightnessData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.mateware.snacky.Snacky;

public class TweakingActivity extends AppCompatActivity {

    @BindView(R.id.activity_tweaking_seekbar)
    SeekBar brightnessSeekbar;

    @BindView(R.id.activity_tweaking_exec_command)
    EditText commandEditText;

    private SnackProgressBarManager snackProgressBarManager;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweaking);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            System.out.println("AmazMod TweakingActivity onCreate exception: " + exception.toString());
            //TODO log to crashlitics
        }
        getSupportActionBar().setTitle(R.string.tweaking);

        ButterKnife.bind(this);

        snackProgressBarManager = new SnackProgressBarManager(findViewById(android.R.id.content))
                .setProgressBarColor(R.color.colorAccent)
                .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                .setTextSize(14)
                .setMessageMaxLines(2)
                .setOnDisplayListener(new SnackProgressBarManager.OnDisplayListener() {
                    @Override
                    public void onShown(SnackProgressBar snackProgressBar, int onDisplayId) {
                        // do something
                    }

                    @Override
                    public void onDismissed(SnackProgressBar snackProgressBar, int onDisplayId) {
                        // do something
                    }
                });

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

                Watch.get().setBrightness(brightnessData).continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(@NonNull Task<Void> task) throws Exception {
                        if (task.isSuccessful()) {
                            Snacky.builder()
                                    .setActivity(TweakingActivity.this)
                                    .setText(R.string.brightness_applied)
                                    .setDuration(Snacky.LENGTH_SHORT)
                                    .build().show();
                        } else {
                            Snacky.builder()
                                    .setActivity(TweakingActivity.this)
                                    .setText(R.string.failed_to_set_brightness)
                                    .setDuration(Snacky.LENGTH_SHORT)
                                    .build().show();
                        }
                        return null;
                    }
                });
            }
        });
    }

    @OnClick(R.id.activity_tweaking_exec_command_run)
    public void execCommand() {
        String command = commandEditText.getText().toString();

        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending))
                .setIsIndeterminate(false)
                .setProgressMax(100)
                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                    @Override
                    public void onActionClick() {
                        snackProgressBarManager.dismissAll();
                    }
                })
                .setShowProgressPercentage(true);
        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        Watch.get()
                .executeShellCommand(command)
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(@NonNull Task<Void> task) throws Exception {
                        SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.file_upload_canceled));
                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                        return null;
                    }
                });
    }
}
