package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class SilenceApplicationData extends Transportable implements Parcelable {

    public static final String EXTRA = "silenceApplication";

    public static final String PACKAGE = "package";
    public static final String MINUTES = "minutes";

    private String packageName;
    private String minutes;

    public SilenceApplicationData() {
    }

    protected SilenceApplicationData(Parcel in) {
        packageName = in.readString();
        minutes = in.readString();
    }

    public static final Creator<SilenceApplicationData> CREATOR = new Creator<SilenceApplicationData>() {
        @Override
        public SilenceApplicationData createFromParcel(Parcel in) {
            return new SilenceApplicationData(in);
        }

        @Override
        public SilenceApplicationData[] newArray(int size) {
            return new SilenceApplicationData[size];
        }
    };

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getMinutes() {
        return minutes;
    }

    public void setMinutes(String minutes) {
        this.minutes = minutes;
    }

    public static SilenceApplicationData fromDataBundle(DataBundle dataBundle) {
        SilenceApplicationData silenceApplicationData = new SilenceApplicationData();

        silenceApplicationData.setPackageName(dataBundle.getString(PACKAGE));
        silenceApplicationData.setMinutes(dataBundle.getString(MINUTES));

        return silenceApplicationData;
    }

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putString(PACKAGE, packageName);
        dataBundle.putString(MINUTES, minutes);

        return dataBundle;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeString(minutes);
    }
}
