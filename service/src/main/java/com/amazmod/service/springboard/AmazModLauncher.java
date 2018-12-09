package com.amazmod.service.springboard;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.BuildConfig;
import com.amazmod.service.Constants;
import com.amazmod.service.MainService;
import com.amazmod.service.R;
import com.amazmod.service.adapters.LauncherAppAdapter;
import com.amazmod.service.models.MenuItems;
import com.amazmod.service.support.AppInfo;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import clc.sliteplugin.flowboard.AbstractPlugin;
import clc.sliteplugin.flowboard.ISpringBoardHostStub;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.content.Context.POWER_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

public class AmazModLauncher extends AbstractPlugin implements WearableListView.ClickListener {

    private Context mContext;
    private View view, home;
    private boolean isActive = false;
    private ISpringBoardHostStub host = null;

    private WidgetSettings settingsWidget;

    private WearableListView listView;
    private TextView battValueTV, unreadMessages, mHeader;
    private ImageView battIconImg;
    private CircledImageView keepAwake, wifiToggle;

    private List<AppInfo> appInfoList;
    private LauncherAppAdapter mAdapter;
    List<MenuItems> items;

    private static Intent intent;
    private static boolean isWakeLockEnabled = false;
    private static int notifications;

    private BroadcastReceiver receiverConnection, receiverSSID;
    private WifiManager wfmgr;
    private Vibrator vibrator;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private static final String MANAGE_APPS = "MANAGE APPS";

    private String[] mItems = { "WiFiToggle",
            "KeepAwake"};

    private int[] mImagesOn = { R.drawable.baseline_wifi_white_24,
            R.drawable.ic_power_cycle_white_24dp};

    private int[] mImagesOff = { R.drawable.baseline_wifi_off_white_24,
            R.drawable.ic_power_off_white_24dp};

    @SuppressLint("WifiManagerPotentialLeak")
    @Override
    public View getView(final Context paramContext) {

        this.mContext = paramContext;
        Log.d(Constants.TAG, "AmazModLauncher getView mContext: " + mContext.toString()
                + " / this: " + this.toString());

        mContext.startService(new Intent(paramContext, MainService.class));

        this.view = LayoutInflater.from(mContext).inflate(R.layout.amazmod_launcher, null);
        Log.d(Constants.TAG, "AmazModLauncher getView layout inflated");

        TextView version = view.findViewById(R.id.launcher_version);
        ImageView imageView = view.findViewById(R.id.launcher_logo);
        final CircledImageView flashLight = view.findViewById(R.id.launcher_setting_03);
        final CircledImageView settings = view.findViewById(R.id.launcher_setting_04);
        final CircledImageView messages = view.findViewById(R.id.launcher_messages);

        mHeader = view.findViewById(R.id.launcher_header);
        wifiToggle = view.findViewById(R.id.launcher_setting_01);
        keepAwake = view.findViewById(R.id.launcher_setting_02);
        battValueTV = view.findViewById(R.id.launcher_batt_value);
        unreadMessages = view.findViewById(R.id.launcher_messages_count);
        battIconImg = view.findViewById(R.id.launcher_batt_icon);
        listView = view.findViewById(R.id.launcher_listview);

        home = view.findViewById(R.id.launcher_home);

        version.setText(BuildConfig.VERSION_NAME);
        mHeader.setText("Apps");
        flashLight.setImageResource(R.drawable.baseline_highlight_white_24);
        settings.setImageResource(R.drawable.outline_settings_white_24);
        messages.setImageResource(R.drawable.notify_icon_24);

        Log.d(Constants.TAG, "AmazModLauncher getView getting SystemServices");

        wfmgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
        powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
        if (powerManager != null)
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AmazMod::KeepAwake");


        items = new ArrayList<>();
        boolean state;
        for (int i=0; i<mItems.length; i++){
            try {
                if (i == 0)
                    state = wfmgr.isWifiEnabled();
                else
                    state = isWakeLockEnabled;
            } catch (NullPointerException e) {
                state = false;
                Log.e(Constants.TAG, "AmazModLauncher getView exception: " + e.toString());
            }
            Log.d(Constants.TAG, "AmazModLauncher getView addItem: " + mItems[i]);
            items.add(new MenuItems(mImagesOn[i], mImagesOff[i], mItems[i], state));
        }

        checkConnection();
        Log.d(Constants.TAG, "AmazModLauncher getView checkConnection");

        wifiToggle.setImageResource(items.get(0).state ? items.get(0).iconResOn : items.get(0).iconResOff);
        keepAwake.setImageResource(items.get(1).state ? items.get(1).iconResOn : items.get(1).iconResOff);

        loadApps();

        intent = new Intent(mContext, LauncherWearGridActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        final Intent launcherIntent;
        launcherIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    mContext.startActivity(launcherIntent);
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.SETTINGS);
                mContext.startActivity(intent);
            }
        });

        messages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.NOTIFICATIONS);
                mContext.startActivity(intent);
            }
        });

        messages.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                refreshMessages(true);
                return true;
            }
        });

        flashLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.FLASHLIGHT);
                mContext.startActivity(intent);
            }
        });

        wifiToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wfmgr.isWifiEnabled()) {
                    items.get(0).state = false;
                    wfmgr.setWifiEnabled(false);
                } else {
                    items.get(0).state = true;
                    wfmgr.setWifiEnabled(true);
                }
                wifiToggle.setImageResource(items.get(0).state ? items.get(0).iconResOn : items.get(0).iconResOff);
            }
        });

        keepAwake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isWakeLockEnabled || wakeLock.isHeld()) {
                    wakeLock.release();
                    Toast.makeText(mContext, "KeepAwake: Disabled", Toast.LENGTH_SHORT).show();
                } else {
                    wakeLock.acquire(60*60*1000L /*1 hour*/);
                    Toast.makeText(mContext, "KeepAwake: Enabled", Toast.LENGTH_SHORT).show();
                }
                isWakeLockEnabled = !isWakeLockEnabled;
                items.get(1).state = isWakeLockEnabled;
                keepAwake.setImageResource(items.get(1).state ? items.get(1).iconResOn : items.get(1).iconResOff);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.INFO);
                mContext.startActivity(intent);
            }
        });

        mHeader.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(mContext, "Reloading Apps…", Toast.LENGTH_SHORT).show();
                loadApps();
                return true;
            }
        });

        //Initialize settings
        settingsWidget = new WidgetSettings(Constants.TAG, mContext);

        Log.d(Constants.TAG, "AmazModLauncher getView packagename: " + mContext.getPackageName()
                + " filesDir: " + mContext.getFilesDir() + " cacheDir: " + mContext.getCacheDir());

        return this.view;
    }

    private void updateCharge() {

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);

        if (batteryStatus != null) {

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryIconId = batteryStatus.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0);

            //Set battery icon and text
            int battery = Math.round((level / (float) scale) * 100f);
            if (battery != 0) {
                String battlvl = Integer.toString(battery) + "%";
                battValueTV.setText(battlvl);
            } else {
                battValueTV.setText("N/A%");
            }

            LevelListDrawable batteryLevel = (LevelListDrawable) mContext.getResources().getDrawable(batteryIconId);
            batteryLevel.setLevel(level);
            battIconImg.setImageDrawable(batteryLevel);
        } else
            Log.e(Constants.TAG, "AmazModLauncher updateCharge error: null batteryStatus!");

    }

    private void refreshView() {
        Log.d(Constants.TAG, "AmazModLauncher refreshView");

        updateCharge();
        checkApps();
        refreshMessages(false);
        refreshIcons();
    }

    private void refreshIcons() {
        boolean state;
        for (int i=0; i<mItems.length; i++){
            try {
                if (i == 0)
                    state = wfmgr.isWifiEnabled();
                else {
                    isWakeLockEnabled = wakeLock.isHeld();
                    state = isWakeLockEnabled;
                }
            } catch (NullPointerException e) {
                state = false;
                Log.e(Constants.TAG, "AmazModLauncher refreshView exception: " + e.toString());
            }
            items.get(i).state = state;
            Log.d(Constants.TAG, "AmazModLauncher refreshView item:" + items.get(i).title + " state: " + items.get(i).state);
        }

        wifiToggle.setImageResource(items.get(0).state ? items.get(0).iconResOn : items.get(0).iconResOff);
        keepAwake.setImageResource(items.get(1).state ? items.get(1).iconResOn : items.get(1).iconResOff);
    }

    private void refreshMessages(boolean reset) {
        String data = Settings.System.getString(mContext.getContentResolver(), "CustomWatchfaceData");
        if (data == null || data.equals("")) {
            Settings.System.putString(mContext.getContentResolver(), "CustomWatchfaceData", "{}");
            notifications = 0;
        }

        try {
            JSONObject json_data = new JSONObject(data);
            if (reset) {
                json_data.put("notifications", 0);
                Settings.System.putString(mContext.getContentResolver(), "CustomWatchfaceData", json_data.toString());
            } else
                notifications = json_data.getInt("notifications");
        } catch (JSONException e) {
            Log.e(Constants.TAG, "AmazModLauncher refreshMessages JSONException: " + e.toString());
            notifications = 0;
        }
        unreadMessages.setText(String.valueOf(notifications));
    }

    private void checkApps() {
        Log.d(Constants.TAG, "AmazModLauncher checkApps");
        //if (settingsWidget.hasKey(WidgetSettings.APP_DELETED) || settingsWidget.hasKey("ADDEDX")) {
            Iterator<String> iter = settingsWidget.getData().keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Object value = settingsWidget.get(key);
                Log.d(Constants.TAG, "AmazModLauncher checkApps key: " + key + " value: " + value);
                if (key.contains("DELETEDX")){
                    final int del = appInfoList.indexOf(value);
                    if (del > 0)
                        Log.d(Constants.TAG, "AmazModLauncher checkApps value: " + value + "appDeleted: "
                                + appInfoList.get(del).getPackageName());
                    settingsWidget.getData().remove("DELETEDX");
                } else if (key.contains("ADDEDX")){
                    loadApps();
                    settingsWidget.getData().remove("ADDEDX");
                }
            }
        //}
    }

    @SuppressLint("CheckResult")
    private void loadApps() {
        Log.i(Constants.TAG,"AmazModLauncher loadApps");

        final Drawable drawable = mContext.getResources().getDrawable(R.drawable.ic_action_select_all);

        Flowable.fromCallable(new Callable<List<AppInfo>>() {
            @Override
            public List<AppInfo> call() throws Exception {
                Log.i(Constants.TAG,"AmazModLauncher loadApps call");
                List<PackageInfo> packageInfoList = mContext.getPackageManager().getInstalledPackages(0);

                List<AppInfo> appInfoList = new ArrayList<>();

                for (PackageInfo packageInfo : packageInfoList) {

                    boolean isSystemApp = (packageInfo.applicationInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0;
                    if  (!isSystemApp && (!packageInfo.packageName.contains("amazmod")) && (!packageInfo.packageName.contains("watchdroid"))
                            && (!packageInfo.packageName.contains("watchface"))) {
                        AppInfo appInfo = createAppInfo(packageInfo);
                        appInfoList.add(appInfo);
                    }
                }

                sortAppInfo(appInfoList);

                AppInfo close = new AppInfo(MANAGE_APPS, "", "", "0", drawable);
                appInfoList.add(close);
                AmazModLauncher.this.appInfoList = appInfoList;
                return appInfoList;
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<List<AppInfo>>() {
                    @Override
                    public void accept(final List<AppInfo> appInfoList) throws Exception {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(Constants.TAG,"AmazModLauncher loadApps run");
                                mAdapter = new LauncherAppAdapter(mContext, appInfoList);
                                listView.setAdapter(mAdapter);
                            }
                        });
                    }
                });

        listView.setLongClickable(true);
        listView.setGreedyTouchMode(true);
        listView.setClickListener(this);
        listView.addOnScrollListener(mOnScrollListener);
    }

    private void sortAppInfo(List<AppInfo> appInfoList) {
        Collections.sort(appInfoList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                return o1.getAppName().compareTo(o2.getAppName());
            }
        });
    }

    private AppInfo createAppInfo(PackageInfo packageInfo) {

        final AppInfo appInfo = new AppInfo();
        Log.i(Constants.TAG,"AmazModLauncher createAppInfo packageName: " + packageInfo.packageName);
        appInfo.setPackageName(packageInfo.packageName);
        appInfo.setAppName(packageInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString());
        appInfo.setVersionName(packageInfo.versionName);
        appInfo.setIcon(packageInfo.applicationInfo.loadIcon(mContext.getPackageManager()));
        return appInfo;
    }

    private void checkConnection() {

        receiverConnection = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                WifiInfo wifiInfo = wfmgr.getConnectionInfo();
                Log.d(Constants.TAG, "AmazModLauncher checkConnection wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Log.d(Constants.TAG, "AmazModLauncher checkConnection wifiInfo.SSID: " + wifiInfo.getSSID());
                Log.d(Constants.TAG, "AmazModLauncher checkConnection action: " + intent.getAction());
                Log.d(Constants.TAG, "AmazModLauncher checkConnection connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
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
                Log.d(Constants.TAG, "AmazModLauncher getSSID wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Log.d(Constants.TAG, "AmazModLauncher getSSID wifiInfo.SSID: " + wifiInfo.getSSID());
                Log.d(Constants.TAG, "AmazModLauncher getSSID action: " + intent.getAction());
                Log.d(Constants.TAG, "AmazModLauncher getSSID connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));

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

    @Override
    public void onTopEmptyRegionClick() {
        //Prevent NullPointerException
        //Toast.makeText(this, "Top empty area tapped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {

        final int itemChosen = viewHolder.getPosition();
        final String name = appInfoList.get(itemChosen).getAppName();
        Log.i(Constants.TAG,"AmazModLauncher onClick itemChosen: " + itemChosen);

        Toast.makeText(mContext, "Opening: " + appInfoList.get(itemChosen).getAppName(), Toast.LENGTH_SHORT).show();

        if (MANAGE_APPS.equals(name)) {
            intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.APPS);
            mContext.startActivity(intent);
        } else {
            Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(appInfoList.get(itemChosen).getPackageName());
            if (launchIntent != null) {
                mContext.startActivity(launchIntent);
            }
        }

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

    private void onShow() {
        // If view loaded (and was inactive)
        if (this.view != null && !this.isActive) {
            // If not the correct view
                // Refresh the view
                this.refreshView();
        }

        // Save state
        this.isActive = true;
    }

    private void onHide() {
        // Save state
        this.isActive = false;
        //Log.d(Constants.TAG, "AmazModPage onHide");
    }

    @Override
    public void onInactive(Bundle paramBundle) {
        super.onInactive(paramBundle);
        this.onHide();
    }
    @Override
    public void onPause() {
        super.onPause();
        this.onHide();
    }
    @Override
    public void onStop() {
        super.onStop();
        this.onHide();
    }

    @Override
    public void onActive(Bundle paramBundle) {
        super.onActive(paramBundle);
        this.onShow();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.onShow();
    }

    /*
     * Below there are commented functions that the widget should have
     */

    // Return the icon for this page, used when the page is disabled in the app list. In this case, the launcher icon is used
    @Override
    public Bitmap getWidgetIcon(Context paramContext) {
        return ((BitmapDrawable) this.mContext.getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap();
    }

    // Return the launcher intent for this page. This might be used for the launcher as well when the page is disabled?
    @Override
    public Intent getWidgetIntent() {
        //Intent localIntent = new Intent();
        //localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        //localIntent.setAction("android.intent.action.MAIN");
        //localIntent.addCategory("android.intent.category.LAUNCHER");
        //localIntent.setComponent(new ComponentName(this.mContext.getPackageName(), "com.huami.watch.deskclock.countdown.CountdownListActivity"));
        return new Intent();
    }


    // Return the title for this page, used when the page is disabled in the app list. In this case, the app name is used
    @Override
    public String getWidgetTitle(Context paramContext) {
        return this.mContext.getResources().getString(R.string.launcher_name);
    }

    // Returns the springboard host
    public ISpringBoardHostStub getHost() {
        return this.host;
    }

    // Called when the page is loading and being bound to the host
    @Override
    public void onBindHost(ISpringBoardHostStub paramISpringBoardHostStub) {
        this.host = paramISpringBoardHostStub;
    }

    // Not sure what this does, can't find it being used anywhere. Best leave it alone
    @Override
    public void onReceiveDataFromProvider(int paramInt, Bundle paramBundle) {
        super.onReceiveDataFromProvider(paramInt, paramBundle);
    }

    // Called when the page is destroyed completely (in app mode). Same as the onDestroy method of an activity
    @Override
    public void onDestroy() {
        if (receiverConnection != null) mContext.unregisterReceiver(receiverConnection);
        if (receiverSSID != null) mContext.unregisterReceiver(receiverSSID);
        super.onDestroy();
    }
}
