package com.edotassi.amazmod.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
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
import androidx.preference.PreferenceManager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.OtherData;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.setup.Setup;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.ui.SettingsActivity;
import com.edotassi.amazmod.ui.card.Card;
import com.edotassi.amazmod.update.UpdateDownloader;
import com.edotassi.amazmod.update.Updater;
import com.edotassi.amazmod.util.Permissions;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.huami.watch.transport.DataBundle;
import com.pixplicity.easyprefs.library.Prefs;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.concurrent.CancellationException;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.ResultShellCommandData;
import amazmod.com.transport.data.WatchStatusData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnLongClick;
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static amazmod.com.transport.Transport.OFFICIAL_REPLY_DEVICE_INFO;
import static amazmod.com.transport.Transport.OFFICIAL_REQUEST_DEVICE_INFO;
import static com.edotassi.amazmod.util.Screen.getModelName;
import static com.edotassi.amazmod.util.Screen.getSerialByModelNo;
import static com.edotassi.amazmod.util.Screen.getWatchInfoBySerialNo;

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
    //@BindView(R.id.card_product_name)
    //TextView productName;
    //@BindView(R.id.card_revision)
    //TextView revision;
    @BindView(R.id.card_serialno)
    TextView serialNo;
    //@BindView(R.id.card_build_date)
    //TextView buildDate;
    //@BindView(R.id.card_build_description)
    //TextView buildDescription;
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
                    .setProgressBarColor(ThemeHelper.getThemeColorAccentId(getActivity()))
                    .setActionTextColor(ThemeHelper.getThemeColorAccentId(getActivity()))
                    .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                    .setTextSize(14)
                    .setMessageMaxLines(2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        int syncInterval = Integer.parseInt(Prefs.getString(Constants.PREF_BATTERY_BACKGROUND_SYNC_INTERVAL, "60"));
        if (System.currentTimeMillis() - timeLastSync > (syncInterval * 30000L)) {
            connecting();
            timeLastSync = System.currentTimeMillis();

            // Try to connect to Amazmod service on watch
            Watch.get().getStatus().continueWith(new Continuation<WatchStatus, Object>() {
                @Override
                public Object then(@NonNull Task<WatchStatus> task) {
                    if (task.isSuccessful()) {
                        // Successful reply from service
                        AmazModApplication.setWatchConnected(true);
                        connected();
                        watchStatus = task.getResult();
                        refresh(watchStatus);
                        String serviceVersionString = watchStatus.getWatchStatusData().getAmazModServiceVersion();
                        Logger.debug("WatchInfoFragment serviceVersionString: " + serviceVersionString);

                        serviceVersion = Integer.parseInt(serviceVersionString.split("-")[0]);
                        Logger.debug("WatchInfoFragment serviceVersion: " + serviceVersion);
                        if (Prefs.getBoolean(Constants.PREF_ENABLE_UPDATE_NOTIFICATION, Constants.PREF_DEFAULT_ENABLE_UPDATE_NOTIFICATION)) {
                            Logger.debug("Checking for OTA updates");
                            Setup.checkServiceUpdate(WatchInfoFragment.this, serviceVersion);
                        }else{
                            Logger.debug("OTA update check disabled");
                        }
                    } else {
                        // No reply from service
                        Logger.debug("WatchInfoFragment isWatchConnected = false");
                        connecting(false);

                        // Try to connect to the watch through the official API
                        Watch.get().sendSimpleData(OFFICIAL_REQUEST_DEVICE_INFO, OFFICIAL_REPLY_DEVICE_INFO,TransportService.TRANSPORT_COMPANION).continueWith(new Continuation<OtherData, Object>() {
                            @Override
                            public Object then(@NonNull Task<OtherData> task) {
                                if (task.isSuccessful()) {
                                    // Successful reply from official API
                                    AmazModApplication.setWatchConnected(true);
                                    connected();

                                    OtherData returnedData = task.getResult();
                                    try {
                                        if (returnedData == null)
                                            throw new NullPointerException("Returned data are null");

                                        DataBundle otherData = returnedData.getOtherData();
                                        JSONObject jSONObject = new JSONObject(otherData.getString("DeviceInfo"));
                                        // Response example
                                        // "AndroidDID":"xxx", "CPUID":"xxx", "LANGUAGE":"en_US", "REGION":"US", "BUILDTYPE":"user", "SN":"xxx", "IS_BOUND":true, "IS_OVERSEA_EDITION":false, "Model":"A1609", "BuildNum":0, "IsExperienceMode":false
                                        Logger.debug("Returned data: " + jSONObject.toString());

                                        // Get watch info
                                        WatchStatusData watchStatusData = new WatchStatusData();
                                        watchStatusData.setAmazModServiceVersion("No service found");
                                        watchStatusData.setRoBuildDescription("N/A");
                                        watchStatusData.setRoBuildDisplayId("N/A");
                                        watchStatusData.setRoBuildHuamiModel("N/A");
                                        watchStatusData.setRoProductName("N/A");
                                        // Get model
                                        if (jSONObject.has("Model")) {
                                            String model = jSONObject.getString("Model");
                                            watchStatusData.setRoBuildHuamiModel(model);
                                            watchStatusData.setRoProductModel( getModelName(model) );
                                        }else{
                                            watchStatusData.setRoBuildHuamiModel("N/A");
                                            watchStatusData.setRoProductModel("N/A");
                                        }
                                        // Get SN
                                        if (jSONObject.has("SN"))
                                            watchStatusData.setRoSerialno(jSONObject.getString("SN"));
                                        else
                                            watchStatusData.setRoSerialno("N/A");

                                        watchStatus = new WatchStatus(watchStatusData.toDataBundle());
                                        refresh(watchStatus);
                                    }catch(Exception e){
                                        Logger.debug("Failed to read official device data: "+e);
                                    }
                                }else{
                                    Logger.debug("Could not get official device info");
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
        WatchStatusData watchStatusData = watchStatus.getWatchStatusData();

        // Tweak returned data
        if ( !watchStatusData.getRoSerialno().equals("N/A") && !watchStatusData.getRoSerialno().isEmpty() ){
            // Serial number found
            Logger.debug("Get model code name & name based on serial number" );
            // Get model code name & name based on serial number (no need for those data + correct name for Pace with Hybrid ROM)
            String[] watchInfo = getWatchInfoBySerialNo( watchStatusData.getRoSerialno() ); // = {Model No, Model Name}
            watchStatusData.setRoBuildHuamiModel( watchInfo[0] );
            watchStatusData.setRoProductModel( watchInfo[1] );
        }else if ( !watchStatusData.getRoBuildHuamiModel().equals("N/A") && !watchStatusData.getRoBuildHuamiModel().isEmpty() ){
            Logger.debug("Get serial & name based on name code");
            // Get part of the serial number based on model code
            watchStatusData.setRoSerialno( getSerialByModelNo(watchStatusData.getRoBuildHuamiModel()) );
            // Get watch name based on code name
            watchStatusData.setRoProductModel( getModelName(watchStatusData.getRoBuildHuamiModel()) );
        }

        Logger.debug("Model code name & name: {}, {} ", watchStatusData.getRoBuildHuamiModel(), watchStatusData.getRoProductModel());

        // Update watch name on the persistent notification
        TransportService.model = watchStatusData.getRoProductModel();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putString(Constants.PREF_WATCH_MODEL, TransportService.model)
                .putString(Constants.PREF_HUAMI_MODEL, watchStatusData.getRoBuildHuamiModel())
                .apply();

        // Update watch info card values
        try {
            onWatchStatus(watchStatusData);
        } catch (NullPointerException e) {
            Logger.error("WatchInfoFragment refresh exception: {}", e.toString());
        }
    }

    public void onWatchStatus(WatchStatusData watchStatusData) {
        // Populate watch info card

        // Amazmod service version and root status
        String amazModServiceVersion = watchStatusData.getAmazModServiceVersion() + ((watchStatusData.getRooted()==1)?" (rooted)":"");
        amazModService.setText(amazModServiceVersion);
        // Serial number
        serialNo.setText( watchStatusData.getRoSerialno() );
        // Watch model code
        huamiModel.setText( watchStatusData.getRoBuildHuamiModel() );
        // Watch model Name
        productModel.setText( watchStatusData.getRoProductModel() );
        // Firmware
        displayId.setText(watchStatusData.getRoBuildDisplayId());
        // Removed unused and unnecessary watchData
        //productName.setText(watchStatusData.getRoProductName());
        //buildDescription.setText(watchStatusData.getRoBuildDescription());
        //productDevice.setText(watchStatusData.getRoProductDevice());
        //productManufacter.setText(watchStatusData.getRoProductManufacter());
        //revision.setText(watchStatusData.getRoRevision());
        //buildDate.setText(watchStatusData.getRoBuildDate());
        //huamiNumber.setText(watchStatusData.getRoBuildHuamiNumber());
        //fingerprint.setText(watchStatusData.getRoBuildFingerprint());

        // Log the values received from watch brightness
        AmazModApplication.currentScreenBrightness = watchStatusData.getScreenBrightness();
        AmazModApplication.currentScreenBrightnessMode = watchStatusData.getScreenBrightnessMode();
        Logger.debug("WatchInfoFragment WatchData SCREEN_BRIGHTNESS_MODE: " + AmazModApplication.currentScreenBrightness);
        Logger.debug("WatchInfoFragment WatchData SCREEN_BRIGHTNESS: " + AmazModApplication.currentScreenBrightness);

        // Heart Rate Bar Chart
        String lastHeartRates = watchStatusData.getLastHeartRates();
        if ( lastHeartRates!=null && !lastHeartRates.isEmpty() ){
            Logger.debug("WatchInfoFragment WatchData HEART RATES: " + lastHeartRates);
            try {
                HeartRateChartFragment f = (HeartRateChartFragment) getActivity().getSupportFragmentManager().findFragmentByTag("heart-rate-chart");
                f.updateChart(lastHeartRates);
            }catch(NullPointerException e) {
                // HeartRate fragment card not found!
                e.printStackTrace();
            }
        }else{
            Logger.debug("WatchInfoFragment WatchData HEART RATES: null or empty");
        }

        // Hourly Chime (update if changed from watch menu)
        boolean hourlyChime = (watchStatusData.getHourlyChime()>0); // 0 = off, 1 = on
        Prefs.putBoolean(Constants.PREF_AMAZMOD_HOURLY_CHIME, hourlyChime);
        Logger.debug("WatchInfoFragment WatchData HOURLY_CHIME: " + hourlyChime);
    }

    @OnLongClick(R.id.watchIconView)
    public boolean onWatchIconLongClick() {
        new MaterialDialog.Builder(getContext())
                .title(R.string.ota_test_title)
                .content(R.string.type_service_number)
                .inputType(InputType.TYPE_CLASS_NUMBER)
                .negativeText(R.string.cancel)
                .input("", "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        // Do something
                        updateAvailable(Integer.parseInt(input.toString()));
                    }
                }).show();
        return true;
    }

    private void connected() {
        isConnectedTV.setTextColor(getResources().getColor((R.color.colorCharging), Objects.requireNonNull(getContext()).getTheme()));
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_is_connected)).toUpperCase());
        watchProgress.setVisibility(View.GONE);
        watchDetail.setVisibility(View.VISIBLE);
        noService.setVisibility(View.GONE);
    }

    private void disconnected() {
        isConnectedTV.setTextColor(getResources().getColor((R.color.colorAccent), Objects.requireNonNull(getContext()).getTheme()));
        isConnectedTV.setText(((String) getResources().getText(R.string.watch_disconnected)).toUpperCase());
        watchProgress.setVisibility(View.GONE);
        watchDetail.setVisibility(View.GONE);
        noService.setVisibility(View.VISIBLE);
    }

    private void connecting() {
        connecting(true);
    }
    private void connecting(boolean withService) {
        if(withService) {
            // Connecting to service
            isConnectedTV.setTextColor(ThemeHelper.getThemeForegroundColor(Objects.requireNonNull(getContext())));
            isConnectedTV.setText(((String) getResources().getText(R.string.watch_connecting)).toUpperCase());
        }else{
            // Connecting to official API
            isConnectedTV.setTextColor(getResources().getColor((R.color.colorAccent), Objects.requireNonNull(getContext()).getTheme()));
            isConnectedTV.setText(((String) getResources().getText(R.string.watch_connecting_no_service)).toUpperCase());
        }
        watchDetail.setVisibility(View.GONE);
        watchProgress.setVisibility(View.VISIBLE);
        noService.setVisibility(View.GONE);
        isConnectedTV.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                SettingsActivity.restartApplication(getContext());
                return true;
            }
        });
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