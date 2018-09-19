package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class RequestShellCommandData extends Transportable implements Parcelable {

    public static final String EXTRA = "request_shell_command";

    private static final String COMMAND = "command";

    private String command;

    public RequestShellCommandData() {}

    public RequestShellCommandData(Parcel in) {
        command = in.readString();
    }

    public static final Creator<RequestShellCommandData> CREATOR = new Creator<RequestShellCommandData>() {
        @Override
        public RequestShellCommandData createFromParcel(Parcel in) {
            return new RequestShellCommandData(in);
        }

        @Override
        public RequestShellCommandData[] newArray(int size) {
            return new RequestShellCommandData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(COMMAND, command);
        return dataBundle;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public static RequestShellCommandData fromDataBundle(DataBundle dataBundle) {
        RequestShellCommandData requestShellCommandData = new RequestShellCommandData();
        requestShellCommandData.setCommand(dataBundle.getString(COMMAND));
        return requestShellCommandData;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(command);
    }
}
