package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPropertyAnimatorListener;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class BatteryData extends Transportable implements Parcelable {

    public static final String EXTRA = "batteryData";

    private static final String LEVEL = "level";
    private static final String CHARGING = "charging";
    private static final String USB_CHARGE = "usb_charge";
    private static final String AC_CHARGE = "ac_charge";

    private float level;
    private boolean charging;
    private boolean usbCharge;
    private boolean acCharge;

    public BatteryData() {}

    protected BatteryData(Parcel in) {
        level = in.readFloat();
        charging = in.readByte() != 0;
        usbCharge = in.readByte() != 0;
        acCharge = in.readByte() != 0;
    }

    public static final Creator<BatteryData> CREATOR = new Creator<BatteryData>() {
        @Override
        public BatteryData createFromParcel(Parcel in) {
            return new BatteryData(in);
        }

        @Override
        public BatteryData[] newArray(int size) {
            return new BatteryData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putFloat(LEVEL, level);
        dataBundle.putBoolean(CHARGING, charging);
        dataBundle.putBoolean(USB_CHARGE, usbCharge);
        dataBundle.putBoolean(AC_CHARGE, acCharge);

        return dataBundle;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public static BatteryData fromBundle(Bundle bundle) {
        return bundle.getParcelable(EXTRA);
    }

    public static BatteryData fromDataBundle(DataBundle dataBundle) {
        BatteryData batteryData = new BatteryData();

        float level = dataBundle.getFloat(LEVEL);
        boolean charging = dataBundle.getBoolean(CHARGING);
        boolean usbCharge = dataBundle.getBoolean(USB_CHARGE);
        boolean acCharge = dataBundle.getBoolean(AC_CHARGE);

        batteryData.setLevel(level);
        batteryData.setCharging(charging);
        batteryData.setUsbCharge(usbCharge);
        batteryData.setAcCharge(acCharge);

        return batteryData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(level);
        dest.writeByte((byte) (charging ? 1 : 0));
        dest.writeByte((byte) (usbCharge ? 1 : 0));
        dest.writeByte((byte) (acCharge ? 1 : 0));
    }

    public float getLevel() {
        return level;
    }

    public void setLevel(float level) {
        this.level = level;
    }

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public boolean isUsbCharge() {
        return usbCharge;
    }

    public void setUsbCharge(boolean usbCharge) {
        this.usbCharge = usbCharge;
    }

    public boolean isAcCharge() {
        return acCharge;
    }

    public void setAcCharge(boolean acCharge) {
        this.acCharge = acCharge;
    }
}
