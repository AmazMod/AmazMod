package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class ResultShellCommandData extends Transportable implements Parcelable {

    public static final String EXTRA = "result_shell_command";

    private static final String RESULT = "result";
    private static final String COMMAND = "command";
    private static final String DURATION = "duration";
    private static final String OUTPUT_LOG = "output_log";
    private static final String ERROR_LOG = "error_log";

    private int result;
    private long duration;
    private String command;
    private String outputLog;
    private String errorLog;

    public ResultShellCommandData() {}

    protected ResultShellCommandData(Parcel in) {
        result = in.readInt();
        duration = in.readLong();
        command = in.readString();
        outputLog = in.readString();
        errorLog = in.readString();
    }

    public static final Creator<ResultShellCommandData> CREATOR = new Creator<ResultShellCommandData>() {
        @Override
        public ResultShellCommandData createFromParcel(Parcel in) {
            return new ResultShellCommandData(in);
        }

        @Override
        public ResultShellCommandData[] newArray(int size) {
            return new ResultShellCommandData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putInt(RESULT, result);
        dataBundle.putLong(DURATION, duration);
        dataBundle.putString(COMMAND, command);
        dataBundle.putString(OUTPUT_LOG, outputLog);
        dataBundle.putString(ERROR_LOG, errorLog);

        return dataBundle;
    }

    public static ResultShellCommandData fromDataBundle(DataBundle dataBundle) {
        ResultShellCommandData resultShellCommand = new ResultShellCommandData();
        resultShellCommand.setResult(dataBundle.getInt(RESULT));
        resultShellCommand.setDuration(dataBundle.getLong(DURATION));
        resultShellCommand.setCommand(dataBundle.getString(COMMAND));
        resultShellCommand.setOutputLog(dataBundle.getString(OUTPUT_LOG));
        resultShellCommand.setErrorLog(dataBundle.getString(ERROR_LOG));
        return resultShellCommand;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getOutputLog() {
        return outputLog;
    }

    public void setOutputLog(String outputLog) {
        this.outputLog = outputLog;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(result);
        dest.writeLong(duration);
        dest.writeString(command);
        dest.writeString(outputLog);
        dest.writeString(errorLog);
    }
}
