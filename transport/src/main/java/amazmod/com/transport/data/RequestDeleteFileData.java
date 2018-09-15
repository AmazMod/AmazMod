package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class RequestDeleteFileData extends Transportable implements Parcelable {

    public static final String EXTRA = "request_delete_file";

    private static final String PATH = "path";

    private String path;

    public RequestDeleteFileData() {
    }

    protected RequestDeleteFileData(Parcel in) {
        path = in.readString();
    }

    public static final Creator<RequestDeleteFileData> CREATOR = new Creator<RequestDeleteFileData>() {
        @Override
        public RequestDeleteFileData createFromParcel(Parcel in) {
            return new RequestDeleteFileData(in);
        }

        @Override
        public RequestDeleteFileData[] newArray(int size) {
            return new RequestDeleteFileData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(PATH, path);
        return dataBundle;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public static RequestDeleteFileData fromDataBundle(DataBundle dataBundle) {
        RequestDeleteFileData requestDeleteFileData = new RequestDeleteFileData();
        requestDeleteFileData.setPath(dataBundle.getString(PATH));
        return requestDeleteFileData;
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
