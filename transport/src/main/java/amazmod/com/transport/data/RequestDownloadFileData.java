package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class RequestDownloadFileData extends Transportable implements Parcelable {

    public static final String EXTRA = "request_download_file";

    private static final String PATH = "path";

    private String path;

    public RequestDownloadFileData() {}

    protected RequestDownloadFileData(Parcel in) {
        path = in.readString();
    }

    public static final Creator<RequestDownloadFileData> CREATOR = new Creator<RequestDownloadFileData>() {
        @Override
        public RequestDownloadFileData createFromParcel(Parcel in) {
            return new RequestDownloadFileData(in);
        }

        @Override
        public RequestDownloadFileData[] newArray(int size) {
            return new RequestDownloadFileData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(PATH, path);
        return dataBundle;
    }

    public static RequestDownloadFileData fromDataBundle(DataBundle dataBundle) {
        RequestDownloadFileData requestDownloadFileData = new RequestDownloadFileData();
        requestDownloadFileData.setPath(dataBundle.getString(PATH));
        return requestDownloadFileData;
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
