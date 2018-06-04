package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class WatchStatusData extends Transportable implements Parcelable {

    private final String EXTRA = "watchStatus";

    public static final String AMAZMOD_SERVICE_VERSION = "AmazModServiceVersion";
    public static final String RO_PRODUCT_DEVICE = "ro.product.device";
    public static final String RO_PRODUCT_MANUFACTER = "ro.product.manufacter";
    public static final String RO_PRODUCT_MODEL = "ro.product.model";
    public static final String RO_REVISION = "ro.revision";
    public static final String RO_SERIALNO = "ro.serialno";
    public static final String RO_BUILD_DATE = "ro.build.date";
    public static final String RO_BUILD_DESCRIPTION = "ro.build.description";
    public static final String RO_BUILD_DISPLAY_ID = "ro.build.display.id";
    public static final String RO_BUILD_HUAMI_MODEL = "ro.build.huami.model";
    public static final String RO_BUILD_HUAMI_NUMBER = "ro.build.huami.number";

    private String amazModServiceVersion;
    private String roProductDevice;
    private String roProductManufacter;
    private String roProductModel;
    private String roProductName;
    private String roRevision;
    private String roSerialno;
    private String roBuildDate;
    private String roBuildDescription;
    private String roBuildDisplayId;
    private String roBuildHuamiModel;
    private String roBuildHuamiNumber;


    protected WatchStatusData(Parcel in) {
        amazModServiceVersion = in.readString();
        roProductDevice = in.readString();
        roProductManufacter = in.readString();
        roProductModel = in.readString();
        roProductName = in.readString();
        roRevision = in.readString();
        roSerialno = in.readString();
        roBuildDate = in.readString();
        roBuildDescription = in.readString();
        roBuildDisplayId = in.readString();
        roBuildHuamiModel = in.readString();
        roBuildHuamiNumber = in.readString();
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
    public void toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(AMAZMOD_SERVICE_VERSION, amazModServiceVersion);
        dataBundle.putString(RO_PRODUCT_DEVICE, roProductDevice);
        dataBundle.putString(RO_PRODUCT_MANUFACTER, roProductManufacter);
        dataBundle.putString(RO_PRODUCT_MODEL, roProductModel);
        dataBundle.putString(RO_REVISION, roRevision);
        dataBundle.putString(RO_SERIALNO, roSerialno);
        dataBundle.putString(RO_BUILD_DATE, roBuildDate);
        dataBundle.putString(RO_BUILD_DESCRIPTION, roBuildDescription);
        dataBundle.putString(RO_BUILD_DISPLAY_ID, roBuildDisplayId);
        dataBundle.putString(RO_BUILD_HUAMI_MODEL, roBuildHuamiNumber);
        dataBundle.putString(RO_BUILD_HUAMI_NUMBER, roBuildHuamiNumber);
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
        dest.writeString(amazModServiceVersion);
        dest.writeString(roProductDevice);
        dest.writeString(roProductManufacter);
        dest.writeString(roProductModel);
        dest.writeString(roProductName);
        dest.writeString(roRevision);
        dest.writeString(roSerialno);
        dest.writeString(roBuildDate);
        dest.writeString(roBuildDescription);
        dest.writeString(roBuildDisplayId);
        dest.writeString(roBuildHuamiModel);
        dest.writeString(roBuildHuamiNumber);
    }
}
