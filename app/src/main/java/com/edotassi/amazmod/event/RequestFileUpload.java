package com.edotassi.amazmod.event;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.data.FileUploadData;

public class RequestFileUpload {

    private FileUploadData fileUploadData;

    public RequestFileUpload(DataBundle dataBundle) {
        fileUploadData = FileUploadData.fromDataBundle(dataBundle);
    }

    public FileUploadData getFileUploadData() {
        return fileUploadData;
    }
}
