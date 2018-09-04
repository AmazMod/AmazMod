package com.edotassi.amazmod.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.FileExplorerAdapter;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.common.util.Strings;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.DirectoryData;
import amazmod.com.transport.data.FileData;
import amazmod.com.transport.data.RequestDirectoryData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import de.mateware.snacky.Snacky;
import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class FileExplorerActivity extends AppCompatActivity {

    private final int FILE_UPLOAD_CODE = 1;

    @BindView(R.id.activity_file_explorer_list)
    ListView listView;
    @BindView(R.id.activity_file_explorer_progress)
    MaterialProgressBar materialProgressBar;

    private List<FileData> fileDataList;
    private FileExplorerAdapter fileExplorerAdapter;

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

        loadFiles("/");

        registerForContextMenu(listView);
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
                    ArrayList<String> paths = data.getStringArrayListExtra
                            (FilePickerActivity.EXTRA_PATHS);

                    if (paths != null) {
                        for (String path : paths) {
                            Uri uri = Uri.parse(path);
                            new MaterialDialog.Builder(this)
                                    .title("File picked")
                                    .content(uri.getPath())
                                    .show();
                        }
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
                return true;
        }

        return false;
    }

    @OnItemClick(R.id.activity_file_explorer_list)
    public void onItemClick(int position) {
        FileData fileData = fileExplorerAdapter.getItem(position);
        if (fileData.isDirectory()) {
            loadFiles(fileData.getPath());
        }
    }

    @OnClick(R.id.activity_file_explorer_upload)
    public void onUpload() {
        // This always works
        Intent i = new Intent(this, FilePickerActivity.class);
        // This works if you defined the intent filter
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        // Set these depending on your use case. These are the defaults.
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, FILE_UPLOAD_CODE);

    }

    private void loadFiles(final String path) {
        stateLoading();

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
                                getSupportActionBar().setTitle(path.equals("/") ? "/" : directoryData.getName());

                                Gson gson = new Gson();
                                String jsonFiles = directoryData.getFiles();
                                List<FileData> filesData = gson.fromJson(jsonFiles, new TypeToken<List<FileData>>() {
                                }.getType());

                                if (!path.equals("/")) {
                                    FileData parentDirectory = new FileData();
                                    parentDirectory.setName("..");
                                    parentDirectory.setDirectory(true);

                                    String[] pathComponents = path.split("/");
                                    String parentPath = TextUtils.join("/", Arrays.copyOf(pathComponents, pathComponents.length - 1));
                                    if (Strings.isEmptyOrWhitespace(parentPath)) {
                                        parentPath = "/";
                                    }

                                    parentDirectory.setPath(parentPath);

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
                            } else {
                                Snacky.builder()
                                        .setActivity(FileExplorerActivity.this)
                                        .setText(R.string.reading_files_failed)
                                        .setDuration(Snacky.LENGTH_SHORT)
                                        .build().show();
                            }
                        } else {
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
    }

    private void stateLoading() {
        listView.setVisibility(View.GONE);
        materialProgressBar.setVisibility(View.VISIBLE);
    }

    private void stateReady() {
        listView.setVisibility(View.VISIBLE);
        materialProgressBar.setVisibility(View.GONE);
    }
}
