package com.amazmod.service.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;

import amazmod.com.transport.Transport;
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

        directoryData.setResult(Transport.RESULT_OK);

        return directoryData;
    }

    public static DirectoryData notFound() {
        DirectoryData directoryData = new DirectoryData();
        directoryData.setResult(Transport.RESULT_NOT_FOUND);
        return directoryData;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
