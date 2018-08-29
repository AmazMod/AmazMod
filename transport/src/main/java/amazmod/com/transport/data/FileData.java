package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;

import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.SafeParcelable;

import amazmod.com.transport.Transportable;

public class FileData extends Transportable implements SafeParcelable {

    public static final String EXTRA = "file";
    public static final String PATH = "path";
    public static final String NAME = "name";
    public static final String IS_DIRECTORY = "isDirectory";
    public static final String SIZE = "size";
    public static final String CREATION_DATE = "CREATION_DATE";
    public static final String LAST_EDIT_DATE = "LAST_EDIT_DATE";
    public static final String PERMISSIONS = "permissions";

    private String path;
    private String name;
    private boolean isDirectory;
    private long size;
    private long creationDate;
    private long lastEditDate;
    private long permissions;

    public FileData() {}

    protected FileData(Parcel in) {
        path = in.readString();
        name = in.readString();
        isDirectory = in.readByte() != 0;
        size = in.readLong();
        creationDate = in.readLong();
        lastEditDate = in.readLong();
        permissions = in.readLong();
    }

    public static final Creator<FileData> CREATOR = new Creator<FileData>() {
        @Override
        public FileData createFromParcel(Parcel in) {
            return new FileData(in);
        }

        @Override
        public FileData[] newArray(int size) {
            return new FileData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(PATH, path);
        dataBundle.putString(NAME, name);
        dataBundle.putBoolean(IS_DIRECTORY, isDirectory);
        dataBundle.putLong(SIZE, size);
        dataBundle.putLong(CREATION_DATE, creationDate);
        dataBundle.putLong(LAST_EDIT_DATE, lastEditDate);
        dataBundle.putLong(PERMISSIONS, permissions);

        return dataBundle;
    }

    public static FileData fromDataBundle(DataBundle dataBundle) {
        FileData fileData = new FileData();

        fileData.setPath(dataBundle.getString(PATH));
        fileData.setName(dataBundle.getString(NAME));
        fileData.setDirectory(dataBundle.getBoolean(IS_DIRECTORY));
        fileData.setSize(dataBundle.getLong(SIZE));
        fileData.setCreationDate(dataBundle.getLong(CREATION_DATE));
        fileData.setLastEditDate(dataBundle.getLong(LAST_EDIT_DATE));
        fileData.setPermissions(dataBundle.getLong(PERMISSIONS));

        return fileData;
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

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(name);
        dest.writeByte((byte) (isDirectory ? 1 : 0));
        dest.writeLong(size);
        dest.writeLong(creationDate);
        dest.writeLong(lastEditDate);
        dest.writeLong(permissions);
    }
}
