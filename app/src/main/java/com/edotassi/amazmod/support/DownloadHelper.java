package com.edotassi.amazmod.support;

import android.os.Environment;

import amazmod.com.transport.Constants;

import java.io.File;

public class DownloadHelper {

    public static boolean deleteDownloadedFile(String name) {
        File fileToDelete = getDownloadedFile(name);
        return !fileToDelete.exists() || fileToDelete.delete();
    }

    public static boolean checkDownloadDirExist() {
        String downloadDirPath = getDownloadDir();
        File downloadDir = new File(downloadDirPath);
        return downloadDir.exists() || downloadDir.mkdir();
    }

    public static File getDownloadedFile(String name) {
        return new File(getDownloadDir() + "/" + name);
    }

    public static String getDownloadDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.DOWNLOAD_DIRECTORY;
    }
}
