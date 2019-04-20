package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class WidgetsData extends Transportable implements Parcelable {

    public static final String EXTRA = "widgets";
    public static final String PACKAGES = "packages";


    private String packages;

    public WidgetsData() {
    }

    protected WidgetsData(Parcel in) {
        packages = in.readString();
    }

    public static final Creator<WidgetsData> CREATOR = new Creator<WidgetsData>() {
        @Override
        public WidgetsData createFromParcel(Parcel in) {
            return new WidgetsData(in);
        }

        @Override
        public WidgetsData[] newArray(int size) {
            return new WidgetsData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putString(PACKAGES, packages);

        return dataBundle;
    }

    public static WidgetsData fromDataBundle(DataBundle dataBundle) {
        WidgetsData settingsData = new WidgetsData();

        settingsData.setPackages(dataBundle.getString(PACKAGES));

        return settingsData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }


    public String getPackages() {
        return packages;
    }
    public void setPackages(String data) {
        this.packages = data;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packages);
    }
}
