package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import amazmod.com.transport.Transportable;

public class RequestUploadFileChunkData extends Transportable implements Parcelable {

    public static final String EXTRA = "request_upload_file_chunk";

    public static final String PATH = "path";
    public static final String INDEX = "chunk_index";
    public static final String TOTAL = "total_chunks";
    public static final String SIZE = "size";
    public static final String BYTES = "bytes";

    private String path;
    private int index;
    private int total;
    private int size;
    private byte[] bytes;

    public RequestUploadFileChunkData() {
    }

    public static RequestUploadFileChunkData fromFile(File file, String destPath, long totalChunks, long chunk, int chunkSize) throws IOException {
        RequestUploadFileChunkData requestUploadFileChunkData = new RequestUploadFileChunkData();

        byte[] bytes = new byte[chunkSize];

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        randomAccessFile.seek(chunk * chunkSize);
        randomAccessFile.read(bytes);

        randomAccessFile.close();

        requestUploadFileChunkData.setPath(destPath);
        requestUploadFileChunkData.setIndex((int) chunk);
        requestUploadFileChunkData.setSize(chunkSize);
        requestUploadFileChunkData.setTotal((int) totalChunks);
        requestUploadFileChunkData.setBytes(bytes);

        return requestUploadFileChunkData;
    }

    protected RequestUploadFileChunkData(Parcel in) {
        path = in.readString();
        index = in.readInt();
        total = in.readInt();
        size = in.readInt();
        bytes = in.createByteArray();
    }

    public static final Creator<RequestUploadFileChunkData> CREATOR = new Creator<RequestUploadFileChunkData>() {
        @Override
        public RequestUploadFileChunkData createFromParcel(Parcel in) {
            return new RequestUploadFileChunkData(in);
        }

        @Override
        public RequestUploadFileChunkData[] newArray(int size) {
            return new RequestUploadFileChunkData[size];
        }
    };

    public static RequestUploadFileChunkData fromDataBundle(DataBundle dataBundle) {
        RequestUploadFileChunkData requestUploadFileChunkData = new RequestUploadFileChunkData();

        requestUploadFileChunkData.setPath(dataBundle.getString(PATH));
        requestUploadFileChunkData.setIndex(dataBundle.getInt(INDEX));
        requestUploadFileChunkData.setTotal(dataBundle.getInt(TOTAL));
        requestUploadFileChunkData.setSize(dataBundle.getInt(SIZE));
        requestUploadFileChunkData.setBytes(dataBundle.getByteArray(BYTES));

        return requestUploadFileChunkData;
    }

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(PATH, path);
        dataBundle.putInt(INDEX, index);
        dataBundle.putInt(TOTAL, total);
        dataBundle.putInt(SIZE, size);
        dataBundle.putByteArray(BYTES, bytes);

        return dataBundle;
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

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
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
        dest.writeString(path);
        dest.writeInt(index);
        dest.writeInt(total);
        dest.writeInt(size);
        dest.writeByteArray(bytes);
    }
}
