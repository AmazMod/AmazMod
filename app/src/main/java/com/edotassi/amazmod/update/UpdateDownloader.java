package com.edotassi.amazmod.update;

import android.content.Context;
import android.os.Environment;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.OnProgressListener;
import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.downloader.Progress;

import java.io.File;

public class UpdateDownloader {

    private int currentDownload = -1;

    public void start(Context context, String url, final Updater updater) {
        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(false)
                .build();
        PRDownloader.initialize(context, config);

        final String downloadUrl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        final String filename = new File(url).getName();
        currentDownload = PRDownloader
                .download(url, downloadUrl, filename)
                .build()
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {
                        double perc = ((progress.currentBytes / (double) progress.totalBytes) * 100.0f);
                        updater.updateDownloadProgress(filename, (int) perc);
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        updater.updateDownloadCompleted(new File(downloadUrl + "/" + filename), filename);
                    }

                    @Override
                    public void onError(Error error) {
                        updater.updateDownloadFailed();
                    }
                });
    }

    public void cancel() {
        if (currentDownload != -1) {
            PRDownloader.cancel(currentDownload);
        }
    }
}
