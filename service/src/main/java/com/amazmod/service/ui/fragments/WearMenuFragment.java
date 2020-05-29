package com.amazmod.service.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.adapters.MenuListAdapter;
import com.amazmod.service.events.HourlyChime;
import com.amazmod.service.events.incoming.EnableLowPower;
import com.amazmod.service.events.incoming.RevokeAdminOwner;
import com.amazmod.service.models.MenuItems;
import com.amazmod.service.springboard.LauncherWearGridActivity;
import com.amazmod.service.springboard.WidgetSettings;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.ExecCommand;
import com.huami.watch.transport.DataBundle;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.VIBRATOR_SERVICE;

public class WearMenuFragment extends Fragment implements WearableListView.ClickListener,
        DelayedConfirmationView.DelayedConfirmationListener {

    private View mainLayout, confirmView;
    private WearableListView listView;
    private DelayedConfirmationView delayedConfirmationView;
    private TextView mHeader, textView1, textView2;
    public static boolean chimeEnabled;

    private MenuItems currentItem;
    List<MenuItems> items;



    private BroadcastReceiver receiverConnection, receiverSSID;
    private Context mContext;
    private MenuListAdapter mAdapter;
    private WifiManager wfmgr;
    private Vibrator vibrator;
    private WidgetSettings widgetSettings;

    private static final int MENU_START = 9;

    private static final String MENU_WIFI = "wifi";
    private static final String MENU_CHIME = "chime";
    private static final String MENU_SCREEN_ON = "screenon";
    private static final String MENU_CLEAN_MEMORY = "cleanmemory";
    private static final String MENU_LPM = "lpm";
    private static final String MENU_REVOKE_ADMIN = "revoke_adm";



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.info("WearMenuFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //notificationData = NotificationData.fromBundle(getArguments());
        Logger.info("WearMenuFragment onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.info("WearMenuFragment onCreateView");

        return inflater.inflate(R.layout.fragment_wear_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.info("WearMenuFragment onViewCreated");

        init();
        updateContent();

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void init() {
        wfmgr = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);

        mainLayout = getActivity().findViewById(R.id.wear_menu_main_layout);
        textView1 = getActivity().findViewById(R.id.wear_menu_confirm_text);
        textView2 = getActivity().findViewById(R.id.wear_menu_cancel_text);
        listView = getActivity().findViewById(R.id.wear_menu_list);
        mHeader = getActivity().findViewById(R.id.wear_menu_header);

        confirmView = getActivity().findViewById(R.id.wear_menu_confirm_layout);
        delayedConfirmationView = getActivity().findViewById(R.id.wear_menu_delayedView);

        widgetSettings = new WidgetSettings(Constants.TAG, mContext);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void updateContent() {

        widgetSettings.reload();
        listView.setLongClickable(true);
        listView.setGreedyTouchMode(true);
        hideConfirm();

        items = makeMenuItems();

        checkConnection();
        loadAdapter("AmazMod");

        delayedConfirmationView.setTotalTimeMs(3000);
        textView1.setText(getString(R.string.proceeding_in_3s));
        textView2.setText(getString(R.string.tap_to_cancel));
    }

    private boolean getSettings(String value) {
        return Settings.Secure.getInt(mContext.getContentResolver(), value, 0) != 0;
    }

    private List<MenuItems> makeMenuItems() {
        List<MenuItems> itemList = new ArrayList<>();
        MenuItems item;

        //Apps Manager
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_apps_mamager),
                R.drawable.ic_action_select_all
        );
        item.setActionWearActivity(LauncherWearGridActivity.APPS);
        itemList.add(item);


        //Files Manager
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_files_mamager),
                R.drawable.outline_folder_white_24
        );
        item.setActionWearActivity(LauncherWearGridActivity.FILES);
        itemList.add(item);


        //Reorder Widgets
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_reorder_widgets),
                R.drawable.outline_widgets_white_24
        );
        item.setActionActivity("com.amazmod.service.springboard.WidgetsReorderActivity");
        itemList.add(item);


        //Screen Settings
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_screen_settings),
                R.drawable.outline_fullscreen_white_24
        );
        item.setActionActivity("com.amazmod.service.ui.ScreenSettingsActivity");
        itemList.add(item);


        //Battery Graph
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_battery_graph),
                R.drawable.battery_unknown_white_24dp
        );
        item.setActionActivity("com.amazmod.service.ui.BatteryGraphActivity");
        itemList.add(item);

        //Wi-Fi Toggle
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_wifi_toggle),
                R.drawable.baseline_wifi_white_24,
                R.drawable.baseline_wifi_off_white_24,
                wfmgr.isWifiEnabled()
        );
        item.setActionCustom(MENU_WIFI);
        itemList.add(item);

        //Wi-Fi Panel
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_wifi_panel),
                R.drawable.baseline_perm_scan_wifi_white_24
        );
        item.setActionCommand("adb shell am start -n com.huami.watch.otawatch/.wifi.WifiListActivity");
        itemList.add(item);


        //Flashlight
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_flashlight),
                R.drawable.baseline_highlight_white_24
        );
        item.setActionWearActivity(LauncherWearGridActivity.FLASHLIGHT);
        itemList.add(item);

        //QR code
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_qrcode),
                R.drawable.ic_qrcode_white_24dp
        );
        item.setActionCommand("adb shell am start -n com.huami.watch.setupwizard/.InitPairQRActivity");
        itemList.add(item);

        //Clean Memory
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_clean_memory),
                R.drawable.outline_clear_all_white_24
        );
        item.setActionCustomDelay(MENU_CLEAN_MEMORY);
        itemList.add(item);

        //Enable LPM
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_lpm),
                R.drawable.ic_action_star
        );
        item.setActionCustomDelay(MENU_LPM);
        itemList.add(item);


        //Set Device Admin
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_grant_admin),
                R.drawable.ic_action_done
        );
        item.setActionCommandDelay("adb shell dpm set-active-admin com.amazmod.service/.receiver.AdminReceiver");
        itemList.add(item);


        //Revoke Device Admin
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_revoke_admin),
                R.drawable.ic_close_white_24dp
        );
        item.setActionCustomDelay(MENU_REVOKE_ADMIN);
        itemList.add(item);

        //Restart Launcher
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_restart_launcher),
                R.drawable.ic_action_refresh
        );
        item.setActionCommandDelay("adb shell am force-stop com.huami.watch.launcher");
        itemList.add(item);

        //Clear Launcher Settings
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_clear_launcher),
                R.drawable.ic_border_none_white_24dp
        );
        item.setActionCommandDelay("adb shell rm -rf /sdcard/.watchfacethumb/*;pm clear com.huami.watch.launcher;am force-stop com.huami.watch.launcher");
        itemList.add(item);

        //Reboot
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_reboot),
                R.drawable.ic_restart_white_24dp
        );
        item.setActionCommandDelay("reboot");
        itemList.add(item);

        //Fastboot
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_reboot_fastboot),
                R.drawable.baseline_adb_white_24
        );
        item.setActionCommandDelay("reboot bootloader");
        itemList.add(item);

        //Recovery
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_reboot_recovery),
                R.drawable.outline_update_white_24
        );
        item.setActionCommandDelay("reboot recovery");
        itemList.add(item);

        //Units
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_units),
                R.drawable.ic_weight_pound_white_24dp,
                R.drawable.ic_weight_kilogram_white_24dp,
                getSettings("measurement")
        );
        item.setActionToggle("measurement");
        itemList.add(item);

        //Disconnect Alert (iOS)
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_anti_lost),
                R.drawable.device_information_white_24x24,
                R.drawable.device_information_off_white_24x24,
                getSettings("huami.watch.localonly.ble_lost_anti_lost")
        );
        item.setActionToggle("huami.watch.localonly.ble_lost_anti_lost");
        itemList.add(item);

        //Away Alert (iOS)
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_away_alert),
                R.drawable.ic_alarm_light_white_24dp,
                R.drawable.ic_alarm_light_off_white_24dp,
                getSettings("huami.watch.localonly.ble_lost_far_away")
        );
        item.setActionToggle("huami.watch.localonly.ble_lost_far_away");
        itemList.add(item);

        //Notifications ScreenOn
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_screenon),
                R.drawable.outline_flash_on_white_24,
                R.drawable.outline_flash_off_white_24,
                widgetSettings.get(Constants.PREF_NOTIFICATIONS_SCREEN_ON, 0) != 0
        );
        item.setActionCustom(MENU_SCREEN_ON);
        itemList.add(item);

        //Change Input Method
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_input_method),
                R.drawable.outline_keyboard_white_24
        );
        item.setActionActivity("com.amazmod.service.ui.InputMethodActivity");
        itemList.add(item);


        //Device Info
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_device_info),
                R.drawable.baseline_info_white_24
        );
        item.setActionWearActivity(LauncherWearGridActivity.INFO);
        itemList.add(item);

        //Hourly Chime
        item = new MenuItems(
                getResources().getString(R.string.activity_menu_hourly_chime),
                R.drawable.hourly_chime_on,
                R.drawable.hourly_chime_off,
                chimeEnabled
        );
        item.setActionCustom(MENU_CHIME);
        itemList.add(item);

        return itemList;
    }


    private void loadAdapter(String header) {
        mHeader.setText(header);

        mAdapter = new MenuListAdapter(mContext, items);

        listView.setAdapter(mAdapter);
        listView.addOnScrollListener(mOnScrollListener);
        listView.setClickListener(this);
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {

        MenuItems item = items.get(viewHolder.getPosition());

        Logger.debug("executing action: TYPE " + item.getActionType() + " // ACTION: " + item.getAction());

        switch (item.getActionType()) {

            case (MenuItems.ACTION_WEAR_ACTIVITY):
                startWearGridActivity(item.getActionWearActivity());
                break;
            case MenuItems.ACTION_COMMAND:
                runCommand(item.getAction());
                break;
            case MenuItems.ACTION_ACTIVITY:
                try {
                    startActivity(Class.forName(item.getAction()));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case MenuItems.ACTION_TOGGLE:
                toggle(item);
                break;
            case MenuItems.ACTION_CUSTOM:
                runCustomCommand(item);
                break;
            case MenuItems.ACTION_COMMAND_DELAY:
            case MenuItems.ACTION_CUSTOM_DELAY:
                runCustomCommandDelay(item);
                break;
            case MenuItems.ACTION_UNDEFINED:
                Toast.makeText(mContext, R.string.action_undefined, Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(mContext, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void runCustomCommand(MenuItems item){
        switch(item.getAction()){
            case MENU_WIFI:
                if (wfmgr.isWifiEnabled()) {
                    items.get(0).setState(false);
                    wfmgr.setWifiEnabled(false);
                } else {
                    items.get(0).setState(true);
                    wfmgr.setWifiEnabled(true);
                }
                mAdapter.notifyDataSetChanged();
                break;
            case MENU_CHIME:
                chimeEnabled = !chimeEnabled;
                HourlyChime.setHourlyChime(mContext, chimeEnabled);
                widgetSettings.set(Constants.PREF_AMAZMOD_HOURLY_CHIME, chimeEnabled);
                items.get(MENU_START + 15).setState(chimeEnabled);
                Logger.debug("Hourly Chime status is: {}", chimeEnabled);
                mAdapter.notifyDataSetChanged();
                Toast.makeText(mContext, getString(R.string.hourly_chime) + " " + (chimeEnabled ? getString(R.string.enabled) : getString(R.string.disabled)), Toast.LENGTH_SHORT).show();
                break;
            case MENU_SCREEN_ON:
                final int status = widgetSettings.get(Constants.PREF_NOTIFICATIONS_SCREEN_ON, 0);
                if (status == 0) {
                    item.setState(true);
                    widgetSettings.set(Constants.PREF_NOTIFICATIONS_SCREEN_ON, 1);
                } else {
                    item.setState(false);
                    widgetSettings.set(Constants.PREF_NOTIFICATIONS_SCREEN_ON, 0);
                }
                mAdapter.notifyDataSetChanged();
                break;
        }

    }

    private void runCustomCommandDelay(MenuItems item){
        currentItem = item;
        beginCountdown();
    }


    private void runCommand(String command) {
        Logger.debug("WearMenuFragment runCommand: " + command);
        if (!command.isEmpty()) {

            new ExecCommand(ExecCommand.ADB, command);
            if (command.contains("launcher")) {
                Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage("com.huami.watch.launcher");
                if (launchIntent != null) {
                    startActivity(launchIntent);
                }
            }
        }
    }

    @Override
    public void onTopEmptyRegionClick() {
        //Prevent NullPointerException
        //Toast.makeText(this, "Top empty area tapped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        if (receiverConnection != null) mContext.unregisterReceiver(receiverConnection);
        if (receiverSSID != null) mContext.unregisterReceiver(receiverSSID);
        super.onDestroy();
    }

    // The following code ensures that the title scrolls as the user scrolls up
    // or down the list
    private WearableListView.OnScrollListener mOnScrollListener =
            new WearableListView.OnScrollListener() {
                @Override
                public void onAbsoluteScrollChange(int i) {
                    // Only scroll the title up from its original base position
                    // and not down.
                    if (i > 0) {
                        mHeader.setY(-i);
                    }
                }

                @Override
                public void onScroll(int i) {
                    // Placeholder
                }

                @Override
                public void onScrollStateChanged(int i) {
                    // Placeholder
                }

                @Override
                public void onCentralPositionChanged(int i) {
                    // Placeholder
                }
            };

    /**
     * Starts the DelayedConfirmationView when user presses "Start Timer" button.
     */
    public void beginCountdown() {
        if (currentItem.getAction() == MENU_LPM){
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.low_power_mode_warning)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            EventBus.getDefault().post(new EnableLowPower(new DataBundle()));
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
            return;
        }
        showConfirm();
        delayedConfirmationView.setPressed(false);
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);
        Logger.debug("WearMenuFragment beginCountdown: " + delayedConfirmationView.isPressed());
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        delayedConfirmationView.reset();
        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);
        hideConfirm();
        Logger.debug("WearMenuFragment onTimerSelected v.isPressed: " + v.isPressed());
    }

    @Override
    public void onTimerFinished(View v) {
        Logger.debug("WearMenuFragment onTimerFinished v.isPressed: " + v.isPressed());
        ((DelayedConfirmationView) v).setListener(null);

        final Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                hideConfirm();
            }
        }, 1000);


        switch(currentItem.getActionType()){
            case MenuItems.ACTION_CUSTOM_DELAY:
                switch (currentItem.getAction()) {
                    case MENU_CLEAN_MEMORY:
                        Toast.makeText(mContext, mContext.getResources().getString(R.string.killing_background_process), Toast.LENGTH_SHORT).show();
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                DeviceUtil.killBackgroundTasks(mContext, true);
                            }
                        }, 1000);
                        break;

                    case MENU_LPM:
                        EventBus.getDefault().post(new EnableLowPower(new DataBundle()));
                        break;

                    case MENU_REVOKE_ADMIN:
                        EventBus.getDefault().post(new RevokeAdminOwner(new DataBundle()));
                        break;

                }
            case MenuItems.ACTION_COMMAND_DELAY:
                runCommand(currentItem.getAction());
                break;
        }

        currentItem = null;
    }

    public void startWearGridActivity(char mode) {

        final Intent intent = new Intent(mContext, LauncherWearGridActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(LauncherWearGridActivity.MODE, mode);
        mContext.startActivity(intent);
    }

    public void startActivity(Class className) {
        final Intent intent = new Intent(mContext, className);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
    }

    public void hideConfirm() {
        //confirmView.getAnimation().setFillAfter(false);
        confirmView.setVisibility(View.GONE);
        mHeader.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        confirmView.setClickable(false);
        confirmView.clearAnimation();
        listView.requestFocus();
        listView.setClickable(true);
        ((LauncherWearGridActivity) getActivity()).setSwipeable(true);
    }

    public void showConfirm() {
        //listView.getAnimation().setFillAfter(false);
        listView.setVisibility(View.GONE);
        mHeader.setVisibility(View.GONE);
        confirmView.setVisibility(View.VISIBLE);
        listView.setClickable(false);
        listView.clearAnimation();
        confirmView.requestFocus();
        confirmView.setClickable(true);
        ((LauncherWearGridActivity) getActivity()).setSwipeable(false);
    }

    private void checkConnection() {

        receiverConnection = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                WifiInfo wifiInfo = wfmgr.getConnectionInfo();
                Logger.debug("WearMenuFragment checkConnection wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Logger.debug("WearMenuFragment checkConnection wifiInfo.SSID: " + wifiInfo.getSSID());
                Logger.debug("WearMenuFragment checkConnection action: " + intent.getAction());
                Logger.debug("WearMenuFragment checkConnection connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    if (wifiInfo.getSupplicantState().toString().equals("COMPLETED"))
                        if (receiverSSID == null)
                            getSSID();
                } else {
                    vibrator.vibrate(100);
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.wifi_disconnected), Toast.LENGTH_SHORT).show();
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mContext.registerReceiver(receiverConnection, intentFilter);
    }

    private void getSSID() {

        receiverSSID = new BroadcastReceiver() {

            boolean flag = false;

            @Override
            public void onReceive(Context context, Intent intent) {
                WifiInfo wifiInfo = wfmgr.getConnectionInfo();
                Logger.debug("WearMenuFragment getSSID wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Logger.debug("WearMenuFragment getSSID wifiInfo.SSID: " + wifiInfo.getSSID());
                Logger.debug("WearMenuFragment getSSID action: " + intent.getAction());
                Logger.debug("WearMenuFragment getSSID connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));

                if (wifiInfo.getSupplicantState().equals(SupplicantState.ASSOCIATING))
                    flag = true;

                if (wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED) && flag) {
                    flag = false;
                    vibrator.vibrate(100);
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.wifi_connected_to) + "\n" + wifiInfo.getSSID(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mContext.registerReceiver(receiverSSID, intentFilter);
    }

    private void toggle(MenuItems item) {
        final int status = Settings.Secure.getInt(mContext.getContentResolver(), item.getAction(), 0);
        Logger.debug("WearMenuFragment toggleUnit toggle: " + item.getAction() + " \\ status: " + status);
        if (status == 0) {
            item.setState(true);
            runCommand("adb shell settings put secure " + item.getAction() + " 1;exit");
            //Settings.Secure.putInt(mContext.getContentResolver(), toggle, 1);
        } else {
            item.setState(false);
            runCommand("adb shell settings put secure " + item.getAction() + " 0;exit");
            //Settings.Secure.putInt(mContext.getContentResolver(), toggle, 0);
        }
        mAdapter.notifyDataSetChanged();
    }
    public static WearMenuFragment newInstance() {
        Logger.info("WearMenuFragment newInstance");
        return new WearMenuFragment();
    }
}