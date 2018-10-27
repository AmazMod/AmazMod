package amazmod.com.transport;

public class Constants {

    public static final String SERVICE_UPDATE_URL = "https://raw.githubusercontent.com/edotassi/AmazMod/master/service-releases/amazmod-service-version.json";
    public static final String SERVICE_UPDATE_FILE_URL = "https://raw.githubusercontent.com/edotassi/AmazMod/master/service-releases/AmazMod-service-%d.apk";
    public static final String TAG = "AmazMod";
    public static final int CHUNK_SIZE = 4096 + 2048 + 2048;
    public static final String INITIAL_PATH = "/sdcard";
    public static final String DOWNLOAD_DIRECTORY = "AmazMod";

    public static final String SHELL_COMMAND_INSTALL_APK = "adb install -r %s";
    public static final String SHELL_COMMAND_REBOOT = "reboot";
    public static final String SHELL_COMMAND_FASTBOOT = "reboot bootloader";
    public static final String SHELL_COMMAND_DPM = "adb shell dpm set-device-owner com.amazmod.service/.AdminReceiver";
    public static final String SHELL_COMMAND_FORCE_STOP_HUAMI_LAUNCHER = "adb shell am force-stop com.huami.watch.launcher";
    public static final String SHELL_COMMAND_ENABLE_APPS_LIST = "touch /sdcard/launcher_config.ini";
    public static final String SHELL_COMMAND_DISABLE_APPS_LIST = "rm /sdcard/launcher_config.ini";
    public static final String SHELL_COMMAND_MKDIR = "mkdir -p";
    public static final String SHELL_COMMAND_RENAME = "mv";

    public static final String FAQ_URL = "https://github.com/edotassi/AmazMod/blob/dev/FAQ.md";

    public static final String PREF_ENABLED_NOTIFICATIONS_PACKAGES = "pref.enabled.notifications.packages";
    public static final String PREF_DISABLE_NOTIFICATIONS = "preference.disable.notifications";
    public static final String PREF_DISABLE_NOTIFICATIONS_REPLIES = "preference.amazmodservice.enable.replies";
    public static final String PREF_NOTIFICATIONS_REPLIES = "preference.amazmodservice.replies";
    public static final String PREF_NOTIFICATIONS_VIBRATION = "preference.amazmodservice.vibration";
    public static final String PREF_NOTIFICATIONS_SCREEN_TIMEOUT = "preference.amazmodservice.screen.timeout";
    public static final String PREF_NOTIFICATIONS_INVERTED_THEME = "preference.amazmodservice.inverted.theme";
    public static final String PREF_NOTIFICATIONS_FONT_SIZE = "preference.amazmodservice.font.size";
    public static final String PREF_DISABLE_BATTERY_CHART = "preference.disable.battery.chart";
    public static final String PREF_BATTERY_BACKGROUND_SYNC_INTERVAL = "preference.battery.background.sync.interval";
    public static final String PREF_BATTERY_CHART_TIME_INTERVAL = "preference.battery.chart.range";
    public static final String PREF_DISABLE_NOTIFATIONS_WHEN_SCREEN_ON = "preference.disable.notifications.when.screen.on";
    public static final String PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI = "preference.notifications.enable.custom.ui";
    public static final String PREF_ENABLE_HARDWARE_KEYS_MUSIC_CONTROL = "preference.enable.hw.keys.music.control";
    public static final String PREF_KEY_FIRST_START = "preference.key.first.start";
    public static final String PREF_FORCE_ENGLISH = "preference.force.english";
    public static final String PREF_DISABLE_NOTIFICATIONS_SCREENON = "preference.amazmodservice.disable.screenon";
    public static final String PREF_DISABLE_STANDARD_NOTIFICATIONS = "preference.disable.standard.notifications";
    public static final String PREF_PHONE_CONNECT_DISCONNECT_ALERT = "preference.phone.connect.disconnect.alert";
    public static final String PREF_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION = "preference.phone.connection.alert.standard.notification";

    public static final String PREF_DEFAULT_NOTIFICATIONS_REPLIES = "[]";
    public static final String PREF_DEFAULT_NOTIFICATIONS_VIBRATION = "300";
    public static final String PREF_DEFAULT_NOTIFICATIONS_SCREEN_TIMEOUT = "7000";
    public static final String PREF_DEFAULT_NOTIFICATIONS_FONT_SIZE = "n";
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS = false;
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES = false;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_CUSTOM_UI = false;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME = false;
    public static final boolean PREF_DEFAULT_KEY_FIRST_START = true;
    public static final boolean PREF_DEFAULT_DISABLE_BATTERY_CHART = false;
    public static final String PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL = "5";
    public static final boolean PREF_DEFAULT_ENABLE_PERSISTENT_NOTIFICATION = true;
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON = false;
    public static final boolean PREF_DEFAULT_DISABLE_STANDARD_NOTIFICATIONS = false;
    public static final boolean PREF_DEFAULT_PHONE_CONNECT_DISCONNECT_ALERT = false;
    public static final boolean PREF_DEFAULT_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION = false;
    public static final String PREF_DEFAULT_COMMAND_HISTORY = "[]";

    public static final int REQUEST_CODE_INTRO = 1;

    public static final String PREF_DISABLE_NOTIFICATIONS_WHEN_DND = "preference.disable.notifications.when.dnd";
    public static final String PREF_DISABLE_REMOVE_NOTIFICATIONS = "preference.disable.remove.notifications";
    public static final String PREF_NOTIFICATIONS_ENABLE_VOICE_APPS = "preference.notifications.enable.voice.apps";
    public static final String PREF_NOTIFICATIONS_ENABLE_LOCAL_ONLY = "preference.notifications.enable.local.only";
    public static final String PREF_NOTIFICATIONS_ENABLE_WHEN_LOCKED = "preference.notifications.enable.when.locked";
    public static final String PREF_NOTIFICATIONS_ENABLE_UNGROUP = "preference.notifications.enable.ungroup";
    public static final String PREF_TIME_LAST_SYNC = "preference.time.last.sync";
    public static final String PREF_WATCH_MODEL = "preference.watch.model";
    public static final String PREF_ENABLE_PERSISTENT_NOTIFICATION = "preference.enable.persistent.notification";
    public static final String PREF_TIME_LAST_SAVE = "preference.time.last.save";

    public static final byte FILTER_CONTINUE = 'C';
    public static final byte FILTER_UNGROUP = 'U';
    public static final byte FILTER_VOICE = 'V';
    public static final byte FILTER_MAPS = 'M';
    public static final byte FILTER_LOCALOK = 'K';

    public static final byte FILTER_PACKAGE = 'P';
    public static final byte FILTER_GROUP = 'G';
    public static final byte FILTER_ONGOING = 'O';
    public static final byte FILTER_LOCAL = 'L';
    public static final byte FILTER_BLOCK = 'B';
    public static final byte FILTER_RETURN = 'R';
    public static final byte FILTER_SCREENON = 'S';
    public static final byte FILTER_SCREENLOCKED = 'N';
    public static final byte FILTER_NOTIFICATIONS_DISABLED = 'D';

    public static final String PREF_WATCHFACE_BACKGROUND_SYNC_INTERVAL = "preference.watchface.background.sync.interval";
    public static final String PREF_TIME_LAST_WATCHFACE_DATA_SYNC = "preference.time.last.watchface.data.sync";
    public static final String PREF_WATCHFACE_SEND_DATA = "preference.watchface.send.data";
    public static final String PREF_WATCHFACE_SEND_DATA_INTERVAL_INDEX = "preference.watchface.send.data.interval.index";
    public static final String PREF_WATCHFACE_SEND_BATTERY_CHANGE = "preference.watchface.send.battery.change";
    public static final String PREF_WATCHFACE_SEND_ALARM_CHANGE = "preference.watchface.send.alarm.change";
    public static final boolean PREF_DEFAULT_WATCHFACE_SEND_DATA = true;
    public static final int PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX = 2;
    public static final boolean PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE = false;
    public static final boolean PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE = true;

    public static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    public static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    public static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;
    public static final int SCREEN_BRIGHTNESS_VALUE_AUTO = -1;

}
