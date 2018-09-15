package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class ResultDeleteFileData extends Transportable implements Parcelable {

    public static final String EXTRA = "result_delete_file";

    private static final String RESULT = "result";

    private int result;

    public ResultDeleteFileData() {
    }

    protected ResultDeleteFileData(Parcel in) {
        result = in.readInt();
    }

    public static final Creator<ResultDeleteFileData> CREATOR = new Creator<ResultDeleteFileData>() {
        @Override
        public ResultDeleteFileData createFromParcel(Parcel in) {
            return new ResultDeleteFileData(in);
        }

        @Override
        public ResultDeleteFileData[] newArray(int size) {
            return new ResultDeleteFileData[size];
        }
    };

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putInt(RESULT, result);
        return dataBundle;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public static ResultDeleteFileData fromDataBundle(DataBundle dataBundle) {
        ResultDeleteFileData resultDeleteFileData = new ResultDeleteFileData();
        resultDeleteFileData.setResult(dataBundle.getInt(RESULT));
        return resultDeleteFileData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(result);
    }
}
