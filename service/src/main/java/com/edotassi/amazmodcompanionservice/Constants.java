package com.edotassi.amazmodcompanionservice;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class Constants {

    public static final String TAG = "Amazmod";
    public static final String TAG_NIGHTSCOUT_PAGE = "Amazmod:Nighscout";

    public static final String PACKAGE_NAME = "com.edotassi.amazmodcompanionservice";

    public static final String ACTION_NIGHTSCOUT_SYNC = "nightscout_sync";

    public static final String INTENT_ACTION_REPLY = "com.amazmod.action.reply";

    public static final String EXTRA_REPLY = "extra.reply";
    public static final String EXTRA_NOTIFICATION_KEY = "extra.notification.key";

    public static final String PREF_NOTIFICATION_SCREEN_TIMEOUT = "pref_notification_screen_timeout";
    public static final String PREF_NOTIFICATION_VIBRATION = "pref_notification_vibration";
    public static final String PREF_NOTIFICATION_CUSTOM_REPLIES = "pref_notification_custom_replies";
    public static final String PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI = "pref_notifications_enable_custom_ui";

    public static final int PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT = 10 * 1000;
    public static final int PREF_DEFAULT_NOTIFICATION_VIBRATION = 350;
    public static final boolean PREF_DEFAULT_NOTIFICATIONS_ENABLE_CUSTOM_UI = false;
}
