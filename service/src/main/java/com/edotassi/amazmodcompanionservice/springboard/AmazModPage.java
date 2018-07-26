package com.edotassi.amazmodcompanionservice.springboard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.edotassi.amazmodcompanionservice.BuildConfig;
import com.edotassi.amazmodcompanionservice.Constants;
import com.edotassi.amazmodcompanionservice.MainService;
//import com.edotassi.amazmodcompanionservice.MessagesListener;
import com.edotassi.amazmodcompanionservice.R;
import com.edotassi.amazmodcompanionservice.springboard.WidgetSettings;
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
//import xiaofei.library.hermeseventbus.HermesEventBus;

public class AmazModPage extends AbstractPlugin {

    private static final String TAG = "AmazModPage";
    private Context mContext;
    private View view;
    private boolean mHasActive = false;
    private ISpringBoardHostStub mHost = null;

//    @BindView(R2.id.amazmod_page_version)
    TextView version, timeSLC;

    @Override
    public View getView(Context paramContext) {

        this.mContext = paramContext;
        paramContext.startService(new Intent(paramContext, MainService.class));

        this.view = LayoutInflater.from(paramContext).inflate(R.layout.amazmod_page, null);

        version = view.findViewById(R.id.amazmod_page_version);
        timeSLC = view.findViewById(R.id.time_since_last_charge);

/**        try {
            ButterKnife.bind(this, view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
**/
        version.setText(BuildConfig.VERSION_NAME);

        return this.view;
    }

    //Return the icon for this page, used when the page is disabled in the app list. In this case, the launcher icon is used
    @Override
    public Bitmap getWidgetIcon(Context paramContext) {
        return ((BitmapDrawable) this.mContext.getDrawable(R.mipmap.ic_launcher)).getBitmap();
    }

    //Return the launcher intent for this page. This might be used for the launcher as well when the page is disabled?
    @Override
    public Intent getWidgetIntent() {
        //No intent required
        return new Intent();
    }

    //Called when the page is shown
    @Override
    public void onActive(Bundle paramBundle) {
        super.onActive(paramBundle);
        //Check if the view is already inflated (reloading)
        if ((!this.mHasActive) && (this.view != null)) {
            //It is, simply refresh
            refreshView();
        }
        //Store active state
        this.mHasActive = true;
    }

    private void refreshView() {
        //Called when the page reloads, check for updates here if you need to
        updateTimeSinceLastCharge();
    }

    //Returns the springboard host
    public ISpringBoardHostStub getHost() {
        return mHost;
    }

    //Called when the page is loading and being bound to the host
    @Override
    public void onBindHost(ISpringBoardHostStub paramISpringBoardHostStub) {
        //Store host
        mHost = paramISpringBoardHostStub;
    }

    //Called when the page is destroyed completely (in app mode). Same as the onDestroy method of an activity
    @Override
    public void onDestroy() {
        super.onDestroy();
        updateTimeSinceLastCharge();
    }

    //Called when the page becomes inactive (the user has scrolled away)
    @Override
    public void onInactive(Bundle paramBundle) {
        super.onInactive(paramBundle);
        updateTimeSinceLastCharge();
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
        if ((!this.mHasActive) && (this.view != null)) {
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

    private void updateTimeSinceLastCharge() {

        // Initialize settings
        WidgetSettings settings = new WidgetSettings(Constants.TAG, mContext);
        //Get date of last full charge
        long lastChargeDate = settings.get(Constants.PREF_DATE_LAST_CHARGE, 0L);

        String dateDiff = " ";

        Log.d(Constants.TAG, "lastChargeDate widget: " + lastChargeDate );
        if (lastChargeDate != 0) {
            long diffInMillies = System.currentTimeMillis() - lastChargeDate;
            List<TimeUnit> units = new ArrayList<>(EnumSet.allOf(TimeUnit.class));
            Collections.reverse(units);
            long milliesRest = diffInMillies;
            for (TimeUnit unit : units) {
                long diff = unit.convert(milliesRest, TimeUnit.MILLISECONDS);
                long diffInMilliesForUnit = unit.toMillis(diff);
                milliesRest = milliesRest - diffInMilliesForUnit;
                if (unit.equals(TimeUnit.DAYS)) {
                    dateDiff += diff + "d : ";
                } else if (unit.equals(TimeUnit.HOURS)) {
                    dateDiff += diff + "h : ";
                } else if (unit.equals(TimeUnit.MINUTES)) {
                    dateDiff += diff + "m";
                    break;
                }
            }
            dateDiff += "\n" + mContext.getResources().getText(R.string.last_charge);
        } else dateDiff += this.mContext.getResources().getText(R.string.last_charge_no_info);

        timeSLC.setText(dateDiff);
    }

//    @Override
//    public void onBindHost(ISpringBoardHostStub iSpringBoardHostStub) {
//    }

}
