package com.amazmod.service.util;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;

import amazmod.com.transport.data.DirectoryData;
import amazmod.com.transport.data.FileData;

public class FileDataFactory {

    public static FileData fromFile(File file) {
        FileData fileData = new FileData();

        fileData.setPath(file.getAbsolutePath());
        fileData.setName(file.getName());
        fileData.setLastEditDate(file.lastModified());
        fileData.setSize(file.length());
        fileData.setDirectory(file.isDirectory());

        return fileData;
    }

    public static DirectoryData directoryFromFile(File file) {
        return directoryFromFile(file, new ArrayList<FileData>());
    }

    public static DirectoryData directoryFromFile(File file, ArrayList<FileData> filesData) {
        DirectoryData directoryData = new DirectoryData();

        directoryData.setPath(file.getAbsolutePath());
        directoryData.setName(file.getName());
        directoryData.setLastEditDate(file.lastModified());

        Gson gson = new Gson();
        directoryData.setFiles(gson.toJson(filesData));

        directoryData.setResult(DirectoryData.RESULT_OK);

        return directoryData;
    }

    public static DirectoryData notFound() {
        DirectoryData directoryData = new DirectoryData();
        directoryData.setResult(DirectoryData.RESULT_NOT_FOUND);
        return directoryData;
    }
}
