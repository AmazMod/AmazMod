package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class SettingsData extends Transportable implements Parcelable {

    public static final String EXTRA = "settings";
    public static final String REPLIES = "replies";
    public static final String VIBRATION = "vibration";
    public static final String SCREEN_TIMEOUT = "screen_timeout";
    public static final String NOTIFICATIONS_CUSTOM_UI = "notifications_custom_ui";
    public static final String DISABLE_NOTIFICATIONS = "disable_notifications";
    public static final String DISABLE_NOTIFICATION_REPLIES = "disable_notification_replies";
    public static final String ENABLE_HARDWARE_KEYS_MUSIC_CONTROL = "enable_hardware_keys_music_control";

    private String replies;
    private int vibration;
    private int screenTimeout;
    private boolean notificationsCustomUi;
    private boolean disableNotifications;
    private boolean disableNotificationsReplies;
    private boolean enableHardwareKeysMusicControl;

    public SettingsData() {
    }

    protected SettingsData(Parcel in) {
        replies = in.readString();
        vibration = in.readInt();
        screenTimeout = in.readInt();
        notificationsCustomUi = in.readByte() != 0;
        disableNotifications = in.readByte() != 0;
        disableNotificationsReplies = in.readByte() != 0;
        enableHardwareKeysMusicControl = in.readByte() != 0;
    }

    public static final Creator<SettingsData> CREATOR = new Creator<SettingsData>() {
        @Override
        public SettingsData createFromParcel(Parcel in) {
            return new SettingsData(in);
        }

        @Override
        public SettingsData[] newArray(int size) {
            return new SettingsData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putString(REPLIES, replies);
        dataBundle.putInt(VIBRATION, vibration);
        dataBundle.putInt(SCREEN_TIMEOUT, screenTimeout);
        dataBundle.putBoolean(NOTIFICATIONS_CUSTOM_UI, notificationsCustomUi);
        dataBundle.putBoolean(DISABLE_NOTIFICATIONS, disableNotifications);
        dataBundle.putBoolean(DISABLE_NOTIFICATION_REPLIES, disableNotificationsReplies);
        dataBundle.putBoolean(ENABLE_HARDWARE_KEYS_MUSIC_CONTROL, enableHardwareKeysMusicControl);

        return dataBundle;
    }

    public static SettingsData fromDataBundle(DataBundle dataBundle) {
        SettingsData settingsData = new SettingsData();

        settingsData.setReplies(dataBundle.getString(REPLIES));
        settingsData.setScreenTimeout(dataBundle.getInt(SCREEN_TIMEOUT));
        settingsData.setVibration(dataBundle.getInt(VIBRATION));
        settingsData.setNotificationsCustomUi(dataBundle.getBoolean(NOTIFICATIONS_CUSTOM_UI));
        settingsData.setDisableNotifications(dataBundle.getBoolean(DISABLE_NOTIFICATIONS));
        settingsData.setDisableNotificationReplies(dataBundle.getBoolean(DISABLE_NOTIFICATION_REPLIES));
        settingsData.setEnableHardwareKeysMusicControl(dataBundle.getBoolean(ENABLE_HARDWARE_KEYS_MUSIC_CONTROL));

        return settingsData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public String getReplies() {
        return replies;
    }

    public void setReplies(String replies) {
        this.replies = replies;
    }

    public int getVibration() {
        return vibration;
    }

    public void setVibration(int vibration) {
        this.vibration = vibration;
    }

    public int getScreenTimeout() {
        return screenTimeout;
    }

    public void setScreenTimeout(int screenTimeout) {
        this.screenTimeout = screenTimeout;
    }

    public boolean isNotificationsCustomUi() {
        return notificationsCustomUi;
    }

    public void setNotificationsCustomUi(boolean notificationsCustomUi) {
        this.notificationsCustomUi = notificationsCustomUi;
    }

    public boolean isDisableNotifications() {
        return disableNotifications;
    }

    public void setDisableNotifications(boolean disableNotifications) {
        this.disableNotifications = disableNotifications;
    }

    public boolean isDisableNotificationsReplies() {
        return disableNotificationsReplies;
    }

    public void setDisableNotificationReplies(boolean disableNotificationReplies) {
        this.disableNotificationsReplies = disableNotificationReplies;
    }

    public boolean isEnableHardwareKeysMusicControl() {
        return enableHardwareKeysMusicControl;
    }

    public void setEnableHardwareKeysMusicControl(boolean enableHardwareKeysMusicControl) {
        this.enableHardwareKeysMusicControl = enableHardwareKeysMusicControl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(replies);
        dest.writeInt(vibration);
        dest.writeInt(screenTimeout);
        dest.writeByte((byte) (notificationsCustomUi ? 1 : 0));
        dest.writeByte((byte) (disableNotifications ? 1 : 0));
        dest.writeByte((byte) (disableNotificationsReplies ? 1 : 0));
        dest.writeByte((byte) (enableHardwareKeysMusicControl ? 1 : 0));
    }
}
