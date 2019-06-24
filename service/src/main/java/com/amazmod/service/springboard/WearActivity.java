package com.amazmod.service.springboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.adapters.MenuListAdapter;
import com.amazmod.service.events.incoming.EnableLowPower;
import com.amazmod.service.events.incoming.RevokeAdminOwner;
import com.amazmod.service.models.MenuItems;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

public class WearActivity extends Activity implements WearableListView.ClickListener,
        DelayedConfirmationView.DelayedConfirmationListener {

    private View mainLayout, confirmView, infoView;
    private ViewGroup viewGroup;
	private WearableListView listView;
	private Button buttonClose;
    private DelayedConfirmationView delayedConfirmationView;
    private TextView mHeader,textView1, textView2, textView02, textView03, textView04;

	private String[] mItems = { "Wi-Fi Toggle",
                                "Wi-Fi Panel",
                                "Flashlight",
                                "QR code",
                                "Enable L.P.M.",
                                "Revoke Device Admin",
                                "Set Device Admin",
                                "Restart Launcher",
                                "Clear Launcher Settings",
                                "Reboot",
                                "Enter Fastboot",
                                "Units",
                                "Disconnect Alert",
                                "Away Alert",
                                "Device Info"};

    private int[] mImagesOn = { R.drawable.baseline_wifi_white_24,
                                R.drawable.baseline_perm_scan_wifi_white_24,
                                R.drawable.baseline_highlight_white_24,
                                R.drawable.ic_qrcode_white_24dp,
                                R.drawable.ic_action_star,
                                R.drawable.ic_close_white_24dp,
			                    R.drawable.ic_action_done,
			                    R.drawable.ic_action_refresh,
			                    R.drawable.ic_border_none_white_24dp,
                                R.drawable.ic_restart_white_24dp,
                                R.drawable.baseline_adb_white_24,
                                R.drawable.ic_weight_pound_white_24dp,
                                R.drawable.device_information_white_24x24,
                                R.drawable.ic_alarm_light_white_24dp,
                                R.drawable.baseline_info_white_24};

    private int[] mImagesOff = {    R.drawable.baseline_wifi_off_white_24,
                                    R.drawable.baseline_perm_scan_wifi_white_24,
                                    R.drawable.baseline_highlight_white_24,
                                    R.drawable.ic_qrcode_white_24dp,
                                    R.drawable.ic_action_star,
                                    R.drawable.ic_close_white_24dp,
                                    R.drawable.ic_action_done,
                                    R.drawable.ic_action_refresh,
                                    R.drawable.ic_border_none_white_24dp,
                                    R.drawable.ic_restart_white_24dp,
                                    R.drawable.baseline_adb_white_24,
                                    R.drawable.ic_weight_kilogram_white_24dp,
                                    R.drawable.device_information_off_white_24x24,
                                    R.drawable.ic_alarm_light_off_white_24dp,
                                    R.drawable.baseline_info_white_24};

    private String[] toggle = { "",
                                "adb shell am start -n com.huami.watch.otawatch/.wifi.WifiListActivity",
                                "",
                                "adb shell am start -n com.huami.watch.setupwizard/.InitPairQRActivity",
                                "",
                                "",
                                "adb shell dpm set-active-admin com.amazmod.service/.receiver.AdminReceiver",
                                "adb shell am force-stop com.huami.watch.launcher",
                                "adb shell rm -rf /sdcard/.watchfacethumb/* && adb shell pm clear com.huami.watch.launcher",
                                "reboot",
                                "reboot bootloader",
                                "measurement",
                                "huami.watch.localonly.ble_lost_anti_lost",
                                "huami.watch.localonly.ble_lost_far_away",
                                ""};

	private int itemChosen;
    private static boolean screenToggle = false;
    private static int screenMode;
    private static int screenBrightness = 999989;
    List<MenuItems> items;

    private BroadcastReceiver receiverConnection, receiverSSID;
    private Context mContext;
    private MenuListAdapter mAdapter;
    private WifiManager wfmgr;
    private Vibrator vibrator;


	@SuppressLint("ClickableViewAccessibility")
    @Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        mContext = this;
        setContentView(R.layout.activity_wear);

        wfmgr = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);

        mainLayout = findViewById(R.id.main_layout);

		listView = findViewById(R.id.list_menu);
		mHeader = findViewById(R.id.header_menu);

        confirmView = findViewById(R.id.confirm_layout);
        infoView = findViewById(R.id.info_layout);
        textView1 = findViewById(R.id.confirm_text);
        textView2 = findViewById(R.id.cancel_text);
        textView02 = findViewById(R.id.textView02);
        textView03 = findViewById(R.id.textView03);
        textView04 = findViewById(R.id.textView04);
        buttonClose = findViewById(R.id.buttonClose);
        delayedConfirmationView = findViewById(R.id.delayedView);

        listView.setLongClickable(true);
        hideInfo();
        hideConfirm();

        items = new ArrayList<>();
        boolean state;
        for (int i=0; i<mItems.length; i++){
            try {
                if (i == 0)
                    state = wfmgr.isWifiEnabled();
                else
                    state = i < 11 || i > 13 || Settings.Secure.getInt(mContext.getContentResolver(), toggle[i], 0) != 0;
            } catch (NullPointerException e) {
                state = true;
                Logger.error("WearActivity onCreate exception: " + e.toString());
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

		mAdapter = new MenuListAdapter(this, items);

		listView.setAdapter(mAdapter);
		listView.addOnScrollListener(mOnScrollListener);
		listView.setClickListener(this);
	}

	@Override
    protected void onResume() {
	    super.onResume();
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
                flashlight();
                break;

            case 3:
                runCommand(toggle[itemChosen]);
                break;

            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
                beginCountdown();
                break;

            case 11:
            case 12:
            case 13:
                toggle(itemChosen);
                break;

            case 14:
                showInfo();
                break;

            default:
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void runCommand(String command) {
        Logger.debug("WearActivity runCommand");
	    if (!command.isEmpty()) {
            try {
                Runtime.getRuntime().exec(command);
            } catch (Exception e) {
                Logger.error("WearActivity onClick exception: " + e.toString());
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
	    if (receiverConnection != null) unregisterReceiver(receiverConnection);
	    if (receiverSSID != null) unregisterReceiver(receiverSSID);
	    if (screenToggle)
	        setMaxBrightness(false);
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
        //button.setVisibility(View.GONE);
        showConfirm();
        delayedConfirmationView.setPressed(false);
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);
        Logger.debug("WearActivity beginCountdown: " + delayedConfirmationView.isPressed());
    }

    @Override
    public void onTimerSelected(View v) {
        v.setPressed(true);
        delayedConfirmationView.reset();
        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);
        hideConfirm();
        Logger.debug("WearActivity onTimerSelected v.isPressed: " + v.isPressed());
    }

    @Override
    public void onTimerFinished(View v) {
        Logger.debug("WearActivity onTimerFinished v.isPressed: " + v.isPressed());
        ((DelayedConfirmationView) v).setListener(null);
        final Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                hideConfirm();
            }
        }, 1000);
        switch (itemChosen) {

            case 4:
                EventBus.getDefault().post(new EnableLowPower(new DataBundle()));
                break;

            case 5:
                EventBus.getDefault().post(new RevokeAdminOwner(new DataBundle()));
                break;
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
                runCommand(toggle[itemChosen]);
                break;
        }
        itemChosen = 0;
    }

    public void hideInfo() {
        infoView.setVisibility(View.GONE);
    }

    public void hideInfo(View v) {
        infoView.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    public void showInfo() {
        setButtonTheme(buttonClose);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (activityManager != null) {
            try {
                activityManager.getMemoryInfo(memoryInfo);
                double freeRAM = memoryInfo.availMem / 0x100000L;
                long elapsedRealtime = SystemClock.elapsedRealtime() ;
                long sleepTime = SystemClock.elapsedRealtime() - SystemClock.uptimeMillis();

                textView02.setText("Uptime: " + formatInterval(elapsedRealtime, false));
                textView03.setText("SleepTime: " + formatInterval(sleepTime, false));
                textView04.setText("Free RAM: " + freeRAM + "MB");
            } catch (Exception ex) {
                Logger.error("WearActivity onCreate exception: " + ex.toString());
            }
        }

        infoView.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
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
                Logger.debug("WearActivity checkConnection wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Logger.debug("WearActivity checkConnection wifiInfo.SSID: " + wifiInfo.getSSID());
                Logger.debug("WearActivity checkConnection action: " + intent.getAction());
                Logger.debug("WearActivity checkConnection connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    if (wifiInfo.getSupplicantState().toString().equals("COMPLETED"))
                        if (receiverSSID == null)
                            getSSID();
                } else {
                    vibrator.vibrate(100);
                    Toast.makeText(getApplicationContext(), "Wi-Fi Disconnected", Toast.LENGTH_SHORT).show();
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        registerReceiver(receiverConnection, intentFilter);
    }

    private void getSSID() {

        receiverSSID = new BroadcastReceiver() {

            boolean flag = false;
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiInfo wifiInfo = wfmgr.getConnectionInfo();
                Logger.debug("WearActivity getSSID wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Logger.debug("WearActivity getSSID wifiInfo.SSID: " + wifiInfo.getSSID());
                Logger.debug("WearActivity getSSID action: " + intent.getAction());
                Logger.debug("WearActivity getSSID connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));

                if (wifiInfo.getSupplicantState().equals(SupplicantState.ASSOCIATED))
                    flag = true;

                if (wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED) && flag) {
                    flag = false;
                    vibrator.vibrate(100);
                    Toast.makeText(getApplicationContext(), "Wi-Fi Connected to:\n" + wifiInfo.getSSID(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(receiverSSID, intentFilter);
    }

    private void setButtonTheme(Button button) {
        /*
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        param.setMargins(20, 12, 20, 12);
        button.setLayoutParams(param);
        button.setPadding(0, 10, 0, 10);
        */
        button.setIncludeFontPadding(false);
        button.setMinHeight(24);
        button.setMinWidth(120);
        button.setText(getResources().getString(R.string.close));
        button.setAllCaps(false);
        button.setTextColor(Color.parseColor("#000000"));
        button.setBackground(mContext.getDrawable(R.drawable.reply_grey));
    }

    public static String formatInterval(final long interval, boolean millis )
    {
        final long hr = TimeUnit.MILLISECONDS.toHours(interval);
        final long min = TimeUnit.MILLISECONDS.toMinutes(interval) % 60;
        final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) % 60;
        final long ms = TimeUnit.MILLISECONDS.toMillis(interval) % 1000;
        if(millis) {
            return String.format(Locale.getDefault(),"%02d:%02d:%02d.%03d", hr, min, sec, ms);
        } else {
            return String.format(Locale.getDefault(),"%02d:%02d:%02d", hr, min, sec );
        }
    }

    public void flashlight() {
        Logger.debug("WearActivity flashlight on");
        listView.setVisibility(View.GONE);
        mainLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
        setMaxBrightness(true);
        mainLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (screenToggle)
                    setMaxBrightness(false);
                Logger.debug("WearActivity flashlight off");
                mainLayout.setBackground(getResources().getDrawable(R.drawable.background));
                listView.setVisibility(View.VISIBLE);
                return false;
            }
        });

    }

    private void setMaxBrightness(boolean mode) {

        if (mode) {
            Logger.debug("WearActivity setScreenModeOff mode tue");
            screenMode = Settings.System.getInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, 0);
            screenBrightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        } else {
            if (screenBrightness != 999989) {
                Logger.debug("WearActivity setScreenModeOff mode false \\ screenMode: " + screenMode);
                Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, screenMode);
                Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightness);
            }
        }
        screenToggle = mode;
    }

    private void toggle(int id) {
        final int status = Settings.Secure.getInt(mContext.getContentResolver(), toggle[id], 0);
        Logger.debug("WearActivity toggleUnit toggle: " + toggle[id] + " \\ status: " + status);
        if ( status == 0) {
            items.get(id).state = true;
            runCommand("adb shell settings put secure " + toggle[id] + " 1");
            //Settings.Secure.putInt(mContext.getContentResolver(), toggle, 1);
        } else {
            items.get(id).state = false;
            runCommand("adb shell settings put secure " + toggle[id] + " 0");
            //Settings.Secure.putInt(mContext.getContentResolver(), toggle, 0);
        }
        mAdapter.notifyDataSetChanged();
    }
}