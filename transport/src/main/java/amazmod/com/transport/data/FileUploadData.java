package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class FileUploadData extends Transportable implements Parcelable {

    public static final String EXTRA = "file_upload";

    public static final String PATH = "path";
    public static final String NAME = "name";
    public static final String SIZE = "size";

    private String path;
    private String name;
    private long size;

    public FileUploadData() {
    }

    public FileUploadData(String path, String name, long size) {
        this.path = path;
        this.name = name;
        this.size = size;
    }

    protected FileUploadData(Parcel in) {
        path = in.readString();
        name = in.readString();
        size = in.readLong();
    }

    public static final Creator<FileUploadData> CREATOR = new Creator<FileUploadData>() {
        @Override
        public FileUploadData createFromParcel(Parcel in) {
            return new FileUploadData(in);
        }

        @Override
        public FileUploadData[] newArray(int size) {
            return new FileUploadData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putString(PATH, path);
        dataBundle.putString(NAME, name);
        dataBundle.putLong(SIZE, size);

        return dataBundle;
    }


    public static FileUploadData fromBundle(Bundle bundle) {
        return bundle.getParcelable(EXTRA);
    }

    public static FileUploadData fromDataBundle(DataBundle dataBundle) {
        FileUploadData fileUploadData = new FileUploadData();

        fileUploadData.setPath(dataBundle.getString(PATH));
        fileUploadData.setName(dataBundle.getString(NAME));
        fileUploadData.setSize(dataBundle.getLong(SIZE));

        return fileUploadData;
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

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(name);
        dest.writeLong(size);
    }
}
