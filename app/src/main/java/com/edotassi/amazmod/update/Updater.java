package com.edotassi.amazmod.update;

import java.io.File;

public interface Updater {
    void updateCheckFailed();
    void updateAvailable(int version);
    void updateDownloadProgress(String filename, int progress);
    void updateDownloadFailed();
    void updateDownloadCompleted(File updateFile, String filename);
}
