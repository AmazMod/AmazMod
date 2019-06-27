package com.amazmod.service.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.view.BoxInsetLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.springboard.WidgetSettings;
import com.amazmod.service.util.SystemProperties;

import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;

public class WearInfoFragment extends Fragment {

    BoxInsetLayout mainLayout;
	private Button buttonClose;
    private TextView build, timeSLCTV, upTime, sleepTime, memory, currentIP;

    private Context mContext;

    private static String timeSLC;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.info("WearInfoFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //notificationSpec = NotificationData.fromBundle(getArguments());
        Logger.info("WearInfoFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.info("WearInfoFragment onCreateView");

        return inflater.inflate(R.layout.fragment_wear_info, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.info("WearInfoFragment onViewCreated");

        updateContent();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

	@SuppressLint("ClickableViewAccessibility")
	private void updateContent() {

        final WidgetSettings widgetSettings = new WidgetSettings(Constants.TAG, mContext);
        //Refresh saved data
        widgetSettings.reload();

        //Get date of last full charge
        long lastChargeDate = widgetSettings.get(Constants.PREF_DATE_LAST_CHARGE, 0L);

        StringBuilder dateDiff = new StringBuilder("  ");

        //Log.d(Constants.TAG, "AmazModWidget updateTimeSinceLastChargeDate data: " + battery + " / " + lastChargeDate );
        if (lastChargeDate != 0L) {
            long diffInMillies = System.currentTimeMillis() - lastChargeDate;
            List<TimeUnit> units = new ArrayList<>(EnumSet.allOf(TimeUnit.class));
            Collections.reverse(units);
            long millisRest = diffInMillies;
            for (TimeUnit unit : units) {
                long diff = unit.convert(millisRest, TimeUnit.MILLISECONDS);
                long diffInMilliesForUnit = unit.toMillis(diff);
                millisRest = millisRest - diffInMilliesForUnit;
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

        timeSLC = dateDiff.toString();

        build = getActivity().findViewById(R.id.wear_info_build);
        timeSLCTV = getActivity().findViewById(R.id.wear_info_timeSLC);
        upTime = getActivity().findViewById(R.id.wear_info_textView02);
        sleepTime = getActivity().findViewById(R.id.wear_info_textView03);
        memory = getActivity().findViewById(R.id.wear_info_textView04);
        currentIP = getActivity().findViewById(R.id.wear_info_textView05);
        buttonClose = getActivity().findViewById(R.id.wear_info_buttonClose);

        mainLayout = getActivity().findViewById(R.id.wear_info_main_layout);
        mainLayout.setBackgroundColor(getResources().getColor(android.R.color.black));

        showInfo();

	}

    @Override
    public void onDestroy() {
	    super.onDestroy();
    }

    @SuppressLint("SetTextI18n")
    public void showInfo() {

        setButtonTheme(buttonClose);
        timeSLCTV.setText(timeSLC);
        build.setText(SystemProperties.getSystemProperty("ro.build.display.id"));
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(ACTIVITY_SERVICE);
        if (activityManager != null) {
            try {
                activityManager.getMemoryInfo(memoryInfo);
                double freeRAM = memoryInfo.availMem / 0x100000L;
                long elapsedRealtime = SystemClock.elapsedRealtime() ;
                long sleepRealtime = SystemClock.elapsedRealtime() - SystemClock.uptimeMillis();
                String ip = getIP();

                upTime.setText("Uptime: " + formatInterval(elapsedRealtime, false));
                sleepTime.setText("SleepTime: " + formatInterval(sleepRealtime, false));
                memory.setText("Free RAM: " + freeRAM + "MB");
                if ((ip != null) && (!ip.equals("0.0.0.0")))
                    currentIP.setText("IP: " + ip);
                else
                    currentIP.setText("IP: (not connected)");
            } catch (Exception ex) {
                Logger.error("WearInfoFragment showInfo exception: " + ex.toString());
            }
        }
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }

    private void setButtonTheme(Button button) {
        button.setIncludeFontPadding(false);
        button.setMinHeight(24);
        button.setMinWidth(120);
        button.setText(getResources().getString(R.string.close));
        button.setAllCaps(true);
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

    /**
     * Get the IP of current Wi-Fi connection
     * @return IP as string
     */
    private String getIP() {
        try {
            WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                    (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        } catch (Exception ex) {
            Logger.error("WearInfoFrament getIP exception: " + ex.toString());
            return null;
        }
    }

    public static WearInfoFragment newInstance() {
        Logger.info("WearInfoFragment newInstance");
        return new WearInfoFragment();
    }
}