package com.edotassi.amazmod.support;

import android.os.Environment;

import amazmod.com.transport.Constants;

import java.io.File;

public class DownloadHelper {

    public static boolean deleteDownloadedFile(String name, byte mode) {
        File fileToDelete = getDownloadedFile(name, mode);
        return !fileToDelete.exists() || fileToDelete.delete();
    }

    public static boolean checkDownloadDirExist(byte mode) {
        String downloadDirPath = getDownloadDir(mode);
        File downloadDir = new File(downloadDirPath);
        return downloadDir.exists() || downloadDir.mkdir();
    }

    public static File getDownloadedFile(String name, byte mode) {
        return new File(getDownloadDir(mode) + "/" + name);
    }

    public static String getDownloadDir(byte mode) {
        if (mode == Constants.MODE_DOWNLOAD)
            return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.DOWNLOAD_DIRECTORY;
        else if (mode == Constants.MODE_SCREENSHOT)
            return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.SCREENSHOT_DIRECTORY;
        else return null;
    }
}
