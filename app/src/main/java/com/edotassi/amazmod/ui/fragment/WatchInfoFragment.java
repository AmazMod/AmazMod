package com.edotassi.amazmod.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.edotassi.amazmod.AmazModApplication;

import amazmod.com.transport.Constants;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.setup.Setup;
import com.edotassi.amazmod.support.FirebaseEvents;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.ui.FileExplorerActivity;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.update.UpdateDownloader;
import com.edotassi.amazmod.update.Updater;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.pixplicity.easyprefs.library.Prefs;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.CancellationException;

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
    @BindView(R.id.card_watch_no_service)
    ConstraintLayout noService;
    @BindView(R.id.card_watch_progress)
    MaterialProgressBar watchProgress;

    private long timeLastSync = 0L;
    private static WatchStatus watchStatus;

    private MaterialDialog updateDialog;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_watch_info, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        snackProgressBarManager = new SnackProgressBarManager(getActivity().findViewById(android.R.id.content))
                .setProgressBarColor(R.color.colorAccent)
                .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                .setTextSize(14)
                .setMessageMaxLines(2);
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
                public Object then(@NonNull Task<WatchStatus> task) throws Exception {
                    if (task.isSuccessful()) {
                        AmazModApplication.isWatchConnected = true;
                        isConnected();
                        watchStatus = task.getResult();
                        refresh(watchStatus);

                        Setup.checkServiceUpdate(WatchInfoFragment.this, watchStatus.getWatchStatusData().getAmazModServiceVersion());
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
                            Crashlytics.logException(e);
                            Log.e(Constants.TAG, "WatchInfoFragment onResume exception: " + e.toString());
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

    public void refresh(WatchStatus watchStatus) {
        TransportService.model = watchStatus.getWatchStatusData().getRoProductModel();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putString(Constants.PREF_WATCH_MODEL, TransportService.model)
                .apply();
        try {
            onWatchStatus(watchStatus);
        } catch (NullPointerException e) {
            Crashlytics.logException(e);
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
        //Log the values received from watch brightness
        AmazModApplication.currentScreenBrightness = watchStatusData.getScreenBrightness();
        AmazModApplication.currentScreenBrightnessMode = watchStatusData.getScreenBrightnessMode();
        Log.d(Constants.TAG, "WatchData SCREEN_BRIGHTNESS_MODE: " + String.valueOf(AmazModApplication.currentScreenBrightness));
        Log.d(Constants.TAG, "WatchData SCREEN_BRIGHTNESS: " + String.valueOf(AmazModApplication.currentScreenBrightness));

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
        Snacky.builder()
                .setText(R.string.cant_check_service_updates)
                .setDuration(Snacky.LENGTH_SHORT)
                .setActivity(getActivity())
                .build()
                .show();
    }

    @Override
    public void updateAvailable(final int version) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new MaterialDialog.Builder(getContext())
                        .title(R.string.new_update_available)
                        .content(getString(R.string.new_service_update_available, String.valueOf(version)))
                        .positiveText(R.string.update)
                        .negativeText(R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                FirebaseAnalytics.getInstance(WatchInfoFragment.this.getContext()).logEvent(FirebaseEvents.INSTALL_SERVICE_UPDATE, null);

                                String url = String.format(Constants.SERVICE_UPDATE_FILE_URL, version);
                                final UpdateDownloader updateDownloader = new UpdateDownloader();

                                updateDialog = new MaterialDialog.Builder(getContext())
                                        .title(R.string.download_in_progress)
                                        .customView(R.layout.dialog_update_progress, false)
                                        .negativeText(R.string.cancel)
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                if (updateDownloader != null) {
                                                    updateDownloader.cancel();
                                                }
                                            }
                                        })
                                        .show();

                                updateDownloader.start(WatchInfoFragment.this.getContext(), url, WatchInfoFragment.this);
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void updateDownloadProgress(String filename, int progress) {
        if (updateDialog == null) {
            return;
        }

        View view = updateDialog.getCustomView();
        ProgressBar progressBar = view.findViewById(R.id.dialog_update_progress_bar);
        TextView fileNameTextView = view.findViewById(R.id.dialog_update_progress_filename);
        TextView percTextView = view.findViewById(R.id.dialog_update_progress_perc);

        progressBar.setProgress(progress);
        fileNameTextView.setText(filename);
        percTextView.setText(String.format("%d%s", progress, "%"));
    }

    @Override
    public void updateDownloadFailed() {
        if (updateDialog == null) {
            return;
        }

        updateDialog.dismiss();
        updateDialog = null;

        Snacky.builder().setActivity(getActivity()).setText(R.string.download_failed).build().show();
    }

    @Override
    public void updateDownloadCompleted(File updateFile, String filename) {
        if (updateDialog == null) {
            return;
        }

        updateDialog.dismiss();
        updateDialog = null;

        uploadUpdate(updateFile, filename);
    }

    private void uploadUpdate(File updateFile, String filename) {
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
                WatchInfoFragment.this.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
        }, cancellationTokenSource.getToken()).continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(@NonNull Task<Void> task) throws Exception {
                snackProgressBarManager.dismissAll();

                if (task.isSuccessful()) {
                    Bundle bundle = new Bundle();
                    bundle.putLong("size", size);
                    bundle.putLong("duration", System.currentTimeMillis() - startedAt);
                    FirebaseAnalytics
                            .getInstance(WatchInfoFragment.this.getContext())
                            .logEvent(FirebaseEvents.UPLOAD_FILE, bundle);

                    installUpdate(destPath);
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
                }

                return null;
            }
        });
    }

    private void installUpdate(String apkAbosultePath) {
        String command = "adb install -r " + apkAbosultePath;
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

        Watch.get().executeShellCommand(command, false, true).continueWith(new Continuation<ResultShellCommand, Object>() {
            @Override
            public Object then(@NonNull Task<ResultShellCommand> task) throws Exception {
                snackProgressBarManager.dismissAll();

                if (task.isSuccessful()) {
                    ResultShellCommand resultShellCommand = task.getResult();
                    ResultShellCommandData resultShellCommandData = resultShellCommand.getResultShellCommandData();

                    if (resultShellCommandData.getResult() == 0) {
                        SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.update_started_watch_reboot_when_update_finish));
                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                    } else {
                        SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.shell_command_failed));
                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                    }
                } else {
                    SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_send_shell_command));
                    snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                }

                return null;
            }
        });
    }
}