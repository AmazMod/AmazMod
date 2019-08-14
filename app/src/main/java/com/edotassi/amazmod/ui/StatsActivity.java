package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.db.model.NotificationEntity_Table;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.support.DownloadHelper;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.util.FilesUtil;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.data.DirectoryData;
import amazmod.com.transport.data.FileData;
import amazmod.com.transport.data.RequestDirectoryData;
import amazmod.com.transport.data.ResultShellCommandData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.mateware.snacky.Snacky;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class StatsActivity extends BaseAppCompatActivity {

    @BindView(R.id.activity_stats_main_container)
    View statsMainContainer;
    @BindView(R.id.activity_stats_root_layout)
    ScrollView rootLayout;
    @BindView(R.id.activity_stats_progress)
    MaterialProgressBar materialProgressBar;
    @BindView(R.id.activity_stats_notifications_last_hour)
    TextView notificationsLastHour;
    @BindView(R.id.activity_stats_notifications_24_hours)
    TextView notificationsLast24Hours;
    @BindView(R.id.activity_stats_notifications_total)
    TextView notificationsTotal;
    @BindView(R.id.activity_stats_logs_content)
    TextView logsContentEditText;

    @BindView(R.id.activity_stats_open_notifications_log)
    Button openNotificationsLogButton;

    private SnackProgressBarManager snackProgressBarManager;
    private static String logFile;

    private final byte[] ALLOWED_FILTERS = {Constants.FILTER_CONTINUE,
                                            Constants.FILTER_UNGROUP,
                                            Constants.FILTER_VOICE,
                                            Constants.FILTER_MAPS,
                                            Constants.FILTER_LOCALOK};

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        logFile = this.getExternalFilesDir(null) + File.separator + Constants.LOGFILE;
        Logger.debug("logFile: {}", logFile);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.stats);
        } catch (NullPointerException exception) {
            Logger.error(exception.getMessage());
            //TODO log to crashlitics
        }

        ButterKnife.bind(this);

        snackProgressBarManager = new SnackProgressBarManager(findViewById(android.R.id.content))
                // (optional) set the view which will animate with SnackProgressBar e.g. FAB when CoordinatorLayout is not used
                //.setViewToMove(floatingActionButton)
                // (optional) change progressBar color, default = R.color.colorAccent
                .setProgressBarColor(ThemeHelper.getThemeColorAccentId(this))
                .setActionTextColor(ThemeHelper.getThemeColorAccentId(this))
                // (optional) change background color, default = BACKGROUND_COLOR_DEFAULT (#FF323232)
                .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                // (optional) change text size, default = 14sp
                .setTextSize(14)
                // (optional) set max lines, default = 2
                .setMessageMaxLines(2)
                // (optional) register onDisplayListener
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

        //Make text scrollable inside ScrollView if needed
        logsContentEditText.setMovementMethod(new ScrollingMovementMethod());
        rootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                logsContentEditText.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }
        });

        logsContentEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                logsContentEditText.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        loadStats();
        loadLogs();
    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_stats_open_notifications_log)
    public void openLog() {
        startActivity(new Intent(this, NotificationsLogActivity.class));
    }

    @OnClick(R.id.activity_stats_generate_bundle)
    public void generateBundle(){
        String generateBundleCmd = ShellCommandHelper.getLogBundleCommand();
        generateLogBundle(generateBundleCmd);
    }

    @OnClick(R.id.activity_stats_clear_logs)
    public void clearLogs(){
        try{
            logsContentEditText.setText("");
            FileWriter fw = new FileWriter(logFile,false);
        }catch (IOException e){
            Logger.error(e,"clearLogs: can't empty file: {}", logFile);
        }

    }

    @OnClick(R.id.activity_stats_send_logs)
    public void sendLogs(){
        final String logContent = logsContentEditText.getText().toString();
        Logger.trace("logTextView: {} logFile: {}", logContent.substring(0, Math.min(logContent.length(), 48)), logFile);

        /* Send only the content of logsContentEditText
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "AmazMod Phone Logs");
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, logContent);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.send_log))); */

        File file = new File(logFile); //Send log file using any app
        if (file.exists()) {
            Uri path = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                    FileProvider.getUriForFile(this, Constants.FILE_PROVIDER, file)
                    : Uri.fromFile(file);
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,"AmazMod Phone Logs");
            sendIntent.putExtra(Intent.EXTRA_STREAM, path);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sendIntent.setType("plain/*");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.send_log)));
        } else
            Toast.makeText(this, getString(R.string.file_not_found), Toast.LENGTH_LONG).show();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            rootLayout.setEnabled(false);
            logsContentEditText.setLines(16);
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rootLayout.setEnabled(true);
            logsContentEditText.setLines(12);
        }
    }

    private void loadLogs(){

        final int lines = Integer.parseInt(Prefs.getString(Constants.PREF_LOG_LINES_SHOWN,
                Constants.PREF_LOG_LINES_SHOWN_DEFAULT));
        Logger.trace("lines: {}", lines);

        final String log = FilesUtil.reverseLines(new File(logFile), lines);
        if (log != null) {
            logsContentEditText.setText(log);
            logsContentEditText.setMovementMethod(new ScrollingMovementMethod());
        } else
            Logger.error("error reading log file");

    }

    @SuppressLint("CheckResult")
    private void loadStats() {
        materialProgressBar.setVisibility(View.VISIBLE);
        statsMainContainer.setVisibility(View.GONE);

        Flowable
                .fromCallable(new Callable<StatsResult>() {
                    @Override
                    public StatsResult call() throws Exception {

                        long total = SQLite
                                .selectCountOf()
                                .from(NotificationEntity.class)
                                .count();

                        long anHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
                        long aDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);

                        long totalAnHourAgo = 0L;
                        long totalADayAgo = 0L;
                        long sum;

                        for (byte f: ALLOWED_FILTERS) {
                            sum = SQLite
                                    .selectCountOf()
                                    .from(NotificationEntity.class)
                                    .where(NotificationEntity_Table.date.greaterThan(anHourAgo))
                                    .and(NotificationEntity_Table.filterResult.eq(f))
                                    .count();
                            totalAnHourAgo += sum;
                        }

                        for (byte f: ALLOWED_FILTERS) {
                            sum = SQLite
                                    .selectCountOf()
                                    .from(NotificationEntity.class)
                                    .where(NotificationEntity_Table.date.greaterThan(aDayAgo))
                                    .and(NotificationEntity_Table.filterResult.eq(f))
                                    .count();
                            totalADayAgo += sum;
                        }

                        StatsResult result = new StatsResult();

                        result.setNotificationsTotal(total);
                        result.setNotificationsTotalADayAgo(totalADayAgo);
                        result.setNotificationsTotalAnHourAgo(totalAnHourAgo);

                        return result;
                    }
                })
                .subscribeOn(Schedulers.computation())
                .subscribe(new Consumer<StatsResult>() {
                    @Override
                    public void accept(final StatsResult result) throws Exception {
                        StatsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notificationsTotal.setText(String.valueOf(result.getNotificationsTotal()));
                                notificationsLastHour.setText(String.valueOf(result.getNotificationsTotalAnHourAgo()));
                                notificationsLast24Hours.setText(String.valueOf(result.getNotificationsTotalADayAgo()));

                                materialProgressBar.setVisibility(View.GONE);
                                statsMainContainer.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
    }

    private class StatsResult {
        private long notificationsTotal;
        private long notificationsTotalAnHourAgo;
        private long notificationsTotalADayAgo;

        public long getNotificationsTotal() {
            return notificationsTotal;
        }

        public void setNotificationsTotal(long notificationsTotal) {
            this.notificationsTotal = notificationsTotal;
        }

        public long getNotificationsTotalAnHourAgo() {
            return notificationsTotalAnHourAgo;
        }

        public void setNotificationsTotalAnHourAgo(long notificationsTotalAnHourAgo) {
            this.notificationsTotalAnHourAgo = notificationsTotalAnHourAgo;
        }

        public long getNotificationsTotalADayAgo() {
            return notificationsTotalADayAgo;
        }

        public void setNotificationsTotalADayAgo(long notificationsTotalADayAgo) {
            this.notificationsTotalADayAgo = notificationsTotalADayAgo;
        }
    }


    //STEP 1: Generate Log Bundle in Watch
    private void generateLogBundle(String command) {
        Logger.debug("Generating log bundle in watch: " + command);
        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.collecting_watch_logs))
                .setIsIndeterminate(true)
                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                    @Override
                    public void onActionClick() {
                        snackProgressBarManager.dismissAll();
                    }
                });
        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        Watch.get().executeShellCommand(command, true, false).continueWith(new Continuation<ResultShellCommand, Object>() {
            @Override
            public Object then(@NonNull Task<ResultShellCommand> task) throws Exception {

                snackProgressBarManager.dismissAll();

                if (task.isSuccessful()) {
                    ResultShellCommand resultShellCommand = task.getResult();
                    ResultShellCommandData resultShellCommandData = resultShellCommand.getResultShellCommandData();

                    if (resultShellCommandData.getResult() == 0) {
                        getBundleFileData(Constants.FILE_LOG_BUNDLE);
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

    //STEP 2: Get Log Bundle Information (file name and size)
    private Task<Void> getBundleFileData(final String file) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        File fFile = new File(file);

        String path = fFile.getParentFile().getPath();

        RequestDirectoryData requestDirectoryData = new RequestDirectoryData();
        requestDirectoryData.setPath(path);

        Watch.get()
                .listDirectory(requestDirectoryData)
                .continueWith(new Continuation<Directory, Object>() {
                    @Override
                    public Object then(@NonNull Task<Directory> task) throws Exception {
                        if (task.isSuccessful()) {

                            Directory directory = task.getResult();
                            DirectoryData directoryData = directory.getDirectoryData();
                            if (directoryData.getResult() == Transport.RESULT_OK) {
                                Gson gson = new Gson();
                                String jsonFiles = directoryData.getFiles();
                                List<FileData> filesData = gson.fromJson(jsonFiles, new TypeToken<List<FileData>>() {
                                }.getType());

                                for (FileData f : filesData){
                                    if (f.getName().equals(fFile.getName())){
                                        downloadBundleFile(f);
                                    }
                                }

                                taskCompletionSource.setResult(null);

                            } else {
                                Snacky.builder()
                                        .setActivity(StatsActivity.this)
                                        .setText(R.string.reading_files_failed)
                                        .setDuration(Snacky.LENGTH_SHORT)
                                        .build().show();
                                taskCompletionSource.setException(new Exception());
                            }
                        } else {
                            taskCompletionSource.setException(task.getException());
                            Snacky.builder()
                                    .setActivity(StatsActivity.this)
                                    .setText(R.string.reading_files_failed)
                                    .setDuration(Snacky.LENGTH_SHORT)
                                    .build().show();
                        }
                        return null;
                    }
                });

        return taskCompletionSource.getTask();
    }

    //STEP 3: Download Log Bundle to Phone
    private void downloadBundleFile(FileData fileData) {
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

        final long size = fileData.getSize();
        final long startedAt = System.currentTimeMillis();

        Watch.get().downloadFile(this, fileData.getPath(), fileData.getName(), size, Constants.MODE_DOWNLOAD,
                new Watch.OperationProgress() {
                    @Override
                    public void update(final long duration, final long byteSent, final long remainingTime, final double progress) {
                        StatsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String remaingSize = Formatter.formatShortFileSize(StatsActivity.this, size - byteSent);
                                double kbSent = byteSent / 1024d;
                                double speed = kbSent / (duration / 1000);
                                DecimalFormat df = new DecimalFormat("#.00");

                                String duration = DurationFormatUtils.formatDuration(remainingTime, "mm:ss", true);
                                String smallMessage = getString(R.string.downloading) + " \"" + fileData.getName() + "\"";
                                String message = smallMessage + "\n" + duration + " - " + remaingSize + " - " + df.format(speed) + " kb/s";

                                progressBar.setMessage(message);
                                snackProgressBarManager.setProgress((int) progress);
                                snackProgressBarManager.updateTo(progressBar);
                            }
                        });
                    }
                }, cancellationTokenSource.getToken())
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(@NonNull Task<Void> task) throws Exception {
                        snackProgressBarManager.dismissAll();
                        if (task.isSuccessful()) {
                            SnackProgressBar snackbar = new SnackProgressBar(
                                    SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.file_downloaded))
                                    .setAction(getString(R.string.share), new SnackProgressBar.OnActionClickListener() {
                                        @Override
                                        public void onActionClick() {
                                            snackProgressBarManager.dismissAll();
                                            File file = new File(DownloadHelper.getDownloadDir(Constants.MODE_DOWNLOAD) + File.separator + fileData.getName());
                                            shareFile(file);
                                        }
                                    });
                            snackProgressBarManager.show(snackbar, Constants.SNACKBAR_LONG10);

                            Bundle bundle = new Bundle();
                            bundle.putLong("size", size);
                            bundle.putLong("duration", System.currentTimeMillis() - startedAt);
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

    private void shareFile(File file) {
        Logger.trace("file: {}", file.toString());

        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "amazmod.amazfit@gmail.com", "diotto@gmail.com" });
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "AmazMod Log Bundle");

        ArrayList<Uri> uris = new ArrayList<>();
        Uri contentUri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?               //Watch log file
                FileProvider.getUriForFile(this, Constants.FILE_PROVIDER, file)
                : Uri.fromFile(file);
        uris.add(contentUri);
        File log = new File(logFile);
        contentUri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?                   //Phone log file
                FileProvider.getUriForFile(this, Constants.FILE_PROVIDER, log)
                : Uri.fromFile(log);
        uris.add(contentUri);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        // Grant temporary read permission to the content URI
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share) + "…"));
    }

}
