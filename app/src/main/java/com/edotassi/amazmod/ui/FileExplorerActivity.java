package com.edotassi.amazmod.ui;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.FileExplorerAdapter;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.event.OtherData;
import com.edotassi.amazmod.event.ResultDeleteFile;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.support.DownloadHelper;
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
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.huami.watch.transport.Transporter;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.tinylog.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import static android.net.ConnectivityManager.NetworkCallback;

public class FileExplorerActivity extends BaseAppCompatActivity implements Transporter.DataListener {

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

    @BindView(R.id.activity_file_explorer_fab_ftpupload)
    FloatingActionButton fabFTPUpload;

    @BindView(R.id.activity_file_explorer_swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    private FileExplorerAdapter fileExplorerAdapter;
    private SnackProgressBarManager snackProgressBarManager;

    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder mBuilder;
    private NetworkCallback networkCallback;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;

    private static final int FILE_UPLOAD_CODE = 1;
    private static final int NOTIF_ID = 0;
    private static final long NOTIFICATION_UPDATE_INTERVAL = 2 * 1000L; /* 2s */

    private static final String PATH = "path";
    private static final String SOURCE = "source";

    private String currentPath, lastPath;
    private long currentTime, lastUpdate;
    private boolean isFabOpen;
    private boolean transferring = false;
    private String localIP = "N/A";
    String defaultFTPip = "192.168.43.1";

    public static boolean continueNotification;

    private Transporter ftpTransporter;

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
                .setMessageMaxLines(4)
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

        // Connect wifi FTP transporter
        ftpTransporterConnect();
    }

    @Override
    public void onDestroy() {
        ftpTransporterDisconnect();
        super.onDestroy();
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
        if (!transferring) {
            finish();
        }
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

        super.onActivityResult(requestCode, resultCode, data);
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

        startActivityForResult(i, FILE_UPLOAD_CODE); */

        // New filepicker
        if (lastPath == null || lastPath.isEmpty())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                lastPath = Environment.getExternalStorageState();
            } else {
                lastPath = Environment.getExternalStorageDirectory().getPath();
            }
        final ArrayList<File> files = new ArrayList<>();

        ChooserDialog chooserDialog;
        if (Screen.isDarkTheme() || MainActivity.systemThemeIsDark) {
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
                .withResources(R.string.file_choose_title, R.string.select, R.string.cancel)
                .enableOptions(false)
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


    @OnClick(R.id.activity_file_explorer_fab_ftpupload)
    public void onFTPUpload() {
        CloseFabMenu();

        // New file picker
        if (lastPath == null || lastPath.isEmpty())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                lastPath = Environment.getExternalStorageState();
            } else {
                lastPath = Environment.getExternalStorageDirectory().getPath();
            }
        final ArrayList<File> files = new ArrayList<>();

        ChooserDialog chooserDialog;
        if (Screen.isDarkTheme() || MainActivity.systemThemeIsDark) {
            chooserDialog = new ChooserDialog(this, R.style.FileChooserStyle_Dark);
        } else {
            chooserDialog = new ChooserDialog(this, R.style.FileChooserStyle_Light);
        }

        chooserDialog
                .withResources(R.string.file_choose_title, R.string.select, R.string.cancel)
                .enableOptions(false)
                .withStartFile(lastPath)
                .withFilter(false, false)
                .enableMultiple(true)
                .withOnDismissListener(dialog -> {
                    if (files.isEmpty())
                        return;

                    uploadFTPFiles(files, currentPath);

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
     * @param files      Array of files
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

            String message = "\"" + file.getName() + "\"";

            createNotification(getString(R.string.sending) + ", " + getString(R.string.wait), message, R.drawable.outline_cloud_upload_24);

            final SnackProgressBar progressBar = new SnackProgressBar(
                    SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending) + " \"" + file.getName() + "\", " + getString(R.string.wait))
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
                            String message = getString(R.string.sending) + " \"" + file.getName() + "\", " + getString(R.string.wait) + " - " + duration + " - " + remaingSize + " - " + df.format(speed) + " kb/s";
                            String messageNotification = "\"" + file.getName() + "\", " + duration + " - " + remaingSize + " - " + df.format(speed) + " kb/s";
                            String smallMessage = getString(R.string.sending);

                            //Logger.debug("continueNotification: {} \\ lastUpdate: {}", continueNotification, lastUpdate);
                            currentTime = System.currentTimeMillis();
                            if (currentTime - lastUpdate > NOTIFICATION_UPDATE_INTERVAL) {
                                if (continueNotification) {

                                    lastUpdate = currentTime;
                                    updateNotification(messageNotification, smallMessage, (int) progress);

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
                public Object then(@NonNull Task<Void> task) {
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
                            createNotification(getString(R.string.file_upload_finished), message, R.drawable.outline_cloud_upload_24);
                            stopNotification(message, false);
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

    private String FTP_destPath;
    private ArrayList<File> FTP_files;
    private File FTP_file;
    private boolean wifiManualEnabled;

    private void uploadFTPFiles(final ArrayList<File> files, final String uploadPath) {
        wifiManualEnabled = false;
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null) {
            updateSnackBarOnUIthreat(getString(R.string.error), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
            return;
        } else if (!mWifiManager.isWifiEnabled()) {
            updateSnackBarOnUIthreat(getString(R.string.turn_on_wifi), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
            return;
            // Enabling wifi here doesn't work because wifi connects to the default network after requesting to connect to watch
            //wifiManualEnabled = true;
            //mWifiManager.setWifiEnabled(true);
        }

        if (!(files.size() > 0)) {
            transferring = false;
            return;
        }

        transferring = true;
        FTP_files = files;
        FTP_file = files.get(0);
        //files.remove(0);

        if (!FTP_file.exists()) {
            fileNotExists();
            return;
        }
        if (FTP_file.isDirectory()) {
            fileIsDirectory();
            return;
        }

        FTP_destPath = uploadPath + "/"; //+ FTP_file.getName();
        //FTP_file_size = FTP_file.length();

        createNotification(getString(R.string.watch_connecting) + ", " + getString(R.string.wait), "\"" + FTP_file.getName() + "\"", R.drawable.ic_wifi_tethering_white_24dp, false);

        updateSnackBarOnUIthreat(getString(R.string.watch_connecting), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_CIRCULAR);

        //Choose how to sent file an go on...
        getTransferringMethod();
        updateSnackBarOnUIthreat(getString(R.string.wifi_transfer_method), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_CIRCULAR);

    }

    // FTP listener onChange
    private String SSID = "huami-amazfit-amazmod-4E68";
    private String pswd = "12345678";

    public void onDataReceived(TransportDataItem item) {
        // Transmitted action
        String action = item.getAction();

        // Get key_new_state
        DataBundle data = item.getData();
        int key_new_state;
        if (data != null)
            key_new_state = data.getInt("key_new_state");
        else {
            Logger.debug("FTP: transporter action: " + action + " (without key_new_state)");
            return;
        }

        if ("on_ap_state_changed".equals(action)) {
            // Watch WiFi AP status changed
            if (key_new_state != 13) {
                if (data.getInt("key_new_state") == 11) {
                    Logger.debug("FTP: watch's WiFi AP disabled");
                } else
                    Logger.debug("FTP: on_ap_state_changed: " + key_new_state);
                return;
            }

            // (State 13 watch WiFi AP is on)
            Logger.debug("FTP: watch's WiFi AP is enabled");
            updateSnackBarOnUIthreat("WiFi Access Point " + getString(R.string.enabled), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);

            // Connect to the network
            if (mConnectivityManager == null)
                mConnectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            // Connect to the network
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                newApiWifiConnection();
            } else {
                networkCallback = null;
                int netId = -1;
                WifiConfiguration wc = new WifiConfiguration();
                wc.SSID = "\"" + SSID + "\"";
                wc.preSharedKey = "\"" + pswd + "\"";
                //wc.status = WifiConfiguration.Status.ENABLED;
                //wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                //wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);//4
                //wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                //wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                //wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

                mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (mWifiManager != null)
                    netId = mWifiManager.addNetwork(wc);

                if (netId >= 0) {
                    Logger.debug("FTP: watch's WiFi AP found: net ID = " + netId);

                    //mWifiManager.disconnect(); // Disconnect from current network
                    // Try to connect to watch network
                    if (mWifiManager.enableNetwork(netId, true)) {
                        //mWifiManager.reconnect();
                        mWifiManager.saveConfiguration();

                        Thread t = new Thread() {
                            @Override
                            public void run() {
                                try {
                                    int seconds_waiting = 0;
                                    int waiting_limit = 15 * (wifiManualEnabled ? 2 : 1);
                                    // Check if connected!
                                    while ((!getSSID(FileExplorerActivity.this).equals("\"" + SSID + "\"") || !isConnected(mConnectivityManager)) && seconds_waiting < waiting_limit) {
                                        Logger.debug("FTP: Waiting for WiFi connection to be established... (Current wifi:" + getSSID(FileExplorerActivity.this) + ", State: " + wifiState(mConnectivityManager) + ")");
                                        // Wait to connect
                                        seconds_waiting++;
                                        Thread.sleep(1000);
                                    }

                                    // Within time?
                                    if (seconds_waiting < waiting_limit) {
                                        Logger.debug("FTP: WiFi connection established (Current wifi:" + getSSID(FileExplorerActivity.this) + "). Sending command to enable FTP.");
                                        updateSnackBarOnUIthreat("WiFi Access Point " + getString(R.string.device_connected), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
                                        ftpTransporter.send("enable_ftp");
                                    } else {
                                        transferring = false;
                                        Logger.debug("WiFi connection to server could not be established.");
                                        ftpTransporter.send("disable_ap");
                                        updateNotification(getString(R.string.cant_upload_file), "\"" + FTP_file.getName() + "\"", false);
                                        updateSnackBarOnUIthreat(getString(R.string.cant_upload_file), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
                                        // Disable WiFi if it was automatically open
                                        if (wifiManualEnabled)
                                            mWifiManager.setWifiEnabled(false);
                                    }
                                } catch (Exception e) {
                                    // failed, close wifi ap
                                    transferring = false;
                                    Logger.debug("FTP: WiFi connection thread crashed: " + e.getMessage());
                                    ftpTransporter.send("disable_ap");
                                    updateNotification(getString(R.string.cant_upload_file), "\"" + FTP_file.getName() + "\"", false);
                                    updateSnackBarOnUIthreat(getString(R.string.cant_upload_file), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
                                    // Disable WiFi if it was automatically open
                                    if (wifiManualEnabled)
                                        mWifiManager.setWifiEnabled(false);
                                }
                            }
                        };

                        t.start();
                    } else {
                        Logger.debug("FTP: WiFi connection to server could not be established.");
                    }
                } else {
                    Logger.debug("FTP: watch's WiFi AP not found.");
                    transferring = false;
                    ftpTransporter.send("disable_ap");
                    updateNotification(getString(R.string.cant_upload_file), "\"" + FTP_file.getName() + "\"", false);
                    updateSnackBarOnUIthreat(getString(R.string.cant_upload_file), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
                    // Disable WiFi if it was automatically open
                    if (wifiManualEnabled)
                        mWifiManager.setWifiEnabled(false);
                }
            }
        } else if ("ftp_on_state_changed".equals(action)) {
            if (key_new_state != 2) {
                if (key_new_state == 1) {
                    Logger.debug("FTP: FTP server disabled");
                } else
                    Logger.debug("FTP: ftp_on_state_changed: " + key_new_state);

                // Close wifi ap
                transferring = false;
                ftpTransporter.send("disable_ap");
                return;
            }

            Logger.debug("FTP: FTP server enabled (connected to WiFi: " + getSSID(FileExplorerActivity.this) + ")");
            updateSnackBarOnUIthreat("FTP server " + getString(R.string.enabled), SnackProgressBarManager.LENGTH_SHORT, SnackProgressBar.TYPE_CIRCULAR);

            // We create ftp connections
            FTPClient ftpClient = new FTPClient();
            int successful = 0;
            int total_files = FTP_files.size();
            try {
                if (!localIP.equals("N/A")) {
                    ftpClient.connect(localIP, 5210);
                } else {
                    ftpClient.connect(defaultFTPip, 5210);
                }
                ftpClient.login("anonymous", "");

                // After connection attempt, you should check the reply code to verify success.
                int reply = ftpClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    Logger.debug("FTP: FTP server refused connection.");
                    transferring = false;
                    updateNotification(getString(R.string.cant_upload_file), "\"" + FTP_file.getName() + "\"", false);
                    updateSnackBarOnUIthreat(getString(R.string.cant_upload_file), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);

                    // close ftp & wifi ap
                    ftpTransporter.send("disable_ftp");
                    ftpTransporter.send("disable_ap");
                } else {
                    Logger.debug("FTP: FTP server connection granted.");
                    // Set FTP transferred as BINARY to avoid corrupted file
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    //If it doesn't exist create it
                    String relative_path = FTP_destPath.replace("/sdcard", "");
                    if (!ftpClient.changeWorkingDirectory(relative_path)) {
                        Logger.debug("FTP: target path doesn't exit. Creating path: " + relative_path);
                        ftpClient.makeDirectory(relative_path);
                        ftpClient.changeWorkingDirectory(relative_path);
                    }

                    ftpClient.setCopyStreamListener(streamListener);

                    // Progress bar
                    final SnackProgressBar progressBar = new SnackProgressBar(
                            SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending) + " \"" + FTP_files.get(0).getName() + "\", " + getString(R.string.wait))
                            .setIsIndeterminate(false)
                            .setProgressMax(100)
                            .setAllowUserInput(true)
                            /*
                            .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                                @Override
                                public void onActionClick() {
                                    snackProgressBarManager.dismissAll();
                                    cancellationTokenSource.cancel();
                                }
                            })
                            */
                            .setShowProgressPercentage(true);
                    SnackBarProgressbar(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

                    // Loop through selected files
                    while (FTP_files.size() > 0) {
                        FTP_file = FTP_files.get(0);
                        // Update data on progressbar
                        progressBar.setMessage(getString(R.string.sending) + " \"" + FTP_file.getName() + "\", " + getString(R.string.wait));
                        SnackBarProgressbar(0); // Set progressbar to 0

                        // Create an InputStream of the zipped file to be uploaded
                        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(FTP_file));
                        // Store file to server
                        if (ftpClient.storeFile(FTP_file.getName(), stream)) {
                            Logger.debug("FTP: file " + (1 + total_files - FTP_files.size()) + " transfer finished.");
                            updateNotification(getString(R.string.file_upload_finished), "\"" + FTP_file.getName() + "\"", false);
                            successful++;
                        } else {
                            Logger.debug("FTP: file " + (1 + total_files - FTP_files.size()) + " transfer failed: " + ftpClient.getReplyString());
                            updateNotification(getString(R.string.cant_upload_file), "\"" + FTP_file.getName() + "\"", false);
                        }
                        FTP_files.remove(0);
                    }
                    dismissAllSnackBars();
                    //Finish up
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                if (ftpClient.isConnected()) {
                    Logger.debug("FTP: connection to server error, but server is connected... Disconnecting...");
                    try {
                        ftpClient.disconnect();
                    } catch (IOException f) {
                        // do nothing
                    }
                }
                unregisterConnectionManager();

                Logger.debug("FTP: connection to server error: " + e.toString());
                updateNotification(getString(R.string.cant_upload_file), "\"" + FTP_file.getName() + "\"", false);
                updateSnackBarOnUIthreat(getString(R.string.cant_upload_file), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
            }
            // Close ftp & wifi ap
            ftpTransporter.send("disable_ftp");
            ftpTransporter.send("disable_ap");
            transferring = false;
            unregisterConnectionManager();
            if (successful > 0)
                updateSnackBarOnUIthreat(getString(R.string.file_upload_finished), true, true, SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
            // Disable WiFi if it was automatically open
            if (wifiManualEnabled)
                mWifiManager.setWifiEnabled(false);

        } else if ("on_ap_enable_result".equals(action)) {
            if (key_new_state == 1)
                Logger.debug("FTP: watch WiFi AP enabled successfully");
            else
                Logger.debug("FTP: on_ap_enable_result (key_new_state = " + key_new_state + ")");
        } else {
            Logger.debug("FTP: transporter action: " + action + " (key_new_state = " + key_new_state + ")");
        }
    }

    // SnackBar functions
    private void dismissAllSnackBars() {
        updateSnackBarOnUIthreat(null, true, 0, 0);
    }

    private void updateSnackBarOnUIthreat(String message, int duration, int type) {
        updateSnackBarOnUIthreat(message, false, duration, type);
    }

    private void updateSnackBarOnUIthreat(String message, boolean dismissAll, int duration, int type) {
        updateSnackBarOnUIthreat(message, dismissAll, false, duration, type);
    }

    private void updateSnackBarOnUIthreat(String message, boolean dismissAll, boolean loadCurrentPath, int duration, int type) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Dismiss all other notifications
                if (dismissAll)
                    snackProgressBarManager.dismissAll();
                // Update notification
                if (message != null)
                    snackProgressBarManager.show(new SnackProgressBar(type, message), duration);
                // Reload UI path
                if (loadCurrentPath)
                    loadPath(currentPath);
            }
        });
    }

    // SnackBar Progressbar
    private void SnackBarProgressbar(SnackProgressBar progressBar, int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Create a progressbar
                snackProgressBarManager.show(progressBar, duration);
            }
        });
    }

    private void SnackBarProgressbar(int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update progressbar
                snackProgressBarManager.setProgress(progress);
            }
        });
    }

    public static boolean isConnected(ConnectivityManager connectivityManager) {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        Logger.debug("FTP: network info: " + ((networkInfo != null) ? networkInfo.getDetailedState() : "null"));

        return networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED;
    }

    public static String wifiState(ConnectivityManager connectivityManager) {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return (networkInfo != null) ? networkInfo.getDetailedState().toString() : "null";
    }

    public static String getSSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ssid = "null";
        if (wifiManager != null) {
            WifiInfo info = wifiManager.getConnectionInfo();
            ssid = info.getSSID();
        }
        return ssid;
    }

    int prev_percent = 0;
    CopyStreamAdapter streamListener = new CopyStreamAdapter() {
        @Override
        public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
            // This method will be called every time bytes are transferred
            int percent = (int) (totalBytesTransferred * 100 / FTP_file.length());

            // Update your progress bar if percentage has changed
            if (prev_percent != percent) {
                prev_percent = percent;
                Logger.debug("FTP: file transfer: " + percent + "%");
                SnackBarProgressbar(percent); // Update progressbar
                updateNotification(getString(R.string.sending) + ", " + getString(R.string.wait), "\"" + FTP_file.getName() + "\"", (int) percent); // Update notification
            }
        }
    };


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

                if (fileData.getName().endsWith(".wfz") && (currentPath.equals(Constants.WATCHFACE_FOLDER))) {
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
            WatchfaceUtil.setWfzWatchFace(fileData.getName());
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
                        public Task<Void> then(@NonNull Task<ResultDeleteFile> task) {
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
            message = "\"" + fileData.getName() + "\"";
        } else
            message = getString(R.string.error);

        createNotification(getString(R.string.downloading) + ", " + getString(R.string.wait), message, R.drawable.outline_cloud_download_24);

        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.downloading) + ", " + getString(R.string.wait))
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

            String finalMessage = message;
            Watch.get().downloadFile(this, fileData.getPath(), fileData.getName(), size, Constants.MODE_DOWNLOAD,
                    new Watch.OperationProgress() {
                        @Override
                        public void update(final long duration, final long byteSent, final long remainingTime, final double progress) {
                            FileExplorerActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String remainingSize = Formatter.formatShortFileSize(FileExplorerActivity.this, size - byteSent);
                                    double kbSent = byteSent / 1024d;
                                    double speed = kbSent / (duration / 1000.0);
                                    DecimalFormat df = new DecimalFormat("#.00");

                                    String duration = DurationFormatUtils.formatDuration(remainingTime, "mm:ss", true);
                                    String message = getString(R.string.downloading) + " \"" + fileData.getName() + "\", " + getString(R.string.wait) + " - " + duration + " - " + remainingSize + " - " + df.format(speed) + " kb/s";
                                    String messageNotification = "\"" + fileData.getName() + "\", " + duration + " - " + remainingSize + " - " + df.format(speed) + " kb/s";
                                    String smallMessage = getString(R.string.downloading);

                                    //Logger.debug("continueNotification: {} \\ lastUpdate: {}", continueNotification, lastUpdate);
                                    currentTime = System.currentTimeMillis();
                                    if (currentTime - lastUpdate > NOTIFICATION_UPDATE_INTERVAL) {
                                        if (continueNotification) {

                                            lastUpdate = currentTime;
                                            updateNotification(messageNotification, smallMessage, (int) progress);

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
                        public Object then(@NonNull Task<Void> task) {
                            snackProgressBarManager.dismissAll();
                            if (task.isSuccessful()) {
                                SnackProgressBar snackbar = new SnackProgressBar(
                                        SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.file_downloaded))
                                        .setAction(getString(R.string.open), new SnackProgressBar.OnActionClickListener() {
                                            @Override
                                            public void onActionClick() {
                                                openFile(fileData);
                                                snackProgressBarManager.dismissAll();
                                            }
                                        });

                                transferring = false;
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                                createNotification(getString(R.string.file_downloaded), finalMessage, R.drawable.outline_cloud_download_24);
                                stopNotification(finalMessage, false);
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
                        public Object then(@NonNull Task<ResultShellCommand> task) {
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
        fabFTPUpload.show();
        fabNewFolder.show();
        bgFabMenu.setVisibility(View.VISIBLE);

        fabMain.animate().rotation(135f);
        bgFabMenu.animate().alpha(1f);
        fabFTPUpload.animate()
                .translationY(-412f)
                .rotation(0f);
        fabUpload.animate()
                .translationY(-284f)
                .rotation(0f);
        fabNewFolder.animate()
                .translationY(-156f)
                .rotation(0f);
    }

    private void CloseFabMenu() {
        isFabOpen = false;

        View[] views = {bgFabMenu, fabNewFolder, fabUpload, fabFTPUpload};

        fabMain.animate().rotation(0f);
        bgFabMenu.animate().alpha(0f);
        fabFTPUpload.animate()
                .translationY(0f)
                .rotation(90f);
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
                    public Object then(@NonNull Task<Directory> task) {
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

                                    Collections.sort(filesData, (left, right) -> {
                                        if (left.isDirectory() && !right.isDirectory()) {
                                            return -1;
                                        }

                                        if (right.isDirectory() && !left.isDirectory()) {
                                            return 0;
                                        }
                                        return left.getName().compareToIgnoreCase(right.getName());
                                    });

                                    fileExplorerAdapter.clear();
                                    fileExplorerAdapter.addAll(filesData);
                                    fileExplorerAdapter.notifyDataSetChanged();
                                    fabMain.show();

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
            public Object then(@NonNull Task<ResultShellCommand> task) {

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
        createNotification(mode, message, icon, true);
    }

    private void createNotification(String mode, String message, int icon, boolean action) {
        continueNotification = true;
        lastUpdate = 0;
        Intent intent = new Intent(getApplicationContext(), FileExplorerActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("source", "notification");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        intent.putExtra("source", "cancel");
        PendingIntent pendingIntentCancel = PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
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
                .setSmallIcon(icon);
        if (action)
            mBuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.close), pendingIntentCancel);
        notificationManager.notify(NOTIF_ID, mBuilder.build());
    }

    private void updateNotification(String message, String smallMessage, int progress) {
        //Show/Update notification with current progress
        mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder)
                .bigText(message)
                //.setBigContentTitle(getString(R.string.downloading))
                .setSummaryText(smallMessage))
                .setOnlyAlertOnce(true)
                //.setContentTitle(getString(R.string.downloading))
                .setContentText(smallMessage)
                .setProgress(100, (int) progress, false);
        notificationManager.notify(NOTIF_ID, mBuilder.build());

    }

    private void updateNotification(String message, String smallMessage) {
        updateNotification(message, smallMessage, true);
    }

    private void updateNotification(String message, String smallMessage, boolean ongoing) {
        //Show/Update notification without progress
        mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder)
                        .bigText(smallMessage)
                        .setBigContentTitle(message)
                //.setSummaryText(smallMessage)
        )
                .setOnlyAlertOnce(true)
                .setOngoing(ongoing)
                //.setContentTitle(getString(R.string.downloading))
                .setContentText(smallMessage)
                .setProgress(0, 0, false);
        notificationManager.notify(NOTIF_ID, mBuilder.build());
    }

    private void stopNotification(String message, boolean mustClose) {
        if (mustClose)
            message = getString(R.string.cancel);

        //Remove progressBar from notification and allow removal of it
        mBuilder.setStyle(new NotificationCompat.BigTextStyle(mBuilder)
                .bigText(message))
                .setAutoCancel(true)
                .setOngoing(false);
        mBuilder.setProgress(0, 0, false);
        notificationManager.notify(NOTIF_ID, mBuilder.build());

        if (mustClose)
            notificationManager.cancel(NOTIF_ID);
    }

    private void openFile(FileData fileData) {
        Logger.trace("name: {} path: {} extension: {}", fileData.getName(), fileData.getPath(), fileData.getExtention());
        if (fileData.isDirectory())
            return;

        File file = DownloadHelper.getDownloadedFile(fileData.getName(), Constants.MODE_DOWNLOAD);
        Logger.trace("file: {}", file.getAbsolutePath());

        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        Uri path = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                FileProvider.getUriForFile(this, Constants.FILE_PROVIDER, file)
                : Uri.fromFile(file);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileData.getExtention());
        Logger.trace("extension: {} mime: {}", fileData.getExtention(), mimeType);

        newIntent.setDataAndType(path, mimeType);
        newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(newIntent);
            //startActivity(Intent.createChooser(newIntent, getString(R.string.open)));
        } catch (ActivityNotFoundException e) {
            SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.shell_command_failed));
            snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_SHORT);
        }
    }

    public void getTransferringMethod() {
        // Get watch's local IP and choose transferring method to use
        Watch.get().sendSimpleData(Transport.LOCAL_IP, null).continueWith(new Continuation<OtherData, Object>() {
            @Override
            public Object then(@NonNull Task<OtherData> task) {
                localIP = "N/A";
                if (task.isSuccessful()) {
                    OtherData returnedData = task.getResult();
                    try {
                        if (returnedData == null)
                            throw new NullPointerException("Returned data are null");

                        DataBundle otherData = returnedData.getOtherData();
                        localIP = otherData.getString("ip");
                        Logger.debug("Watch IP is: " + localIP);

                    } catch (Exception e) {
                        Logger.debug("failed reading IP data: " + e);
                    }
                }
                if (ftpTransporter.isTransportServiceConnected()) {
                    Logger.debug("FTP: sending enable_ap action.");
                    ftpTransporter.send("start_service");

                    DataBundle dataBundle = new DataBundle();
                    dataBundle.putInt("key_keymgmt", 4); // WPA2
                    dataBundle.putString("key_ssid", SSID);
                    dataBundle.putString("key_pswd", pswd);

                    if (!localIP.equals("N/A") && !localIP.equals(defaultFTPip)) {
                        ftpTransporter.send("enable_ftp");
                        Logger.debug("Watch IP found, you are connected on the same WiFi");
                        updateSnackBarOnUIthreat(getString(R.string.watch_same_wifi), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_CIRCULAR);
                    } else {
                        // Enable watch WiFi AP
                        ftpTransporter.send("enable_ap", dataBundle);
                        Logger.debug("Watch IP in empty, so will go with WiFi AP");
                        updateSnackBarOnUIthreat(getString(R.string.watch_empty_ip), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_CIRCULAR);
                    }
                } else {
                    Logger.debug("FTP: transporter is not connected.");
                    transferring = false;
                    updateSnackBarOnUIthreat(getString(R.string.cant_upload_file), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
                }
                return null;
            }
        });
    }

    @TargetApi(29)
    public void newApiWifiConnection() {
        //This is used to connect to WiFi AP on api >=29
        networkCallback = new NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                updateSnackBarOnUIthreat("WiFi Access Point " + getString(R.string.device_connected), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
                ftpTransporter.send("enable_ftp");
                Logger.debug("FTP api29: watch's WiFi available");
                super.onAvailable(network);
                mConnectivityManager.bindProcessToNetwork(network);
            }

            @Override
            public void onUnavailable() {
                transferring = false;
                ftpTransporter.send("disable_ap");
                Logger.debug("FTP api29: watch's WiFi Unavailable");
                updateNotification(getString(R.string.cant_upload_file), "\"" + FTP_file.getName() + "\"", false);
                updateSnackBarOnUIthreat(getString(R.string.cant_upload_file), SnackProgressBarManager.LENGTH_LONG, SnackProgressBar.TYPE_HORIZONTAL);
                super.onUnavailable();
            }
        };

        final NetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(SSID)
                .setWpa2Passphrase(pswd)
                .build();
        final NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        mConnectivityManager.requestNetwork(request, networkCallback);
    }

    private void unregisterConnectionManager() {
        if (mConnectivityManager != null && networkCallback != null)
            try {
                mConnectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
                mConnectivityManager.bindProcessToNetwork(null);
            } catch (Exception e) {
                Logger.debug("unregisterConnectionManager: " + e);
            }
    }

    public void ftpTransporterConnect() {
        // Set up FTP transporter listener
        Logger.debug("FTP: connecting transporter");
        ftpTransporter = Transporter.get(this, "com.huami.wififtp");
        ftpTransporter.addDataListener(this);
        if (!ftpTransporter.isTransportServiceConnected())
            ftpTransporter.connectTransportService();
    }

    public void ftpTransporterDisconnect() {
        try {
            if (ftpTransporter.isTransportServiceConnected()) {
                ftpTransporter.removeDataListener(this);
                ftpTransporter.disconnectTransportService();
                Logger.debug("FTP: transporter disconnected");
                ftpTransporter = null;
            }
        } catch (NullPointerException ex) {
            Logger.error("ftpTransporterDisconnect exception: " + ex.getMessage());
        }
    }
}
