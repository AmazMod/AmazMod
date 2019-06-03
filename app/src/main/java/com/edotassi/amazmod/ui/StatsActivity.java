package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.db.model.NotificationEntity_Table;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.ResultShellCommandData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class StatsActivity extends BaseAppCompatActivity {

    @BindView(R.id.activity_stats_main_container)
    View statsMainContainer;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
        }

        getSupportActionBar().setTitle(R.string.stats);
        ButterKnife.bind(this);

        snackProgressBarManager = new SnackProgressBarManager(findViewById(android.R.id.content))
                // (optional) set the view which will animate with SnackProgressBar e.g. FAB when CoordinatorLayout is not used
                //.setViewToMove(floatingActionButton)
                // (optional) change progressBar color, default = R.color.colorAccent
                .setProgressBarColor(R.color.colorAccent)
                // (optional) change background color, default = BACKGROUND_COLOR_DEFAULT (#FF323232)
                .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                // (optional) change text size, default = 14sp
                .setTextSize(14)
                // (optional) set max lines, default = 2
                .setMessageMaxLines(2)
                // (optional) register onDisplayListener
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
        String renameCmd = ShellCommandHelper.getLogBundleCommand();
        execCommandAndReload(renameCmd);
    }

    private void execCommandAndReload(String command) {
        Logger.debug("Sending command to watch: " + command);
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

        Watch.get().executeShellCommand(command, true, false).continueWith(new Continuation<ResultShellCommand, Object>() {
            @Override
            public Object then(@NonNull Task<ResultShellCommand> task) throws Exception {

                snackProgressBarManager.dismissAll();

                if (task.isSuccessful()) {
                    ResultShellCommand resultShellCommand = task.getResult();
                    ResultShellCommandData resultShellCommandData = resultShellCommand.getResultShellCommandData();

                    if (resultShellCommandData.getResult() == 0) {
                        Toast.makeText(getApplicationContext(), "Log Bundle generated at WATCH in /sdcard/log_bundle.log.gz", Toast.LENGTH_LONG).show();
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


    @OnClick(R.id.activity_stats_clear_logs)
    public void clearLogs(){
        try{
            logsContentEditText.setText("");
            FileWriter fw = new FileWriter(Constants.LOGFILE,false);
        }catch (IOException e){
            Logger.error(e,"clearLogs: can't empty file " + Constants.LOGFILE);
        }

    }

    @OnClick(R.id.activity_stats_send_logs)
    public void sendLogs(){
        /*
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        Uri uri = Uri.fromFile(new File(Constants.LOGFILE));
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(shareIntent, "Share Log"));*/
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "AmazMod Phone Logs");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, logsContentEditText.getText());
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.send_log)));
    }

    private void loadLogs(){
        try {
            // How to read file into String before Java 7
            InputStream is = new FileInputStream(Constants.LOGFILE);
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));

            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();

            while (line != null) {
                sb.append(line).append("\n");
                line = buf.readLine();
            }

            String fileAsString = sb.toString();
            logsContentEditText.setText(fileAsString);
            logsContentEditText.setMovementMethod(new ScrollingMovementMethod());
        } catch (IOException e){
            Logger.error(e, "loadLogs: Cant read file " + Constants.LOGFILE);
        }

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
}
