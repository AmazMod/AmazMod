package com.amazmod.service.springboard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.MainService;
import com.amazmod.service.R;
import com.amazmod.service.R2;
import com.amazmod.service.events.NightscoutDataEvent;
import com.amazmod.service.events.NightscoutRequestSyncEvent;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.mikepenz.iconics.Iconics;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import clc.sliteplugin.flowboard.AbstractPlugin;
import clc.sliteplugin.flowboard.ISpringBoardHostStub;
import xiaofei.library.hermeseventbus.HermesEventBus;

/**
 * Created by edoardotassinari on 09/04/18.
 */

public class NightscoutPage extends AbstractPlugin {

    private Context mContext;
    private View mView;
    private boolean mHasActive = false;
    private ISpringBoardHostStub mHost = null;

    private boolean eventBusConnected;
    private long lastDate;
    private String lastDirection;
    private String trendArrow;
    private String lastSgv;
    private float lastDelta;



    //private Map<String, String> directionsIcons = new HashMap<String, String>() {{
      //  put("DoubleUp", "{gmd_arrow_upward}{gmd_arrow_upward}");
        //put("SingleUp", "{gmd_arrow_upward}");
        //put("FortyFiveUp", "{gmd_trending_up}");
        //put("Flat", "{gmd_trending_flat}");
        //put("FortyFiveDown", "{gmd_trending_down}");
        //put("SingleDown", "{gmd_arrow_downward}");
        //put("DoubleDown", "{gmd_arrow_downward}{gmd_arrow_downward}");
    //}};

    @BindView(R2.id.nightscout_sgv_textview)
    TextView sgv;
    @BindView(R2.id.nightscout_date_textview)
    TextView date;
    @BindView(R2.id.nightscout_delta_text_view)
    TextView delta;

   // @BindView(R2.id.nightscout_direction_textview)
    //IconicsTextView direction;

    @Override
    public View getView(Context paramContext) {
        Intent intent = new Intent(paramContext, MainService.class);
        paramContext.startService(intent);

        mContext = paramContext;

        initIcons(paramContext);

        Log.d(Constants.TAG_NIGHTSCOUT_PAGE, "getView()" + paramContext.getPackageName());

        mView = LayoutInflater.from(paramContext).inflate(R.layout.nightscoout_page, null);

        try {
            ButterKnife.bind(this, mView);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return mView;
    }

    @OnClick(R2.id.nightscout_refresh_button)
    public void requestSync() {
        HermesEventBus.getDefault().post(new NightscoutRequestSyncEvent());
        Toast.makeText(mContext, "Request sent", Toast.LENGTH_LONG).show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateData(NightscoutDataEvent nightscoutDataEvent) {
        Log.d(Constants.TAG_NIGHTSCOUT_PAGE, "NightscoutDataEvent received");

// getting the data from Hermes

        lastDate = nightscoutDataEvent.getDate();
        lastSgv = String.valueOf(nightscoutDataEvent.getSgv());
        lastDirection = String.valueOf(nightscoutDataEvent.getDirection());
        lastDelta = nightscoutDataEvent.getDelta();

        if (sgv != null) {

            trendArrow= "";
            if (lastDirection.equals("DoubleUp")) {
                trendArrow= " ⇈";
            } else if (lastDirection.equals("SingleUp")) {
                trendArrow= " ↑";
            } else if (lastDirection.equals("FortyFiveUp")) {
                trendArrow= " ↗";
            } else if (lastDirection.equals("Flat")) {
                trendArrow= " →";
            } else if (lastDirection.equals("FortyFiveDown")) {
                trendArrow= " ↘";
            } else if (lastDirection.equals("SingleDown")) {
                trendArrow= " ↓";
            } else if (lastDirection.equals("DoubleDown")) {
                trendArrow= " ⇊";
            }

            sgv.setText(lastSgv+trendArrow);

            sgv.setTextColor(Color.WHITE);
            if (Integer.valueOf(lastSgv) < 80) {sgv.setTextColor(Color.RED);}
            if (Integer.valueOf(lastSgv) > 180) {sgv.setTextColor(Color.RED);}
        }

        if (delta != null) {
            if (lastDelta > 0) {
                delta.setText("+" + String.valueOf(String.format(Locale.ENGLISH,"%.1f", lastDelta)) + " mg/dl");
            } else {
                delta.setText(String.valueOf(String.format(Locale.ENGLISH,"%.1f", lastDelta)) + " mg/dl");
            }
        }

        if (date != null) {
                date.setText(TimeAgo.using(nightscoutDataEvent.getDate()));
        }

    }

    //Return the icon for this page, used when the page is disabled in the app list. In this case, the launcher icon is used
    @Override
    public Bitmap getWidgetIcon(Context paramContext) {
        return ((BitmapDrawable) this.mContext.getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap();
    }

    //Return the launcher intent for this page. This might be used for the launcher as well when the page is disabled?
    @Override
    public Intent getWidgetIntent() {
        Intent localIntent = new Intent();
        /*localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        localIntent.setAction("android.intent.action.MAIN");
        localIntent.addCategory("android.intent.category.LAUNCHER");
        localIntent.setComponent(new ComponentName(this.mContext.getPackageName(), "com.huami.watch.deskclock.countdown.CountdownListActivity"));*/
        return localIntent;
    }

    //Return the title for this page, used when the page is disabled in the app list. In this case, the app name is used
    @Override
    public String getWidgetTitle(Context paramContext) {
        return this.mContext.getResources().getString(R.string.app_name);
    }

    //Called when the page is shown
    @Override
    public void onActive(Bundle paramBundle) {
        super.onActive(paramBundle);
        //Check if the view is already inflated (reloading)
        if ((!this.mHasActive) && (this.mView != null)) {
            //It is, simply refresh
            refreshView();
        }

        if (!eventBusConnected) {
            initIPC(mHost.getHostWindow().getContext());
        }

        if (date != null) {
            date.setText(TimeAgo.using(lastDate));
        }

        //Store active state
        this.mHasActive = true;
    }

    private void refreshView() {
        //Called when the page reloads, check for updates here if you need to
        //Done :-) now it gets updated every time we enter the widget
        HermesEventBus.getDefault().post(new NightscoutRequestSyncEvent());
    }

    //Returns the springboard host
    public ISpringBoardHostStub getHost() {
        return mHost;
    }

    //Called when the page is loading and being bound to the host
    @Override
    public void onBindHost(ISpringBoardHostStub paramISpringBoardHostStub) {
        Log.d(Constants.TAG, "onBindHost");
        //Store host
        mHost = paramISpringBoardHostStub;

        Context context = paramISpringBoardHostStub.getHostWindow().getContext();
        if (context == null) {
            Log.d(Constants.TAG, "onBindHost: context is null!");
        } else {
            //Intent intent = new Intent(context, MainService.class);
            //context.sendBroadcastAsUser(intent, android.os.Process.myUserHandle());
            //context(intent);
        }
    }

    //Called when the page is destroyed completely (in app mode). Same as the onDestroy method of an activity
    @Override
    public void onDestroy() {
        HermesEventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    //Called when the page becomes inactive (the user has scrolled away)
    @Override
    public void onInactive(Bundle paramBundle) {
        super.onInactive(paramBundle);
        //Store active state
        this.mHasActive = false;
    }

    //Called when the page is paused (in app mode)
    @Override
    public void onPause() {
        super.onPause();
        this.mHasActive = false;
    }

    //Not sure what this does, can't find it being used anywhere. Best leave it alone
    @Override
    public void onReceiveDataFromProvider(int paramInt, Bundle paramBundle) {
        super.onReceiveDataFromProvider(paramInt, paramBundle);
    }

    //Called when the page is shown again (in app mode)
    @Override
    public void onResume() {
        super.onResume();
        //Check if view already loaded
        if ((!this.mHasActive) && (this.mView != null)) {
            //It is, simply refresh
            this.mHasActive = true;
            refreshView();
        }
        //Store active state
        this.mHasActive = true;
    }

    //Called when the page is stopped (in app mode)
    @Override
    public void onStop() {
        super.onStop();
        this.mHasActive = false;
    }

    private void initIPC(Context context) {
        Log.d(Constants.TAG_NIGHTSCOUT_PAGE, "initIPC");

        if (context.getApplicationContext() == null) {
            Log.w(Constants.TAG_NIGHTSCOUT_PAGE, "application context null!!!");
            return;
        }

        try {
            HermesEventBus.getDefault().connectApp(context, Constants.PACKAGE_NAME);
            HermesEventBus.getDefault().register(this);

            eventBusConnected = true;
            Log.d(Constants.TAG_NIGHTSCOUT_PAGE, "eventBus connected");
        } catch (Exception ex) {
            Log.d(Constants.TAG_NIGHTSCOUT_PAGE, "initIPC failed");
            ex.printStackTrace();
        }
    }

    private void initIcons(Context context) {
        Log.d(Constants.TAG_NIGHTSCOUT_PAGE, "initIcons");

        Iconics.init(context);
    }
}
