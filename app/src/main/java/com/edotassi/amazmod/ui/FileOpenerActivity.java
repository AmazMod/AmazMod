package com.edotassi.amazmod.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.support.FirebaseEvents;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.concurrent.CancellationException;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class FileOpenerActivity extends AppCompatActivity {

    @BindView(R.id.activity_file_opner_progress)
    MaterialProgressBar watchProgress;
    @BindView(R.id.isConnected)
    TextView isConnected;

    private SnackProgressBarManager snackProgressBarManager;

    private static final int WRITE_OK = 0;
    private static final int INVALID_FILE = 1;
    private static final int WATCH_NOT_CONNECTED = 2;
    private static final int UPLOAD_ERROR = 3;
    private static final int WRITE_ERROR = 4;

    private static int result;
    private static String filePath;
    private static String fileName;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_opener);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            System.out.println("AmazMod AboutActivity onCreate exception: " + exception.toString());
            //TODO log to crashlitics
        }
        getSupportActionBar().setTitle(R.string.file_uploader);


        ButterKnife.bind(this);

        snackProgressBarManager = new SnackProgressBarManager(this.findViewById(android.R.id.content))
                .setProgressBarColor(R.color.colorAccent)
                .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                .setTextSize(14)
                .setMessageMaxLines(2);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri uri = intent.getData();
        Log.d(Constants.TAG, "FileOpenerActivity onCreate action: " + action + " uri: " + uri);

        filePath = null;

        try {
            final String saveDir = this.getExternalFilesDir(null).toString();

            if (action.compareTo(Intent.ACTION_VIEW) == 0) {
                String scheme = intent.getScheme();
                ContentResolver resolver = getContentResolver();
                Log.d(Constants.TAG, "FileOpenerActivity onCreate saveDir: " + saveDir);

                try {

                    if (scheme != null) {
                        if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {

                            fileName = getContentName(resolver, uri);
                            Log.v(Constants.TAG, "FileOpenerActivity Content uri: " + uri + " name: " + fileName);
                            InputStream input = resolver.openInputStream(uri);
                            filePath = saveDir + File.separator + fileName;
                            inputStreamToFile(input, fileName);

                        } else if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {

                            fileName = uri.getLastPathSegment();
                            Log.v(Constants.TAG, "FileOpenerActivity File uri: " + uri + " name: " + fileName);
                            InputStream input = resolver.openInputStream(uri);
                            filePath = saveDir + File.separator + fileName;
                            inputStreamToFile(input, fileName);

                        } else if (scheme.compareTo("http") == 0) {
                            Log.v(Constants.TAG, "FileOpenerActivity HTTP");
                            // TODO Import from HTTP!

                        } else if (scheme.compareTo("ftp") == 0) {
                            Log.v(Constants.TAG, "FileOpenerActivity FTP");
                            // TODO Import from FTP!
                        }
                    }
                } catch (FileNotFoundException e) {
                    Log.e(Constants.TAG, "FileOpenerActivity onResume exception: " + e.toString());
                    result = WRITE_ERROR;
                }
            }
        } catch (NullPointerException e) {
            Log.e(Constants.TAG, "FileOpenerActivity onResume NullPointerException: " + e.toString());
            result = WRITE_ERROR;
        }

        Log.d(Constants.TAG, "FileOpenerActivity onCreate filePath: " + filePath);

        if (filePath != null) {
            if (filePath.contains(".wfz")) {
                result = WRITE_OK;
            } else {
                result = INVALID_FILE;
            }
        } else
            result = WRITE_ERROR;;

    }

    @Override
    public void onResume() {
        super.onResume();

        connecting();
        Watch.get().getStatus().continueWith(new Continuation<WatchStatus, Object>() {
            @Override
            public Object then(@NonNull Task<WatchStatus> task) throws Exception {
                if (task.isSuccessful()) {
                    Log.d(Constants.TAG, "FileOpenerActivity onResume isWatchConnected = true");
                    connected();
                    AmazModApplication.isWatchConnected = true;

                    File file = new File(filePath);
                    if (result == WRITE_OK)
                        uploadWfz(file, fileName);

                } else {
                    Log.d(Constants.TAG, "FileOpenerActivity onResume isWatchConnected = false");
                    disconnected();
                    AmazModApplication.isWatchConnected = false;
                    try {
                        Snacky
                                .builder()
                                .setActivity(FileOpenerActivity.this)
                                .setText(R.string.failed_load_watch_status)
                                .setDuration(Snacky.LENGTH_SHORT)
                                .build()
                                .show();
                    } catch (Exception e) {
                        Crashlytics.logException(e);
                        Log.e(Constants.TAG, "FileOpenerActivity onResume exception: " + e.toString());
                    }
                    result = WATCH_NOT_CONNECTED;
                    showResult();
                }
                return null;
            }
        });
    }

    private String getContentName(ContentResolver resolver, Uri uri){
        Cursor cursor = resolver.query(uri, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
            if (nameIndex >= 0) {
                return cursor.getString(nameIndex);
            } else {
                cursor.close();
                return null;
            }
        } else {
            return null;
        }
    }

    private void inputStreamToFile(InputStream in, String file) {
        Log.d(Constants.TAG, "FileOpenerActivity inputStreamToFile in: " + in.toString() + " file: " + file);

        long length = 0;
        try {
            FileOutputStream out = new FileOutputStream(this.getExternalFilesDir(null) + File.separator + file);
            byte[] buffer = new byte[1024];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
                length+=c;
            }
            in.close();
            out.close();
        } catch (Exception e) {
            Log.e(Constants.TAG, "FileOpenerActivity inputStreamToFile exception: " + e.toString());
            result = WRITE_ERROR;
        } finally {
            Log.d(Constants.TAG, "FileOpenerActivity inputStreamToFile length: " + length);
        }

    }

    private void uploadWfz(File updateFile, String filename) {
        if (!AmazModApplication.isWatchConnected) {
            return;
        }

        setWindowFlags(true);

        final String destPath = "/sdcard/WatchFace/" + filename;
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
                if (FileOpenerActivity.this != null) {
                    FileOpenerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(Constants.TAG, "FileOpenerActivity uploadWfz destPath: " + destPath);

                            String remaingSize = Formatter.formatShortFileSize(FileOpenerActivity.this, size - byteSent);
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
                    showResult();
                    Bundle bundle = new Bundle();
                    bundle.putLong("size", size);
                    bundle.putLong("duration", System.currentTimeMillis() - startedAt);
                    if (FileOpenerActivity.this != null) {
                        FirebaseAnalytics
                                .getInstance(FileOpenerActivity.this)
                                .logEvent(FirebaseEvents.UPLOAD_FILE, bundle);
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
                    result = UPLOAD_ERROR;
                    showResult();
                    throw new Exception("watch.getUploadFile Exception");
                }
                return null;
            }
        });
    }

    private void showResult() {
        String msg;
        setWindowFlags(false);

        switch (result) {
            case WRITE_OK:
                msg = "Success!";
                break;
            default:
                msg = "Error: " + result;
                break;

        }
        new MaterialDialog.Builder(this)
                .canceledOnTouchOutside(false)
                .title(R.string.file_uploader)
                .content(msg)
                .positiveText("OK")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                    }
                })
                .show();
    }

    private void setWindowFlags(boolean enable) {
        final int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

        if (enable) {
            getWindow().addFlags(flags);
        } else {
            getWindow().clearFlags(flags);
        }

    }

    private void connected() {
        isConnected.setTextColor(getResources().getColor(R.color.colorCharging));
        isConnected.setText(((String) getResources().getText(R.string.watch_is_connected)).toUpperCase());
        watchProgress.setVisibility(View.GONE);
    }

    private void disconnected() {
        isConnected.setTextColor(getResources().getColor(R.color.colorAccent));
        isConnected.setText(((String) getResources().getText(R.string.watch_disconnected)).toUpperCase());
        watchProgress.setVisibility(View.GONE);
    }

    private void connecting() {
        isConnected.setTextColor(getResources().getColor(R.color.mi_text_color_secondary_light));
        isConnected.setText(((String) getResources().getText(R.string.watch_connecting)).toUpperCase());
        watchProgress.setVisibility(View.VISIBLE);
    }
}