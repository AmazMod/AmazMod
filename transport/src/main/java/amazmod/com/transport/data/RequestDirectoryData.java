package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transport;
import amazmod.com.transport.Transportable;

public class RequestDirectoryData extends Transportable implements Parcelable {

    public static final String EXTRA = "request_directory";

    public static final String PATH = "path";

    private String path;

    public RequestDirectoryData() {}

    protected RequestDirectoryData(Parcel in) {
        path = in.readString();
    }

    public static final Creator<RequestDirectoryData> CREATOR = new Creator<RequestDirectoryData>() {
        @Override
        public RequestDirectoryData createFromParcel(Parcel in) {
            return new RequestDirectoryData(in);
        }

        @Override
        public RequestDirectoryData[] newArray(int size) {
            return new RequestDirectoryData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(PATH, path);
        return dataBundle;
    }

    public static RequestDirectoryData fromDataBundle(DataBundle dataBundle) {
        RequestDirectoryData requestDirectoryData = new RequestDirectoryData();
        requestDirectoryData.setPath(dataBundle.getString(PATH));
        return requestDirectoryData;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
    }
}
