package com.edotassi.amazmodcompanionservice;

/**
 * Created by edoardotassinari on 04/04/18.
 */

public class Constants {

    public static final String TAG = "Amazmod";
    public static final String TAG_NIGHTSCOUT_PAGE = "Amazmod:Nighscout";
    public static final String TAG_NOTIFICATION_MANAGER = "Amazmod:NotManager";

    public static final String PACKAGE_NAME = "com.edotassi.amazmodcompanionservice";

    public static final String TRANSPORTER_MODULE = "com.edotassi.amazmod";
    public static final String TRANSPORTER_MODULE_NOTIFICATIONS = "com.amazmod.notifications";

    public static final String ACTION_NIGHTSCOUT_SYNC = "nightscout_sync";
    public static final String ACTION_SETTINGS_SYNC = "settings_sync";
    public static final String ACTION_INBOUND_INFO = "inbound_info";

    public static final String ACTION_OUTBOUND_INFO = "outbound_info";
    public static final String ACTION_REPLY = "reply";

    public static final String PREF_NOTIFICATION_SCREEN_TIMEOUT = "pref_notification_screen_timeout";
    public static final String PREF_NOTIFICATION_VIBRATION = "pref_notification_vibration";
    public static final String PREF_NOTIFICATION_CUSTOM_REPLIES = "pref_notification_custom_replies";

    public static final int PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT = 10 * 1000;
    public static final int PREF_DEFAULT_NOTIFICATION_VIBRATION = 350;
}
