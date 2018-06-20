package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class BrightnessData extends Transportable implements Parcelable {

    public static final String EXTRA = "brightness";

    public static final String LEVEL = "level";

    private int level;

    public BrightnessData() {
    }

    protected BrightnessData(Parcel in) {
        level = in.readInt();
    }

    public static final Creator<BrightnessData> CREATOR = new Creator<BrightnessData>() {
        @Override
        public BrightnessData createFromParcel(Parcel in) {
            return new BrightnessData(in);
        }

        @Override
        public BrightnessData[] newArray(int size) {
            return new BrightnessData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putInt(LEVEL, level);

        return dataBundle;
    }


    public static BrightnessData fromBundle(Bundle bundle) {
        return bundle.getParcelable(EXTRA);
    }

    public static BrightnessData fromDataBundle(DataBundle dataBundle) {
        BrightnessData brightnessData = new BrightnessData();

        brightnessData.setLevel(dataBundle.getInt(LEVEL));

        return brightnessData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(level);
    }
}
