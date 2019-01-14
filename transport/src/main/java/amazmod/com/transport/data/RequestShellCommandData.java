package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class RequestShellCommandData extends Transportable implements Parcelable {

    public static final String EXTRA = "request_shell_command";

    private static final String COMMAND = "command";
    private static final String WAIT_OUTPUT = "wait_output";
    private static final String REBOOT = "reboot";

    private String command;
    private boolean waitOutput;
    private boolean reboot;

    public RequestShellCommandData() {
    }

    public RequestShellCommandData(Parcel in) {
        command = in.readString();
        waitOutput = in.readByte() == 1;
        reboot = in.readByte() == 1;
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
        dataBundle.putBoolean(WAIT_OUTPUT, waitOutput);
        dataBundle.putBoolean(REBOOT, reboot);
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
        requestShellCommandData.setWaitOutput(dataBundle.getBoolean(WAIT_OUTPUT));
        requestShellCommandData.setReboot(dataBundle.getBoolean(REBOOT));

        return requestShellCommandData;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isWaitOutput() {
        return waitOutput;
    }

    public void setWaitOutput(boolean waitOutput) {
        this.waitOutput = waitOutput;
    }

    public boolean isReboot() {
        return reboot;
    }

    public void setReboot(boolean reboot) {
        this.reboot = reboot;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(command);
        dest.writeByte((byte) (waitOutput ? 1 : 0));
        dest.writeByte((byte) (reboot ? 1 : 0));
    }
}
