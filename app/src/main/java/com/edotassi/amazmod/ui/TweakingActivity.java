package com.edotassi.amazmod.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.support.FirebaseEvents;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

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

                            Bundle bundle = new Bundle();
                            bundle.putInt("value", seekBar.getProgress());
                            FirebaseAnalytics
                                    .getInstance(TweakingActivity.this)
                                    .logEvent(FirebaseEvents.TWEAKING_BRIGHTENESS_CHANGE, bundle);
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

    @OnClick(R.id.activity_tweaking_reboot)
    public void reboot() {
        execCommandInternally(ShellCommandHelper.getReboot());

        FirebaseAnalytics.getInstance(this).logEvent(FirebaseEvents.SHELL_COMMAND_REBOOT, null);
    }

    @OnClick(R.id.activity_tweaking_restart_launcher)
    public void restartLauncher() {
        execCommandInternally(ShellCommandHelper.getForceStopHuamiLauncher());

        FirebaseAnalytics.getInstance(this).logEvent(FirebaseEvents.SHELL_COMMAND_RESTART_LAUNCHER, null);
    }

    @OnClick(R.id.activity_tweaking_enable_apps_list)
    public void enableAppsList() {
        execCommandInternally(ShellCommandHelper.getEnableAppsList());

        FirebaseAnalytics.getInstance(this).logEvent(FirebaseEvents.SHELL_COMMAND_ENABLE_APPS_LIST, null);
    }

    @OnClick(R.id.activity_tweaking_disable_apps_list)
    public void disableAppList() {
        execCommandInternally(ShellCommandHelper.getDisableAppsList());

        FirebaseAnalytics.getInstance(this).logEvent(FirebaseEvents.SHELL_COMMAND_DISABLE_APPS_LIST, null);
    }

    @OnClick(R.id.activity_tweaking_enable_lpm)
    public void enableLpm() {
        new MaterialDialog.Builder(this)
                .title(R.string.enable_low_power)
                .content(R.string.enable_lpm_content)
                .positiveText(R.string.continue_label)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();

                        final SnackProgressBar progressBar = new SnackProgressBar(
                                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending))
                                .setIsIndeterminate(true)
                                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                                    @Override
                                    public void onActionClick() {
                                        snackProgressBarManager.dismissAll();
                                    }
                                });
                        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

                        Watch.get()
                                .enableLowPower()
                                .continueWith(new Continuation<Void, Object>() {
                                    @Override
                                    public Object then(@NonNull Task<Void> task) throws Exception {
                                        SnackProgressBar snackbar;
                                        if (task.isSuccessful()) {
                                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.shell_command_sent));

                                            FirebaseAnalytics
                                                    .getInstance(TweakingActivity.this)
                                                    .logEvent(FirebaseEvents.TWEAKING_ENABLE_LPM, null);
                                        } else {
                                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.cant_send_shell_command));
                                        }

                                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                                        return null;
                                    }
                                });
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @OnClick(R.id.activity_tweaking_revoke_admin)
    public void revokeAdminOwner() {
        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending))
                .setIsIndeterminate(true)
                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                    @Override
                    public void onActionClick() {
                        snackProgressBarManager.dismissAll();
                    }
                });
        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        Watch.get()
                .revokeAdminOwner()
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(@NonNull Task<Void> task) throws Exception {
                        SnackProgressBar snackbar;
                        if (task.isSuccessful()) {
                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.shell_command_sent));

                            FirebaseAnalytics
                                    .getInstance(TweakingActivity.this)
                                    .logEvent(FirebaseEvents.TWEAKING_DISABLE_ADMIN, null);
                        } else {
                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.cant_send_shell_command));
                        }

                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                        return null;
                    }
                });
    }

    @OnClick(R.id.activity_tweaking_exec_command_run)
    public void execCommand() {
        try {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ex) {
            Crashlytics.logException(ex);
        }

        String command = commandEditText.getText().toString();
        execCommandInternally(command);

        FirebaseAnalytics.getInstance(this).logEvent(FirebaseEvents.SHELL_COMMAND_EXECUTED, null);
    }

    private void execCommandInternally(String command) {
        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending))
                .setIsIndeterminate(true)
                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                    @Override
                    public void onActionClick() {
                        snackProgressBarManager.dismissAll();
                    }
                });
        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        Watch.get().executeShellCommand(command).continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(@NonNull Task<Void> task) throws Exception {
                SnackProgressBar snackbar;
                if (task.isSuccessful()) {
                    snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.shell_command_sent));
                } else {
                    snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_send_shell_command));
                }

                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                return null;
            }
        });
    }
}
