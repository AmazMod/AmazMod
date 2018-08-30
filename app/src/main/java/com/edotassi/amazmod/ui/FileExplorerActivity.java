package com.edotassi.amazmod.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.FileExplorerAdapter;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.common.util.Strings;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import amazmod.com.transport.data.DirectoryData;
import amazmod.com.transport.data.FileData;
import amazmod.com.transport.data.RequestDirectoryData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class FileExplorerActivity extends AppCompatActivity {

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
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @OnItemClick(R.id.activity_file_explorer_list)
    public void onItemClick(int position) {
        FileData fileData = fileExplorerAdapter.getItem(position);
        if (fileData.isDirectory()) {
            loadFiles(fileData.getPath());
        }
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
                            if (directoryData.getResult() == DirectoryData.RESULT_OK) {
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
