package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class WatchStatusData extends Transportable implements Parcelable {

    private static final String EXTRA = "watchStatus";

    public static final String AMAZMOD_SERVICE_VERSION = "AmazModServiceVersion";
    //public static final String RO_PRODUCT_DEVICE = "ro.product.device";
    //public static final String RO_PRODUCT_MANUFACTER = "ro.product.manufacturer";
    public static final String RO_PRODUCT_MODEL = "ro.product.model";
    public static final String RO_PRODUCT_NAME = "ro.product.name";
    //public static final String RO_REVISION = "ro.revision";
    public static final String RO_SERIALNO = "ro.serialno";
    //public static final String RO_BUILD_DATE = "ro.build.date";
    public static final String RO_BUILD_DESCRIPTION = "ro.build.description";
    public static final String RO_BUILD_DISPLAY_ID = "ro.build.display.id";
    public static final String RO_BUILD_HUAMI_MODEL = "ro.build.huami.model";
    //public static final String RO_BUILD_HUAMI_NUMBER = "ro.build.huami.number";
    //public static final String RO_BUILD_FINGERPRINT = "ro.build.fingerprint";
    public static final String SCREEN_BRIGHTNESS = "watch.brightness";
    public static final String SCREEN_BRIGHTNESS_MODE = "watch.brightness_mode";
    public static final String LAST_HEART_RATES = "watch.last_heart_rates";


    private String amazModServiceVersion;
    //private String roProductDevice;
    //private String roProductManufacter;
    private String roProductModel;
    private String roProductName;
    //private String roRevision;
    private String roSerialno;
    //private String roBuildDate;
    private String roBuildDescription;
    private String roBuildDisplayId;
    private String roBuildHuamiModel;
    //private String roBuildHuamiNumber;
    //private String roBuildFingerprint;
    private int screenBrightness;
    private int screenBrightnessMode;
    private String lastHeartRates;

    public WatchStatusData() {
    }

    protected WatchStatusData(Parcel in) {
        amazModServiceVersion = in.readString();
        //roProductDevice = in.readString();
        //roProductManufacter = in.readString();
        roProductModel = in.readString();
        roProductName = in.readString();
        //roRevision = in.readString();
        roSerialno = in.readString();
        //roBuildDate = in.readString();
        roBuildDescription = in.readString();
        roBuildDisplayId = in.readString();
        roBuildHuamiModel = in.readString();
        //roBuildHuamiNumber = in.readString();
        //roBuildFingerprint = in.readString();
        screenBrightness = in.readInt();
        screenBrightnessMode = in.readInt();
        lastHeartRates = in.readString();
    }

    public static final Creator<WatchStatusData> CREATOR = new Creator<WatchStatusData>() {
        @Override
        public WatchStatusData createFromParcel(Parcel in) {
            return new WatchStatusData(in);
        }

        @Override
        public WatchStatusData[] newArray(int size) {
            return new WatchStatusData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(AMAZMOD_SERVICE_VERSION, amazModServiceVersion);
        //dataBundle.putString(RO_PRODUCT_DEVICE, roProductDevice);
        //dataBundle.putString(RO_PRODUCT_MANUFACTER, roProductManufacter);
        dataBundle.putString(RO_PRODUCT_MODEL, roProductModel);
        dataBundle.putString(RO_PRODUCT_NAME, roProductName);
        //dataBundle.putString(RO_REVISION, roRevision);
        dataBundle.putString(RO_SERIALNO, roSerialno);
        //dataBundle.putString(RO_BUILD_DATE, roBuildDate);
        dataBundle.putString(RO_BUILD_DESCRIPTION, roBuildDescription);
        dataBundle.putString(RO_BUILD_DISPLAY_ID, roBuildDisplayId);
        dataBundle.putString(RO_BUILD_HUAMI_MODEL, roBuildHuamiModel);
        //dataBundle.putString(RO_BUILD_HUAMI_NUMBER, roBuildHuamiNumber);
        //dataBundle.putString(RO_BUILD_FINGERPRINT, roBuildFingerprint);
        dataBundle.putInt(SCREEN_BRIGHTNESS,screenBrightness);
        dataBundle.putInt(SCREEN_BRIGHTNESS_MODE,screenBrightnessMode);
        dataBundle.putString(LAST_HEART_RATES, lastHeartRates);

        return dataBundle;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public static WatchStatusData fromBundle(Bundle bundle) {
        return bundle.getParcelable(EXTRA);
    }

    public static WatchStatusData fromDataBundle(DataBundle dataBundle) {
        WatchStatusData watchStatusData = new WatchStatusData();

        //watchStatusData.setRoBuildFingerprint(dataBundle.getString(RO_BUILD_FINGERPRINT));
        watchStatusData.setRoSerialno(dataBundle.getString(RO_SERIALNO));
        //watchStatusData.setRoRevision(dataBundle.getString(RO_REVISION));
        watchStatusData.setRoProductName(dataBundle.getString(RO_PRODUCT_NAME));
        watchStatusData.setRoProductModel(dataBundle.getString(RO_PRODUCT_MODEL));
        //watchStatusData.setRoProductManufacter(dataBundle.getString(RO_PRODUCT_MANUFACTER));
        //watchStatusData.setRoProductDevice(dataBundle.getString(RO_PRODUCT_DEVICE));
        //watchStatusData.setRoBuildHuamiNumber(dataBundle.getString(RO_BUILD_HUAMI_NUMBER));
        watchStatusData.setRoBuildHuamiModel(dataBundle.getString(RO_BUILD_HUAMI_MODEL));
        watchStatusData.setRoBuildDisplayId(dataBundle.getString(RO_BUILD_DISPLAY_ID));
        watchStatusData.setAmazModServiceVersion(dataBundle.getString(AMAZMOD_SERVICE_VERSION));
        //watchStatusData.setRoBuildDate(dataBundle.getString(RO_BUILD_DATE));
        watchStatusData.setRoBuildDescription(dataBundle.getString(RO_BUILD_DESCRIPTION));
        watchStatusData.setScreenBrightness(dataBundle.getInt(SCREEN_BRIGHTNESS));
        watchStatusData.setScreenBrightnessMode(dataBundle.getInt(SCREEN_BRIGHTNESS_MODE));
        watchStatusData.setLastHeartRates(dataBundle.getString(LAST_HEART_RATES));

        return watchStatusData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(amazModServiceVersion);
        //dest.writeString(roProductDevice);
        //dest.writeString(roProductManufacter);
        dest.writeString(roProductModel);
        dest.writeString(roProductName);
        //dest.writeString(roRevision);
        dest.writeString(roSerialno);
        //dest.writeString(roBuildDate);
        dest.writeString(roBuildDescription);
        dest.writeString(roBuildDisplayId);
        dest.writeString(roBuildHuamiModel);
        //dest.writeString(roBuildHuamiNumber);
        //dest.writeString(roBuildFingerprint);
        dest.writeInt(screenBrightness);
        dest.writeInt(screenBrightnessMode);
        dest.writeString(lastHeartRates);
    }

    public String getAmazModServiceVersion() {
        return amazModServiceVersion;
    }

    public void setAmazModServiceVersion(String amazModServiceVersion) {
        this.amazModServiceVersion = amazModServiceVersion;
    }

    //public String getRoProductDevice() {
    //    return roProductDevice;
    //}

    //public void setRoProductDevice(String roProductDevice) {
    //    this.roProductDevice = roProductDevice;
    //}

    //public String getRoProductManufacter() {
    //    return roProductManufacter;
    //}

    //public void setRoProductManufacter(String roProductManufacter) {
    //    this.roProductManufacter = roProductManufacter;
    //}

    public String getRoProductModel() {
        return roProductModel;
    }

    public void setRoProductModel(String roProductModel) {
        this.roProductModel = roProductModel;
    }

    public String getRoProductName() {
        return roProductName;
    }

    public void setRoProductName(String roProductName) {
        this.roProductName = roProductName;
    }

    //public String getRoRevision() {
    //    return roRevision;
    //}

    //public void setRoRevision(String roRevision) {
    //    this.roRevision = roRevision;
    //}

    public String getRoSerialno() {
        return roSerialno;
    }

    public void setRoSerialno(String roSerialno) {
        this.roSerialno = roSerialno;
    }

    //public String getRoBuildDate() {
    //    return roBuildDate;
    //}

    //public void setRoBuildDate(String roBuildDate) {
    //    this.roBuildDate = roBuildDate;
    //}

    public String getRoBuildDescription() {
        return roBuildDescription;
    }

    public void setRoBuildDescription(String roBuildDescription) {
        this.roBuildDescription = roBuildDescription;
    }

    public String getRoBuildDisplayId() {
        return roBuildDisplayId;
    }

    public void setRoBuildDisplayId(String roBuildDisplayId) {
        this.roBuildDisplayId = roBuildDisplayId;
    }

    public String getRoBuildHuamiModel() {
        return roBuildHuamiModel;
    }

    public void setRoBuildHuamiModel(String roBuildHuamiModel) {
        this.roBuildHuamiModel = roBuildHuamiModel;
    }

    //public String getRoBuildHuamiNumber() {
    //    return roBuildHuamiNumber;
    //}

    //public void setRoBuildHuamiNumber(String roBuildHuamiNumber) {
    //    this.roBuildHuamiNumber = roBuildHuamiNumber;
    //}

    //public String getRoBuildFingerprint() {
    //    return roBuildFingerprint;
    //}

    //public void setRoBuildFingerprint(String roBuildFingerprint) {
    //    this.roBuildFingerprint = roBuildFingerprint;
    //}

    public int getScreenBrightness() {
        return screenBrightness;
    }

    public void setScreenBrightness(int screenBrightness) {
        this.screenBrightness = screenBrightness;
    }

    public int getScreenBrightnessMode() {
        return screenBrightnessMode;
    }

    public void setScreenBrightnessMode(int screenBrightnessMode) {
        this.screenBrightnessMode = screenBrightnessMode;
    }

    public String getLastHeartRates() {
        return lastHeartRates;
    }

    public void setLastHeartRates(String lastHeartRates) {
        this.lastHeartRates = lastHeartRates;
    }
}
