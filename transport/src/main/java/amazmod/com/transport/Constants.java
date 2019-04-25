package amazmod.com.transport;

public class Constants {

    public static final String SERVICE_UPDATE_URL = "https://raw.githubusercontent.com/edotassi/AmazMod/master/service-releases/version.json";
    public static final String SERVICE_UPDATE_FILE_URL = "https://raw.githubusercontent.com/edotassi/AmazMod/master/service-releases/AmazMod-service-%d.apk";
    public static final String SERVICE_UPDATE_SCRIPT_URL = "https://raw.githubusercontent.com/edotassi/AmazMod/dev/app/update_service_apk.sh";
    public static final String TAG = "AmazMod";
    public static final int CHUNK_SIZE = 4096 + 2048 + 2048;
    public static final String INITIAL_PATH = "/sdcard";
    public static final String DOWNLOAD_DIRECTORY = "AmazMod";
    public static final String SCREENSHOT_DIRECTORY = "AmazMod/Screenshots";
    public static final byte MODE_DOWNLOAD = 'D';
    public static final byte MODE_SCREENSHOT = 'S';
    public static final String BLOCK_APP = "999999";

    public static final String SHELL_COMMAND_INSTALL_APK = "install_apk %s";
    public static final String SHELL_COMMAND_REBOOT = "reboot";
    public static final String SHELL_COMMAND_FASTBOOT = "reboot bootloader";
    public static final String SHELL_COMMAND_DPM = "adb shell dpm set-active-admin com.amazmod.service/.AdminReceiver";
    public static final String SHELL_COMMAND_FORCE_STOP_HUAMI_LAUNCHER = "adb shell am force-stop com.huami.watch.launcher";
    public static final String SHELL_COMMAND_ENABLE_APPS_LIST = "touch /sdcard/launcher_config.ini";
    public static final String SHELL_COMMAND_DISABLE_APPS_LIST = "rm /sdcard/launcher_config.ini";
    public static final String SHELL_COMMAND_MKDIR = "mkdir -p \"%s\"";
    public static final String SHELL_COMMAND_RENAME = "mv \"%s\" \"%s\"";
    public static final String SHELL_COMMAND_COMPRESS = "busybox tar cvzf \"%s\" -C \"%s\" \"%s\"";
    public static final String SHELL_COMMAND_EXTRACT = "busybox tar xvzf \"%s\" -C \"%s\"";
    public static final String SHELL_COMMAND_REMOVE_RECURSIVELY = "rm -rf \"%s\"";
    public static final String SHELL_COMMAND_SCREENSHOT = "adb shell screencap";

    public static final String FAQ_URL = "https://github.com/edotassi/AmazMod/blob/dev/FAQ.md";

    public static final String PREF_ENABLED_NOTIFICATIONS_PACKAGES = "pref.enabled.notifications.packages";
    public static final String PREF_ENABLED_NOTIFICATIONS_PACKAGES_FILTERS = "pref.enabled.notifications.packages.filters";
    //public static final String PREF_DISABLE_NOTIFICATIONS = "preference.disable.notifications";
    public static final String PREF_ENABLE_NOTIFICATIONS = "preference.enable.notifications";
    public static final String PREF_DISABLE_NOTIFICATIONS_REPLIES = "preference.amazmodservice.enable.replies";
    public static final String PREF_NOTIFICATIONS_REPLIES = "preference.amazmodservice.replies";
    public static final String PREF_NOTIFICATIONS_VIBRATION = "preference.amazmodservice.vibration";
    public static final String PREF_NOTIFICATIONS_SCREEN_TIMEOUT = "preference.amazmodservice.screen.timeout";
    public static final String PREF_NOTIFICATIONS_INVERTED_THEME = "preference.amazmodservice.inverted.theme";
    public static final String PREF_NOTIFICATIONS_FONT_SIZE = "preference.amazmodservice.font.size";
    public static final String PREF_BATTERY_CHART = "preference.battery.chart";
    public static final Boolean PREF_BATTERY_CHART_DEFAULT = true;
    public static final String PREF_BATTERY_BACKGROUND_SYNC_INTERVAL = "preference.battery.background.sync.interval";
    public static final String PREF_BATTERY_CHART_TIME_INTERVAL = "preference.battery.chart.range";
    public static final String PREF_BATTERY_WATCH_ALERT = "preference.battery.watch.alert";
    public static final String PREF_BATTERY_WATCH_ALREADY_ALERTED = "preference.battery.watch.already.alerted";
    public static final String PREF_BATTERY_WATCH_CHARGED = "preference.battery.watch.charged";
    public static final String PREF_BATTERY_PHONE_ALERT = "preference.battery.phone.alert";
    //public static final String PREF_DISABLE_NOTIFATIONS_WHEN_SCREEN_ON = "preference.disable.notifications.when.screen.on";
    public static final String PREF_ENABLE_NOTIFATIONS_WHEN_SCREEN_ON = "preference.enable.notifications.when.screen.on";
    public static final String PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI = "preference.notifications.enable.custom.ui";
    public static final String PREF_KEY_FIRST_START = "preference.key.first.start";
    public static final String PREF_LANGUAGE = "preference.language";
    public static final String PREF_DISABLE_NOTIFICATIONS_SCREENON = "preference.amazmodservice.disable.screenon";
    //public static final String PREF_DISABLE_STANDARD_NOTIFICATIONS = "preference.disable.standard.notifications";
    public static final String PREF_PHONE_CONNECT_DISCONNECT_ALERT = "preference.phone.connect.disconnect.alert";
    public static final String PREF_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION = "preference.phone.connection.alert.standard.notification";
    public static final String PREF_ENABLE_UPDATE_NOTIFICATION = "preference.enable.update.notification";
    public static final String PREF_ENABLE_DEVELOPER_MODE = "preference.enable.developer.mode";
    public static final String PREF_NOTIFICATIONS_DISABlE_DELAY = "preference.amazmodservice.disable.delay";
    public static final String PREF_NOTIFICATIONS_ENABLE_DELAY = "preference.amazmodservice.enable.delay";
    public static final String PREF_AMAZMOD_FIRST_WIDGET = "preference.amazmod.first.widget";
    //public static final String PREF_NOTIFICATIONS_DISABLE_LARGE_ICON = "preference.disable.notification.largeicon";
    //public static final String PREF_NOTIFICATIONS_DISABLE_PICTURE = "preference.disable.notification.picture";

    public static final String PREF_NOTIFICATIONS_LARGE_ICON = "preference.notification.largeicon";
    public static final boolean PREF_NOTIFICATIONS_LARGE_ICON_DEFAULT = true;
    public static final String PREF_NOTIFICATIONS_IMAGES = "preference.notification.images";
    public static final boolean PREF_NOTIFICATIONS_IMAGES_DEFAULT = true;

    public static final String PREF_LOG_TO_FILE = "preference.logs.logtofile";
    public static final boolean PREF_LOG_TO_FILE_DEFAULT = true;


    public static final String PREF_LOG_TO_FILE_LEVEL = "preference.logs.logtofile.level";
    public static final String PREF_LOG_TO_FILE_LEVEL_DEFAULT = "ERROR";


    //public static final String PREF_NOTIFICATION_DELETE_BUTTON = "preference.amazmodservice.notification.enable.deletebutton";
    public static final String PREF_NOTIFICATION_SCHEDULER = "preference.notification.scheduler";
    public static final Boolean PREF_NOTIFICATION_SCHEDULER_DEFAULT = false;

    public static final String PREF_LANGUAGE_AUTO = "auto";
    public static final String PREF_DEFAULT_NOTIFICATIONS_REPLIES = "[]";
    public static final String PREF_DEFAULT_NOTIFICATIONS_VIBRATION = "300";
    public static final String PREF_DEFAULT_NOTIFICATIONS_SCREEN_TIMEOUT = "7000";
    public static final String PREF_DEFAULT_NOTIFICATIONS_FONT_SIZE = "n";
    //public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS = false;
    public static final boolean PREF_DEFAULT_ENABLE_NOTIFICATIONS = true;
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES = false;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_CUSTOM_UI = false;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME = false;
    public static final boolean PREF_DEFAULT_KEY_FIRST_START = true;
    public static final boolean PREF_DEFAULT_DISABLE_BATTERY_CHART = false;
    public static final String PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL = "5";
    public static final String PREF_DEFAULT_BATTERY_WATCH_ALERT = "0";
    public static final String PREF_DEFAULT_BATTERY_PHONE_ALERT = "0";
    public static final boolean PREF_DEFAULT_ENABLE_PERSISTENT_NOTIFICATION = true;
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON = false;
    public static final boolean PREF_DEFAULT_DISABLE_STANDARD_NOTIFICATIONS = false;
    public static final boolean PREF_DEFAULT_PHONE_CONNECT_DISCONNECT_ALERT = false;
    public static final boolean PREF_DEFAULT_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION = false;
    public static final String PREF_DEFAULT_COMMAND_HISTORY = "[]";
    public static final boolean PREF_DEFAULT_ENABLE_UPDATE_NOTIFICATION = true;
    //public static final boolean PREF_DEFAULT_NOTIFICATIONS_DISABLE_DELAY = false;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_ENABLE_DELAY = true;
    public static final boolean PREF_DEFAULT_AMAZMOD_FIRST_WIDGET = true;

    public static final int REQUEST_CODE_INTRO = 1;

    public static final String PREF_DISABLE_NOTIFICATIONS_WHEN_DND = "preference.disable.notifications.when.dnd";
    public static final String PREF_DISABLE_REMOVE_NOTIFICATIONS = "preference.disable.remove.notifications";
    public static final String PREF_NOTIFICATIONS_ENABLE_VOICE_APPS = "preference.notifications.enable.voice.apps";
    public static final String PREF_NOTIFICATIONS_ENABLE_LOCAL_ONLY = "preference.notifications.enable.local.only";
    public static final String PREF_NOTIFICATIONS_ENABLE_WHEN_LOCKED = "preference.notifications.enable.when.locked";
    public static final String PREF_NOTIFICATIONS_ENABLE_UNGROUP = "preference.notifications.enable.ungroup";
    public static final String PREF_TIME_LAST_SYNC = "preference.time.last.sync";
    public static final String PREF_WATCH_MODEL = "preference.watch.model";
    public static final String PREF_HUAMI_MODEL = "preference.watch.huami";
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
    public static final byte FILTER_SILENCE = 'I';
    public static final byte FILTER_TEXT = 'T';

    public static final String PREF_WATCHFACE_BACKGROUND_SYNC_INTERVAL = "preference.watchface.background.sync.interval";
    public static final String PREF_WATCHFACE_CALENDAR_EVENTS_DAYS = "preference.watchface.calendar.events.days";
    public static final String PREF_WATCHFACE_LAST_CALENDAR_EVENTS = "preference.watchface.last.calendar.events.days";
    public static final String PREF_WATCHFACE_LAST_BATTERY = "preference.watchface.last.battery";
    public static final String PREF_WATCHFACE_LAST_ALARM = "preference.watchface.last.alarm";
    public static final String PREF_TIME_LAST_WATCHFACE_DATA_SYNC = "preference.time.last.watchface.data.sync";
    public static final String PREF_WATCHFACE_SEND_DATA = "preference.watchface.send.data";
    public static final String PREF_WATCHFACE_SEND_DATA_INTERVAL_INDEX = "preference.watchface.send.data.interval.index";
    public static final String PREF_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX = "preference.watchface.send.data.calendar.events.days.index";
    public static final String PREF_WATCHFACE_SEND_BATTERY_CHANGE = "preference.watchface.send.battery.change";
    public static final String PREF_WATCHFACE_SEND_ALARM_CHANGE = "preference.watchface.send.alarm.change";
    public static final String PREF_WATCHFACE_CALENDAR_SOURCE = "preference.watchface.calendar.source";
    public static final String PREF_WATCHFACE_CALENDAR_ICS_URL = "preference.watchface.calendar.ics.url";

    public static final boolean PREF_DEFAULT_WATCHFACE_SEND_DATA = true;
    public static final int PREF_DEFAULT_WATCHFACE_SEND_DATA_INTERVAL_INDEX = 2;
    public static final int PREF_DEFAULT_WATCHFACE_SEND_DATA_CALENDAR_EVENTS_DAYS_INDEX = 2;
    public static final boolean PREF_DEFAULT_WATCHFACE_SEND_BATTERY_CHANGE = false;
    public static final boolean PREF_DEFAULT_WATCHFACE_SEND_ALARM_CHANGE = false;

    public static final String PREF_CALENDAR_SOURCE_LOCAL = "local";
    public static final String PREF_CALENDAR_SOURCE_ICS = "ics";
    public static final String PREF_NOTIFICATIONS_LOG_SHOW_ONLY_SELECTED = "preference.notifications.log.show.only.selected";

    public static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    public static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    public static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;
    public static final int SCREEN_BRIGHTNESS_VALUE_AUTO = -1;

    public static final String LOGFILE = "/data/user/0/com.edotassi.amazmod/files/amazmod.log";

    public static final String PERSISTENT_NOTIFICATION_CHANNEL = "com.edotassi.amazmod.persistent.notification.channel";

    public static final String[] BUILD_VERGE_MODELS = {"A1811", "A1801"};
}
