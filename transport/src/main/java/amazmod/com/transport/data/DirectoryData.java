package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import java.util.ArrayList;

import amazmod.com.transport.Transportable;

public class DirectoryData extends Transportable implements Parcelable {

    public static final String EXTRA = "directory";
    public static final String PATH = "path";
    public static final String NAME = "name";
    public static final String FILES = "files";
    public static final String CREATION_DATE = "creationDate";
    public static final String LAST_EDIT_DATE = "lastEditDate";
    public static final String PERMISSIONS = "permissions";
    public static final String RESULT = "result";

    private String path;
    private String name;
    private String jsonFiles;
    private long creationDate;
    private long lastEditDate;
    private long permissions;
    private long result;

    public DirectoryData() {
    }

    protected DirectoryData(Parcel in) {
        path = in.readString();
        name = in.readString();
        jsonFiles = in.readString();
        creationDate = in.readLong();
        lastEditDate = in.readLong();
        permissions = in.readLong();
        result = in.readLong();
    }

    public static final Creator<DirectoryData> CREATOR = new Creator<DirectoryData>() {
        @Override
        public DirectoryData createFromParcel(Parcel in) {
            return new DirectoryData(in);
        }

        @Override
        public DirectoryData[] newArray(int size) {
            return new DirectoryData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(PATH, path);
        dataBundle.putString(NAME, name);
        dataBundle.putString(FILES, jsonFiles);
        dataBundle.putLong(CREATION_DATE, creationDate);
        dataBundle.putLong(LAST_EDIT_DATE, lastEditDate);
        dataBundle.putLong(PERMISSIONS, permissions);
        dataBundle.putLong(RESULT, result);

        return dataBundle;
    }

    public static DirectoryData fromDataBundle(DataBundle dataBundle) {
        DirectoryData directoryData = new DirectoryData();

        directoryData.setPath(dataBundle.getString(PATH));
        directoryData.setName(dataBundle.getString(NAME));
        directoryData.setFiles(dataBundle.getString(FILES));
        directoryData.setCreationDate(dataBundle.getLong(CREATION_DATE));
        directoryData.setLastEditDate(dataBundle.getLong(LAST_EDIT_DATE));
        directoryData.setPermissions(dataBundle.getLong(PERMISSIONS));
        directoryData.setResult(dataBundle.getLong(RESULT));

        return directoryData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFiles() {
        return jsonFiles;
    }

    public void setFiles(String files) {
        this.jsonFiles = files;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(long lastEditDate) {
        this.lastEditDate = lastEditDate;
    }

    public long getPermissions() {
        return permissions;
    }

    public void setPermissions(long permissions) {
        this.permissions = permissions;
    }

    public long getResult() {
        return result;
    }

    public void setResult(long result) {
        this.result = result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(name);
        dest.writeString(jsonFiles);
        dest.writeLong(creationDate);
        dest.writeLong(lastEditDate);
        dest.writeLong(permissions);
        dest.writeLong(result);
    }
}
