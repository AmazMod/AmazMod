package com.amazmod.service;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class Constants {

    public static final String TAG = "AmazMod";
    public static final String TAG_NIGHTSCOUT_PAGE = "Amazmod:Nighscout";

    public static final String PACKAGE_NAME = "com.edotassi.amazmod";
    public static final String SERVICE_NAME = "com.amazmod.service";
    public static final String LAUNCHER_CLASSNAME = "com.amazmod.service.springboard.AmazModLauncher";

    public static final String ACTION_NIGHTSCOUT_SYNC = "nightscout_sync";

    public static final String INTENT_ACTION_REPLY = "com.amazmod.action.reply";

    public static final String EXTRA_REPLY = "extra.reply";
    public static final String EXTRA_NOTIFICATION_KEY = "extra.notification.key";
    public static final String EXTRA_NOTIFICATION_ID = "extra.notification.id";

    public static final String PREF_DISABLE_NOTIFICATIONS = "preference.disable.notifications";
    public static final String PREF_DISABLE_NOTIFICATIONS_REPLIES = "preference.enable.replies";
    public static final String PREF_NOTIFICATION_SCREEN_TIMEOUT = "pref_notification_screen_timeout";
    public static final String PREF_NOTIFICATION_VIBRATION = "pref_notification_vibration";
    public static final String PREF_NOTIFICATION_CUSTOM_REPLIES = "pref_notification_custom_replies";
    public static final String PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI = "pref_notifications_enable_custom_ui";
    public static final String PREF_ENABLE_HARDWARE_KEYS_MUSIC_CONTROL = "pref_enable_hardware_keys_music_control";
    public static final String PREF_DATE_LAST_CHARGE = "pref_battery_date_last_charge";
    public static final String PREF_BATT_LEVEL = "pref_battery_level";
    public static final String PREF_BATT_ICON_ID = "pref_battery_icon_ID";
    public static final String PREF_DATE_LAST_BATTERY_SYNC = "pref_battery_date_last_sync";
    public static final String PREF_NOTIFICATIONS_SCREEN_ON = "pref_notifications_screen_on";
    public static final String PREF_NOTIFICATIONS_INVERTED_THEME = "pref_notifications_inverted_theme";
    public static final String PREF_NOTIFICATIONS_FONT_SIZE = "pref_notifications_font_size";
    public static final String PREF_DISABLE_NOTIFICATIONS_SCREENON = "pref_notification_screenon";
    public static final String PREF_SHAKE_TO_DISMISS_GRAVITY = "pref_shake_to_dismiss_gravity";
    public static final String PREF_SHAKE_TO_DISMISS_NUM_OF_SHAKES = "pref_shake_to_dismiss_num_of_shakes";
    public static final String PREF_PHONE_CONNECTION_ALERT = "pref_phone_connection_alert";
    public static final String PREF_PHONE_CONNECTION_ALERT_STANDARD_NOTIFICATION = "pref_phone_connection_alert_standard_notification";
    public static final String PREF_DISABLE_DELAY = "pref_notification_delay";
    public static final String PREF_AMAZMOD_FIRST_WIDGET = "pref_amazmod_first_widget";
    public static final String PREF_BATTERY_WATCH_ALERT = "pref_battery_watch_alert";
    public static final String PREF_BATTERY_PHONE_ALERT = "pref_battery_phone_alert";
    public static final String PREF_SPRINGBOARD_ORDER = "pref_springboard_order";
    public static final String PREF_HIDDEN_APPS = "pref_hidden_apps";
    public static final String PREF_BATTERY_GRAPH_DAYS = "pref_battery_graph_days";
    public static final String PREF_AMAZMOD_OFFICIAL_WIDGETS_ORDER = "pref_amazmod_official_widgets_order";

    public static final String CUSTOM_WATCHFACE_DATA = "CustomWatchfaceData";


    public static final int PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT = 10 * 1000;
    public static final int PREF_DEFAULT_NOTIFICATION_VIBRATION = 350;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_ENABLE_CUSTOM_UI = false;
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES = false;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_INVERTED_THEME = true;
    public static final String PREF_DEFAULT_NOTIFICATIONS_FONT_SIZE = "n";
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON = false;
    public static final boolean PREF_DEFAULT_DISABLE_DELAY = false;
    public static final String PREF_DEFAULT_LOCALE = "pref_default_locale";


    public static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    public static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
    public static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

    public static final String SCREEN_BRIGHTNESS= "screen_brightness";

    public static final String RED = "red";
    public static final String BLUE = "blue";
    public static final String GREY = "grey";

    public static final String BLOCK_APP = "999999";

    public static final String TEXT = "text";
    public static final String TIME = "time";
    public static final String APP_TAG = "app_tag";
    public static final String MY_APP = "my_app";
    public static final String OTHER_APP = "other_app";
    public static final String MODE = "mode";
    public static final String INSTALL = "install";
    public static final String DELETE = "delete";
    public static final String NORMAL = "normal";
    public static final String PKG = "package_name";
    public static final String DELETED_APP = "deleted_app";
    public static final String ADDED_APP = "added_app";

    public static final int VIBRATION_SHORT = 100;
    public static final int VIBRATION_LONG = 700;

    public static final String[] BUILD_VERGE_MODELS = {"A1811", "A1801"};

    public static final String WIDGET_ORDER_IN = "springboard_widget_order_in";
    public static final String WIDGET_ORDER_OUT = "springboard_widget_order_out";

}
