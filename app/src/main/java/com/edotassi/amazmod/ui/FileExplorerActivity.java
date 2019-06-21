package com.edotassi.amazmod.ui;

import android.animation.Animator;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.FileExplorerAdapter;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.event.ResultDeleteFile;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.util.Screen;
import com.edotassi.amazmod.util.WatchfaceUtil;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.common.util.Strings;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.tinylog.Logger;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.data.DirectoryData;
import amazmod.com.transport.data.FileData;
import amazmod.com.transport.data.RequestDeleteFileData;
import amazmod.com.transport.data.RequestDirectoryData;
import amazmod.com.transport.data.ResultShellCommandData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import de.mateware.snacky.Snacky;

public class FileExplorerActivity extends BaseAppCompatActivity {

    @BindView(R.id.activity_file_explorer_list)
    ListView listView;

    @BindView(R.id.activity_file_explorer_fab_bg)
    View bgFabMenu;

    @BindView(R.id.activity_file_explorer_fab_main)
    FloatingActionButton fabMain;

    @BindView(R.id.activity_file_explorer_fab_newfolder)
    FloatingActionButton fabNewFolder;

    @BindView(R.id.activity_file_explorer_fab_upload)
    FloatingActionButton fabUpload;

    @BindView(R.id.activity_file_explorer_swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    private FileExplorerAdapter fileExplorerAdapter;
    private SnackProgressBarManager snackProgressBarManager;

    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder mBuilder;

    private static final int FILE_UPLOAD_CODE = 1;
    private static final int NOTIF_ID = 0;
    private static final long NOTIFICATION_UPDATE_INTERVAL = 2 * 1000L; /* 2s */

    private static final String PATH = "path";
    private static final String SOURCE = "source";

    private String currentPath, lastPath;
    private long currentTime, lastUpdate;
    private boolean isFabOpen;
    private boolean transferring = false;

    public static boolean continueNotification;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String source = null;
        if (intent.hasExtra(SOURCE)) {
            source = intent.getStringExtra(SOURCE);
        }

        Logger.debug("onNewIntent source: {}", source);

        if ("cancel".equals(source)) {
            continueNotification = false;
            transferring = false;
            stopNotification(null, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.debug("onResume getFlags: " + getIntent().getFlags());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent(); // gets the previously created intent
        if (intent.hasExtra(PATH)) {
            currentPath = intent.getStringExtra(PATH);
        } else {
            currentPath = Constants.INITIAL_PATH;
        }

        Logger.debug("currentPath = {} source = {}", currentPath, intent.getStringExtra(SOURCE));

        setContentView(R.layout.activity_file_explorer);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.file_explorer);
        } catch (NullPointerException ex) {
            Logger.error("onCreate exception: " + ex.getMessage());
        }

        ButterKnife.bind(this);

        fileExplorerAdapter = new FileExplorerAdapter(this, R.layout.row_file_explorer, new ArrayList<>());
        listView.setAdapter(fileExplorerAdapter);

        loadPath(currentPath);

        registerForContextMenu(listView);

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

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            private int lastFirstVisibleItem = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {

                int topRowVerticalPosition = (listView == null || listView.getChildCount() == 0) ?
                        0 : listView.getChildAt(0).getTop();
                swipeRefreshLayout.setEnabled((topRowVerticalPosition >= 0));

                if (lastFirstVisibleItem < firstVisibleItem) {
                    fabMain.hide();
                }
                if (lastFirstVisibleItem > firstVisibleItem) {
                    fabMain.show();
                }
                lastFirstVisibleItem = firstVisibleItem;

            }
        });

        swipeRefreshLayout.setColorSchemeColors(ThemeHelper.getThemeColorAccent(this));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Logger.debug("onRefresh");
                loadPath(currentPath);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Logger.debug("onBackPressed");
        if (currentPath.equals("/")) {
            if (!transferring) {
                finish();
            }
        } else {
            loadPath(getParentDirectoryPath(currentPath));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (!transferring)
            finish();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.debug("onActivityResult");
        switch (requestCode) {
            case FILE_UPLOAD_CODE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    //List<Uri> files = Utils.getSelectedFilesFromResult(data);
                    //uploadFiles(files, currentPath);
                }
                break;
            case 0:
            default:
                Logger.error("requestCode: {}", requestCode);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            System.out.println("FileExplorerActivity ORIENTATION PORTRAIT");
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            System.out.println("FileExplorerActivity ORIENTATION LANDSCAPE");
        }
    }


    @OnItemClick(R.id.activity_file_explorer_list)
    public void onItemClick(int position) {
        FileData fileData = fileExplorerAdapter.getItem(position);
        if (fileData != null && fileData.isDirectory()) {
            loadPath(fileData.getPath());
        }
    }

    @OnClick(R.id.activity_file_explorer_fab_upload)
    public void onUpload() {
        CloseFabMenu();

        /* Old filepicker
        Intent i = new Intent(this, FilePickerActivity.class);

        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, FILE_UPLOAD_CODE);
        */

        /* New filepicker */
        if (lastPath == null || lastPath.isEmpty())
            lastPath = Environment.getExternalStorageDirectory().getPath();

        final ArrayList<File> files = new ArrayList<>();

        ChooserDialog chooserDialog;
        if (Screen.isDarkTheme()) {
            chooserDialog = new ChooserDialog(this, R.style.FileChooserStyle_Dark);
        } else {
            chooserDialog = new ChooserDialog(this, R.style.FileChooserStyle_Light);
        }

        chooserDialog
                //.withResources(R.string.title_choose_file,
                //        R.string.title_choose, R.string.dialog_cancel)
                //.disableTitle(true)
                //.titleFollowsDir(true)
                //.displayPath(true)
                .enableOptions(true)
                .withStartFile(lastPath)
                .withFilter(false, false)
                .enableMultiple(true)
                .withOnDismissListener(dialog -> {
                    if (files.isEmpty())
                        return;

                        /* Used for testing pusposes only
                        ArrayList<String> paths = new ArrayList<>();
                        for (File file : files) {
                            paths.add(file.getAbsolutePath());
                        }

                        AlertDialog.Builder builder = Screen.isDarkTheme() ? new AlertDialog.Builder(this,
                                R.style.FileChooserDialogStyle_Dark) : new AlertDialog.Builder(this, R.style.FileChooserDialogStyle);
                        builder.setTitle(files.size() + " files selected:")
                                .setAdapter(new ArrayAdapter<>(this,
                                        android.R.layout.simple_expandable_list_item_1, paths), null)
                                .create()
                                .show();
                        */

                    uploadFiles(files, currentPath);

                    })
                    .withOnBackPressedListener(dialog -> {
                        files.clear();
                        dialog.dismiss();
                    })
                    .withOnLastBackPressedListener(dialog -> {
                        files.clear();
                        dialog.dismiss();
                    })
                    .withNegativeButtonListener((dialog, which) -> {
                        files.clear();
                        dialog.dismiss();
                    })
                    .withChosenListener((dir, dirFile) -> {
                        lastPath = dir;

                        if (dirFile.isDirectory()) {
                            chooserDialog.dismiss();
                            return;
                        }

                        if (!files.remove(dirFile)) {
                            files.add(dirFile);
                        }
                    });

        chooserDialog.withOnBackPressedListener(dialog -> chooserDialog.goBack());
        chooserDialog.build().show();

    }

    @OnClick(R.id.activity_file_explorer_fab_main)
    public void fabMainClick() {
        if (!isFabOpen)
            ShowFabMenu();
        else
            CloseFabMenu();
    }

    @OnClick(R.id.activity_file_explorer_fab_bg)
    public void fabMenuClick() {
        CloseFabMenu();
    }

    @OnClick(R.id.activity_file_explorer_fab_newfolder)
    public void fabNewFolderClick() {
        CloseFabMenu();
        new MaterialDialog.Builder(this)
                .title(R.string.new_folder)
                .content(R.string.type_folder_name)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .negativeText(R.string.cancel)
                .input("", "", new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        // Do something
                        String newDirPath = currentPath + "/" + input.toString();
                        execCommandAndReload(ShellCommandHelper.getMakeDirCommand(newDirPath));
                    }
                }).show();
    }

    /**
     * Receives a list of files and uploads them (in series)
     *
     * @param files Array of files
     * @param uploadPath files will be uploaded to this Path
     */
    //private void uploadFiles(final List<Uri> files, final String uploadPath) {
    private void uploadFiles(final ArrayList<File> files, final String uploadPath) {
        if (files.size() > 0) {
            transferring = true;
            //final File file = Utils.getFileForUri(files.get(0));
            final File file = files.get(0);
            files.remove(0);
            //final String path = file.getAbsolutePath();

            if (!file.exists()) {
                fileNotExists();
                return;
            }
            if (file.isDirectory()) {
                fileIsDirectory();
                return;
            }

            final String destPath = uploadPath + "/" + file.getName();
            final long size = file.length();
            //final long startedAt = System.currentTimeMillis();

            final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

            String message = getString(R.string.sending) + " \"" + file.getName() + "\"";

            createNotification(getString(R.string.sending), message, R.drawable.outline_cloud_upload_24);

            final SnackProgressBar progressBar = new SnackProgressBar(
                    SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending) + " \"" + file.getName() + "\"")
                    .setIsIndeterminate(false)
                    .setProgressMax(100)
                    .setAllowUserInput(true)
                    .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                        @Override
                        public void onActionClick() {
                            snackProgressBarManager.dismissAll();
                            cancellationTokenSource.cancel();
                        }
                    })
                    .setShowProgressPercentage(true);
            snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

            Watch.get().uploadFile(file, destPath, new Watch.OperationProgress() {
                @Override
                public void update(final long duration, final long byteSent, final long remainingTime, final double progress) {
                    FileExplorerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String remaingSize = Formatter.formatShortFileSize(FileExplorerActivity.this, size - byteSent);
                            double kbSent = byteSent / 1024d;
                            double speed = kbSent / (duration / 1000.0);
                            DecimalFormat df = new DecimalFormat("#.00");

                            String duration = DurationFormatUtils.formatDuration(remainingTime, "mm:ss", true);
                            String smallMessage = getString(R.string.sending) + " \"" + file.getName() + "\"";
                            String message = smallMessage + "\n" + duration + " - " + remaingSize + " - " + df.format(speed) + " kb/s";

                            //Logger.debug("continueNotification: {} \\ lastUpdate: {}", continueNotification, lastUpdate);
                            currentTime = System.currentTimeMillis();
                            if (currentTime - lastUpdate > NOTIFICATION_UPDATE_INTERVAL) {
                                if (continueNotification) {

                                    lastUpdate = currentTime;
                                    updateNotification(message, smallMessage, (int) progress);

                                } else {

                                    lastUpdate = Long.MAX_VALUE;
                                    stopNotification(null, true);
                                }
                            }

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
                        //if there are no more files to upload, reload (or show information)
                        if (files.size() == 0) {
                            if (currentPath.equals(uploadPath)) {
                                loadPath(uploadPath);
                            } else {
                                SnackProgressBar snackbar = new SnackProgressBar(
                                        SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.file_upload_finished))
                                        .setAction(getString(R.string.view_files), new SnackProgressBar.OnActionClickListener() {
                                            @Override
                                            public void onActionClick() {
                                                snackProgressBarManager.dismissAll();
                                                loadPath(uploadPath);
                                            }
                                        });
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                            }

                            stopNotification(getString(R.string.file_upload_finished), false);
                            transferring = false;

                        } else {
                            uploadFiles(files, uploadPath);
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
                            transferring = false;
                            stopNotification(getString(R.string.file_download_canceled), true);

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
                            uploadFiles(files, uploadPath);
                            //uploading = false;
                            stopNotification(getString(R.string.cant_upload_file), false);

                        }

                    }
                    return null;
                }
            });
        } else {
            transferring = false;
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View
            view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_file_explorer_context, contextMenu);

        int position = ((AdapterView.AdapterContextMenuInfo) contextMenuInfo).position;
        FileData fileData = fileExplorerAdapter.getItem(position);

        if (fileData != null) {
            if (fileData.isDirectory()) {
                menuInflater.inflate(R.menu.activity_file_explorer_folder, contextMenu);
            } else {
                if (fileData.getName().endsWith(".apk")) {
                    menuInflater.inflate(R.menu.activity_file_explorer_apk_file, contextMenu);
                }

                if (fileData.getName().endsWith(".tar.gz") || fileData.getName().endsWith(".tgz")) {
                    menuInflater.inflate(R.menu.activity_file_explorer_targz_file, contextMenu);
                }

                if (fileData.getName().endsWith(".wfz") && (currentPath.equals(Constants.WATCHFACE_FOLDER) )) {
                    menuInflater.inflate(R.menu.activity_file_explorer_wfz, contextMenu);
                }
            }
        }

        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        int index = ((AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo()).position;

        switch (menuItem.getItemId()) {
            case R.id.action_activity_file_explorer_download:
                downloadFile(index);
                return true;
            case R.id.action_activity_file_explorer_delete:
                deleteFile(index);
                return true;
            case R.id.action_activity_file_explorer_install_apk:
                installApk(index);
                return true;
            case R.id.action_activity_file_explorer_rename:
                renameFile(index);
                return true;
            case R.id.action_activity_file_explorer_extract:
                extract(index);
                return true;
            case R.id.action_activity_file_explorer_compress:
                compress(index);
                return true;
            case R.id.action_activity_file_explorer_set_watchface:
                setWatchface(index);
                return true;
        }

        return false;
    }


    private void renameFile(int index) {
        final FileData fileData = fileExplorerAdapter.getItem(index);
        CloseFabMenu();
        String title, content;
        if (fileData != null) {
            if (fileData.isDirectory()) {
                title = getString(R.string.rename_folder);
                content = getString(R.string.type_folder_name);
            } else {
                title = getString(R.string.rename_file);
                content = getString(R.string.type_file_name);
            }
            new MaterialDialog.Builder(this)
                    .title(title)
                    .content(content)
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .negativeText(R.string.cancel)
                    .input("", fileData.getName(), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                            // Do something
                            String newName = currentPath + "/" + input.toString();
                            String oldName = fileData.getPath();
                            String renameCmd = ShellCommandHelper.getRenameCommand(oldName, newName);
                            execCommandAndReload(renameCmd);
                        }
                    }).show();
        }
    }

    private void compress(int index) {
        final FileData fileData = fileExplorerAdapter.getItem(index);
        String compressCmd = null;
        if (fileData != null) {
            compressCmd = ShellCommandHelper.getCompressCommand(currentPath, fileData.getName());
        }
        execCommandAndReload(compressCmd);
    }

    private void setWatchface(int index) {
        final FileData fileData = fileExplorerAdapter.getItem(index);
        if (fileData != null) {
            WatchfaceUtil.setWfzWatchFace(this,fileData.getName());
        }
    }

    private void extract(int index) {
        final FileData fileData = fileExplorerAdapter.getItem(index);
        String extractCmd = null;
        if (fileData != null) {
            extractCmd = ShellCommandHelper.getExtractCommand(fileData.getPath(), getParentDirectoryPath(fileData.getPath()));
        }
        execCommandAndReload(extractCmd);
    }

    private void deleteFolder(int index) {
        final FileData fileData = fileExplorerAdapter.getItem(index);
        new MaterialDialog.Builder(this)
                .canceledOnTouchOutside(false)
                .title(R.string.delete)
                .content(getString(R.string.delete_folder_and_contents, fileData.getName()))
                .positiveText(R.string.delete)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        String command = ShellCommandHelper.getRemoveRecursivelyCommand(fileData.getPath());
                        execCommandAndReload(command);
                    }
                })
                .show();
    }

    private void deleteFile(int index) {
        final FileData fileData = fileExplorerAdapter.getItem(index);
        if (fileData != null) {
            if (fileData.isDirectory()) {
                deleteFolder(index);
                return;
            }
            final SnackProgressBar deletingSnackbar = new SnackProgressBar(
                    SnackProgressBar.TYPE_CIRCULAR, getString(R.string.deleting))
                    .setIsIndeterminate(true);

            snackProgressBarManager.show(deletingSnackbar, SnackProgressBarManager.LENGTH_INDEFINITE);

            RequestDeleteFileData requestDeleteFileData = new RequestDeleteFileData();
            requestDeleteFileData.setPath(fileData.getPath());

            Watch.get().deleteFile(requestDeleteFileData)
                    .continueWithTask(new Continuation<ResultDeleteFile, Task<Void>>() {
                        @Override
                        public Task<Void> then(@NonNull Task<ResultDeleteFile> task) throws Exception {
                            snackProgressBarManager.dismissAll();

                            boolean success = task.isSuccessful();
                            int result = Objects.requireNonNull(task.getResult()).getResultDeleteFileData().getResult();
                            if (success && (result == Transport.RESULT_OK)) {

                                return loadPath(getParentDirectoryPath(fileData.getPath()));
                            } else {
                                SnackProgressBar snackbar = new SnackProgressBar(
                                        SnackProgressBar.TYPE_NORMAL, getString(R.string.cant_delete_file))
                                        .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                            @Override
                                            public void onActionClick() {
                                                snackProgressBarManager.dismissAll();
                                            }
                                        });
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);

                                return Tasks.forException(Objects.requireNonNull(task.getException()));
                            }
                        }
                    });
        }
    }

    private void downloadFile(int index) {
        final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        final FileData fileData = fileExplorerAdapter.getItem(index);

        String message = null;
        if (fileData != null) {
            message = getString(R.string.downloading) + " \"" + fileData.getName() + "\"";
        } else
            message = getString(R.string.error);

        createNotification(getString(R.string.downloading), message, R.drawable.outline_cloud_download_24);

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

        if (fileData != null) {
            final long size = fileData.getSize();
            transferring = true;

            Watch.get().downloadFile(this, fileData.getPath(), fileData.getName(), size, Constants.MODE_DOWNLOAD,
                    new Watch.OperationProgress() {
                        @Override
                        public void update(final long duration, final long byteSent, final long remainingTime, final double progress) {
                            FileExplorerActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String remaingSize = Formatter.formatShortFileSize(FileExplorerActivity.this, size - byteSent);
                                    double kbSent = byteSent / 1024d;
                                    double speed = kbSent / (duration / 1000.0);
                                    DecimalFormat df = new DecimalFormat("#.00");

                                    String duration = DurationFormatUtils.formatDuration(remainingTime, "mm:ss", true);
                                    String smallMessage = getString(R.string.downloading) + " \"" + fileData.getName() + "\"";
                                    String message = smallMessage + "\n" + duration + " - " + remaingSize + " - " + df.format(speed) + " kb/s";

                                    //Logger.debug("continueNotification: {} \\ lastUpdate: {}", continueNotification, lastUpdate);
                                    currentTime = System.currentTimeMillis();
                                    if (currentTime - lastUpdate > NOTIFICATION_UPDATE_INTERVAL) {
                                        if (continueNotification) {

                                            lastUpdate = currentTime;
                                            updateNotification(message, smallMessage, (int) progress);

                                        } else {

                                            lastUpdate = Long.MAX_VALUE;
                                            stopNotification(null, true);
                                        }
                                    }

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
                                        .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                            @Override
                                            public void onActionClick() {
                                                snackProgressBarManager.dismissAll();
                                            }
                                        });

                                transferring = false;
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                                stopNotification(getString(R.string.file_downloaded), false);

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

                                    transferring = false;
                                    snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                                    stopNotification(getString(R.string.file_download_canceled), true);

                                } else {
                                    SnackProgressBar snackbar = new SnackProgressBar(
                                            SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_download_file))
                                            .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                                @Override
                                                public void onActionClick() {
                                                    snackProgressBarManager.dismissAll();
                                                }
                                            });

                                    transferring = false;
                                    snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                                    stopNotification(getString(R.string.cant_download_file), false);

                                }
                            }

                            return null;
                        }
                    });
        }
    }

    private void installApk(int index) {
        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending))
                .setIsIndeterminate(true)
                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                    @Override
                    public void onActionClick() {
                        snackProgressBarManager.dismissAll();
                    }
                })
                .setShowProgressPercentage(true);
        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        final FileData fileData = fileExplorerAdapter.getItem(index);
        if (fileData != null)
            Watch.get()
                    .executeShellCommand(ShellCommandHelper.getApkInstall(fileData.getPath()))
                    .continueWith(new Continuation<ResultShellCommand, Object>() {
                        @Override
                        public Object then(@NonNull Task<ResultShellCommand> task) throws Exception {
                            snackProgressBarManager.dismissAll();
                            if (task.getResult() != null)
                                if (task.isSuccessful() && (task.getResult().getResultShellCommandData().getResult() == 0)) {
                                    new MaterialDialog.Builder(FileExplorerActivity.this)
                                            .title(R.string.apk_install_started_title)
                                            .content(R.string.apk_install_started)
                                            .positiveText("OK")
                                            .show();

                                } else {
                                    SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_start_apk_install));
                                    snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                                }
                            else {
                                SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.activity_files_file_error));
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                            }
                            return null;
                        }
                    });
    }

    private void ShowFabMenu() {
        isFabOpen = true;
        fabUpload.show();
        fabNewFolder.show();
        bgFabMenu.setVisibility(View.VISIBLE);

        fabMain.animate().rotation(135f);
        bgFabMenu.animate().alpha(1f);
        fabUpload.animate()
                .translationY(-284f)
                .rotation(0f);
        fabNewFolder.animate()
                .translationY(-156f)
                .rotation(0f);
    }

    private void CloseFabMenu() {
        isFabOpen = false;

        View[] views = {bgFabMenu, fabNewFolder, fabUpload};

        fabMain.animate().rotation(0f);
        bgFabMenu.animate().alpha(0f);
        fabUpload.animate()
                .translationY(0f)
                .rotation(90f);
        fabNewFolder.animate()
                .translationY(0f)
                .rotation(90f).setListener(new FabAnimatorListener(views));
    }

    private class FabAnimatorListener implements Animator.AnimatorListener {
        View[] viewsToHide;

        public FabAnimatorListener(View[] viewsToHide) {
            this.viewsToHide = viewsToHide;
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!isFabOpen)
                for (View view : viewsToHide)
                    view.setVisibility(View.GONE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }

    private Task<Void> loadPath(final String path) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        stateLoading();

        RequestDirectoryData requestDirectoryData = new RequestDirectoryData();
        requestDirectoryData.setPath(path);

        Watch.get()
                .listDirectory(requestDirectoryData)
                .continueWith(new Continuation<Directory, Object>() {
                    @Override
                    public Object then(@NonNull Task<Directory> task) throws Exception {
                        if (task.isSuccessful()) {
                            currentPath = path;

                            Directory directory = task.getResult();
                            if (directory != null) {
                                DirectoryData directoryData = directory.getDirectoryData();
                                if (directoryData.getResult() == Transport.RESULT_OK) {
                                    Objects.requireNonNull(getSupportActionBar()).setTitle(path.equals("/") ? "/" : directoryData.getName());

                                    Gson gson = new Gson();
                                    String jsonFiles = directoryData.getFiles();
                                    List<FileData> filesData = gson.fromJson(jsonFiles, new TypeToken<List<FileData>>() {
                                    }.getType());

                                    if (!path.equals("/")) {
                                        FileData parentDirectory = new FileData();
                                        parentDirectory.setName("..");
                                        parentDirectory.setDirectory(true);

                                        parentDirectory.setPath(getParentDirectoryPath(path));

                                        filesData.add(0, parentDirectory);
                                    }

                                    Collections.sort(filesData, new Comparator<FileData>() {
                                        @Override
                                        public int compare(FileData left, FileData right) {
                                            if (left.isDirectory() && !right.isDirectory()) {
                                                return -1;
                                            }

                                            if (right.isDirectory() && !left.isDirectory()) {
                                                return 0;
                                            }

                                            return left.getName().compareTo(right.getName());
                                        }
                                    });

                                    fileExplorerAdapter.clear();
                                    fileExplorerAdapter.addAll(filesData);
                                    fileExplorerAdapter.notifyDataSetChanged();

                                    taskCompletionSource.setResult(null);

                                } else {
                                    Snacky.builder()
                                            .setActivity(FileExplorerActivity.this)
                                            .setText(R.string.reading_files_failed)
                                            .setDuration(Snacky.LENGTH_SHORT)
                                            .build().show();
                                    taskCompletionSource.setException(new Exception());
                                }
                            }
                        } else {
                            taskCompletionSource.setException(Objects.requireNonNull(task.getException()));
                            Snacky.builder()
                                    .setActivity(FileExplorerActivity.this)
                                    .setText(R.string.reading_files_failed)
                                    .setDuration(Snacky.LENGTH_SHORT)
                                    .build().show();
                        }

                        stateReady();

                        return null;
                    }
                });

        return taskCompletionSource.getTask();
    }

    private void stateLoading() {
        swipeRefreshLayout.setRefreshing(true);
        listView.setVisibility(View.GONE);
    }

    private void stateReady() {
        swipeRefreshLayout.setRefreshing(false);
        listView.setVisibility(View.VISIBLE);
    }

    private String getParentDirectoryPath(String path) {
        String[] pathComponents = path.split("/");
        String parentPath = TextUtils.join("/", Arrays.copyOf(pathComponents, pathComponents.length - 1));
        if (Strings.isEmptyOrWhitespace(parentPath)) {
            parentPath = "/";
        }

        return parentPath;
    }

    private void fileNotExists() {
        Snacky.builder()
                .setActivity(FileExplorerActivity.this)
                .setText(R.string.file_not_exists)
                .setDuration(Snacky.LENGTH_SHORT)
                .build().show();
    }

    private void fileIsDirectory() {
        Snacky.builder()
                .setActivity(FileExplorerActivity.this)
                .setText(R.string.cant_uplaod_directory)
                .setDuration(Snacky.LENGTH_SHORT)
                .build().show();
    }

    private void execCommandAndReload(String command) {
        if (command == null) {
            Logger.error("null command");
            return;
        }
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
                    ResultShellCommandData resultShellCommandData = null;
                    if (resultShellCommand != null) {
                        resultShellCommandData = resultShellCommand.getResultShellCommandData();

                        if (resultShellCommandData.getResult() == 0) {
                            //Toast.makeText(getApplicationContext(), resultShellCommandData.getOutputLog(), Toast.LENGTH_LONG).show();
                            loadPath(currentPath);
                        } else {
                            SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.shell_command_failed));
                            snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                        }
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

    private void createNotification(String mode, String message, int icon) {
        continueNotification = true;
        lastUpdate = 0;
        Intent intent = new Intent(getApplicationContext(), FileExplorerActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("source", "notification");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int)System.currentTimeMillis(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        intent.putExtra("source", "cancel");
        PendingIntent pendingIntentCancel = PendingIntent.getActivity(this, (int)System.currentTimeMillis(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationManager = NotificationManagerCompat.from(this);
        mBuilder = new NotificationCompat.Builder(this, Constants.TAG);
        mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder)
                .bigText(message)
                .setBigContentTitle(mode)
                .setSummaryText(message))
                .setContentTitle(mode)
                .setContentText(message)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.close), pendingIntentCancel)
                .setSmallIcon(icon);
        notificationManager.notify(NOTIF_ID, mBuilder.build());
    }

    private void updateNotification(String message, String smallMessage, int progress) {
        //Show/Update notification with current progress
        mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder)
                .bigText(message)
                .setBigContentTitle(getString(R.string.downloading))
                .setSummaryText(smallMessage))
                .setOnlyAlertOnce(true)
                .setContentTitle(getString(R.string.downloading))
                .setContentText(smallMessage);
        mBuilder.setProgress(100, (int) progress, false);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIF_ID, mBuilder.build());

    }

    private void stopNotification(String message, boolean mustClose) {
        if (mustClose)
            message = getString(R.string.cancel);

        //Remove progressBar from notification and allow removal of it
        mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder)
                .bigText(message))
                .setOngoing(false);
        mBuilder.setProgress(0, 0, false);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIF_ID, mBuilder.build());

        if (mustClose)
            notificationManager.cancel(NOTIF_ID);
    }


}
