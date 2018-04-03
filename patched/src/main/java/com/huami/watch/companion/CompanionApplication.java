package com.huami.watch.companion;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.activeandroid.ActiveAndroid;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.edotasx.amazfit.*;
import com.edotasx.amazfit.boot.Boot;
import com.edotasx.amazfit.preference.PreferenceManager;
import com.huami.passport.AccountManager;
import com.huami.watch.companion.IME.IMEservice;
import com.huami.watch.companion.agps.AGpsSyncHelper;
import com.huami.watch.companion.agps.AGpsSyncService;
import com.huami.watch.companion.bind.BindUtil;
import com.huami.watch.companion.ble.lostwarning.BleLostWarningManager;
import com.huami.watch.companion.cloud.httpsupport.WearHttpSupportInterface;
import com.huami.watch.companion.components.bluetoothproxyserver.HttpProxyServer;
import com.huami.watch.companion.config.Config;
import com.huami.watch.companion.datacollection.DataCollection;
import com.huami.watch.companion.device.Device;
import com.huami.watch.companion.device.DeviceManager;
import com.huami.watch.companion.device.DeviceUtil;
import com.huami.watch.companion.findphone.AskAndAnswer;
import com.huami.watch.companion.mediac.CommandHandler;
import com.huami.watch.companion.notification.NotificationManager;
import com.huami.watch.companion.otaphone.service.OtaService;
import com.huami.watch.companion.sync.SyncDeviceInfoHelper;
import com.huami.watch.companion.sync.SyncWatchFaceBgHelper;
import com.huami.watch.companion.sync.SyncWatchHealthHelper;
import com.huami.watch.companion.sync.SyncWatchSportHelper;
import com.huami.watch.companion.sync.throttle.SyncThrottler;
import com.huami.watch.companion.unlock.MiuiAPI;
import com.huami.watch.companion.util.Analytics;
import com.huami.watch.companion.util.AppEnterForegroundCallBack;
import com.huami.watch.companion.util.AppUtil;
import com.huami.watch.companion.util.Box;
import com.huami.watch.companion.util.DeviceCompatibility;
import com.huami.watch.companion.util.RxBus;
import com.huami.watch.companion.util.StorageUtil;
import com.huami.watch.companion.wearcalling.CallingWearHelper;
import com.huami.watch.companion.weather.WeatherService;
import com.huami.watch.companion.wififtp.WatchWifiFtpUtil;
import com.huami.watch.connect.PhoneConnectionApplication;
import com.huami.watch.hmwatchmanager.bt_connect.BGService_msg;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.httpsupport.global.ConnectionReceiver;
import com.huami.watch.util.Log;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.connection.FileDownloadUrlConnection;
import com.liulishuo.filedownloader.services.DownloadMgrInitialParams;

import java.net.Proxy;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;
import lanchon.dexpatcher.annotation.DexAction;
import lanchon.dexpatcher.annotation.DexEdit;
import lanchon.dexpatcher.annotation.DexIgnore;
import lanchon.dexpatcher.annotation.DexReplace;
import lanchon.dexpatcher.annotation.DexWrap;

/**
 * Created by edoardotassinari on 12/02/18.
 */

@DexEdit(defaultAction = DexAction.IGNORE)
public class CompanionApplication extends PhoneConnectionApplication {

    @DexIgnore
    private static Context a;
    @DexIgnore
    private Transporter j;
    @DexIgnore
    private Transporter o;
    @DexIgnore
    private AskAndAnswer f;
    @DexIgnore
    private CallingWearHelper g;
    @DexIgnore
    private DataCollection h;
    @DexIgnore
    private HttpProxyServer e;
    @DexIgnore
    private Transporter.ChannelListener k;
    @DexIgnore
    private Transporter.DataListener l;
    @DexIgnore
    private Transporter.DataListener m;
    @DexIgnore
    private Transporter.DataListener n;
    @DexIgnore
    private Transporter.DataListener p;
    @DexIgnore
    private BroadcastReceiver q;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint({"MissingSuperCall", "ResourceType"})
    @DexWrap
    public void onCreate() {
        Boolean enableRtl = PreferenceManager.getBoolean(this, Constants.PREFERENCE_ENABLE_RTL, false);
        Configuration configuration = getResources().getConfiguration();
        configuration.setLayoutDirection(new Locale(enableRtl ? "fa" : "en"));
        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());

        Boolean disableCrashReporting = PreferenceManager.getBoolean(this, Constants.PREFERENCE_DISABLE_CRASH_REPORTING, false);
        CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder().disabled(!disableCrashReporting.booleanValue()).build();
        Fabric.with(this, new Crashlytics.Builder().core(crashlyticsCore).build());

        a = this;
        if (!Config.isOversea() && !b()) {
            return;
        }

        Box.initDefault(this);
        a();
        Log.Settings settings = Log.init();
        settings.setLogLevel(Config.isDebug() ? Log.LogLevel.FULL : Log.LogLevel.FILE_ONLY).setLogFile(StorageUtil.getLogFile(this));
        Log.d("CompanionApp", "OnAppCreate : " + AppUtil.getVersionNameAndCode(this), new Object[0]);
        super.onCreate();

        /*
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e("CompanionApp", "Crash(" + thread + ")", throwable, new Object[0]);
                System.exit(1);
            }
        });
        */

        Analytics.config(true, Config.isDebug(), true, this, AppUtil.getMetaDataChannel(this));
        AccountManager.getDefault(this).setTestMode(Config.isTestHosts());
        //AccountManager.getDefault(this).setGlobalMode(true);
        ActiveAndroid.initialize(this, Config.isDebug());
        NotificationManager.getManager(this).init();
        if (DeviceCompatibility.MIUI.isMIUI(this)) {
            MiuiAPI.getInstance(this).init();
        }
        SyncThrottler.init(this);
        BindUtil.unbindUnfinishedDevice(this);
        j = Transporter.get(this, "com.huami.watch.companion");
        j.addChannelListener(k);
        j.addDataListener(l);
        j.connectTransportService();
        Transporter object = Transporter.get(this, "com.huami.watch.companion.syncdata");
        object.addDataListener(m);
        object.connectTransportService();
        object = Transporter.get(this, "com.huami.action.notification");
        object.addDataListener(n);
        object.connectTransportService();
        o = Transporter.get(this, "com.huami.watch.health");
        o.addDataListener(p);
        o.connectTransportService();
        if (f == null) {
            f = new AskAndAnswer(this);
        }
        if (g == null) {
            g = new CallingWearHelper(this);
        }
        if (h == null) {
            h = new DataCollection(this);
        }

        startService(new Intent(this, BGService_msg.class));
        startService(new Intent(this, OtaService.class));
        startService(new Intent(this, IMEservice.class));

        SyncWatchFaceBgHelper.getHelper().startFileTransporter(this);
        AGpsSyncHelper.getHelper().startFileTransporter(this);
        if (DeviceManager.getManager(this).hasBoundDevice()) {
            AGpsSyncService.scheduleSync(this, "AppOnCreate");
            startService(new Intent(this, WeatherService.class));
        }
        a(this);
        SyncWatchHealthHelper.getHelper().init(this);
        SyncWatchSportHelper.getHelper().init(this);
        WearHttpSupportInterface.doInit(this);

/* Ok */

        ConnectionReceiver connectionReceiver = new ConnectionReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("com.huami.watch.WATCH_CONNED_4_COMPANION");
        intentFilter.addAction("com.huami.watch.companion.action.UnbindDeviceStart");
        intentFilter.addAction("com.huami.watch.httpsupport.COLLECT_DATA");
        intentFilter.addAction("com.huami.watch.companion.action.HOST_START_SYNC_DATA");
        registerReceiver(connectionReceiver, intentFilter);

        TimeChangedReceiver timeChangedReceiver = new CompanionApplication.TimeChangedReceiver();

        IntentFilter timeSet = new IntentFilter("android.intent.action.TIME_SET");
        timeSet.addAction("android.intent.action.TIMEZONE_CHANGED");
        registerReceiver(timeChangedReceiver, timeSet);

        IntentFilter wifiStateChangeConnectivityChange = new IntentFilter();
        wifiStateChangeConnectivityChange.addAction("android.net.wifi.STATE_CHANGE");
        wifiStateChangeConnectivityChange.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(q, wifiStateChangeConnectivityChange);

        new RxConsumerStarter().init(this);

        BindUtil.connectCurrentDevice(this);

        Device device = DeviceManager.getManager(this).getCurrentDevice();

        if (device != null && DeviceUtil.hasEmptyInfo(device)) {
            SyncDeviceInfoHelper.getHelper(this).startAsync();
        }
        PlayFlavor.initApp(this);
        e = new HttpProxyServer(getApplicationContext());
        e.init();
        if (getResources().getBoolean(2131427330)) {
            CommandHandler.getInstance(this);
        }
        WatchWifiFtpUtil.init(getApplicationContext());
        registerActivityLifecycleCallbacks(new AppEnterForegroundCallBack());
        BleLostWarningManager.init(getApplicationContext());
        if (DeviceCompatibility.isHuaWeiPhone() && !DeviceCompatibility.isO()) {
            //DummyJobService.startSchedule(this);
        }
        FileDownloader.init(getApplicationContext(), new DownloadMgrInitialParams.InitCustomMaker().connectionCreator(new FileDownloadUrlConnection.Creator(new FileDownloadUrlConnection.Configuration().connectTimeout(15000).readTimeout(15000).proxy(Proxy.NO_PROXY))));

        Boot.sharedInstance(this).run();
    }

    @DexIgnore
    public static Context getContext() {
        return null;
    }

    @DexIgnore
    private void a() {
    }

    @DexIgnore
    private void a(final Context context) {
    }

    @DexIgnore
    private boolean b() {
        return false;
    }

    @DexIgnore
    public class TimeChangedReceiver extends BroadcastReceiver {

        @DexIgnore
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    @DexIgnore
    private void a(Context context, String string2) {
    }

    @DexReplace
    public void a(Context context, String string2, String string3, boolean bl2) {
        Log.i("CompanionApp", "OnDeviceUnbound : " + string2 + ", " + string3 + ", IsActiveDevice : " + bl2, new Object[0]);
        if (bl2) {
            this.a(context, string2);
        }
    }
}
