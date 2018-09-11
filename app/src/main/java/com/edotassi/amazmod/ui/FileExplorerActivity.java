package com.edotassi.amazmod.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.FileExplorerAdapter;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.event.ResultDeleteFile;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.common.util.Strings;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.ocpsoft.prettytime.PrettyTime;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.Date;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.DirectoryData;
import amazmod.com.transport.data.FileData;
import amazmod.com.transport.data.RequestDeleteFileData;
import amazmod.com.transport.data.RequestDirectoryData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class FileExplorerActivity extends AppCompatActivity {

    private final int FILE_UPLOAD_CODE = 1;

    @BindView(R.id.activity_file_explorer_list)
    ListView listView;
    @BindView(R.id.activity_file_explorer_progress)
    MaterialProgressBar materialProgressBar;

    private List<FileData> fileDataList;
    private FileExplorerAdapter fileExplorerAdapter;
    private SnackProgressBarManager snackProgressBarManager;

    private String currentPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception ex) {
        }

        getSupportActionBar().setTitle(R.string.file_explorer);

        ButterKnife.bind(this);

        fileExplorerAdapter = new FileExplorerAdapter(this, R.layout.row_file_explorer, new ArrayList<FileData>());
        listView.setAdapter(fileExplorerAdapter);

        loadPath(Constants.INITIAL_PATH);

        registerForContextMenu(listView);

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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_UPLOAD_CODE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    List<Uri> files = Utils.getSelectedFilesFromResult(data);

                    if (files.size() > 0) {
                        File file = Utils.getFileForUri(files.get(0));
                        final String path = file.getAbsolutePath();

                        if (!file.exists()) {
                            fileNotExists();
                            return;
                        }
                        if (file.isDirectory()) {
                            fileIsDirectory();
                            return;
                        }

                        final String destPath = currentPath + "/" + file.getName();
                        final long size = file.length();

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

                        Watch.get().uploadFile(file, destPath, new Watch.OperationProgress() {
                            @Override
                            public void update(final long duration, final long byteSent, final long remainingTime, final double progress) {
                                FileExplorerActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String remaingSize = Formatter.formatShortFileSize(FileExplorerActivity.this, size - byteSent);
                                        double kbSent = byteSent / 1024d;
                                        double speed = kbSent / (duration / 1000);
                                        DecimalFormat df = new DecimalFormat("#.00");

                                        PrettyTime p = new PrettyTime();
                                        Date finishDate = new Date(System.currentTimeMillis() + (remainingTime * 1000));
                                        String duration = p.formatDurationUnrounded(finishDate);

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
                                    loadPath(getParentDirectoryPath(destPath));
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
                }
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.activity_file_explorer_context, contextMenu);

        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_activity_file_explorer_download:
                return true;
            case R.id.action_activity_file_explorer_delete:
                deleteFile(((AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo()).position);
                return true;
        }

        return false;
    }

    private void deleteFile(int index) {
        final SnackProgressBar deletingSnackbar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.deleting))
                .setIsIndeterminate(true);

        snackProgressBarManager.show(deletingSnackbar, SnackProgressBarManager.LENGTH_INDEFINITE);

        final FileData fileData = fileExplorerAdapter.getItem(index);

        RequestDeleteFileData requestDeleteFileData = new RequestDeleteFileData();
        requestDeleteFileData.setPath(fileData.getPath());

        Watch.get().deleteFile(requestDeleteFileData)
                .continueWithTask(new Continuation<ResultDeleteFile, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<ResultDeleteFile> task) throws Exception {
                        snackProgressBarManager.dismissAll();

                        boolean success = task.isSuccessful();
                        int result = task.getResult().getResultDeleteFileData().getResult();
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

                            return Tasks.forException(task.getException());
                        }
                    }
                });
    }

    @OnItemClick(R.id.activity_file_explorer_list)
    public void onItemClick(int position) {
        FileData fileData = fileExplorerAdapter.getItem(position);
        if (fileData.isDirectory()) {
            loadPath(fileData.getPath());
        }
    }

    @OnClick(R.id.activity_file_explorer_upload)
    public void onUpload() {
        Intent i = new Intent(this, FilePickerActivity.class);

        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, FILE_UPLOAD_CODE);
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
                            DirectoryData directoryData = directory.getDirectoryData();
                            if (directoryData.getResult() == Transport.RESULT_OK) {
                                getSupportActionBar().setTitle(path.equals("/") ? "/" : directoryData.getName());

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
                        } else {
                            taskCompletionSource.setException(task.getException());
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
        listView.setVisibility(View.GONE);
        materialProgressBar.setVisibility(View.VISIBLE);
    }

    private void stateReady() {
        listView.setVisibility(View.VISIBLE);
        materialProgressBar.setVisibility(View.GONE);
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
}
