package com.amazmod.service.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.SwipeDismissFrameLayout;
import android.view.WindowManager;

import com.amazmod.service.R;
import com.amazmod.service.adapters.GridViewPagerAdapter;
import com.amazmod.service.support.HorizontalGridViewPager;
import com.amazmod.service.ui.fragments.NotificationFragment;
import com.amazmod.service.ui.fragments.WearAppsFragment;
import com.amazmod.service.ui.fragments.WearFilesFragment;
import com.amazmod.service.ui.fragments.WearFlashlightFragment;
import com.amazmod.service.ui.fragments.WearInfoFragment;
import com.amazmod.service.ui.fragments.WearMenuFragment;
import com.amazmod.service.ui.fragments.WearNotificationsFragment;

import org.tinylog.Logger;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BaseWearGridActivity extends Activity {

    @BindView(R.id.activity_base_wear_swipe_layout)
    SwipeDismissFrameLayout gridSwipeLayout;
    @BindView(R.id.activity_base_wear_root_layout)
    BoxInsetLayout rootLayout;

    public static final String MODE = "mode";
    public static final char APPS = 'A';
    public static final char SETTINGS = 'S';
    public static final char INFO = 'I';
    public static final char FLASHLIGHT = 'F';
    public static final char NOTIFICATIONS = 'N';
    public static final char FILES = 'B';
    public static final char NULL = 'X';

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_base_wear);

        ButterKnife.bind(this);

        gridSwipeLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
                                   @Override
                                   public void onDismissed(SwipeDismissFrameLayout layout) {
                                       finish();
                                   }
                               });

        HorizontalGridViewPager mGridViewPager = findViewById(R.id.activity_base_wear_grid_pager);
        DotsPageIndicator mPageIndicator = findViewById(R.id.activity_base_wear_grid_page_indicator);
        mPageIndicator.setPager(mGridViewPager);

        clearBackStack();

        final Intent intent = getIntent();
        char mode = intent.getCharExtra(MODE, 'S');

        setWindowFlags(true);
        Logger.debug("BaseWearGridActivity mode: " + mode);

        final ArrayList<Fragment> fragList = new ArrayList<>();

        switch (mode) {
            case APPS:
                fragList.add(WearAppsFragment.newInstance());
                break;

            case SETTINGS:
                fragList.add(WearMenuFragment.newInstance());
                break;

            case INFO:
                fragList.add(WearInfoFragment.newInstance());
                break;

            case FLASHLIGHT:
                fragList.add(WearFlashlightFragment.newInstance());
                break;

            case NOTIFICATIONS:
                fragList.add(WearNotificationsFragment.newInstance());
                break;

            case FILES:
                fragList.add(WearFilesFragment.newInstance());
                break;

            case NULL:
                Logger.warn("BaseWearGridActivity NULL");
                break;

            default:
                Logger.warn("BaseWearGridActivity no fragments selected!");

        }

        if (NULL != mode) {
            GridViewPagerAdapter adapter;
            adapter = new GridViewPagerAdapter(getBaseContext(), this.getFragmentManager(), fragList);
            mGridViewPager.setAdapter(adapter);
        }

    }

    private void clearBackStack() {
        FragmentManager manager = this.getFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            Logger.warn("BaseWearGridActivity ***** clearBackStack getBackStackEntryCount: " + manager.getBackStackEntryCount());
            while (manager.getBackStackEntryCount() > 0){
                manager.popBackStackImmediate();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        setWindowFlags(false);
        super.finish();
        overridePendingTransition(0, R.anim.fade_out);
        Logger.info("BaseWearGridActivity finish");
    }

    private void setWindowFlags(boolean enable) {

        final int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

        if (enable) {
            getWindow().addFlags(flags);
            WindowManager.LayoutParams params = getWindow().getAttributes();

            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            getWindow().setAttributes(params);

        } else {
            getWindow().clearFlags(flags);
        }
    }

}
