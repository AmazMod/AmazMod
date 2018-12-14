package com.amazmod.service.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.adapters.MenuListAdapter;
import com.amazmod.service.events.incoming.EnableLowPower;
import com.amazmod.service.events.incoming.RevokeAdminOwner;
import com.amazmod.service.models.MenuItems;
import com.amazmod.service.springboard.LauncherWearGridActivity;
import com.amazmod.service.springboard.WidgetsReorderActivity;
import com.amazmod.service.ui.InputMethodActivity;
import com.amazmod.service.util.DeviceUtil;
import com.huami.watch.transport.DataBundle;

import java.util.ArrayList;
import java.util.List;

import xiaofei.library.hermeseventbus.HermesEventBus;

import static android.content.Context.VIBRATOR_SERVICE;

public class WearMenuFragment extends Fragment implements WearableListView.ClickListener,
        DelayedConfirmationView.DelayedConfirmationListener {

    private View mainLayout, confirmView;
	private WearableListView listView;
    private DelayedConfirmationView delayedConfirmationView;
    private TextView mHeader, textView1, textView2;

	private String[] mItems = { "Wi-Fi Toggle",
                                "Wi-Fi Panel",
                                "Flashlight",
                                "Reorder Widgets",
                                "QR code",
                                "Clean Memory",
                                "Enable L.P.M.",
                                "Revoke Device Admin",
                                "Set Device Admin",
                                "Restart Launcher",
                                "Clear Launcher Settings",
                                "Reboot",
                                "Enter Fastboot",
                                "Enter Recovery",
                                "Units",
                                "Disconnect Alert",
                                "Away Alert",
                                "Change Input Method",
                                "Device Info"};

    private int[] mImagesOn = { R.drawable.baseline_wifi_white_24,
                                R.drawable.baseline_perm_scan_wifi_white_24,
                                R.drawable.baseline_highlight_white_24,
                                R.drawable.outline_settings_white_24,
                                R.drawable.ic_qrcode_white_24dp,
                                R.drawable.outline_clear_all_white_24,
                                R.drawable.ic_action_star,
                                R.drawable.ic_close_white_24dp,
			                    R.drawable.ic_action_done,
			                    R.drawable.ic_action_refresh,
			                    R.drawable.ic_border_none_white_24dp,
                                R.drawable.ic_restart_white_24dp,
                                R.drawable.baseline_adb_white_24,
                                R.drawable.outline_update_white_24,
                                R.drawable.ic_weight_pound_white_24dp,
                                R.drawable.device_information_white_24x24,
                                R.drawable.ic_alarm_light_white_24dp,
                                R.drawable.outline_keyboard_white_24,
                                R.drawable.baseline_info_white_24};

    private int[] mImagesOff = {    R.drawable.baseline_wifi_off_white_24,
                                    R.drawable.baseline_perm_scan_wifi_white_24,
                                    R.drawable.baseline_highlight_white_24,
                                    R.drawable.outline_settings_white_24,
                                    R.drawable.ic_qrcode_white_24dp,
                                    R.drawable.outline_clear_all_white_24,
                                    R.drawable.ic_action_star,
                                    R.drawable.ic_close_white_24dp,
                                    R.drawable.ic_action_done,
                                    R.drawable.ic_action_refresh,
                                    R.drawable.ic_border_none_white_24dp,
                                    R.drawable.ic_restart_white_24dp,
                                    R.drawable.baseline_adb_white_24,
                                    R.drawable.outline_update_white_24,
                                    R.drawable.ic_weight_kilogram_white_24dp,
                                    R.drawable.device_information_off_white_24x24,
                                    R.drawable.ic_alarm_light_off_white_24dp,
                                    R.drawable.outline_keyboard_white_24,
                                    R.drawable.baseline_info_white_24};

    private String[] toggle = { "",
                                "adb shell am start -n com.huami.watch.otawatch/.wifi.WifiListActivity;exit&",
                                "",
                                "",
                                "adb shell am start -n com.huami.watch.setupwizard/.InitPairQRActivity;exit&",
                                "kill-all",
                                "",
                                "",
                                "adb shell dpm set-active-admin com.amazmod.service/.AdminReceiver;exit&",
                                "adb shell am force-stop com.huami.watch.launcher;exit&",
                                "adb shell rm -rf /sdcard/.watchfacethumb/*;pm clear com.huami.watch.launcher;am force-stop com.huami.watch.launcher;exit&",
                                "reboot",
                                "reboot bootloader",
                                "reboot recovery",
                                "measurement",
                                "huami.watch.localonly.ble_lost_anti_lost",
                                "huami.watch.localonly.ble_lost_far_away",
                                "",
                                ""};

	private int itemChosen;
    List<MenuItems> items;

    private BroadcastReceiver receiverConnection, receiverSSID;
    private Context mContext;
    private MenuListAdapter mAdapter;
    private WifiManager wfmgr;
    private Vibrator vibrator;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Log.i(Constants.TAG,"WearMenuFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //notificationData = NotificationData.fromBundle(getArguments());
        Log.i(Constants.TAG,"WearMenuFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.i(Constants.TAG,"WearMenuFragment onCreateView");

        return inflater.inflate(R.layout.activity_wear_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(Constants.TAG,"WearMenuFragment onViewCreated");

        updateContent();

    }

    @Override
    public void onStart() {
        super.onStart();

    }

	@SuppressLint("ClickableViewAccessibility")
	private void updateContent() {

        wfmgr = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);

        mainLayout = getActivity().findViewById(R.id.wear_menu_main_layout);
        textView1 = getActivity().findViewById(R.id.wear_menu_confirm_text);
        textView2 = getActivity().findViewById(R.id.wear_menu_cancel_text);
		listView = getActivity().findViewById(R.id.wear_menu_list);
		mHeader = getActivity().findViewById(R.id.wear_menu_header);

        confirmView = getActivity().findViewById(R.id.wear_menu_confirm_layout);
        delayedConfirmationView = getActivity().findViewById(R.id.wear_menu_delayedView);

        listView.setLongClickable(true);
        listView.setGreedyTouchMode(true);
        hideConfirm();

        items = new ArrayList<>();
        boolean state;
        for (int i=0; i<mItems.length; i++){
            try {
                if (i == 0)
                    state = wfmgr.isWifiEnabled();
                else
                    state = i < 14 || i > 16 || Settings.Secure.getInt(mContext.getContentResolver(), toggle[i], 0) != 0;
            } catch (NullPointerException e) {
                state = true;
                Log.e(Constants.TAG, "WearMenuFragment onCreate exception: " + e.toString());
            }
            items.add(new MenuItems(mImagesOn[i], mImagesOff[i], mItems[i], state));
        }

        checkConnection();
        loadAdapter("AmazMod");

        delayedConfirmationView.setTotalTimeMs(3000);
        textView1.setText("Proceeding in 3s…");
        textView2.setText("Tap to cancel");

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

	    itemChosen = viewHolder.getPosition();
        switch (itemChosen) {

            case 0:
                if (wfmgr.isWifiEnabled()) {
                    items.get(0).state = false;
                    wfmgr.setWifiEnabled(false);
                } else {
                    items.get(0).state = true;
                    wfmgr.setWifiEnabled(true);
                }
                mAdapter.notifyDataSetChanged();
                break;

            case 1:
                runCommand(toggle[itemChosen]);
                break;

            case 2:
                startWearGridActivity(LauncherWearGridActivity.FLASHLIGHT);
                break;
            case 3:
                final Intent intent_widget = new Intent(mContext, WidgetsReorderActivity.class);
                intent_widget.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                mContext.startActivity(intent_widget);
                break;
            case 4:
                runCommand(toggle[itemChosen]);
                break;

            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
                beginCountdown();
                break;

            case 14:
            case 15:
            case 16:
                toggle(itemChosen);
                break;

            case 17:
                final Intent intent = new Intent(mContext, InputMethodActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                mContext.startActivity(intent);
                break;

            case 18:
                startWearGridActivity(LauncherWearGridActivity.INFO);
                break;

            default:
                Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void runCommand(String command) {
        Log.d(Constants.TAG, "WearMenuFragment runCommand: " + command);
	    if (!command.isEmpty()) {
            try {
                Runtime.getRuntime().exec(command, null, Environment.getExternalStorageDirectory());
            } catch (Exception e) {
                Log.e(Constants.TAG, "WearMenuFragment runCommand exception: " + e.toString());
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
        showConfirm();
        delayedConfirmationView.setPressed(false);
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);
        Log.d(Constants.TAG, "WearMenuFragment beginCountdown: " + delayedConfirmationView.isPressed());
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        delayedConfirmationView.reset();
        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);
        hideConfirm();
        Log.d(Constants.TAG, "WearMenuFragment onTimerSelected v.isPressed: " + v.isPressed());
    }

    @Override
    public void onTimerFinished(View v) {
        Log.d(Constants.TAG, "WearMenuFragment onTimerFinished v.isPressed: " + v.isPressed());
        ((DelayedConfirmationView) v).setListener(null);
        final Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                hideConfirm();
            }
        }, 1000);
        switch (itemChosen) {

            case 5:
                Toast.makeText(mContext, "Killing background processes…", Toast.LENGTH_SHORT).show();
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                    DeviceUtil.killBackgroundTasks(mContext);
                    }
                }, 1000);
                break;

            case 6:
                HermesEventBus.getDefault().post(new EnableLowPower(new DataBundle()));
                break;

            case 7:
                HermesEventBus.getDefault().post(new RevokeAdminOwner(new DataBundle()));
                break;

            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
                runCommand(toggle[itemChosen]);
                break;
        }
        itemChosen = 0;
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

    public void hideConfirm() {
        //confirmView.getAnimation().setFillAfter(false);
        confirmView.setVisibility(View.GONE);
        mHeader.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        confirmView.setClickable(false);
        confirmView.clearAnimation();
        listView.requestFocus();
        listView.setClickable(true);
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
    }

    private void checkConnection() {

        receiverConnection = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                WifiInfo wifiInfo = wfmgr.getConnectionInfo();
                Log.d(Constants.TAG, "WearMenuFragment checkConnection wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Log.d(Constants.TAG, "WearMenuFragment checkConnection wifiInfo.SSID: " + wifiInfo.getSSID());
                Log.d(Constants.TAG, "WearMenuFragment checkConnection action: " + intent.getAction());
                Log.d(Constants.TAG, "WearMenuFragment checkConnection connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    if (wifiInfo.getSupplicantState().toString().equals("COMPLETED"))
                        if (receiverSSID == null)
                            getSSID();
                } else {
                    vibrator.vibrate(100);
                    Toast.makeText(mContext, "Wi-Fi Disconnected", Toast.LENGTH_SHORT).show();
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
                Log.d(Constants.TAG, "WearMenuFragment getSSID wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Log.d(Constants.TAG, "WearMenuFragment getSSID wifiInfo.SSID: " + wifiInfo.getSSID());
                Log.d(Constants.TAG, "WearMenuFragment getSSID action: " + intent.getAction());
                Log.d(Constants.TAG, "WearMenuFragment getSSID connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));

                if (wifiInfo.getSupplicantState().equals(SupplicantState.ASSOCIATED))
                    flag = true;

                if (wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED) && flag) {
                    flag = false;
                    vibrator.vibrate(100);
                    Toast.makeText(mContext, "Wi-Fi Connected to:\n" + wifiInfo.getSSID(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mContext.registerReceiver(receiverSSID, intentFilter);
    }

    private void toggle(int id) {
        final int status = Settings.Secure.getInt(mContext.getContentResolver(), toggle[id], 0);
        Log.d(Constants.TAG, "WearMenuFragment toggleUnit toggle: " + toggle[id] + " \\ status: " + status);
        if ( status == 0) {
            items.get(id).state = true;
            runCommand("adb shell settings put secure " + toggle[id] + " 1;exit");
            //Settings.Secure.putInt(mContext.getContentResolver(), toggle, 1);
        } else {
            items.get(id).state = false;
            runCommand("adb shell settings put secure " + toggle[id] + " 0;exit");
            //Settings.Secure.putInt(mContext.getContentResolver(), toggle, 0);
        }
        mAdapter.notifyDataSetChanged();
    }

    public static WearMenuFragment newInstance() {
        Log.i(Constants.TAG,"WearMenuFragment newInstance");
        return new WearMenuFragment();
    }
}