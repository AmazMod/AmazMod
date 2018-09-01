package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class WatchfaceData extends Transportable implements Parcelable {

    public static final String EXTRA = "watchface";
    public static final String SEND_DATA = "send_data";
    public static final String SEND_BATTERY_CHANGE = "send_battery_change";
    public static final String SEND_ALARM_CHANGE = "send_alarm_change";

    private boolean send_data;
    private int send_data_interval;
    private boolean send_on_battery_change;
    private boolean send_on_alarm_change;

    public WatchfaceData() {
    }

    protected WatchfaceData(Parcel in) {
        send_data = in.readByte() != 0;
        send_on_battery_change = in.readByte() != 0;
        send_on_alarm_change = in.readByte() != 0;
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

        dataBundle.putBoolean(SEND_DATA, send_data);
        dataBundle.putBoolean(SEND_BATTERY_CHANGE, send_on_battery_change);
        dataBundle.putBoolean(SEND_ALARM_CHANGE, send_on_alarm_change);

        return dataBundle;
    }

    public static WatchfaceData fromDataBundle(DataBundle dataBundle) {
        WatchfaceData settingsData = new WatchfaceData();

        settingsData.setSendData(dataBundle.getBoolean(SEND_DATA));
        settingsData.setSendBatteryChange(dataBundle.getBoolean(SEND_BATTERY_CHANGE));
        settingsData.setSendAlarmChange(dataBundle.getBoolean(SEND_ALARM_CHANGE));

        return settingsData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public boolean getSendData() {
        return send_data;
    }
    public void setSendData(boolean data) {
        this.send_data = data;
    }

    public boolean getSendBatteryChange() {
        return send_on_battery_change;
    }
    public void setSendBatteryChange(boolean data) {
        this.send_on_battery_change = data;
    }

    public boolean getSendAlarmChange() {
        return send_on_alarm_change;
    }
    public void setSendAlarmChange(boolean data) {
        this.send_on_alarm_change = data;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (send_data ? 1 : 0));
        dest.writeByte((byte) (send_on_battery_change ? 1 : 0));
        dest.writeByte((byte) (send_on_alarm_change ? 1 : 0));
    }
}
