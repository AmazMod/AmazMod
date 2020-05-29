package com.edotassi.amazmod.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.OtherData;
import com.edotassi.amazmod.event.RequestFileUpload;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.support.DownloadHelper;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.util.FilesUtil;
import com.edotassi.amazmod.util.Screen;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tinylog.Logger;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.CancellationException;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.FileUploadData;
import amazmod.com.transport.data.ResultShellCommandData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.mateware.snacky.Snacky;

import static android.graphics.Bitmap.CompressFormat.PNG;

public class TweakingActivity extends BaseAppCompatActivity implements Transporter.DataListener{

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
    private Context mContext;
    private Transporter ftpTransporter;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Screen.isDarkTheme() || MainActivity.systemThemeIsDark) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppTheme);
        }

        mContext = this;
        setContentView(R.layout.activity_tweaking);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.tweaking);
        } catch (NullPointerException ex) {
            Logger.error(ex, "TweakingActivity onCreate exception: {}", ex.getMessage());
            //TODO log to crashlitics
        }

        ButterKnife.bind(this);

        snackProgressBarManager = new SnackProgressBarManager(findViewById(android.R.id.content))
                .setProgressBarColor(ThemeHelper.getThemeColorAccentId(this))
                .setActionTextColor(ThemeHelper.getThemeColorAccentId(this))
                .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                .setTextSize(14)
                .setMessageMaxLines(2)
                .setOnDisplayListener(new SnackProgressBarManager.OnDisplayListener() {
                    @Override
                    public void onShown(@NonNull SnackProgressBar snackProgressBar, int onDisplayId) {
                        // do something
                    }

                    @Override
                    public void onDismissed(@NonNull SnackProgressBar snackProgressBar, int onDisplayId) {
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
        if(Screen.isStratos3()) {
            autoBrightnessSwitch.setChecked(false);
            autoBrightnessSwitch.setEnabled(false);
            brightnessSeekbar.setEnabled(true);
            brightnessEditText.setEnabled(true);
            updateBrightnessButton.setEnabled(true);
        } else {
            autoBrightnessSwitch.setChecked(autoBrightness);
            brightnessSeekbar.setEnabled(!autoBrightness);
            brightnessEditText.setEnabled(!autoBrightness);
            updateBrightnessButton.setEnabled(!autoBrightness);
        }
        brightnessSeekbar.setProgress(AmazModApplication.currentScreenBrightness);

        EventBus.getDefault().register(this);

        // Set up FTP transporter listener
        ftpTransporter = Transporter.get(this, "com.huami.wififtp");
        ftpTransporter.addDataListener(this);
        if(!ftpTransporter.isTransportServiceConnected())
            ftpTransporter.connectTransportService();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);

        Logger.debug("FTP: disconnect transporter");
        if(ftpTransporter.isTransportServiceConnected()) {
            ftpTransporter.removeDataListener(this);
            ftpTransporter.disconnectTransportService();
            Logger.debug("FTP: transporter disconnected");
            ftpTransporter = null;
        }

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            System.out.println("D/AmazMod TweakingActivity ORIENTATION PORTRAIT");
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            System.out.println("D/AmazMod TweakingActivity ORIENTATION LANDSCAPE");
        }
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
            int value = Integer.valueOf(textValue);

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
        execCommandInternally(ShellCommandHelper.getReboot(),false);

    }

    @OnClick(R.id.activity_tweaking_restart_launcher)
    public void restartLauncher() {
        execCommandInternally(ShellCommandHelper.getForceStopHuamiLauncher(),false);

    }

    @OnClick(R.id.activity_tweaking_enable_apps_list)
    public void enableAppsList() {
        execCommandInternally(ShellCommandHelper.getEnableAppsList());
    }

    @OnClick(R.id.activity_tweaking_disable_apps_list)
    public void disableAppList() {
        execCommandInternally(ShellCommandHelper.getDisableAppsList());

    }

    @OnClick(R.id.activity_tweaking_reboot_bootloader)
    public void rebootBootloader() {
        execCommandInternally(ShellCommandHelper.getRebootBootloader(),false);

    }

    @OnClick(R.id.activity_tweaking_set_admin)
    public void setAdmin() {
        execCommandInternally(ShellCommandHelper.getDPM());

    }

    @OnClick(R.id.activity_tweaking_screenshot)
    public void screenshot() {
        execCommandInternally(ShellCommandHelper.getScreenshot());

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
                                    public Object then(@NonNull Task<Void> task) {
                                        SnackProgressBar snackbar;
                                        if (task.isSuccessful()) {
                                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.shell_command_sent));

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
                    public Object then(@NonNull Task<Void> task) {
                        SnackProgressBar snackbar;
                        if (task.isSuccessful()) {
                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.shell_command_sent));

                        } else {
                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.cant_send_shell_command));
                        }

                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                        return null;
                    }
                });
    }

    @OnClick(R.id.activity_tweaking_clear_adb)
    public void clearAdb() {
        execCommandInternally(ShellCommandHelper.getClearAdb());
        snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.adb_clear_command_sent)), SnackProgressBarManager.LENGTH_LONG);
    }

    @OnClick(R.id.activity_tweaking_exec_command_run)
    public void execCommand() {
        try {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ex) {
            Logger.error(ex);
        }

        String command = commandEditText.getText().toString();
        execCommandInternally(command);
        FilesExtrasActivity.saveCommandToHistory(command);

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
                Logger.error("Returned from CommandHistoryActivity without selecting any command");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void execCommandInternally(String command) {
        execCommandInternally(command,true);
    }

    private void execCommandInternally(String command, boolean wait) {
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

        Watch.get().executeShellCommand(command, wait, false).continueWith(new Continuation<ResultShellCommand, Object>() {
            @Override
            public Object then(@NonNull Task<ResultShellCommand> task) {

                snackProgressBarManager.dismissAll();
                String snackBarText;

                if (task.isSuccessful()) {
                    ResultShellCommand resultShellCommand = task.getResult();
                    if (resultShellCommand != null) {
                        ResultShellCommandData resultShellCommandData = resultShellCommand.getResultShellCommandData();

                        if (resultShellCommandData.getResult() == 0) {
                            shellResultCodeTextView.setText(String.valueOf(resultShellCommandData.getResult()));
                            shellResultEditText.setText(resultShellCommandData.getOutputLog());
                            snackBarText = "success";

                        } else {
                            shellResultCodeTextView.setText(String.valueOf(resultShellCommandData.getResult()));
                            shellResultEditText.setText(String.format("%s\n%s", resultShellCommandData.getOutputLog(), resultShellCommandData.getErrorLog()));
                            snackBarText = getString(R.string.shell_command_failed);
                        }
                    } else
                        snackBarText = getString(R.string.shell_command_failed);

                } else {
                    shellResultCodeTextView.setText("");
                    shellResultEditText.setText("");
                    snackBarText = getString(R.string.cant_send_shell_command);
                }

                if (!snackBarText.equals("success")) {
                    SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, snackBarText);
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
            public Object then(@NonNull Task<Void> task) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestFileUpload(RequestFileUpload requestFileUpload) {
        final FileUploadData fileUploadData = requestFileUpload.getFileUploadData();
        Logger.debug("TweakingActivity requestFileUpload path: " + fileUploadData.getPath());

        //Toast.makeText(this, "ScreenShot taken\nwait for download", Toast.LENGTH_LONG).show();

        final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.downloading))
                .setIsIndeterminate(false)
                .setProgressMax(100)
                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                    @Override
                    public void onActionClick() {
                        snackProgressBarManager.dismissAll();
                        cancellationTokenSource.cancel();
                    }
                })
                .setShowProgressPercentage(true);
        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        final long size = fileUploadData.getSize();
        final long startedAt = System.currentTimeMillis();

        Watch.get().downloadFile(this, fileUploadData.getPath(), fileUploadData.getName(), size, Constants.MODE_SCREENSHOT,
                new Watch.OperationProgress() {
            @Override
            public void update(final long duration, final long byteSent, final long remainingTime, final double progress) {
                TweakingActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String remaingSize = Formatter.formatShortFileSize(TweakingActivity.this, size - byteSent);
                        double kbSent = byteSent / 1024d;
                        double speed = kbSent / (duration / 1000);
                        DecimalFormat df = new DecimalFormat("#.00");

                        String duration = DurationFormatUtils.formatDuration(remainingTime, "mm:ss", true);
                        String message = getString(R.string.sending) + " - " + duration + " - " + remaingSize + " - " + df.format(speed) + " kb/s";

                        progressBar.setMessage(message);
                        snackProgressBarManager.setProgress((int) progress);
                        snackProgressBarManager.updateTo(progressBar);
                    }
                });
            }
        }, cancellationTokenSource.getToken())
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(@NonNull Task<Void> task) {
                        snackProgressBarManager.dismissAll();
                        if (task.isSuccessful()) {
                            SnackProgressBar snackbar = new SnackProgressBar(
                                    SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.file_downloaded))
                                    .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                        @Override
                                        public void onActionClick() {
                                            snackProgressBarManager.dismissAll();
                                        }
                                    });
                            snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);

                            final File screenshot = new File(DownloadHelper.getDownloadDir(Constants.MODE_SCREENSHOT) + "/" + fileUploadData.getName());

                            if (screenshot.exists()) {
                                Drawable drawable = Drawable.createFromPath(screenshot.getAbsolutePath());
                                if (drawable != null) {

                                    // Rotate and re-save image on Verge
                                    if(Screen.isVerge()){
                                        // Rotate
                                        drawable = FilesUtil.getRotateDrawable(drawable,180f);
                                        // Re-Save (reopen because drawable is bad quality)
                                        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                                        BitmapFactory.Options options=new BitmapFactory.Options();
                                        options.inDensity=dm.densityDpi;
                                        options.inScreenDensity=dm.densityDpi;
                                        options.inTargetDensity=dm.densityDpi;
                                        Bitmap bmp = BitmapFactory.decodeFile(screenshot.getAbsolutePath(),options);
                                        Matrix matrix = new Matrix();
                                        matrix.postRotate(180);
                                        Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                                        if(!FilesUtil.saveBitmapToFile(screenshot,rotatedBitmap, PNG, 100))
                                            Logger.error("Verge's screenshot could not be saved after rotation");
                                    }

                                    new MaterialDialog.Builder(mContext)
                                            .canceledOnTouchOutside(false)
                                            .icon(drawable)
                                            .title("Screenshot")
                                            .positiveText(R.string.open)
                                            .negativeText(R.string.cancel)
                                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                    final Intent intent = new Intent(Intent.ACTION_VIEW)//
                                                            .setDataAndType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                                                                            FileProvider.getUriForFile(mContext, Constants.FILE_PROVIDER, screenshot)
                                                                            : Uri.fromFile(screenshot), "image/*")
                                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                                    startActivity(intent);
                                                }
                                            })
                                            .show();
                                }
                            }

                        } else {
                            if (task.getException() instanceof CancellationException) {
                                SnackProgressBar snackbar = new SnackProgressBar(
                                        SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.file_download_canceled))
                                        .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                            @Override
                                            public void onActionClick() {
                                                snackProgressBarManager.dismissAll();
                                            }
                                        });
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                            } else {
                                SnackProgressBar snackbar = new SnackProgressBar(
                                        SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_download_file))
                                        .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                            @Override
                                            public void onActionClick() {
                                                snackProgressBarManager.dismissAll();
                                            }
                                        });
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                            }
                        }
                        return null;
                    }
                });
    }

    @OnClick(R.id.activity_tweaking_wifi_ap_on)
    public void enable_wifi_ftp() {
        wifi_ftp_toggle(1, 3);
    }

    @OnClick(R.id.activity_tweaking_wifi_ap_off)
    public void disable_wifi_ftp() {
        wifi_ftp_toggle(0, 3);
    }

    @OnClick(R.id.activity_tweaking_ftp_on)
    public void enable_ftp() {
        wifi_ftp_toggle(3, 1);
    }

    @OnClick(R.id.activity_tweaking_ftp_off)
    public void disable_ftp() {
        wifi_ftp_toggle(3, 0);
    }

    String SSID = "huami-amazfit-amazmod-4E68";
    String pswd = "12345678";
    String defaultFTPip = "192.168.43.1";
    String defaultPort = "5210";
    public void wifi_ftp_toggle(int wifi, int ftp) {
        // 0: off, 1: on, 3: do nothing
        String message = getString(R.string.error);
        if(ftpTransporter.isTransportServiceConnected()) {
            // Toggle WiFi AP
            if(wifi == 0)
                ftpTransporter.send("disable_ap");
            else if(wifi == 1){
                ftpTransporter.send("start_service");
                DataBundle dataBundle = new DataBundle();
                dataBundle.putInt("key_keymgmt", 4); // WPA2
                dataBundle.putString("key_ssid", SSID);
                dataBundle.putString("key_pswd", pswd);
                // Enable watch WiFi AP / FTP
                ftpTransporter.send("enable_ap", dataBundle);
            }
            // Toggle FTP
            if(ftp == 0)
                ftpTransporter.send("disable_ftp");
            else if(ftp == 1)
                ftpTransporter.send("enable_ftp");

            // Toast message
            message = ( (wifi<3) ? ( (wifi==0) ? getString(R.string.wifi_ap_dissabling) : getString(R.string.wifi_ap_enabling) ) + ((ftp<3)?"\n":"") : "" ) + ((ftp<3)? ( (ftp==0) ? getString(R.string.ftp_dissabling) : getString(R.string.ftp_enabling) ) :"");
        }
        // Message
        snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, message), SnackProgressBarManager.LENGTH_LONG);
    }

    String TAG = "Tweak-menu-FTP: ";
    public void onDataReceived(TransportDataItem item) {
        // Transmitted action
        String action = item.getAction();

        // Get key_new_state
        DataBundle data = item.getData();
        int key_new_state;
        if (data != null)
            key_new_state = data.getInt("key_new_state");
        else {
            Logger.debug(TAG+"transporter action: "+action+" (without key_new_state)");
            return;
        }

        if ("on_ap_state_changed".equals(action)) {
            // Watch WiFi AP status changed
            if (key_new_state != 13 ){
                if(data.getInt("key_new_state") == 11) {
                    Logger.debug(TAG + "watch's WiFi AP disabled");
                    snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, "WiFi Access Point " + getString(R.string.disabled)), SnackProgressBarManager.LENGTH_SHORT);
                }else
                    Logger.debug(TAG+"on_ap_state_changed: " + key_new_state);
                return;
            }

            // (State 13 watch WiFi AP is on)
            Logger.debug(TAG+"watch's WiFi AP is enabled");
            // WiFi AP enabled.
            snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, "WiFi    : "+SSID+"\nPassword: "+pswd)
                    .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                @Override
                public void onActionClick() {
                    snackProgressBarManager.dismissAll();
                }
            }), SnackProgressBarManager.LENGTH_INDEFINITE);
        } else if ("ftp_on_state_changed".equals(action)) {
            if (key_new_state != 2 ){
                if(key_new_state == 1) {
                    Logger.debug(TAG + "FTP server disabled");
                    snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, "FTP server " + getString(R.string.disabled)), SnackProgressBarManager.LENGTH_SHORT);
                }else
                    Logger.debug(TAG+"ftp_on_state_changed: "+ key_new_state);

                return;
            }

            // FTP enabled
            Logger.debug(TAG+"FTP server enabled.");
            getWatchLocalIP(true);

        }else if("on_ap_enable_result".equals(action)){
            if(key_new_state == 1)
                Logger.debug(TAG+"watch WiFi AP enabled successfully");
            else
                Logger.debug(TAG+"on_ap_enable_result (key_new_state = "+key_new_state+")");
        }else{
            Logger.debug(TAG+"transporter action: "+action+" (key_new_state = "+key_new_state+")");
        }
    }

    public void getWatchLocalIP(){
        getWatchLocalIP(false);
    }

    public void getWatchLocalIP(boolean ftp){
        // Get watch's local IP
        Watch.get().sendSimpleData(Transport.LOCAL_IP,null).continueWith(new Continuation<OtherData, Object>() {
            @Override
            public Object then(@NonNull Task<OtherData> task) {
                String message = getString(R.string.error);
                if (task.isSuccessful()) {
                    OtherData returnedData = task.getResult();
                    try {
                        if (returnedData == null)
                            throw new NullPointerException("Returned data are null");

                        DataBundle otherData = returnedData.getOtherData();

                        String localIP = otherData.getString("ip");
                        if (ftp) {
                            if (localIP.equals("N/A"))
                                localIP = defaultFTPip;
                            localIP = localIP + ":" + defaultPort;
                            message = "FTP server " + getString(R.string.enabled) + ".\n" + getString(R.string.local_ip)+": " + localIP;
                        }else{
                            if (localIP.equals("N/A"))
                                message = getString(R.string.watch_no_wifi);
                            else if(localIP.equals(defaultFTPip))
                                message = getString(R.string.local_ip)+": " + localIP + " (localhost)";
                            else
                                message = getString(R.string.local_ip)+": " + localIP;
                        }
                        Logger.debug(TAG+"watch local IP is " + localIP);
                        //Toast.makeText(mContext, "Watch's local IP is " + localIP, Toast.LENGTH_SHORT).show();
                    }catch(Exception e){
                        Logger.debug(TAG+"failed reading IP data: "+e);
                    }
                } else {
                    Logger.error(task.getException(), "Task sendSimpleData action \"local_ip\" failed");
                }

                // Show notification
                SnackProgressBar snackbar = new SnackProgressBar(
                        SnackProgressBar.TYPE_HORIZONTAL, message)
                        .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                            @Override
                            public void onActionClick() {
                                snackProgressBarManager.dismissAll();
                            }
                        });
                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_INDEFINITE);
                return null;
            }
        });
    }

    @OnClick(R.id.activity_tweaking_watch_local_ip)
    public void watch_local_IP() {
        getWatchLocalIP();
    }
}
