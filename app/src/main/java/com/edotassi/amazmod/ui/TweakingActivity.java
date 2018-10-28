package com.edotassi.amazmod.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.CommandHistoryEntity;
import com.edotassi.amazmod.db.model.CommandHistoryEntity_Table;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.support.FirebaseEvents;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.ResultShellCommandData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.mateware.snacky.Snacky;

public class TweakingActivity extends AppCompatActivity {

    @BindView(R.id.activity_tweaking_seekbar)
    SeekBar brightnessSeekbar;

    @BindView(R.id.activity_tweaking_exec_command)
    EditText commandEditText;

    @BindView(R.id.activity_tweaking_brightness_value)
    EditText brightnessEditText;

    @BindView(R.id.activity_tweaking_shell_result_code)
    TextView shellResultCodeTextView;

    @BindView(R.id.activity_tweaking_shell_result)
    TextView shellResultEditText;

    @BindView(R.id.activity_tweaking_button_update_brightness)
    Button updateBrightnessButton;

    @BindView(R.id.activity_tweaking_switcht_auto_brightness)
    Switch autoBrightnessSwitch;

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
                brightnessEditText.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //brightnessEditText.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //updateBrightness(seekBar.getProgress());
            }
        });
        boolean autoBrightness = (AmazModApplication.currentScreenBrightnessMode == Constants.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        autoBrightnessSwitch.setChecked(autoBrightness);
        brightnessSeekbar.setEnabled(!autoBrightness);
        brightnessEditText.setEnabled(!autoBrightness);
        updateBrightnessButton.setEnabled(!autoBrightness);
        brightnessSeekbar.setProgress(AmazModApplication.currentScreenBrightness);
    }

    @OnClick(R.id.activity_tweaking_switcht_auto_brightness)
    public void changeAutoBrightness() {
        boolean autoBrightness = autoBrightnessSwitch.isChecked();
        brightnessSeekbar.setEnabled(!autoBrightness);
        brightnessEditText.setEnabled(!autoBrightness);
        updateBrightnessButton.setEnabled(!autoBrightness);
        if (autoBrightness){
            updateBrightness(Constants.SCREEN_BRIGHTNESS_VALUE_AUTO);
        }
    }

    @OnClick(R.id.activity_tweaking_button_update_brightness)
    public void updateBrightness() {
        try {
            String textValue = brightnessEditText.getText().toString();
            Integer value = Integer.valueOf(textValue);

            if ((value < 1) || (value > 255)) {
                Snacky.builder()
                        .setActivity(this)
                        .setText(R.string.brightness_bad_value_entered)
                        .build()
                        .show();
            } else {
                updateBrightness(value);
            }
        } catch (Exception ex) {
            Snacky.builder()
                    .setActivity(this)
                    .setText(R.string.brightness_bad_value_entered)
                    .build()
                    .show();
        }
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

    @OnClick(R.id.activity_tweaking_reboot_bootloader)
    public void rebootBootloader() {
        execCommandInternally(ShellCommandHelper.getRebootBootloader());

        FirebaseAnalytics.getInstance(this).logEvent(FirebaseEvents.SHELL_COMMAND_REBOOT_BOOTLOADER, null);
    }

    @OnClick(R.id.activity_tweaking_set_admin)
    public void setAdmin() {
        execCommandInternally(ShellCommandHelper.getDPM());

        FirebaseAnalytics.getInstance(this).logEvent(FirebaseEvents.SHELL_COMMAND_ENABLE_ADMIN, null);
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
        saveCommandToHistory(command);

        FirebaseAnalytics.getInstance(this).logEvent(FirebaseEvents.SHELL_COMMAND_EXECUTED, null);
    }

    public final static int REQ_CODE_COMMAND_HISTORY = 100;

    @OnClick(R.id.activity_tweaking_command_history)
    public void loadCommandHistory() {
        Intent child = new Intent(this, CommandHistoryActivity.class);
        startActivityForResult(child, REQ_CODE_COMMAND_HISTORY);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE_COMMAND_HISTORY) {
            try {
                String command = data.getExtras().getString("COMMAND");
                commandEditText.setText(command);
            } catch (NullPointerException e) {
                System.out.println("Returned from CommandHistoryActivity without selecting any command");
            }
        }
    }

    private void saveCommandToHistory(String command) {
        CommandHistoryEntity previousSameCommand = SQLite
                .select()
                .from(CommandHistoryEntity.class)
                .where(CommandHistoryEntity_Table.command.eq(command))
                .querySingle();

        if (previousSameCommand != null) {
            previousSameCommand.setDate(System.currentTimeMillis());
            FlowManager
                    .getModelAdapter(CommandHistoryEntity.class)
                    .update(previousSameCommand);
        } else {
            CommandHistoryEntity commandHistoryEntity = new CommandHistoryEntity();
            commandHistoryEntity.setCommand(command);
            commandHistoryEntity.setDate(System.currentTimeMillis());

            FlowManager
                    .getModelAdapter(CommandHistoryEntity.class)
                    .insert(commandHistoryEntity);
        }
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

        Watch.get().executeShellCommand(command,true,false).continueWith(new Continuation<ResultShellCommand, Object>() {
            @Override
            public Object then(@NonNull Task<ResultShellCommand> task) throws Exception {

                snackProgressBarManager.dismissAll();

                if (task.isSuccessful()) {
                    ResultShellCommand resultShellCommand = task.getResult();
                    ResultShellCommandData resultShellCommandData = resultShellCommand.getResultShellCommandData();

                    if (resultShellCommandData.getResult() == 0) {

                        shellResultCodeTextView.setText(String.valueOf(resultShellCommandData.getResult()));
                        shellResultEditText.setText(resultShellCommandData.getOutputLog());
                    } else {
                        SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.shell_command_failed));

                        shellResultCodeTextView.setText(String.valueOf(resultShellCommandData.getResult()));
                        shellResultEditText.setText(resultShellCommandData.getOutputLog() + "\n" + resultShellCommandData.getErrorLog());

                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                    }
                } else {
                    SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_send_shell_command));

                    shellResultCodeTextView.setText("");
                    shellResultEditText.setText("");

                    snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                }

                return null;
            }
        });
    }

    private void updateBrightness(final int value) {
        BrightnessData brightnessData = new BrightnessData();
        brightnessData.setLevel(value);
        brightnessSeekbar.setProgress(value);

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
                    bundle.putInt("value", value);
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
}
