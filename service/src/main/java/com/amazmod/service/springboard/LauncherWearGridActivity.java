package com.amazmod.service.springboard;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.SwipeDismissFrameLayout;
import android.view.MotionEvent;

import com.amazmod.service.R;
import com.amazmod.service.adapters.GridViewPagerAdapter;
import com.amazmod.service.support.ActivityFinishRunnable;
import com.amazmod.service.support.HorizontalGridViewPager;
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

public class LauncherWearGridActivity extends Activity {

    @BindView(R.id.activity_launcher_wear_swipe_layout)
    SwipeDismissFrameLayout gridSwipeLayout;
    @BindView(R.id.activity_launcher_wear_root_layout)
    BoxInsetLayout rootLayout;

    public static final String MODE = "mode";
    public static final char APPS = 'A';
    public static final char SETTINGS = 'S';
    public static final char INFO = 'I';
    public static final char FLASHLIGHT = 'F';
    public static final char NOTIFICATIONS = 'N';
    public static final char NOTIFICATIONS_FROM_WATCHFACE = 'W';
    public static final char FILES = 'B';

    private static char mode;
    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    private HorizontalGridViewPager mGridViewPager;

    @Override
    public void startActivity(Intent intent) {
        Logger.debug("LauncherWearGridActivity startActivity");
        super.startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);

        final char newMode = newIntent.getCharExtra(MODE, 'S');
        Logger.debug("LauncherWearGridActivity onNewIntent newMode: {}", newMode);

        if (newMode != mode)
            setGrid(newMode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mode = getIntent().getCharExtra(MODE, 'S');

        Logger.debug("LauncherWearGridActivity onCreate mode: " + mode);

        setContentView(R.layout.activity_launcher_wear);

        ButterKnife.bind(this);

        gridSwipeLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                finish();
            }
        });

        mGridViewPager = findViewById(R.id.activity_launcher_wear_grid_pager);
        DotsPageIndicator mPageIndicator = findViewById(R.id.activity_launcher_wear_grid_page_indicator);
        mPageIndicator.setPager(mGridViewPager);

        setGrid(mode);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.debug("LauncherWearGridActivity onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Logger.debug("LauncherWearGridActivity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.debug("LauncherWearGridActivity onDestroy");
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        Logger.debug("LauncherWearGridActivity finish");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Logger.debug("LauncherWearGridActivity dispatchTouchEvent");
        findViewById(R.id.activity_launcher_wear_root_layout).dispatchTouchEvent(event);
        if (NOTIFICATIONS_FROM_WATCHFACE == mode)
            startTimerFinish();
        return false;
    }

    private void setGrid(char mode){

        clearBackStack();
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
                fragList.add(WearNotificationsFragment.newInstance(false));
                break;

            case NOTIFICATIONS_FROM_WATCHFACE:
                fragList.add(WearNotificationsFragment.newInstance(true));
                break;

            case FILES:
                fragList.add(WearFilesFragment.newInstance());
                break;

            default:
                Logger.warn("LauncherWearGridActivity no fragments selected!");
                finish();

        }

        GridViewPagerAdapter adapter;
        adapter = new GridViewPagerAdapter(getBaseContext(), this.getFragmentManager(), fragList);
        mGridViewPager.setAdapter(adapter);

        if (NOTIFICATIONS_FROM_WATCHFACE == mode) {
            handler = new Handler();
            activityFinishRunnable = new ActivityFinishRunnable(this);
            startTimerFinish();
        }
    }

    public void startTimerFinish() {
        Logger.debug("LauncherWearGridActivity startTimerFinish");
        if (activityFinishRunnable != null)
            handler.removeCallbacks(activityFinishRunnable);
        handler.postDelayed(activityFinishRunnable, 12000);
    }

    public void stopTimerFinish() {
        Logger.debug("LauncherWearGridActivity stopTimerFinish");
        if (activityFinishRunnable != null)
            handler.removeCallbacks(activityFinishRunnable);
    }

    private void clearBackStack() {
        FragmentManager manager = this.getFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            Logger.warn("LauncherWearGridActivity ***** clearBackStack getBackStackEntryCount: " + manager.getBackStackEntryCount());
            while (manager.getBackStackEntryCount() > 0){
                manager.popBackStackImmediate();
            }
        }
    }

    public void setSwipeable(boolean status){
        gridSwipeLayout.setSwipeable(status);
    }

}

