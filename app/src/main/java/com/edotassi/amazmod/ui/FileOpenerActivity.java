package com.edotassi.amazmod.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.support.FirebaseEvents;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.util.FilesUtil;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.concurrent.CancellationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class FileOpenerActivity extends BaseAppCompatActivity {

    @BindView(R.id.activity_file_opner_progress)
    MaterialProgressBar watchProgress;
    @BindView(R.id.isConnected)
    TextView isConnected;

    private SnackProgressBarManager snackProgressBarManager;

    private static final String WRITE_OK = "write_ok";
    private static final String INSTALL_OK = "install_ok";
    private static final String INSTALL_ERROR = "install_ERROR";
    private static final String INVALID_FILE = "invalid_file";
    private static final String WATCH_NOT_CONNECTED = "watch_not_connected";
    private static final String UPLOAD_ERROR = "upload_error";
    private static final String WRITE_ERROR = "write_error";
    private static final String INVALID_ACTION = "invalid_action";
    private static final String NO_WORKDIR = "no_work_dir";

    private static final String UPLOAD_WFZ = "WFZ";
    private static final String UPLOAD_APK = "APK";

    private static final String WORK_DIR = "workdir"; //Dir inside cache folder

    private static String inputFileName = null;
    private static String workDir;
    private static String result = "";

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
            Logger.error("AboutActivity onCreate exception: " + exception.toString());
            //TODO log to crashlitics
        }
        getSupportActionBar().setTitle(R.string.file_uploader);

        ButterKnife.bind(this);

        snackProgressBarManager = new SnackProgressBarManager(this.findViewById(android.R.id.content))
                .setProgressBarColor(R.color.colorAccent)
                .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                .setTextSize(14)
                .setMessageMaxLines(2);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String scheme = intent.getScheme();
        final Uri uri = intent.getData();
        final ContentResolver resolver = getContentResolver();

        String fileName = null;
        String filePath = null;
        String uploadType = "";

        if (!createWorkDir()) {
            result = NO_WORKDIR;
            showResult();
        }

        Logger.debug("FileOpenerActivity onCreate action: " + action + " scheme: " + scheme + " uri: " + uri);

        if (action != null && scheme != null && uri != null) {
            // Logs for future improvements
            //Logger.debug("FileOpenerActivity onCreate action: " + action.compareTo(Intent.ACTION_VIEW));
            //Logger.debug("FileOpenerActivity onCreate content: " + scheme.compareTo(ContentResolver.SCHEME_CONTENT));
            //Logger.debug("FileOpenerActivity onCreate file: " + scheme.compareTo(ContentResolver.SCHEME_FILE));
            //Logger.debug("FileOpenerActivity onCreate file-name: " + getContentName(resolver, uri) +", "+uri.getLastPathSegment() +", "+uri.getPath()+", "+uri.getQueryParameter("o"));

            if (action.compareTo(Intent.ACTION_VIEW) == 0) {

                if (scheme.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                    inputFileName = getContentName(resolver, uri);

                    // Opera URI
                    if(inputFileName==null && uri.getQueryParameter("o")!=null) {
                        Pattern filenamePattern = Pattern.compile("([^/]+)\\.(apk|wfz)");
                        Matcher matched = filenamePattern.matcher(uri.getQueryParameter("o"));
                        if(matched.find())
                            inputFileName = matched.group();
                    }
                }else if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0)
                    inputFileName = uri.getLastPathSegment();

                if (inputFileName != null) {
                    fileName = inputFileName.toLowerCase();

                    //Logger.debug("FileOpenerActivity onCreate filename: " + fileName);

                    if (fileName.endsWith(".wfz"))
                        uploadType = UPLOAD_WFZ;
                    else if (fileName.endsWith(".apk"))
                        uploadType = UPLOAD_APK;
                    else
                        result = INVALID_FILE;

                } else
                    result = INVALID_FILE;

                if (!INVALID_FILE.equals(result)) {

                    if (copyToCache(resolver, uri, inputFileName)) {
                        filePath = workDir + File.separator + inputFileName;
                        result = WRITE_OK;
                    } else
                        result = WRITE_ERROR;
                }

            } else
                result = INVALID_ACTION;
        } else
            result = INVALID_ACTION;

        Logger.debug("FileOpenerActivity onCreate result: " + result + " filePath: " + filePath);

        if (WRITE_OK.equals(result))
            promptInstall(filePath, inputFileName, uploadType);
        else
            showResult();

    }

    @Override
    public void finish() {
        deleteWorkDir(new File(workDir));
        super.finish();
    }

    private boolean createWorkDir() {
        workDir = this.getCacheDir().getAbsolutePath() + File.separator + WORK_DIR;
        File file = new File(workDir);
        return file.exists() || file.mkdir();
    }

    public static boolean deleteWorkDir(File dir) {
        if (dir != null) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (String child : children) {
                    //Log.d(Constants.TAG, "FileOpenerActivity deleteWorkDir child: " + child);
                    boolean success = deleteWorkDir(new File(dir, child));
                    if (!success)
                        return false;
                }
            }
            return dir.delete();
        } else
            return false;
    }

    private void promptInstall(final String path, final String name, final String type) {

        Logger.debug("FileOpenerActivity promptInstall type: " + type);

        String title = String.format(getString(R.string.file_uploader_title), type);

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .canceledOnTouchOutside(false)
                .title(title)
                .customView(R.layout.dialog_file_opener, false)
                .positiveText("OK")
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        checkConnection(path, name, type);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                    }
                }).build();

        View view = dialog.getView();
        TextView msgTextView = view.findViewById(R.id.dialog_file_opener_name);
        TextView authorTextView = view.findViewById(R.id.dialog_file_opener_author);
        ImageView imageView = view.findViewById(R.id.dialog_file_opener_image);


        if (UPLOAD_APK.equals(type)) {
            Bundle bundle = FilesUtil.getApkInfo(this, path);
            Bitmap bitmap = bundle.getParcelable(FilesUtil.APP_ICON);
            String label = bundle.getString(FilesUtil.APP_LABEL);
            String pkg = bundle.getString(FilesUtil.APP_PKG);
            LinearLayout.LayoutParams imageViewParams = new LinearLayout.LayoutParams(
                    ViewGroup.MarginLayoutParams.WRAP_CONTENT,
                    ViewGroup.MarginLayoutParams.WRAP_CONTENT);
            imageViewParams.gravity = Gravity.TOP;
            imageViewParams.gravity = Gravity.CENTER_HORIZONTAL;
            imageView.setLayoutParams(imageViewParams);
            if (bitmap != null)
                imageView.setImageBitmap(bitmap);
            else
                imageView.setVisibility(View.INVISIBLE);
            msgTextView.setText(String.format(getString(R.string.file_uploader_label),label));
            authorTextView.setText(String.format(getString(R.string.file_uploader_package),pkg));

        } else if (UPLOAD_WFZ.equals(type)) {
            try {
                FilesUtil.unzip(path, workDir);
            } catch (IOException e) {
                Logger.error("FileOpenerActivity promptInstall unzip exception: " + e.toString());
            }
            final File descriptionXML = new File(workDir + File.separator + "description.xml");
            if (descriptionXML.exists()){
                msgTextView.setText(String.format(getString(R.string.file_uploader_name),
                        FilesUtil.getTagValueFromXML("title", descriptionXML)));
                authorTextView.setText(String.format(getString(R.string.file_uploader_author),
                        FilesUtil.getTagValueFromXML("author", descriptionXML)));
                String previewIMG = workDir + File.separator + FilesUtil.getTagValueFromXML("preview", descriptionXML);
                imageView.setImageDrawable(Drawable.createFromPath(previewIMG));
            }
        }

        dialog.show();

    }

    private void showResult() {
        Logger.debug("FileOpenerActivity showResult result: " + result + " workDir: " + workDir);

        String title, msg;
        setWindowFlags(false);

        switch (result) {
            case WRITE_OK:
                title = "Success!";
                break;

            case INSTALL_OK:
                title = "Success!";
                break;

            default:
                title = "Error!";
                break;

        }

        msg = msg = "File: " + inputFileName + "\nResult: " + result;

        if (INSTALL_OK.equals(result))
            msg += "\n\n" + getString(R.string.apk_install_started);

        new MaterialDialog.Builder(this)
                .canceledOnTouchOutside(false)
                .title(title)
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

    private boolean copyToCache(ContentResolver resolver, Uri uri, String fileName) {

        try {
            InputStream input = resolver.openInputStream(uri);
            return FilesUtil.inputStreamToFile(input, workDir, fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
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

    private void checkConnection(final String filePath, final String fileName, final String uploadType) {

        connecting();
        final File file = new File(filePath);

        Watch.get().getStatus().continueWith(new Continuation<WatchStatus, Object>() {
            @Override
            public Object then(@NonNull Task<WatchStatus> task) throws Exception {
                if (task.isSuccessful()) {
                    Logger.debug("FileOpenerActivity checkConnection isWatchConnected = true");
                    connected();
                    uploadFile(file, fileName, uploadType);

                } else {
                    Logger.debug("FileOpenerActivity checkConnection isWatchConnected = false");
                    disconnected();
                    try {
                        Snacky
                                .builder()
                                .setActivity(FileOpenerActivity.this)
                                .setText(R.string.failed_load_watch_status)
                                .setDuration(Snacky.LENGTH_SHORT)
                                .build()
                                .show();
                    } catch (Exception e) {
                        Logger.error(e);
                        Logger.error("FileOpenerActivity checkConnection exception: " + e.toString());
                    }
                    result = WATCH_NOT_CONNECTED;
                    showResult();
                }
                return null;
            }
        });
    }

    private void uploadFile(final File uploadFile, String filename, final String uploadType) {

        setWindowFlags(true);
        String path;
        switch (uploadType) {
            case UPLOAD_WFZ:
                path = "/sdcard/WatchFace/" + filename;
                break;
            case UPLOAD_APK:
                path = "/sdcard/" + filename;
                break;
            default:
                return;
        }

        final String destPath = path;
        final long size = uploadFile.length();
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

        Watch.get().uploadFile(uploadFile, destPath, new Watch.OperationProgress() {
            @Override
            public void update(final long duration, final long byteSent, final long remainingTime, final double progress) {
                if (FileOpenerActivity.this != null) {
                    FileOpenerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Logger.debug("FileOpenerActivity uploadFile destPath: " + destPath);

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
                    Bundle bundle = new Bundle();
                    bundle.putLong("size", size);
                    bundle.putLong("duration", System.currentTimeMillis() - startedAt);
                    FirebaseAnalytics
                            .getInstance(FileOpenerActivity.this)
                            .logEvent(FirebaseEvents.UPLOAD_FILE, bundle);
                    if (uploadType.equals(UPLOAD_APK))
                        installUpload(destPath);
                    else
                        showResult();

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

    private void installUpload(String destPath) {
        Watch.get()
                .executeShellCommand(ShellCommandHelper.getApkInstall(destPath), false, true)
                .continueWith(new Continuation<ResultShellCommand, Object>() {
                    @Override
                    public Object then(@NonNull Task<ResultShellCommand> task) throws Exception {
                        snackProgressBarManager.dismissAll();
                        if (task.getResult() != null)
                            if (task.isSuccessful() && (task.getResult().getResultShellCommandData().getResult() == 0)) {
                                result = INSTALL_OK;

                                Bundle bundle = new Bundle();
                                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "APK");
                                FirebaseAnalytics
                                        .getInstance(FileOpenerActivity.this)
                                        .logEvent(FirebaseEvents.APK_INSTALL, bundle);
                            } else {
                                SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_start_apk_install));
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                                result = INSTALL_ERROR;
                            }
                        else {
                            SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.activity_files_file_error));
                            snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                            result = INSTALL_ERROR;
                        }
                        showResult();
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