package com.amazmod.service.springboard;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.SwipeDismissFrameLayout;
import android.util.Log;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.adapters.GridViewPagerAdapter;
import com.amazmod.service.support.HorizontalGridViewPager;
import com.amazmod.service.ui.fragments.WearAppsFragment;
import com.amazmod.service.ui.fragments.WearMenuFragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WearGridActivity extends Activity {

    @BindView(R.id.activity_wear_grid_swipe_layout)
    SwipeDismissFrameLayout gridSwipeLayout;
    @BindView(R.id.activity_wear_grid_root_layout)
    BoxInsetLayout rootLayout;

    private Context mContext;
    private HorizontalGridViewPager mGridViewPager;
    private DotsPageIndicator mPageIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mContext = this;

        setContentView(R.layout.activity_wear_grid);

        ButterKnife.bind(this);

        gridSwipeLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
                                   @Override
                                   public void onDismissed(SwipeDismissFrameLayout layout) {
                                       finish();
                                   }
                               }
        );

        mGridViewPager = findViewById(R.id.grid_pager);
        mPageIndicator = findViewById(R.id.grid_page_indicator);
        mPageIndicator.setPager(mGridViewPager);

        clearBackStack();

        GridViewPagerAdapter adapter;
        List<Fragment> items = new ArrayList<Fragment>();
        items.add(WearMenuFragment.newInstance());
        items.add(WearAppsFragment.newInstance());
        adapter = new GridViewPagerAdapter(getBaseContext(), this.getFragmentManager(), items);

        mGridViewPager.setAdapter(adapter);

    }

    private void clearBackStack() {
        FragmentManager manager = this.getFragmentManager();
        if (manager.getBackStackEntryCount() > 0) {
            Log.w(Constants.TAG, "WearGridActivity ***** clearBackStack getBackStackEntryCount: " + manager.getBackStackEntryCount());
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

        Log.i(Constants.TAG, "WearGridActivity finish");
    }

}
