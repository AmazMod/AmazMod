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
import android.os.Handler;
import android.os.PowerManager;
import android.os.StatFs;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.R;
import com.amazmod.service.adapters.AppInfoAdapter;
import com.amazmod.service.helper.RecyclerTouchListener;
import com.amazmod.service.support.AppInfo;
import com.amazmod.service.ui.FileViewerWebViewActivity;
import com.amazmod.service.util.CaseInsensitiveFileComparator;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.ExecCommand;

import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    //private static final String REFRESH = "Refresh";
    private static final String PARENT_DIR = "..";

    private static final String APK_MIME = "application/vnd.android.package-archive";
    private static final String PNG_MIME = "image/png";
    private static final String JPG_MIME = "image/jpeg";
    private static final String TXT_MIME = "text/plain";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.info("WearFilesFragment onAttach mContext: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("WearFilesFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.info("WearFilesFragment onCreateView");
        return inflater.inflate(R.layout.fragment_wear_files, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.info("WearFilesFragment onViewCreated");

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

        Logger.info("WearFilesFragment onItemClick filePath: " + filePath);

        if (PARENT_DIR.equals(fileName) && PARENT_DIR.equals(filePath)) {
            mAdapter.clear();
            mCurrentDir = getPreviousDir();
            loadFiles(mCurrentDir);

        } else if (getResources().getString(R.string.refresh).equals(fileName) && getResources().getString(R.string.refresh).equals(filePath)) {

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

        Logger.info("WearFilesFragment onLongClick filePath: " + filePath);

        if (!(PARENT_DIR.equals(fileName) && PARENT_DIR.equals(filePath))
                && !(getResources().getString(R.string.refresh).equals(fileName) && getResources().getString(R.string.refresh).equals(filePath))) {

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
        mHeader.setSelected(true);
        progressBar = getActivity().findViewById(R.id.wear_files_loading_spinner);

        listView.setLongClickable(true);
        listView.setGreedyTouchMode(true);
        listView.addOnScrollListener(mOnScrollListener);

        listView.addOnItemTouchListener(new RecyclerTouchListener(mContext, listView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Logger.debug( "WearFilesFragment addOnItemTouchListener onClick");
                onItemClick(position);
            }

            @Override
            public void onLongClick(View view, int position) {
                Logger.debug( "WearFilesFragment addOnItemTouchListener onLongClick");
                onItemLongClick(position);
            }
        }));

        List<AppInfo> fileInfoList = new ArrayList<>();
        mAdapter = new AppInfoAdapter(mContext, fileInfoList);
        listView.setAdapter(mAdapter);

        mHistory = new Stack<>();

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            mCurrentDir = Environment.getExternalStorageDirectory();
            Logger.info( "WearFilesFragment init mCurrentDir: " + String.valueOf(mCurrentDir));

        } else {

            Logger.error( "External storage unavailable");
        }

        mHeader.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showToast(getString(R.string.free_space)+": " + formatBytes(getFreeSpace(mCurrentDir.getPath())));
                return true;
            }
        });

        loadFiles(mCurrentDir);
    }

    @SuppressLint("CheckResult")
    private void loadFiles(final File file) {
        Logger.debug( "WearFilesFragment loadFiles file: " + file.toString());
        wearFilesFrameLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        Flowable.fromCallable(new Callable<List<AppInfo>>() {
            @Override
            public List<AppInfo> call() {
                Logger.info( "WearFilesFragment loadFiles call");

                List<AppInfo> appInfoList = getFilesList(file);
                WearFilesFragment.this.fileInfoList = appInfoList;

                Logger.debug( "WearFilesFragment loadFiles appInfoList.size: " + appInfoList.size());
                return appInfoList;
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<List<AppInfo>>() {
                    @Override
                    public void accept(final List<AppInfo> appInfoList) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Logger.info("WearFilesFragment loadFiles run appInfoList.size: " + appInfoList.size());

                                setHeader();
                                mAdapter.addAll(appInfoList);

                                progressBar.setVisibility(View.GONE);
                                wearFilesFrameLayout.setVisibility(View.VISIBLE);

                                listView.post(new Runnable() {
                                    public void run() {
                                        Logger.debug("WearFilesFragment loadFiles scrollToTop");
                                        listView.smoothScrollToPosition(0);
                                    }
                                });

                            }
                        });
                    }
                }, throwable -> {
                        Logger.error("WearFilesFragment: Flowable: subscribeOn: " + throwable.getMessage());
                });

    }

    private void setHeader() {
        if (isRoot())
            mHeader.setText(getResources().getString(R.string.root));
        else
            mHeader.setText(mCurrentDir.getName());

    }

    private void changeDir(File file) {
        Logger.debug( "WearFilesFragment changeDir file: " + file.toString());

        mAdapter.clear();
        setPreviousDir(mCurrentDir);
        mCurrentDir = file;
        loadFiles(mCurrentDir);

    }

    private void openFile(File file) {

        Uri fileUri = Uri.fromFile(file);
        Logger.debug( "WearFilesFragment openFile fileUri: " + fileUri.toString());

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
                showToast(String.format(getString(R.string.no_app_found)+" %s!", mimeType));
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

        Logger.debug( "WearFilesFragment getFilesList allFiles.size: " + allFiles.size());

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

        fileInfo = new AppInfo(getResources().getString(R.string.refresh), getResources().getString(R.string.reload_files), getResources().getString(R.string.refresh), "0", refreshDrawable);
        appInfoList.add(fileInfo);

        return appInfoList;
    }

    private AppInfo createFileInfo(File file) {
        Logger.debug( "WearFilesFragment createFileInfo file: " + file.toString());

        final AppInfo appInfo = new AppInfo();

        appInfo.setAppName(file.getName());
        appInfo.setVersionName(file.getAbsolutePath());
        appInfo.setSize("");
        //Logger.debug( "WearFilesFragment createFileInfo absolutePath: "
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
        Logger.debug( "WearFilesFragment getAllFiles f: " + f.toString());

        File[] allFiles = f.listFiles();
        List<File> dirs = new ArrayList<>();
        List<File> files = new ArrayList<>();

        for (File file : allFiles) {
            String fileName = file.getName();
            Logger.debug( "WearFilesFragment getAllFiles file: " + fileName);

            if  (!fileName.startsWith(".")) {
                if (file.isDirectory()) {
                    dirs.add(file);
                } else {
                    files.add(file);
                }
            }
        }

        if (!dirs.isEmpty())
            Collections.sort(dirs, new CaseInsensitiveFileComparator());
        if (!files.isEmpty()) {
            Collections.sort(files, new CaseInsensitiveFileComparator());
            dirs.addAll(files);
        }

        Logger.debug( "WearFilesFragment getAllFiles dirs.size: " + dirs.size());
        return dirs;
    }

    public String getMimeType(Uri uri) {

        final String fileUri = uri.toString();
        Logger.debug( "WearFilesFragment getMimeType uri: " + fileUri);

        String mimeType = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());

        Logger.debug( "WearFilesFragment getMimeType extension: " + extension);

        if (MimeTypeMap.getSingleton().hasExtension(extension)) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        if (mimeType == null && (fileUri.toLowerCase().endsWith(".jpg") || fileUri.toLowerCase().endsWith(".jpeg")))
            mimeType = JPG_MIME;

        Logger.debug( "WearFilesFragment getMimeType mimeType: " + mimeType);

        return mimeType;
    }

    public static long getFreeSpace(String path) {
        Logger.debug( "WearFilesFragment getFreeSpace path: " + path);

        File file = new File(path);
        long freeSpace = file.getFreeSpace();
        long usableSpace = file.getUsableSpace();
        Logger.debug( "WearFilesFragment getFreeSpace freeSpace: " + String.valueOf(freeSpace)
                + " \\ usableSpace: " + String.valueOf(usableSpace));

        StatFs stat = new StatFs(path);
        return (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
    }

    @SuppressLint("DefaultLocale")
    public static String formatBytes(long bytes) {

        Logger.debug( "WearFilesFragment formatBytes bytes: " + bytes);

        String retStr;
        float kb = 1024L;
        float mb = kb * kb;
        float gb = kb * mb;

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
        Logger.debug("WearFilesFragment openApk file: " + file.toString());

        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.install_app))
                .setMessage(getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        showToast(getString(R.string.please_wait_installation));
                        if (file.toString().contains("service-")) {
                            //DeviceUtil.systemPutAdb(mContext,"screen_off_timeout", "200000");
                            new ExecCommand("adb shell settings put system screen_off_timeout 200000");
                            sleep(1000);
                            new ExecCommand("adb install -r " + file.getAbsolutePath());
                        } else {
                            final PowerManager.WakeLock myWakeLock = DeviceUtil.installApkAdb(mContext, file, false);
                            new Handler().postDelayed(new Runnable() { //Release wakelock after 10s when installing from File Manager
                                public void run() {
                                    if (myWakeLock != null && myWakeLock.isHeld())
                                        myWakeLock.release();
                                }
                            }, 10000 /* 10s */);
                        }
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
        Logger.debug( "WearFilesFragment deleteFile file: " + file.toString());

        new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.delete))
                .setMessage(getResources().getString(R.string.confirmation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            boolean result = true;
                            if (file.isDirectory()){
                                try {
                                    deleteDirectoryRecursionJava(file);
                                }catch (IOException ex){
                                    result = false;
                                }
                            }else{
                                result = file.delete();
                            }
                            if (result) {
                                if (!(fileInfoList == null))
                                    fileInfoList.clear();
                                loadFiles(mCurrentDir);
                                showToast(getString(R.string.deleted)+"!");
                            } else
                                showToast(getString(R.string.error_deleting)+"!");
                        } catch (Exception ex) {
                            Logger.error(ex,"WearFilesFragment deleteFile Exception" + ex.getMessage());
                            showToast(getString(R.string.error_deleting)+"!");
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }


    void deleteDirectoryRecursionJava(File file) throws IOException {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectoryRecursionJava(entry);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete " + file);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Logger.error(e.getMessage());
        }
    }

    private void showToast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    public static WearFilesFragment newInstance() {
        Logger.info("WearFilesFragment newInstance");
        return new WearFilesFragment();
    }

}