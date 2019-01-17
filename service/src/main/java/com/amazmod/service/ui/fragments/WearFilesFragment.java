package com.amazmod.service.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.adapters.AppInfoAdapter;
import com.amazmod.service.helper.RecyclerTouchListener;
import com.amazmod.service.support.AppInfo;
import com.amazmod.service.ui.FileViewerWebViewActivity;
import com.amazmod.service.util.DeviceUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class WearFilesFragment extends Fragment {

    private RelativeLayout wearFilesFrameLayout;
	private WearableListView listView;
    private TextView mHeader;
    private ProgressBar progressBar;

    private Context mContext;

    private List<AppInfo> fileInfoList;
    private AppInfoAdapter mAdapter;

    private File mCurrentDir;
    private Stack<File> mHistory;

    private static final String REFRESH = "Refresh";
    private static final String PARENT_DIR = "..";

    private static final String APK_MIME = "application/vnd.android.package-archive";
    private static final String PNG_MIME = "image/png";
    private static final String JPG_MIME = "image/jpeg";
    private static final String TXT_MIME = "text/plain";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Log.i(Constants.TAG,"WearFilesFragment onAttach mContext: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(Constants.TAG,"WearFilesFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(Constants.TAG,"WearFilesFragment onCreateView");
        return inflater.inflate(R.layout.activity_wear_files, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(Constants.TAG,"WearFilesFragment onViewCreated");

        init();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onItemClick(int position) {

        final String fileName = fileInfoList.get(position).getAppName();
        String filePath = fileInfoList.get(position).getVersionName();

        Log.i(Constants.TAG,"WearFilesFragment onItemClick filePath: " + filePath);

        if (PARENT_DIR.equals(fileName) && PARENT_DIR.equals(filePath)) {
            mAdapter.clear();
            mCurrentDir = getPreviousDir();
            loadFiles(mCurrentDir);

        } else if (REFRESH.equals(fileName) && REFRESH.equals(filePath)) {

            if (!(fileInfoList == null))
                fileInfoList.clear();

            loadFiles(mCurrentDir);

        } else {

            File file = new File (filePath);
            if (file.exists()) {

                if (file.isDirectory())
                    changeDir(file);
                else
                    openFile(file);

            }
        }

    }

    public void onItemLongClick(int position) {

        final String fileName = fileInfoList.get(position).getAppName();
        String filePath = fileInfoList.get(position).getVersionName();

        Log.i(Constants.TAG,"WearFilesFragment onLongClick filePath: " + filePath);

        if (!(PARENT_DIR.equals(fileName) && PARENT_DIR.equals(filePath))
                && !(REFRESH.equals(fileName) && REFRESH.equals(filePath))) {

            File file = new File (filePath);
            if (file.exists()) {
                deleteFile(file);
            }
        }

    }

    private void init() {

        wearFilesFrameLayout = getActivity().findViewById(R.id.wear_files_frame_layout);
        listView = getActivity().findViewById(R.id.wear_files_list);
        mHeader = getActivity().findViewById(R.id.wear_files_header);
        progressBar = getActivity().findViewById(R.id.wear_files_loading_spinner);

        listView.setLongClickable(true);
        listView.setGreedyTouchMode(true);
        listView.addOnScrollListener(mOnScrollListener);

        listView.addOnItemTouchListener(new RecyclerTouchListener(mContext, listView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.d(Constants.TAG, "WearFilesFragment addOnItemTouchListener onClick");
                onItemClick(position);
            }

            @Override
            public void onLongClick(View view, int position) {
                Log.d(Constants.TAG, "WearFilesFragment addOnItemTouchListener onLongClick");
                onItemLongClick(position);
            }
        }));

        List<AppInfo> fileInfoList = new ArrayList<>();
        mAdapter = new AppInfoAdapter(mContext, fileInfoList);
        listView.setAdapter(mAdapter);

        mHistory = new Stack<>();

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            mCurrentDir = Environment.getExternalStorageDirectory();
            Log.i(Constants.TAG, "WearFilesFragment init mCurrentDir: " + String.valueOf(mCurrentDir));

        } else {

            Log.e(Constants.TAG, "External storage unavailable");
        }

        mHeader.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showToast("Free space: " + formatBytes(getFreeSpace(mCurrentDir.getPath())));
                return true;
            }
        });

        loadFiles(mCurrentDir);
    }

    @SuppressLint("CheckResult")
    private void loadFiles(final File file) {
        Log.d(Constants.TAG, "WearFilesFragment loadFiles file: " + file.toString());
        wearFilesFrameLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        Flowable.fromCallable(new Callable<List<AppInfo>>() {
            @Override
            public List<AppInfo> call() throws Exception {
                Log.i(Constants.TAG, "WearFilesFragment loadFiles call");

                List<AppInfo> appInfoList = getFilesList(file);
                WearFilesFragment.this.fileInfoList = appInfoList;

                Log.d(Constants.TAG, "WearFilesFragment loadFiles appInfoList.size: " + appInfoList.size());
                return appInfoList;
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<List<AppInfo>>() {
                    @Override
                    public void accept(final List<AppInfo> appInfoList) throws Exception {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(Constants.TAG,"WearFilesFragment loadFiles run appInfoList.size: " + appInfoList.size());

                                setHeader();
                                mAdapter.addAll(appInfoList);

                                progressBar.setVisibility(View.GONE);
                                wearFilesFrameLayout.setVisibility(View.VISIBLE);

                                listView.post(new Runnable() {
                                    public void run() {
                                        Log.d(Constants.TAG, "WearFilesFragment loadFiles scrollToTop");
                                        listView.smoothScrollToPosition(0);
                                    }
                                });

                            }
                        });
                    }
                });

    }

    private void setHeader() {
        if (isRoot())
            mHeader.setText(getResources().getString(R.string.root));
        else
            mHeader.setText(mCurrentDir.getName());

    }

    private void changeDir(File file) {
        Log.d(Constants.TAG, "WearFilesFragment changeDir file: " + file.toString());

        mAdapter.clear();
        setPreviousDir(mCurrentDir);
        mCurrentDir = file;
        loadFiles(mCurrentDir);

    }

    private void openFile(File file) {

        Uri fileUri = Uri.fromFile(file);
        Log.d(Constants.TAG, "WearFilesFragment openFile fileUri: " + fileUri.toString());

        String mimeType = getMimeType(fileUri);

        if (mimeType != null) {

            switch (mimeType) {
                case APK_MIME:
                    openApk(file);
                    return;
                case PNG_MIME:
                case JPG_MIME:
                case TXT_MIME:
                    openFileViewer(fileUri, mimeType);
                    return;
                default:
            }

            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(fileUri, mimeType);
                getActivity().startActivity(i);
            } catch (ActivityNotFoundException e) {
                showToast(String.format("No app found to handle %s!", mimeType));
            }
        } else {
            showToast("Unknown file type!");
        }
    }


    // The following code ensures that the title scrolls as the user scrolls up
    // or down the list
    private WearableListView.OnScrollListener mOnScrollListener =
            new WearableListView.OnScrollListener() {
                @Override
                public void onAbsoluteScrollChange(int i) {
                    // Only scroll the title up from its original base position
                    // and not down.
                    if (i > 0) {
                        mHeader.setY(-i);
                    }
                }

                @Override
                public void onScroll(int i) {
                    // Placeholder
                }

                @Override
                public void onScrollStateChanged(int i) {
                    // Placeholder
                }

                @Override
                public void onCentralPositionChanged(int i) {
                    // Placeholder
                }
            };

    private List<AppInfo> getFilesList(File file) {

        final Drawable refreshDrawable = getResources().getDrawable(R.drawable.outline_refresh_white_24);
        final Drawable folderDrawable = getResources().getDrawable(R.drawable.outline_folder_white_24);

        final List<File> allFiles = getAllFiles(file);
        List<AppInfo> appInfoList = new ArrayList<>();
        AppInfo fileInfo;

        Log.d(Constants.TAG, "WearFilesFragment getFilesList allFiles.size: " + allFiles.size());

        if (!isRoot()) {
            fileInfo = new AppInfo(PARENT_DIR, getResources().getString(R.string.go_up), PARENT_DIR, "0", folderDrawable);
            appInfoList.add(fileInfo);
        }

        if (allFiles.size() > 0 ) {
            for (File f : allFiles) {
                fileInfo = createFileInfo(f);
                appInfoList.add(fileInfo);
            }

        }

        fileInfo = new AppInfo(REFRESH, "Reload files", REFRESH, "0", refreshDrawable);
        appInfoList.add(fileInfo);

        return appInfoList;
    }

    private AppInfo createFileInfo(File file) {
        Log.d(Constants.TAG, "WearFilesFragment createFileInfo file: " + file.toString());

        final AppInfo appInfo = new AppInfo();

        appInfo.setAppName(file.getName());
        appInfo.setVersionName(file.getAbsolutePath());
        appInfo.setSize("");
        //Log.d(Constants.TAG, "WearFilesFragment createFileInfo absolutePath: "
        //        + appInfo.getVersionName() + " \\ path: " + appInfo.getSize());

        if (file.isDirectory()) {
            appInfo.setPackageName("Dir");
            appInfo.setIcon(getResources().getDrawable(R.drawable.outline_folder_white_24));
        } else {
            appInfo.setPackageName(formatBytes(file.length()));
            appInfo.setIcon(getResources().getDrawable(R.drawable.outline_insert_drive_file_white_24));
        }

        return appInfo;
    }


    public boolean isRoot() {
        return mHistory.isEmpty();
    }

    public File getPreviousDir() {
        return mHistory.pop();
    }

    public void setPreviousDir(File file) {
        mHistory.add(file);

    }

    public List<File> getAllFiles(File f) {
        Log.d(Constants.TAG, "WearFilesFragment getAllFiles f: " + f.toString());

        File[] allFiles = f.listFiles();
        List<File> dirs = new ArrayList<>();
        List<File> files = new ArrayList<>();

        for (File file : allFiles) {
            String fileName = file.getName();
            Log.d(Constants.TAG, "WearFilesFragment getAllFiles file: " + fileName);

            if  (!fileName.startsWith(".")) {
                if (file.isDirectory()) {
                    dirs.add(file);
                } else {
                    files.add(file);
                }
            }
        }

        if (!dirs.isEmpty())
            Collections.sort(dirs);
        if (!files.isEmpty()) {
            Collections.sort(files);
            dirs.addAll(files);
        }

        Log.d(Constants.TAG, "WearFilesFragment getAllFiles dirs.size: " + dirs.size());
        return dirs;
    }

    public String getMimeType(Uri uri) {

        final String fileUri = uri.toString();
        Log.d(Constants.TAG, "WearFilesFragment getMimeType uri: " + fileUri);

        String mimeType = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.getPath());

        Log.d(Constants.TAG, "WearFilesFragment getMimeType extension: " + extension);

        if (MimeTypeMap.getSingleton().hasExtension(extension)) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        if (mimeType == null && (fileUri.toLowerCase().endsWith(".jpg") || fileUri.toLowerCase().endsWith(".jpeg")))
            mimeType = JPG_MIME;

        Log.d(Constants.TAG, "WearFilesFragment getMimeType mimeType: " + mimeType);

        return mimeType;
    }

    public static long getFreeSpace(String path) {
        Log.d(Constants.TAG, "WearFilesFragment getFreeSpace path: " + path);

        File file = new File(path);
        long freeSpace = file.getFreeSpace();
        long usableSpace = file.getUsableSpace();
        Log.d(Constants.TAG, "WearFilesFragment getFreeSpace freeSpace: " + String.valueOf(freeSpace)
                + " \\ usableSpace: " + String.valueOf(usableSpace));

        StatFs stat = new StatFs(path);
        return (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
    }

    @SuppressLint("DefaultLocale")
    public static String formatBytes(long bytes) {

        Log.d(Constants.TAG, "WearFilesFragment formatBytes bytes: " + bytes);

        String retStr;
        long kb = 1024L;
        long mb = kb * kb;
        long gb = kb * mb;

        if (bytes > gb) {
            float gbs = bytes / gb;
            retStr = String.format("%.2f", gbs) + " GB";
        }
        else if (bytes > mb) {
            float mbs = bytes / mb;
            retStr = String.format("%.2f", mbs) + " MB";

        } else if (bytes > kb) {
            float kbs = bytes / kb;
            retStr = String.format("%.2f", kbs) + " kB";
        } else
            retStr = (Long.valueOf(bytes)).toString() + " B";

        return retStr;
    }

    private void openApk(final File file) {
        Log.d(Constants.TAG, "WearFilesFragment openApk file: " + file.toString());

        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.install_app))
                .setMessage(getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        DeviceUtil.installApkAdb(mContext, file, false);
                        showToast("Please wait until installation finishes…");
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();

    }

    private void openFileViewer(Uri fileUri, String mimeType){

        Intent intent = new Intent(mContext, FileViewerWebViewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(FileViewerWebViewActivity.FILE_URI, fileUri.toString());
        intent.putExtra(FileViewerWebViewActivity.MIME_TYPE, mimeType);
        mContext.startActivity(intent);

    }

    private void deleteFile(final File file) {
        Log.d(Constants.TAG, "WearFilesFragment deleteFile file: " + file.toString());

        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.delete))
                .setMessage(getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            boolean result = file.delete();
                            if (result) {
                                if (!(fileInfoList == null))
                                    fileInfoList.clear();
                                loadFiles(mCurrentDir);
                                showToast("File deleted");
                            } else
                                showToast("Error deleting file!");
                        } catch (Exception ex) {
                            Log.e(Constants.TAG, "WearFilesFragment deleteFile Exception" + ex.getMessage(), ex);
                            showToast("Error deleting file!");
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    public static WearFilesFragment newInstance() {
        Log.i(Constants.TAG,"WearFilesFragment newInstance");
        return new WearFilesFragment();
    }

}