package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class WatchfaceData extends Transportable implements Parcelable {

    public static final String EXTRA = "watchface";
    public static final String SHOW_ALTITUDE = "show_altitude";
    public static final String SHOW_BATTERY = "show_battery";

    private boolean altitude;
    private boolean battery;

    public WatchfaceData() {
    }

    protected WatchfaceData(Parcel in) {
        altitude = in.readByte() != 0;
        battery = in.readByte() != 0;
    }

    public static final Creator<WatchfaceData> CREATOR = new Creator<WatchfaceData>() {
        @Override
        public WatchfaceData createFromParcel(Parcel in) {
            return new WatchfaceData(in);
        }

        @Override
        public WatchfaceData[] newArray(int size) {
            return new WatchfaceData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putBoolean(SHOW_ALTITUDE, altitude);
        dataBundle.putBoolean(SHOW_BATTERY, battery);

        return dataBundle;
    }

    public static WatchfaceData fromDataBundle(DataBundle dataBundle) {
        WatchfaceData settingsData = new WatchfaceData();

        settingsData.setShowAltitude(dataBundle.getBoolean(SHOW_ALTITUDE));
        settingsData.setShowBattery(dataBundle.getBoolean(SHOW_BATTERY));

        return settingsData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public boolean getShowBattery() {
        return battery;
    }

    public void setShowBattery(boolean battery) {
        this.battery = battery;
    }
    public boolean getShowAltitude() {
        return altitude;
    }

    public void setShowAltitude(boolean altitude) {
        this.altitude = altitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (altitude ? 1 : 0));
        dest.writeByte((byte) (battery ? 1 : 0));
    }
}
