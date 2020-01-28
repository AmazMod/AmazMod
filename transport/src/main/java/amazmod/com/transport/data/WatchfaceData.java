package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class WatchfaceData extends Transportable implements Parcelable {

    public static final String EXTRA = "watchface";
    public static final String BATTERY = "battery";
    public static final String ALARM = "alarm";
    public static final String EXPIRE = "expire";
    public static final String CALENDAR_EVENTS = "calendar_events";
    public static final String WEATHER_DATA = "weather_data";


    private int battery;
    private long expire;
    private String alarm;
    private String calendar_events;
    private String weather_data;

    public WatchfaceData() {
    }

    protected WatchfaceData(Parcel in) {
        battery = in.readInt();
        alarm = in.readString();
        expire = in.readLong();
        calendar_events = in.readString();
        weather_data = in.readString();
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

        dataBundle.putInt(BATTERY, battery);
        dataBundle.putString(ALARM, alarm);
        dataBundle.putLong(EXPIRE, expire);
        dataBundle.putString(CALENDAR_EVENTS, calendar_events);
        dataBundle.putString(WEATHER_DATA, weather_data);

        return dataBundle;
    }

    public static WatchfaceData fromDataBundle(DataBundle dataBundle) {
        WatchfaceData settingsData = new WatchfaceData();

        settingsData.setBattery(dataBundle.getInt(BATTERY));
        settingsData.setAlarm(dataBundle.getString(ALARM));
        settingsData.setExpire(dataBundle.getLong(EXPIRE));
        settingsData.setCalendarEvents(dataBundle.getString(CALENDAR_EVENTS));
        settingsData.setWeatherData(dataBundle.getString(WEATHER_DATA));

        return settingsData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public int getBattery() {
        return battery;
    }
    public void setBattery(int data) {
        this.battery = data;
    }

    public String getAlarm() {
        return alarm;
    }
    public void setAlarm(String data) {
        this.alarm = data;
    }

    public Long getExpire() {
        return expire;
    }
    public void setExpire(Long data) {
        this.expire = data;
    }

    public String getCalendarEvents() {
        return calendar_events;
    }
    public void setCalendarEvents(String data) {
        this.calendar_events = data;
    }

    public String getWeatherData() {
        return weather_data;
    }
    public void setWeatherData(String data) {
        this.weather_data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(battery);
        dest.writeString(alarm);
        dest.writeLong(expire);
        dest.writeString(calendar_events);
        dest.writeString(weather_data);
    }
}
