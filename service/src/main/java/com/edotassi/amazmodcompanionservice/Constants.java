package com.edotassi.amazmodcompanionservice;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class Constants {

    public static final String TAG = "AmazMod";
    public static final String TAG_NIGHTSCOUT_PAGE = "Amazmod:Nighscout";

    public static final String PACKAGE_NAME = "com.edotassi.amazmodcompanionservice";

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
    public static final String PREF_DATE_LAST_CHARGE = "pref_battery_date_last_charge";
    public static final String PREF_BATT_LEVEL = "pref_battery_level";
    public static final String PREF_BATT_ICON_ID = "pref_battery_icon_ID";

    public static final int PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT = 10 * 1000;
    public static final int PREF_DEFAULT_NOTIFICATION_VIBRATION = 350;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_ENABLE_CUSTOM_UI = false;
    public static final boolean PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES = false;

}
