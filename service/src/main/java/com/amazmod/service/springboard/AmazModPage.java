package com.amazmod.service.springboard;

import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
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
import com.amazmod.service.util.SystemProperties;
//import com.edotassi.amazmodcompanionservice.MessagesListener;
//import com.edotassi.amazmodcompanionservice.R2;

//import butterknife.BindView;
//import butterknife.ButterKnife;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import clc.sliteplugin.flowboard.AbstractPlugin;
import clc.sliteplugin.flowboard.ISpringBoardHostStub;

public class AmazModPage extends AbstractPlugin {

    private static final String TAG = "AmazModPage";
    private Context mContext;
    private View view;
    private boolean isActive = false;
    private boolean lowPower = true;
    private ISpringBoardHostStub host = null;

    private WidgetSettings settingsWidget;

    //    @BindView(R2.id.amazmod_page_version)
    private TextView version, timeSLC, battValueTV;
    private ImageView battIconImg, imageView;

    @Override
    public View getView(final Context paramContext) {

        this.mContext = paramContext;
        paramContext.startService(new Intent(paramContext, MainService.class));

        this.view = LayoutInflater.from(paramContext).inflate(R.layout.amazmod_page, null);

        version = view.findViewById(R.id.amazmod_page_version);
        timeSLC = view.findViewById(R.id.time_since_last_charge);
        battValueTV = view.findViewById(R.id.battValue);
        battIconImg = view.findViewById(R.id.battIcon);
        imageView = view.findViewById(R.id.imageView);

/*       try {
            ButterKnife.bind(this, view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
*/
        version.setText(BuildConfig.VERSION_NAME);

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (SystemProperties.setSystemProperty("sys.state.powerlow", lowPower ? "true" : "false") != null) {
                    Log.d(Constants.TAG, "AmazModPage getView longClick sys.state.powerlow: "
                            + SystemProperties.getSystemProperty("sys.state.powerlow"));
                }
                BluetoothAdapter btmgr = BluetoothAdapter.getDefaultAdapter();
                final String result = SystemProperties.getSystemProperty("sys.state.powerlow");
                if (result != null) {
                    if (result.equals("true")){
                        Log.i(Constants.TAG, "AmazModPage getView disable BT");
                        btmgr.disable();
                        try {
                            WifiManager wfmgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                            if (wfmgr.isWifiEnabled()) {
                                Log.i(Constants.TAG, "AmazModPage getView disable WiFi");
                                wfmgr.setWifiEnabled(false);
                            }
                        } catch (NullPointerException e) {
                            Log.e(Constants.TAG, "AmazModPage getView longClick exception: " + e.toString());
                        }
                        SystemProperties.goToSleep(mContext);
                        lowPower = false;
                    } else {
                        btmgr.enable();
                        lowPower = true;
                    }
                }
                Toast.makeText(paramContext, "lowPower: " + !lowPower, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        //Initialize settings
        settingsWidget = new WidgetSettings(Constants.TAG, mContext);
        Log.d(Constants.TAG, "AmazModPage getView");

        return this.view;
    }

    private void updateTimeSinceLastCharge() {

        //Refresh saved data
        settingsWidget.reload();

        //Intent batteryStatus = mContext.registerReceiver(null, ifilter);
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryIconId = batteryStatus.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0);

        //Set battery icon and text
        int battery = Math.round((level / (float)scale) * 100f);
        if (battery != 0) {
            String battlvl = Integer.toString(battery) + "%";
            battValueTV.setText(battlvl);
        } else {
            battValueTV.setText("N/A%");
        }

        LevelListDrawable batteryLevel = (LevelListDrawable) mContext.getResources().getDrawable(batteryIconId);
        batteryLevel.setLevel(level);
        battIconImg.setImageDrawable(batteryLevel);


        //Get date of last full charge
        long lastChargeDate = settingsWidget.get(Constants.PREF_DATE_LAST_CHARGE, 0L);

        StringBuilder dateDiff = new StringBuilder("  ");

        Log.d(Constants.TAG, "AmazModPage updateTimeSinceLastCharge level: " + level
                + " / scale: " + scale + " / batteryIconId: " + batteryIconId + " /" + dateDiff + lastChargeDate);

        //Log.d(Constants.TAG, "AmazModWidget updateTimeSinceLastChargeDate data: " + battery + " / " + lastChargeDate );
        if (lastChargeDate != 0L) {
            long diffInMillies = System.currentTimeMillis() - lastChargeDate;
            List<TimeUnit> units = new ArrayList<>(EnumSet.allOf(TimeUnit.class));
            Collections.reverse(units);
            long milliesRest = diffInMillies;
            for (TimeUnit unit : units) {
                long diff = unit.convert(milliesRest, TimeUnit.MILLISECONDS);
                long diffInMilliesForUnit = unit.toMillis(diff);
                milliesRest = milliesRest - diffInMilliesForUnit;
                if (unit.equals(TimeUnit.DAYS)) {
                    dateDiff.append(diff).append("d : ");
                } else if (unit.equals(TimeUnit.HOURS)) {
                    dateDiff.append(diff).append("h : ");
                } else if (unit.equals(TimeUnit.MINUTES)) {
                    dateDiff.append(diff).append("m");
                    break;
                }
            }
            dateDiff.append("\n").append(mContext.getResources().getText(R.string.last_charge));
        } else dateDiff.append(mContext.getResources().getText(R.string.last_charge_no_info));

        timeSLC.setText(dateDiff.toString());
    }

    private void refreshView() {
        //Called when the page reloads, check for updates here if you need to
        updateTimeSinceLastCharge();
        //Log.d(Constants.TAG, "AmazModPage refreshView ");
    }

    /*
     * Widget active/deactivate state management
     */

    // On widget show
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

    // On widget hide
    private void onHide() {
        // Save state
        this.isActive = false;
        //Log.d(Constants.TAG, "AmazModPage onHide");
    }

    // Events for widget hide
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

    // Events for widget show
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
     * Below where are unchanged functions that the widget should have
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
        return this.mContext.getResources().getString(R.string.app_name);
    }

    // Returns the springboard host
    public ISpringBoardHostStub getHost() {
        return this.host;
    }

    // Called when the page is loading and being bound to the host
    @Override
    public void onBindHost(ISpringBoardHostStub paramISpringBoardHostStub) {
        // Log.d(widget.TAG, "onBindHost");
        //Store host
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
        //Log.d(Constants.TAG, "AmazModPage onDestroy");
        super.onDestroy();
    }

}
