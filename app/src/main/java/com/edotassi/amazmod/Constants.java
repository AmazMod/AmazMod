package com.edotassi.amazmod;

public class Constants {

    public static final String PACKAGE = "com.edotassi.amazmod";
    public static final String TAG = "AmazMod";

    public static final String FAQ_URL = "https://github.com/edotassi/AmazMod/blob/dev/FAQ.md";

    public static final String PREF_ENABLED_NOTIFICATIONS_PACKAGES = "pref.enabled.notifications.packages";
    public static final String PREF_DISABLE_NOTIFICATIONS = "preference.disable.notifications";
    public static final String PREF_DISABLE_NOTIFICATIONS_REPLIES = "preference.amazmodservice.enable.replies";
    public static final String PREF_NOTIFICATIONS_REPLIES = "preference.amazmodservice.replies";
    public static final String PREF_NOTIFICATIONS_VIBRATION = "preference.amazmodservice.vibration";
    public static final String PREF_NOTIFICATIONS_SCREEN_TIMEOUT = "preference.amazmodservice.screen.timeout";
    public static final String PREF_DISABLE_BATTERY_CHART = "preference.disable.battery.chart";
    public static final String PREF_BATTERY_BACKGROUND_SYNC_INTERVAL = "preference.battery.background.sync.interval";
    public static final String PREF_BATTERY_CHART_TIME_INTERVAL = "preference.battery.chart.range";
    public static final String PREF_DISABLE_NOTIFATIONS_WHEN_SCREEN_ON = "preference.disable.notifications.when.screen.on";
    public static final String PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI = "preference.notifications.enable.custom.ui";
    public static final String PREF_KEY_FIRST_START = "preference.key.first.start";
    public static final String PREF_FORCE_ENGLISH = "preference.force.english";

    public static final String PREF_DEFAULT_NOTIFICATIONS_REPLIES = "[]";
    public static final String PREF_DEFAULT_NOTIFICATIONS_VIBRATION = "300";
    public static final String PREF_DEFAULT_NOTIFICATIONS_SCREEN_TIMEOUT = "7000";
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS = false;
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES = false;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_CUSTOM_UI = false;
    public static final boolean PREF_DEFAULT_KEY_FIRST_START = true;
    public static final boolean PREF_DEFAULT_DISABLE_BATTERY_CHART = false;
    public static final String PREF_DEFAULT_BATTERY_CHART_TIME_INTERVAL = "5";

    public static final int REQUEST_CODE_INTRO = 1;

    public static final String PREF_DISABLE_NOTIFICATIONS_WHEN_DND = "preference.disable.notifications.when.dnd" ;
    public static final String PREF_DISABLE_REMOVE_NOTIFICATIONS = "preference.disable.remove.notifications";
    public static final String PREF_NOTIFICATIONS_ENABLE_VOICE_APPS = "preference.notifications.enable.voice.apps";
    public static final String PREF_NOTIFICATIONS_ENABLE_LOCAL_ONLY = "preference.notifications.enable.local.only";
    public static final String PREF_NOTIFICATIONS_ENABLE_WHEN_LOCKED = "preference.notifications.enable.when.locked";
    public static final String PREF_NOTIFICATIONS_ENABLE_UNGROUP = "preference.notifications.enable.ungroup";
    public static final String PREF_TIME_LAST_SYNC = "preference.time.last.sync";

    public static final byte FILTER_CONTINUE = 'C';
    public static final byte FILTER_UNGROUP = 'U';
    public static final byte FILTER_VOICE = 'V';
    public static final byte FILTER_MAPS = 'M';
    public static final byte FILTER_LOCALOK = 'K';

    public static final byte FILTER_PACKAGE = 'P';
    public static final byte FILTER_GROUP = 'G';
    public static final byte FILTER_ONGOING = 'O';
    public static final byte FILTER_LOCAL= 'L';
    public static final byte FILTER_BLOCK = 'B';
    public static final byte FILTER_RETURN = 'R';
}
