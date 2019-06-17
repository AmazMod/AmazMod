package com.amazmod.service.springboard;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.SwipeDismissFrameLayout;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.adapters.GridViewPagerAdapter;
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

    private Context mContext;
    private HorizontalGridViewPager mGridViewPager;
    private DotsPageIndicator mPageIndicator;

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

        this.mContext = this;

        setContentView(R.layout.activity_launcher_wear);

        ButterKnife.bind(this);

        gridSwipeLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
                                   @Override
                                   public void onDismissed(SwipeDismissFrameLayout layout) {
                                       finish();
                                   }
                               }
        );

        mGridViewPager = findViewById(R.id.activity_launcher_wear_grid_pager);
        mPageIndicator = findViewById(R.id.activity_laucnher_wear_grid_page_indicator);
        mPageIndicator.setPager(mGridViewPager);

        clearBackStack();

        final Intent intent = getIntent();
        final char mode = intent.getCharExtra(MODE, 'S');

        Logger.debug("LauncherWearGridActivity mode: " + mode);

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

            default:
                Logger.warn("LauncherWearGridActivity no fragments selected!");

        }

        //final Fragment[] items = new Fragment[fragList.size()];
        //fragList.toArray(items);

        GridViewPagerAdapter adapter;
        adapter = new GridViewPagerAdapter(getBaseContext(), this.getFragmentManager(), fragList);

        mGridViewPager.setAdapter(adapter);

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
        super.finish();
        Logger.info("LauncherWearGridActivity finish");
    }

}
