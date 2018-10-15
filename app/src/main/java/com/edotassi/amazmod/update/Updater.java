package com.edotassi.amazmod.update;

public interface Updater {
    void updateCheckFailed();
    void updateAvailable(int version);
    void updateDownloadProgress(String filename, int progress);
    void updateDownloadFailed();
    void updateDownloadCompleted();
}
