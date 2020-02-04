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
    public static final String NOTIFICATION_SOUND = "notification_sound";
    public static final String DISABLE_NOTIFICATIONS = "disable_notifications";
    public static final String DISABLE_NOTIFICATION_REPLIES = "disable_notification_replies";
    public static final String ENABLE_HARDWARE_KEYS_MUSIC_CONTROL = "enable_hardware_keys_music_control";
    public static final String ENABLE_INVERTED_THEME = "enable_inverted_theme";
    public static final String FONT_SIZE = "font_size";
    public static final String DISABLE_NOTIFICATION_SCREENON = "disable_notification_screenon";
    public static final String SHAKE_TO_DISMISS_GRAVITY = "shake_to_dismiss_gravity";
    public static final String SHAKE_TO_DISMISS_NUM_OF_SHAKES = "shake_to_dismiss_num_of_shakes";
    public static final String PHONE_CONNECTION_ALERT = "phone_connection_alert";
    public static final String PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION = "phone_connection_alert_standard_notification";
    public static final String DEFAULT_LOCALE = "default_locale";
    public static final String DISABLE_DELAY = "disable_reply_delay";
    public static final String AMAZMOD_KEEP_WIDGET = "amazmod_keep_widget";
    public static final String REQUEST_SELF_RELOAD = "request_self_reload";
    public static final String AMAZMOD_OVERLAY_LAUNCHER = "amazmod_overlay_launcher";
    public static final String AMAZMOD_HOURLY_CHIME = "amazmod_hourly_chime";
    public static final String AMAZMOD_HEARTRATE_DATA = "amazmod_heartrate_data";
    public static final String BATTERY_WATCH_ALERT = "battery_watch_alert";
    public static final String BATTERY_PHONE_ALERT = "battery_phone_alert";
    public static final String LOG_LINES = "log_lines";


    private String replies;
    private int vibration;
    private int screenTimeout;
    private boolean notificationsCustomUi;
    private boolean notificationSound;
    private boolean disableNotifications;
    private boolean disableNotificationsReplies;
    private boolean enableHardwareKeysMusicControl;
    private boolean enableInvertedTheme;
    private String fontSize;
    private boolean disableNotificationsScreenOn;
    private int shakeToDismissGravity;
    private int shakeToDismissNumOfShakes;
    private boolean phoneConnectionAlert;
    private boolean phoneConnectionAlertStandardNotification;
    private String defaultLocale;
    private boolean disableDelay;
    private boolean amazModKeepWidget;
    private boolean requestSelfReload;
    private boolean overlayLauncher;
    private boolean hourlyChime;
    private boolean heartrateData;
    private int batteryWatchAlert;
    private int batteryPhoneAlert;
    private int logLines;

    public SettingsData() {
    }

    protected SettingsData(Parcel in) {
        replies = in.readString();
        vibration = in.readInt();
        screenTimeout = in.readInt();
        notificationsCustomUi = in.readByte() != 0;
        notificationSound = in.readByte() != 0;
        disableNotifications = in.readByte() != 0;
        disableNotificationsReplies = in.readByte() != 0;
        enableHardwareKeysMusicControl = in.readByte() != 0;
        enableInvertedTheme = in.readByte() != 0;
        fontSize = in.readString();
        disableNotificationsScreenOn = in.readByte() != 0;
        shakeToDismissGravity = in.readInt();
        shakeToDismissNumOfShakes = in.readInt();
        phoneConnectionAlert = in.readByte() != 0;
        phoneConnectionAlertStandardNotification = in.readByte() != 0;
        defaultLocale = in.readString();
        disableDelay = in.readByte() != 0;
        amazModKeepWidget = in.readByte() != 0;
        requestSelfReload = in.readByte() != 0;
        heartrateData = in.readByte() != 0;
        overlayLauncher = in.readByte() != 0;
        hourlyChime = in.readByte() != 0;
        batteryWatchAlert = in.readInt();
        batteryPhoneAlert = in.readInt();
        logLines = in.readInt();
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
        dataBundle.putBoolean(NOTIFICATION_SOUND, notificationSound);
        dataBundle.putBoolean(DISABLE_NOTIFICATIONS, disableNotifications);
        dataBundle.putBoolean(DISABLE_NOTIFICATION_REPLIES, disableNotificationsReplies);
        dataBundle.putBoolean(ENABLE_HARDWARE_KEYS_MUSIC_CONTROL, enableHardwareKeysMusicControl);
        dataBundle.putBoolean(ENABLE_INVERTED_THEME, enableInvertedTheme);
        dataBundle.putString(FONT_SIZE, fontSize);
        dataBundle.putBoolean(DISABLE_NOTIFICATION_SCREENON, disableNotificationsScreenOn);
        dataBundle.putInt(SHAKE_TO_DISMISS_GRAVITY, shakeToDismissGravity);
        dataBundle.putInt(SHAKE_TO_DISMISS_NUM_OF_SHAKES, shakeToDismissNumOfShakes);
        dataBundle.putBoolean(PHONE_CONNECTION_ALERT, phoneConnectionAlert);
        dataBundle.putBoolean(PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION, phoneConnectionAlertStandardNotification);
        dataBundle.putString(DEFAULT_LOCALE, defaultLocale);
        dataBundle.putBoolean(DISABLE_DELAY, disableDelay);
        dataBundle.putBoolean(AMAZMOD_KEEP_WIDGET, amazModKeepWidget);
        dataBundle.putBoolean(REQUEST_SELF_RELOAD, requestSelfReload);
        dataBundle.putBoolean(AMAZMOD_OVERLAY_LAUNCHER, overlayLauncher);
        dataBundle.putBoolean(AMAZMOD_HOURLY_CHIME, hourlyChime);
        dataBundle.putBoolean(AMAZMOD_HEARTRATE_DATA, heartrateData);
        dataBundle.putInt(BATTERY_WATCH_ALERT, batteryWatchAlert);
        dataBundle.putInt(BATTERY_PHONE_ALERT, batteryPhoneAlert);
        dataBundle.putInt(LOG_LINES, logLines);

        return dataBundle;
    }

    public static SettingsData fromDataBundle(DataBundle dataBundle) {
        SettingsData settingsData = new SettingsData();

        settingsData.setReplies(dataBundle.getString(REPLIES));
        settingsData.setScreenTimeout(dataBundle.getInt(SCREEN_TIMEOUT));
        settingsData.setVibration(dataBundle.getInt(VIBRATION));
        settingsData.setNotificationsCustomUi(dataBundle.getBoolean(NOTIFICATIONS_CUSTOM_UI));
        settingsData.setNotificationSound(dataBundle.getBoolean(NOTIFICATION_SOUND));
        settingsData.setDisableNotifications(dataBundle.getBoolean(DISABLE_NOTIFICATIONS));
        settingsData.setDisableNotificationReplies(dataBundle.getBoolean(DISABLE_NOTIFICATION_REPLIES));
        settingsData.setEnableHardwareKeysMusicControl(dataBundle.getBoolean(ENABLE_HARDWARE_KEYS_MUSIC_CONTROL));
        settingsData.setInvertedTheme(dataBundle.getBoolean(ENABLE_INVERTED_THEME));
        settingsData.setFontSize(dataBundle.getString(FONT_SIZE));
        settingsData.setDisableNotificationScreenOn(dataBundle.getBoolean(DISABLE_NOTIFICATION_SCREENON));
        settingsData.setShakeToDismissGravity(dataBundle.getInt(SHAKE_TO_DISMISS_GRAVITY));
        settingsData.setShakeToDismissNumOfShakes(dataBundle.getInt(SHAKE_TO_DISMISS_NUM_OF_SHAKES));
        settingsData.setPhoneConnectionAlert(dataBundle.getBoolean(PHONE_CONNECTION_ALERT));
        settingsData.setPhoneConnectionAlertStandardNotification(dataBundle.getBoolean(PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION));
        settingsData.setDefaultLocale(dataBundle.getString(DEFAULT_LOCALE));
        settingsData.setDisableDelay(dataBundle.getBoolean(DISABLE_DELAY));
        settingsData.setAmazModKeepWidget(dataBundle.getBoolean(AMAZMOD_KEEP_WIDGET));
        settingsData.setRequestSelfReload(dataBundle.getBoolean(REQUEST_SELF_RELOAD));
        settingsData.setOverlayLauncher(dataBundle.getBoolean(AMAZMOD_OVERLAY_LAUNCHER));
        settingsData.setHourlyChime(dataBundle.getBoolean(AMAZMOD_HOURLY_CHIME));
        settingsData.setHeartrateData(dataBundle.getBoolean(AMAZMOD_HEARTRATE_DATA));
        settingsData.setBatteryWatchAlert(dataBundle.getInt(BATTERY_WATCH_ALERT));
        settingsData.setBatteryPhoneAlert(dataBundle.getInt(BATTERY_PHONE_ALERT));
        settingsData.setLogLines(dataBundle.getInt(LOG_LINES));

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

    public boolean isNotificationSound() {
        return notificationSound;
    }

    public void setNotificationSound(boolean notificationSound) {
        this.notificationSound = notificationSound;
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

    public boolean isInvertedTheme() {
        return enableInvertedTheme;
    }

    public void setInvertedTheme(boolean enableInvertedTheme) {
        this.enableInvertedTheme = enableInvertedTheme;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public boolean isDisableNotificationsScreenOn() {
        return disableNotificationsScreenOn;
    }

    public void setDisableNotificationScreenOn(boolean disableNotificationsScreenOn) {
        this.disableNotificationsScreenOn = disableNotificationsScreenOn;
    }

    public boolean isDisableDelay() {
        return disableDelay;
    }

    public void setDisableDelay(boolean disableDelay) {
        this.disableDelay = disableDelay;
    }

    public boolean isAmazModKeepWidget() {
        return amazModKeepWidget;
    }

    public boolean isRequestSelfReload() { return requestSelfReload; }

    public boolean isOverlayLauncher() {
        return overlayLauncher;
    }

    public boolean isHourlyChime() {
        return hourlyChime;
    }

    public boolean isHeartrateData() {
        return heartrateData;
    }

    public void setAmazModKeepWidget(boolean amazModKeepWidget) {
        this.amazModKeepWidget = amazModKeepWidget;
    }

    public void setRequestSelfReload(boolean requestSelfReload) {
        this.requestSelfReload = requestSelfReload;
    }

    public void setOverlayLauncher(boolean overlayLauncher) {
        this.overlayLauncher = overlayLauncher;
    }

    public void setHourlyChime(boolean hourlyChime) {
        this.hourlyChime = hourlyChime;
    }

    public void setHeartrateData(boolean heartrateData) {
        this.heartrateData = heartrateData;
    }

    public int getBatteryWatchAlert() {
        return batteryWatchAlert;
    }

    public void setBatteryWatchAlert(int batteryWatchAlert) {
        this.batteryWatchAlert = batteryWatchAlert;
    }

    public int getBatteryPhoneAlert() {
        return batteryWatchAlert;
    }

    public void setBatteryPhoneAlert(int batteryPhoneAlert) {
        this.batteryPhoneAlert = batteryPhoneAlert;
    }

    public int getLogLines() {
        return logLines;
    }

    public void setLogLines(int logLines) {
        this.logLines = logLines;
    }

    public int getShakeToDismissGravity() {
        return shakeToDismissGravity;
    }

    public void setShakeToDismissGravity(int shakeToDismissGravity) {
        this.shakeToDismissGravity = shakeToDismissGravity;
    }

    public int getShakeToDismissNumOfShakes() {
        return shakeToDismissNumOfShakes;
    }

    public void setShakeToDismissNumOfShakes(int shakeToDismissNumOfShakes) {
        this.shakeToDismissNumOfShakes = shakeToDismissNumOfShakes;
    }

    public boolean isPhoneConnectionAlert() {
        return phoneConnectionAlert;
    }

    public void setPhoneConnectionAlert(boolean phoneConnectionAlert) {
        this.phoneConnectionAlert = phoneConnectionAlert;
    }

    public boolean isPhoneConnectionAlertStandardNotification() {
        return phoneConnectionAlertStandardNotification;
    }

    public void setPhoneConnectionAlertStandardNotification(boolean phoneConnectionAlertStandardNotification) {
        this.phoneConnectionAlertStandardNotification = phoneConnectionAlertStandardNotification;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
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
        dest.writeByte((byte) (notificationSound ? 1 : 0));
        dest.writeByte((byte) (disableNotifications ? 1 : 0));
        dest.writeByte((byte) (disableNotificationsReplies ? 1 : 0));
        dest.writeByte((byte) (enableHardwareKeysMusicControl ? 1 : 0));
        dest.writeByte((byte) (enableInvertedTheme ? 1 : 0));
        dest.writeString(fontSize);
        dest.writeByte((byte) (disableNotificationsScreenOn ? 1 : 0));
        dest.writeInt(shakeToDismissGravity);
        dest.writeInt(shakeToDismissNumOfShakes);
        dest.writeByte((byte) (phoneConnectionAlert ? 1 : 0));
        dest.writeByte((byte) (phoneConnectionAlertStandardNotification ? 1 : 0));
        dest.writeString(defaultLocale);
        dest.writeByte((byte) (disableDelay ? 1 : 0));
        dest.writeByte((byte) (amazModKeepWidget ? 1 : 0));
        dest.writeByte((byte) (requestSelfReload ? 1 : 0));
        dest.writeByte((byte) (hourlyChime ? 1 : 0));
        dest.writeByte((byte) (heartrateData ? 1 : 0));
        dest.writeInt(batteryWatchAlert);
        dest.writeInt(batteryWatchAlert);
        dest.writeInt(logLines);
    }
}
