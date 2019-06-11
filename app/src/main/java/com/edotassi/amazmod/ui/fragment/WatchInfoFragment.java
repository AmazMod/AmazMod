package com.edotassi.amazmod.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.setup.Setup;
import com.edotassi.amazmod.support.FirebaseEvents;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.update.UpdateDownloader;
import com.edotassi.amazmod.update.Updater;
import com.edotassi.amazmod.util.Permissions;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.pixplicity.easyprefs.library.Prefs;
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
import amazmod.com.transport.data.ResultShellCommandData;
import amazmod.com.transport.data.WatchStatusData;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class WatchInfoFragment extends Card implements Updater {

    private SnackProgressBarManager snackProgressBarManager;

    @BindView(R.id.card_amazmodservice)
    TextView amazModService;
    //@BindView(R.id.card_product_device)
    //TextView productDevice;
    //@BindView(R.id.card_product_manufacter)
    //TextView productManufacter;
    @BindView(R.id.card_product_model)
    TextView productModel;
    @BindView(R.id.card_product_name)
    TextView productName;
    //@BindView(R.id.card_revision)
    //TextView revision;
    @BindView(R.id.card_serialno)
    TextView serialNo;
    //@BindView(R.id.card_build_date)
    //TextView buildDate;
    @BindView(R.id.card_build_description)
    TextView buildDescription;
    @BindView(R.id.card_display_id)
    TextView displayId;
    @BindView(R.id.card_huami_model)
    TextView huamiModel;
    //@BindView(R.id.card_huami_number)
    //TextView huamiNumber;
    //@BindView(R.id.card_build_fingerprint)
    //TextView fingerprint;
    @BindView(R.id.card_watch)
    CardView card;


    @BindView(R.id.isConnectedTV)
    TextView isConnectedTV;
    @BindView(R.id.card_watch_detail)
    LinearLayout watchDetail;
    @BindView(R.id.card_watch_no_service)
    ConstraintLayout noService;
    @BindView(R.id.card_watch_progress)
    MaterialProgressBar watchProgress;

    private long timeLastSync = 0L;
    private static WatchStatus watchStatus;
    private static int serviceVersion;

    private MaterialDialog updateDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_watch_info, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        EventBus.getDefault().register(this);

        if (getActivity() != null) {
            snackProgressBarManager = new SnackProgressBarManager(getActivity().findViewById(android.R.id.content))
                    .setProgressBarColor(R.color.colorAccent)
                    .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                    .setTextSize(14)
                    .setMessageMaxLines(2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        int syncInterval = Integer.valueOf(Prefs.getString(Constants.PREF_BATTERY_BACKGROUND_SYNC_INTERVAL, "60"));
        if (System.currentTimeMillis() - timeLastSync > (syncInterval * 30000L)) {
            connecting();
            timeLastSync = System.currentTimeMillis();

            Watch.get().getStatus().continueWith(new Continuation<WatchStatus, Object>() {
                @Override
                public Object then(@NonNull Task<WatchStatus> task) {
                    if (task.isSuccessful()) {
                        AmazModApplication.setWatchConnected(true);
                        isConnected();
                        watchStatus = task.getResult();
                        refresh(watchStatus);
                        String serviceVersionString = watchStatus.getWatchStatusData().getAmazModServiceVersion();
                        Logger.debug("WatchInfoFragment serviceVersionString: " + serviceVersionString);
                        if (serviceVersionString.contains("_("))
                            serviceVersionString = "1588";
                        serviceVersion = Integer.valueOf(serviceVersionString);
                        Logger.debug("WatchInfoFragment serviceVersion: " + serviceVersion);
                        if (Prefs.getBoolean(Constants.PREF_ENABLE_UPDATE_NOTIFICATION, Constants.PREF_DEFAULT_ENABLE_UPDATE_NOTIFICATION)) {
                            Setup.checkServiceUpdate(WatchInfoFragment.this, serviceVersionString);
                        }
                    } else {
                        Logger.debug("WatchInfoFragment isWatchConnected = false");
                        AmazModApplication.setWatchConnected(false);
                        if (getActivity() != null) {
                            try {
                                Snacky
                                        .builder()
                                        .setActivity(getActivity())
                                        .setText(R.string.failed_load_watch_status)
                                        .setDuration(Snacky.LENGTH_SHORT)
                                        .build()
                                        .show();
                            } catch (Exception e) {
                                Logger.error("WatchInfoFragment onResume exception: " + e.toString());
                            }
                        }
                        disconnected();
                    }
                    return null;
                }
            });
        }
    }

    @Override
    public String getName() {
        return "watch-info";
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(WatchStatus watchStatus) {
        TransportService.model = watchStatus.getWatchStatusData().getRoProductModel();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putString(Constants.PREF_WATCH_MODEL, TransportService.model)
                .putString(Constants.PREF_HUAMI_MODEL, watchStatus.getWatchStatusData().getRoBuildHuamiModel())
                .apply();
        try {
            onWatchStatus(watchStatus);
        } catch (NullPointerException e) {
            Logger.error("WatchInfoFragment refresh exception: {}", e.toString());
        }
    }

    public void onWatchStatus(WatchStatus watchStatus) {
        WatchStatusData watchStatusData = watchStatus.getWatchStatusData();

        amazModService.setText(watchStatusData.getAmazModServiceVersion());
        productModel.setText(watchStatusData.getRoProductModel());
        productName.setText(watchStatusData.getRoProductName());
        huamiModel.setText(watchStatusData.getRoBuildHuamiModel());
        displayId.setText(watchStatusData.getRoBuildDisplayId());
        buildDescription.setText(watchStatusData.getRoBuildDescription());
        serialNo.setText(watchStatusData.getRoSerialno());
        //Removed unused and unnecessary watchData
        //productDevice.setText(watchStatusData.getRoProductDevice());
        //productManufacter.setText(watchStatusData.getRoProductManufacter());
        //revision.setText(watchStatusData.getRoRevision());
        //buildDate.setText(watchStatusData.getRoBuildDate());
        //huamiNumber.setText(watchStatusData.getRoBuildHuamiNumber());
        //fingerprint.setText(watchStatusData.getRoBuildFingerprint());
        //Log the values received from watch brightness
        AmazModApplication.currentScreenBrightness = watchStatusData.getScreenBrightness();
        AmazModApplication.currentScreenBrightnessMode = watchStatusData.getScreenBrightnessMode();
        Logger.debug("WatchInfoFragment WatchData SCREEN_BRIGHTNESS_MODE: " + String.valueOf(AmazModApplication.currentScreenBrightness));
        Logger.debug("WatchInfoFragment WatchData SCREEN_BRIGHTNESS: " + String.valueOf(AmazModApplication.currentScreenBrightness));

        // Heart Rate Bar Chart
        Logger.debug("WatchInfoFragment WatchData HEART RATES: " + watchStatusData.getLastHeartRates());
        String lastHeartRates = watchStatusData.getLastHeartRates();
        try {
            HeartRateChartFragment f = (HeartRateChartFragment) getActivity().getSupportFragmentManager().findFragmentByTag("heart-rate-chart");
            f.updateChart(lastHeartRates);
        }catch(NullPointerException e) {
            // HeartRate fragment card not found!
            e.printStackTrace();
        }
    }

    private void isConnected() {
        isConnectedTV.setTextColor(getResources().getColor(R.color.colorCharging));
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_is_connected)).toUpperCase());
        watchProgress.setVisibility(View.GONE);
        watchDetail.setVisibility(View.VISIBLE);
        noService.setVisibility(View.GONE);
    }

    private void disconnected() {
        isConnectedTV.setTextColor(getResources().getColor(R.color.colorAccent));
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_disconnected)).toUpperCase());
        watchProgress.setVisibility(View.GONE);
        watchDetail.setVisibility(View.GONE);
        noService.setVisibility(View.VISIBLE);
    }

    private void connecting() {
        isConnectedTV.setTextColor(getResources().getColor(R.color.mi_text_color_secondary_light));
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_connecting)).toUpperCase());
        watchDetail.setVisibility(View.GONE);
        watchProgress.setVisibility(View.VISIBLE);
        noService.setVisibility(View.GONE);
    }

    @Override
    public void updateCheckFailed() {
        if (getActivity() != null && getContext() != null) {
            Snacky.builder()
                    .setText(R.string.cant_check_service_updates)
                    .setDuration(Snacky.LENGTH_SHORT)
                    .setActivity(getActivity())
                    .build()
                    .show();
        }
    }

    @Override
    public void updateAvailable(final int version) {
        if (getActivity() != null && getContext() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new MaterialDialog.Builder(getContext())
                            .canceledOnTouchOutside(false)
                            .title(R.string.new_update_available)
                            .content(new StringBuilder(getString(R.string.new_service_update_available, String.valueOf(version)))
                                    .append(".\n")
                                    .append(getString(R.string.update_process_warning))
                                    .toString())
                            .positiveText(R.string.update)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @SuppressLint("DefaultLocale")
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    if (WatchInfoFragment.this.getContext() != null) {

                                        if (!Permissions.checkWriteExternalStoragePermission(getContext(), getActivity())) {
                                            return;
                                        }

                                        FirebaseAnalytics.getInstance(WatchInfoFragment.this.getContext()).logEvent(FirebaseEvents.INSTALL_SERVICE_UPDATE, null);

                                        setWindowFlags(true);
                                        final UpdateDownloader updateDownloader = new UpdateDownloader();
                                        if (serviceVersion < 1697) {
                                            Logger.debug("WatchInfoFragment updateAvailable: " + Constants.SERVICE_UPDATE_SCRIPT_URL);
                                            updateDownloader.start(WatchInfoFragment.this.getContext(), Constants.SERVICE_UPDATE_SCRIPT_URL, WatchInfoFragment.this);
                                        }

                                        String url = String.format(Constants.SERVICE_UPDATE_FILE_URL, version);

                                        Logger.debug("WatchInfoFragment updateAvailable: " + url);
                                        updateDialog = new MaterialDialog.Builder(getContext())
                                                .canceledOnTouchOutside(false)
                                                .title(R.string.download_in_progress)
                                                .customView(R.layout.dialog_update_progress, false)
                                                .negativeText(R.string.cancel)
                                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                        updateDownloader.cancel();
                                                    }
                                                })
                                                .show();

                                        updateDownloader.start(WatchInfoFragment.this.getContext(), url, WatchInfoFragment.this);

                                    }
                                }
                            })
                            .show();
                }
            });
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void updateDownloadProgress(String filename, int progress) {
        if (updateDialog == null) {
            return;
        }

        View view = updateDialog.getCustomView();
        if (view != null) {
            ProgressBar progressBar = view.findViewById(R.id.dialog_update_progress_bar);
            TextView fileNameTextView = view.findViewById(R.id.dialog_update_progress_filename);
            TextView percTextView = view.findViewById(R.id.dialog_update_progress_perc);

            progressBar.setProgress(progress);
            fileNameTextView.setText(filename);
            percTextView.setText(String.format("%d%s", progress, "%"));
        }
    }

    @Override
    public void updateDownloadFailed() {
        if (updateDialog == null) {
            return;
        }

        updateDialog.dismiss();
        updateDialog = null;
        if (getActivity() != null)
            Snacky.builder().setActivity(getActivity()).setText(R.string.download_failed).build().show();
    }

    @Override
    public void updateDownloadCompleted(File updateFile, String filename) {
        if (updateDialog == null) {
            return;
        }

        if (filename.contains("update_service_apk")) {
            uploadUpdate(updateFile, filename);
            return;
        }

        updateDialog.dismiss();
        updateDialog = null;

        uploadUpdate(updateFile, filename);
    }

    private void uploadUpdate(File updateFile, String filename) {
        if ((getActivity() == null) || (getContext() == null)) {
            return;
        }

        final String destPath = "/sdcard/" + filename;
        final long size = updateFile.length();
        final long startedAt = System.currentTimeMillis();

        final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending))
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

        Watch.get().uploadFile(updateFile, destPath, new Watch.OperationProgress() {
            @Override
            public void update(final long duration, final long byteSent, final long remainingTime, final double progress) {
                if (WatchInfoFragment.this.getActivity() != null) {
                    WatchInfoFragment.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Logger.debug("WatchInfoFragment uploadUpdate destPath: " + destPath);

                            String remaingSize = Formatter.formatShortFileSize(WatchInfoFragment.this.getContext(), size - byteSent);
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
            }
        }, cancellationTokenSource.getToken()).continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(@NonNull Task<Void> task) throws Exception {
                snackProgressBarManager.dismissAll();

                if (task.isSuccessful()) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("size", size);
                    bundle.putLong("duration", System.currentTimeMillis() - startedAt);
                    if (WatchInfoFragment.this.getContext() != null) {
                        FirebaseAnalytics
                                .getInstance(WatchInfoFragment.this.getContext())
                                .logEvent(FirebaseEvents.UPLOAD_FILE, bundle);
                    }
                    if (destPath.contains("AmazMod-service")) {
                        installUpdate(destPath);
                    }

                } else {
                    if (task.getException() instanceof CancellationException) {
                        SnackProgressBar snackbar = new SnackProgressBar(
                                SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.file_upload_canceled))
                                .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                    @Override
                                    public void onActionClick() {
                                        snackProgressBarManager.dismissAll();
                                    }
                                });
                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                    } else {
                        SnackProgressBar snackbar = new SnackProgressBar(
                                SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_upload_file))
                                .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                    @Override
                                    public void onActionClick() {
                                        snackProgressBarManager.dismissAll();
                                    }
                                });
                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                    }
                    throw new Exception("watch.getUploadFile Exception");
                }
                return null;
            }
        });
    }

    private void installUpdate(String apkAbsolutePath) {

        //String command = String.format("adb install -r %s&", apkAbsolutePath);

        String command = ShellCommandHelper.getApkInstall(apkAbsolutePath);

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

        Watch.get().executeShellCommand(command, false, false).continueWith(new Continuation<ResultShellCommand, Object>() {
            @Override
            public Object then(@NonNull Task<ResultShellCommand> task) throws Exception {
                snackProgressBarManager.dismissAll();

                if (task.isSuccessful()) {
                    ResultShellCommand resultShellCommand = task.getResult();
                    if (resultShellCommand != null) {
                        ResultShellCommandData resultShellCommandData = resultShellCommand.getResultShellCommandData();

                        if (resultShellCommandData.getResult() == 0) {
                            SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.update_started_watch_reboot_when_update_finish));
                            snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                        } else {
                            SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.shell_command_failed));
                            snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                        }
                    }
                } else {
                    SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_send_shell_command));
                    snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                    throw new Exception("executeShellCommand Exception");
                }
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setWindowFlags(false);
                    }
                }, 8000);

                return null;
            }
        });
    }

    private void setWindowFlags(boolean enable) {
        final int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

        if (getActivity() != null) {
            if (enable) {
                getActivity().getWindow().addFlags(flags);
            } else {
                getActivity().getWindow().clearFlags(flags);
            }
        }
    }
}