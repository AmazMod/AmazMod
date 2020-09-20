package amazmod.com.transport.data;

import com.huami.watch.transport.DataBundle;

public class SleepData {
    //public String EXTRA_PAUSE_TIMESTAMP = "ptimestamp"; //Needed for ACTION_SET_PAUSE

    private String EXTRA_ACTION = "sleepaction";
    private int action;

    private String EXTRA_SUSPENDED = "suspended";
    private boolean suspended;

    private String EXTRA_BATCH_SIZE = "batchsize";
    private long batchsize;

    private String EXTRA_DELAY = "delay";
    private int delay;

    private String EXTRA_HOUR = "hour";
    private int hour;
    private String EXTRA_MINUTE = "min";
    private int minute;
    private String EXTRA_TIMESTAMP = "timestamp";
    private long timestamp;

    private String EXTRA_TITLE = "title";
    private String title;
    private String EXTRA_TEXT = "text";
    private String text;

    private String EXTRA_REPEAT = "repeat";
    private int repeat;

    private String EXTRA_MAX_DATA = "maxdata";
    private float[] max_data;
    private String EXTRA_MAX_RAW_DATA = "maxrawdata";
    private float[] max_raw_data;
    private String EXTRA_HRDATA = "hrdata";
    private float[] hrdata;

    public void setAction(int action) {
        this.action = action;
    }

    public int getAction() {
        return action;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public long getBatchsize() {
        return batchsize;
    }

    public void setBatchsize(long batchsize) {
        this.batchsize = batchsize;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public float[] getMax_data() {
        return max_data;
    }

    public void setMax_data(float[] max_data) {
        this.max_data = max_data;
    }

    public float[] getMax_raw_data() {
        return max_raw_data;
    }

    public void setMax_raw_data(float[] max_raw_data) {
        this.max_raw_data = max_raw_data;
    }

    public float[] getHrdata() {
        return hrdata;
    }

    public void setHrdata(float[] hrdata) {
        this.hrdata = hrdata;
    }

    public DataBundle toDataBundle(DataBundle dataBundle){
        dataBundle.putInt(EXTRA_ACTION, action);
        switch(action){
            case actions.ACTION_SET_SUSPENDED:
                dataBundle.putBoolean(EXTRA_SUSPENDED, suspended);
                break;
            case actions.ACTION_SET_BATCH_SIZE:
                dataBundle.putLong(EXTRA_BATCH_SIZE, batchsize);
                break;
            case actions.ACTION_START_ALARM:
                dataBundle.putInt(EXTRA_DELAY, delay);
                break;
            case actions.ACTION_UPDATE_ALARM:
                dataBundle.putInt(EXTRA_HOUR, hour);
                dataBundle.putInt(EXTRA_MINUTE, minute);
                dataBundle.putLong(EXTRA_TIMESTAMP, timestamp);
                break;
            case actions.ACTION_SHOW_NOTIFICATION:
                dataBundle.putString(EXTRA_TITLE, title);
                dataBundle.putString(EXTRA_TEXT, text);
                break;
            case actions.ACTION_HINT:
                dataBundle.putInt(EXTRA_REPEAT, repeat);
                break;
            case actions.ACTION_DATA_UPDATE:
                dataBundle.putFloatArray(EXTRA_MAX_DATA, max_data);
                dataBundle.putFloatArray(EXTRA_MAX_RAW_DATA, max_raw_data);
                break;
            case actions.ACTION_HRDATA_UPDATE:
                dataBundle.putFloatArray(EXTRA_HRDATA, hrdata);
                break;
            default:
                break;
        }
        return dataBundle;
    }

    public void fromDataBundle(DataBundle dataBundle){
        action = dataBundle.getInt(EXTRA_ACTION);
        switch(action) {
            case actions.ACTION_SET_SUSPENDED:
                suspended = dataBundle.getBoolean(EXTRA_SUSPENDED);
                break;
            case actions.ACTION_SET_BATCH_SIZE:
                batchsize = dataBundle.getLong(EXTRA_BATCH_SIZE);
                break;
            case actions.ACTION_START_ALARM:
                delay = dataBundle.getInt(EXTRA_DELAY);
                break;
            case actions.ACTION_UPDATE_ALARM:
                hour = dataBundle.getInt(EXTRA_HOUR);
                minute = dataBundle.getInt(EXTRA_MINUTE);
                timestamp = dataBundle.getLong(EXTRA_TIMESTAMP);
                break;
            case actions.ACTION_SHOW_NOTIFICATION:
                title = dataBundle.getString(EXTRA_TITLE);
                text = dataBundle.getString(EXTRA_TEXT);
                break;
            case actions.ACTION_HINT:
                repeat = dataBundle.getInt(EXTRA_REPEAT);
                break;
            case actions.ACTION_DATA_UPDATE:
                max_data = dataBundle.getFloatArray(EXTRA_MAX_DATA);
                max_raw_data = dataBundle.getFloatArray(EXTRA_MAX_RAW_DATA);
                break;
            case actions.ACTION_HRDATA_UPDATE:
                hrdata = dataBundle.getFloatArray(EXTRA_HRDATA);
                break;
            default:
                break;
        }
    }

    public static class actions {
        //Actions from phone
        public static final int ACTION_START_TRACKING = 0;
        public static final int ACTION_STOP_TRACKING = 1;
        //public static final int ACTION_SET_PAUSE = 2; //Not added yet
        public static final int ACTION_SET_SUSPENDED = 3;
        public static final int ACTION_SET_BATCH_SIZE = 4;
        public static final int ACTION_START_ALARM = 5;
        public static final int ACTION_STOP_ALARM = 6;
        public static final int ACTION_UPDATE_ALARM = 7;
        public static final int ACTION_SHOW_NOTIFICATION = 8;
        public static final int ACTION_HINT = 9;
        //Actions from watch
        public static final int ACTION_DATA_UPDATE = 10;
        public static final int ACTION_HRDATA_UPDATE = 11;
        //public static final int ACTION_PAUSE_FROM_WATCH = 12; //Not added yet
        //public static final int ACTION_RESUME_FROM_WATCH = 13; //Not added yet
        public static final int ACTION_SNOOZE_FROM_WATCH = 14;
        public static final int ACTION_DISMISS_FROM_WATCH = 15;
    }
}
