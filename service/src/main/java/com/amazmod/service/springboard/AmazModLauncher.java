package com.amazmod.service.springboard;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
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
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.BuildConfig;
import com.amazmod.service.Constants;
import com.amazmod.service.MainService;
import com.amazmod.service.R;
import com.amazmod.service.adapters.LauncherAppAdapter;
import com.amazmod.service.helper.RecyclerTouchListener;
import com.amazmod.service.models.MenuItems;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.support.AppInfo;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.BatteryGraphActivity;
import com.amazmod.service.util.DeviceUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;
import org.tinylog.Logger;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import clc.sliteplugin.flowboard.AbstractPlugin;
import clc.sliteplugin.flowboard.ISpringBoardHostStub;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.content.Context.POWER_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

public class AmazModLauncher extends AbstractPlugin {

    private Context mContext;
    private View view, appmenu, home;
    private boolean isActive = false;
    private ISpringBoardHostStub host = null;

    private WidgetSettings widgetSettings;

    private WearableListView listView;
    private TextView battValueTV, unreadMessages, mHeader;
    private ImageView battIconImg;
    private CircledImageView keepAwake, wifiToggle;

    private List<AppInfo> appInfoList;
    private List<String> appsList;
    private List<MenuItems> items;
    private List<String> hiddenAppsList;
    private LauncherAppAdapter mAdapter;

    private static Intent intent;
    private static boolean isWakeLockEnabled = false;
    private static int notifications;

    private BroadcastReceiver receiverConnection, receiverSSID;
    private WifiManager wfmgr;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLockDim, wakeLockScreenOn;

    private static final String MANAGE_APPS = "App Manager";
    private static final String MANAGE_FILES = "File Manager";
    private static final String MENU_ENTRY = "MENU ENTRY";
    private static final String[] HIDDEN_APPS = {   "amazmod",
                                                    "watchdroid",
                                                    "touchone",
                                                    "watchface"};


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
        mContext.startService(new Intent(paramContext, MainService.class));

        Logger.debug("AmazModLauncher getView: " + mContext.getPackageName());

        this.view = LayoutInflater.from(mContext).inflate(R.layout.amazmod_launcher, null);

        //Initialize settings
        widgetSettings = new WidgetSettings(Constants.TAG, mContext);

        setupLanguage();

        Logger.debug("AmazModLauncher getView getting SystemServices");
        wfmgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
        PowerManager powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLockDim = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AmazeLauncher:KeepAwakeDim");
            wakeLockScreenOn = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "AmazeLauncher:KeepAwakeScreenOn");
        }

        init();
        checkConnection();
        loadApps(false);

        Logger.debug("AmazModLauncher getView packagename: " + mContext.getPackageName()
                + " filesDir: " + mContext.getFilesDir() + " cacheDir: " + mContext.getCacheDir());

        return this.view;
    }

    private void init() {

        Logger.trace("AmazModLauncher init");

        TextView version = view.findViewById(R.id.launcher_version);
        ImageView imageView = view.findViewById(R.id.launcher_logo);
        LinearLayout batteryLayout = view.findViewById(R.id.launcher_battery_layout);
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

        //home = view.findViewById(R.id.launcher_home);
        appmenu = view.findViewById(R.id.launcher_appmenu);

        version.setText(String.valueOf(BuildConfig.VERSION_CODE));
        mHeader.setText(mContext.getResources().getString(R.string.apps));
        flashLight.setImageResource(R.drawable.baseline_highlight_white_24);
        settings.setImageResource(R.drawable.outline_settings_white_24);
        messages.setImageResource(R.drawable.notify_icon_24);

        hiddenAppsList = new ArrayList<>();
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
                Logger.error("AmazModLauncher getView exception: " + e.getMessage());
            }
            Logger.debug("AmazModLauncher getView addItem: " + mItems[i]);
            items.add(new MenuItems(mItems[i], mImagesOn[i], mImagesOff[i], state));
        }

        wifiToggle.setImageResource(items.get(0).getIcon());
        keepAwake.setImageResource(items.get(1).getIcon());

        intent = new Intent(mContext, LauncherWearGridActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        /* Disabled
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.startActivity(launcherIntent);
            }
        });
        */

        appmenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (widgetSettings.get(Constants.PREF_AMAZMOD_KEEP_WIDGET, true)) {
                    Intent appList = new Intent("com.huami.watch.launcher.EXTERNAL_COMMAND.TO_APPLIST");
                    mContext.sendBroadcast(appList);
                } else {
                    final Intent launcherIntent;
                    launcherIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
                    launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(launcherIntent);
                }
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

        unreadMessages.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                NotificationStore.setNotificationCount(mContext, 0);
                refreshMessages();
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
                    items.get(0).setState(false);
                    wfmgr.setWifiEnabled(false);
                } else {
                    items.get(0).setState(true);
                    wfmgr.setWifiEnabled(true);
                }
                wifiToggle.setImageResource(items.get(0).getIcon());
            }
        });

        keepAwake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (wakeLockDim.isHeld() || wakeLockScreenOn.isHeld()) {
                    if (wakeLockDim.isHeld())
                        wakeLockDim.release();
                    if (wakeLockScreenOn.isHeld())
                        wakeLockScreenOn.release();
                    isWakeLockEnabled = false;
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.keep_awake_disabled), Toast.LENGTH_SHORT).show();
                } else {
                    wakeLockDim.acquire(60*60*1000L /*1 hour*/);
                    isWakeLockEnabled = true;
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.keep_awake_dim_enabled), Toast.LENGTH_SHORT).show();
                }
                items.get(1).setState(isWakeLockEnabled);
                keepAwake.setImageResource(items.get(1).getIcon());
            }
        });

        keepAwake.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                if (!wakeLockScreenOn.isHeld()) {
                    wakeLockScreenOn.acquire(60*60*1000L /*1 hour*/);
                    isWakeLockEnabled = true;
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.keep_awake_screen_on_enabled), Toast.LENGTH_SHORT).show();
                    items.get(1).setState(isWakeLockEnabled);
                    keepAwake.setImageResource(items.get(1).getIcon());
                }

                return true;
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
                Toast.makeText(mContext, mContext.getResources().getString(R.string.reloading_apps), Toast.LENGTH_SHORT).show();
                widgetSettings.set(Constants.PREF_HIDDEN_APPS, "[]");
                loadApps(false);
                return true;
            }
        });

        batteryLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent batteryGrapshIntent = new Intent(mContext, BatteryGraphActivity.class);
                batteryGrapshIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                mContext.startActivity(batteryGrapshIntent);
            }
        });

        listView.addOnItemTouchListener(new RecyclerTouchListener(mContext, listView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Logger.debug("AmazModLauncher init addOnItemTouchListener onClick");
                onItemClick(position);
            }

            @Override
            public void onLongClick(View view, int position) {
                Logger.debug("AmazModLauncher init addOnItemTouchListener onLongClick");
                onItemLongClick(position);
            }
        }));
    }

    private void updateCharge() {

        widgetSettings.reload();
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);

        if (batteryStatus != null) {

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryIconId = batteryStatus.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0);

            //Set battery icon and text
            int battery = Math.round((level / (float) scale) * 100f);
            String msg;
            if (battery != 0) {
                String battlvl = battery + "%";
                msg = battlvl;
                widgetSettings.set(Constants.PREF_BATT_LEVEL, battlvl);
            } else {
                msg = "N/A%";
            }

            LevelListDrawable batteryLevel = (LevelListDrawable) mContext.getResources().getDrawable(batteryIconId);
            batteryLevel.setLevel(level);

            getHost().runTaskOnUI(AmazModLauncher.this, new Runnable() {
                @Override
                public void run() {
                    if (battValueTV != null)
                        battValueTV.setText(msg);
                    if (battIconImg != null)
                        battIconImg.setImageDrawable(batteryLevel);
                }
            });

        } else
            Logger.error("AmazModLauncher updateCharge error: null batteryStatus!");

    }

    private void refreshView() {
        Logger.trace("AmazModLauncher refreshView");

        updateCharge();
        checkApps();
        refreshMessages();
        refreshIcons();
    }

    private void refreshIcons() {
        boolean state;
        for (int i=0; i<mItems.length; i++){
            try {
                if (i == 0)
                    state = wfmgr.isWifiEnabled();
                else {
                    isWakeLockEnabled = wakeLockDim.isHeld() || wakeLockScreenOn.isHeld();
                    state = isWakeLockEnabled;
                }
            } catch (NullPointerException e) {
                state = false;
                Logger.debug("AmazModLauncher refreshView exception: " + e.getMessage());
            }
            items.get(i).setState(state);
            Logger.debug("AmazModLauncher refreshView item:" + items.get(i).getTitle() + " state: " + items.get(i).getState());
        }

        wifiToggle.setImageResource(items.get(0).getIcon());
        keepAwake.setImageResource(items.get(1).getIcon());
    }

    private void refreshMessages() {
        notifications = DeviceUtil.notificationCounter(mContext);
        getHost().runTaskOnUI(AmazModLauncher.this, new Runnable() {
            @Override
            public void run() {
                if (unreadMessages != null)
                    unreadMessages.setText(String.valueOf(notifications));
            }
        });
    }

    private void checkApps() {
        Logger.trace("AmazModLauncher checkApps");
        if (widgetSettings.reload()) {
            if (widgetSettings.hasKey(Constants.DELETED_APP) || widgetSettings.hasKey(Constants.ADDED_APP)) {
                Iterator<String> iterator = widgetSettings.getData().keys();
                boolean forceLoadApps = false;
                boolean updateHiddenApps = false;
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Object value = widgetSettings.get(key);
                    if (Constants.DELETED_APP.equals(key) || Constants.ADDED_APP.equals(key)) {
                        Logger.debug("AmazModLauncher checkApps key: " + key + " value: " + value);
                        if (Constants.DELETED_APP.equals(key))
                            updateHiddenApps = true;
                        forceLoadApps = true;
                        widgetSettings.getData().remove(key);
                        widgetSettings.save();
                    }
                }
                if (forceLoadApps)
                    loadApps(updateHiddenApps);
            }
        }
    }

    @SuppressLint("CheckResult")
    private void loadApps(boolean updateHiddenApps) {
        Logger.trace("AmazModLauncher loadApps");

        loadHiddenApps(updateHiddenApps);
        final Drawable appsDrawable = mContext.getResources().getDrawable(R.drawable.ic_action_select_all);
        final Drawable filesDrawable = mContext.getResources().getDrawable(R.drawable.outline_folder_white_24);

        Flowable.fromCallable(new Callable<List<AppInfo>>() {
            @Override
            public List<AppInfo> call() {
                Logger.info("AmazModLauncher loadApps call");
                List<PackageInfo> packageInfoList = mContext.getPackageManager().getInstalledPackages(0);

                List<AppInfo> appInfoList = new ArrayList<>();

                for (PackageInfo packageInfo : packageInfoList) {

                    boolean isSystemApp = (packageInfo.applicationInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) > 0;
                    if  (!isSystemApp && (!isHiddenApp(packageInfo.packageName))) {
                        AppInfo appInfo = createAppInfo(packageInfo);
                        appInfoList.add(appInfo);
                    }
                }

                sortAppInfo(appInfoList);
                AppInfo appInfo = new AppInfo(MANAGE_FILES, "", MENU_ENTRY, "0", filesDrawable);
                appInfoList.add(appInfo);
                appInfo = new AppInfo(MANAGE_APPS, "", MENU_ENTRY, "0", appsDrawable);
                appInfoList.add(appInfo);
                AmazModLauncher.this.appInfoList = appInfoList;
                return appInfoList;
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<List<AppInfo>>() {
                    @Override
                    public void accept(final List<AppInfo> appInfoList) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Logger.info("AmazModLauncher loadApps run");
                                mAdapter = new LauncherAppAdapter(mContext, appInfoList);
                                listView.setAdapter(mAdapter);
                            }
                        });
                    }
                }, throwable -> {
                    Logger.error("AmazModLauncher: Flowable: subscribeOn: " + throwable.getMessage());
                });

        listView.setLongClickable(true);
        listView.setGreedyTouchMode(true);
        listView.addOnScrollListener(mOnScrollListener);
    }

    private boolean isHiddenApp(String packageName) {

        boolean result = false;
        if (StringUtils.indexOfAny(packageName, HIDDEN_APPS) >= 0)
            result = true;
        else {
            if (hiddenAppsList.contains(packageName))
                result = true;
        }

        Logger.info("AmazModLauncher isHiddenApp packageName: " + packageName + " \\ result: " + result);
        return result;
    }

    private void sortAppInfo(List<AppInfo> appInfoList) {
        Collections.sort(appInfoList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                return o1.getAppName().compareToIgnoreCase(o2.getAppName());
            }
        });
    }

    private AppInfo createAppInfo(PackageInfo packageInfo) {

        final AppInfo appInfo = new AppInfo();
        Logger.info("AmazModLauncher createAppInfo packageName: " + packageInfo.packageName);
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
                Logger.debug("AmazModLauncher checkConnection wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Logger.debug("AmazModLauncher checkConnection wifiInfo.SSID: " + wifiInfo.getSSID());
                Logger.debug("AmazModLauncher checkConnection action: " + intent.getAction());
                Logger.debug("AmazModLauncher checkConnection connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
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
                Logger.debug("AmazModLauncher getSSID wifiInfo.getSupplicantState: " + wifiInfo.getSupplicantState());
                Logger.debug("AmazModLauncher getSSID wifiInfo.SSID: " + wifiInfo.getSSID());
                Logger.debug("AmazModLauncher getSSID action: " + intent.getAction());
                Logger.debug("AmazModLauncher getSSID connected: " + intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));

                if (wifiInfo.getSupplicantState().equals(SupplicantState.ASSOCIATING))
                    flag = true;

                if (wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED) && flag) {
                    flag = false;
                    vibrator.vibrate(100);
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.wifi_connected_to)+ "\n" + wifiInfo.getSSID(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mContext.registerReceiver(receiverSSID, intentFilter);
    }

    private void onItemClick(final int itemChosen) {

        String name = appInfoList.get(itemChosen).getAppName();
        final String version = appInfoList.get(itemChosen).getVersionName();
        //Logger.debug("AmazModLauncher onClick itemChosen: " + itemChosen);
        if (name.equals("File Manager")){
            name = mContext.getResources().getString(R.string.file_manager);
        }
        if (name.equals("App Manager")){
            name = mContext.getResources().getString(R.string.app_manager);
        }
        Toast.makeText(mContext, mContext.getResources().getString(R.string.opening)+" "+ name, Toast.LENGTH_SHORT).show();

        if (MANAGE_APPS.equals(appInfoList.get(itemChosen).getAppName()) && MENU_ENTRY.equals(version)) {

            intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.APPS);
            mContext.startActivity(intent);

        } else if (MANAGE_FILES.equals(appInfoList.get(itemChosen).getAppName()) && MENU_ENTRY.equals(version)) {

            intent.putExtra(LauncherWearGridActivity.MODE, LauncherWearGridActivity.FILES);
            mContext.startActivity(intent);

        } else {

            Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(appInfoList.get(itemChosen).getPackageName());
            if (launchIntent != null)
                mContext.startActivity(launchIntent);

        }

    }

    private void onItemLongClick(final int itemChosen) {

        Logger.debug("AmazModLauncher onItemLongClick itemChosen: " + itemChosen);

        String packageName = appInfoList.get(itemChosen).getPackageName();
        String name = appInfoList.get(itemChosen).getAppName();
        String version = appInfoList.get(itemChosen).getVersionName();

        if (!(MANAGE_APPS.equals(name) && MENU_ENTRY.equals(version))
                && !(MANAGE_FILES.equals(name) && MENU_ENTRY.equals(version))) {
            hiddenAppsList.add(packageName);
            Collections.sort(hiddenAppsList);
            String pref = new Gson().toJson(hiddenAppsList);
            widgetSettings.set(Constants.PREF_HIDDEN_APPS, pref);
            Toast.makeText(mContext, appInfoList.get(itemChosen).getAppName() +" "+ mContext.getResources().getString(R.string.hidden), Toast.LENGTH_SHORT).show();
            //refreshList();
            appInfoList.remove(itemChosen);
            mAdapter.notifyDataSetChanged();
        }

    }

    private void loadHiddenApps(boolean update) {
        if (widgetSettings.reload()) {
            String hiddenAppsJson = widgetSettings.get(Constants.PREF_HIDDEN_APPS, "[]");
            boolean save = false;
            try {
                Type listType = new TypeToken<List<String>>() {
                }.getType();
                hiddenAppsList = new Gson().fromJson(hiddenAppsJson, listType);
                if (hiddenAppsList.isEmpty())
                        return;
                if (update && (!appInfoList.isEmpty())) {
                    for (Object value : hiddenAppsList) {
                        final int del = appInfoList.indexOf(value);
                        if (del < 0) {
                            Logger.debug("AmazModLauncher loadHiddenApps remove: " + value);
                            hiddenAppsList.remove(value);
                            save = true;
                        }
                    }
                    if (save) {
                        String pref = new Gson().toJson(hiddenAppsList);
                        widgetSettings.set(Constants.PREF_HIDDEN_APPS, pref);
                        widgetSettings.save();
                    }
                }
            } catch (Exception ex) {
                Logger.error("AmazModLauncher loadHiddenApps exception: " + ex.getMessage());
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

    /*
     * Below there are standard widget methods
     */

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
        //Logger.debug("AmazModLauncher onHide");
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

    private void setupLanguage() {
        // Load settings
        SettingsManager settingsManager = new SettingsManager(mContext);
        // Get phone app language
        String language = DeviceUtil.systemGetString(mContext, "AmazModLocale");
        Logger.debug("Amazmod locale app language: "+language);

        if (language == null)
            return;

        if (language.contains("iw")) {
            if (!new File("/system/fonts/NotoSansHebrew-Regular.ttf").exists()) {
                language = "en_EN";
                Logger.debug("Amazmod locale: Hebrew font is missing, setting english language");
            } else
                Logger.debug("Amazmod locale: Hebrew font exist, setting language as normal");
        }else if (language.contains("ar")) {
            if (!new File("/system/fonts/NotoSansArabic-Regular.ttf").exists()) {
                language = "en_EN";
                Logger.debug("Amazmod locale: Arabic font is missing, setting english language");
            } else
                Logger.debug("Amazmod locale: Arabic font exist, setting language as normal");
        }
        Resources res = mContext.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        android.content.res.Configuration conf = res.getConfiguration();
        conf.locale = getLocaleByLanguageCode(language);
        res.updateConfiguration(conf, dm);

        Logger.debug("Amazmod locale set:"+getLocaleByLanguageCode(language));
    }

    private static Locale getLocaleByLanguageCode(String languageCode) {
        String[] languageCodes = languageCode.split("_");
        if (languageCodes.length > 1) {
            return new Locale(languageCodes[0], languageCodes[1]);
        } else {
            return new Locale(languageCode, languageCode.toUpperCase());
        }
    }
}
