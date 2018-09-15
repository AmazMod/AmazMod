package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class ResultDownloadFileChunkData extends Transportable implements Parcelable {

    public static final String EXTRA = "result_download_file_chunk";

    private static final String NAME = "name";
    private static final String INDEX = "index";
    private static final String BYTES = "bytes";

    private String name;
    private int index;
    private byte[] bytes;

    public ResultDownloadFileChunkData() {
    }

    protected ResultDownloadFileChunkData(Parcel in) {
        name = in.readString();
        index = in.readInt();
        bytes = in.createByteArray();
    }

    public static final Creator<ResultDownloadFileChunkData> CREATOR = new Creator<ResultDownloadFileChunkData>() {
        @Override
        public ResultDownloadFileChunkData createFromParcel(Parcel in) {
            return new ResultDownloadFileChunkData(in);
        }

        @Override
        public ResultDownloadFileChunkData[] newArray(int size) {
            return new ResultDownloadFileChunkData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(NAME, name);
        dataBundle.putInt(INDEX, index);
        dataBundle.putByteArray(BYTES, bytes);

        return dataBundle;
    }

    public static ResultDownloadFileChunkData fromDataBundle(DataBundle dataBundle) {
        ResultDownloadFileChunkData resultDownloadFileChunkData = new ResultDownloadFileChunkData();
        resultDownloadFileChunkData.setName(dataBundle.getString(NAME));
        resultDownloadFileChunkData.setIndex(dataBundle.getInt(INDEX));
        resultDownloadFileChunkData.setBytes(dataBundle.getByteArray(BYTES));
        return resultDownloadFileChunkData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(index);
        dest.writeByteArray(bytes);
    }
}
