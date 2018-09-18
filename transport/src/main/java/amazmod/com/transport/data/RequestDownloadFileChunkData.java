package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class RequestDownloadFileChunkData extends Transportable implements Parcelable {

    public static final String EXTRA = "request_download_file_chunk";

    private static final String PATH = "path";
    private static final String INDEX = "index";

    private String path;
    private int index;

    public RequestDownloadFileChunkData() {}

    protected RequestDownloadFileChunkData(Parcel in) {
        path = in.readString();
        index = in.readInt();
    }

    public static final Creator<RequestDownloadFileChunkData> CREATOR = new Creator<RequestDownloadFileChunkData>() {
        @Override
        public RequestDownloadFileChunkData createFromParcel(Parcel in) {
            return new RequestDownloadFileChunkData(in);
        }

        @Override
        public RequestDownloadFileChunkData[] newArray(int size) {
            return new RequestDownloadFileChunkData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(PATH, path);
        dataBundle.putInt(INDEX, index);

        return dataBundle;
    }

    public static RequestDownloadFileChunkData fromDataBundle(DataBundle dataBundle) {
        RequestDownloadFileChunkData requestDownloadFileChunkData = new RequestDownloadFileChunkData();

        requestDownloadFileChunkData.setIndex(dataBundle.getInt(INDEX));
        requestDownloadFileChunkData.setPath(dataBundle.getString(PATH));

        return requestDownloadFileChunkData;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeInt(index);
    }
}
