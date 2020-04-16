package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.WidgetsAdapter;
import com.edotassi.amazmod.helpers.DynamicEventsHelper;

import org.tinylog.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class WidgetsActivity extends BaseAppCompatActivity{

    @BindView(R.id.activity_widgets_selector_progress)
    MaterialProgressBar materialProgressBar;

    private RecyclerView mRecyclerView;
    private WidgetsAdapter mAdapter;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.widgets);
        } catch (NullPointerException exception) {
            Logger.error("Exception: " + exception.getMessage());
        }

        ButterKnife.bind(this);

        mRecyclerView = findViewById(R.id.activity_widgets_selector_list);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        DynamicEventsHelper.DynamicEventsCallback callback = new DynamicEventsHelper.DynamicEventsCallback() {
            @Override
            public void onItemMove(int initialPosition, int finalPosition) {
                mAdapter.onItemMove(initialPosition, finalPosition);
            }

            @Override
            public void removeItem(int position) {
                //mAdapter.removeItem(position);
            }

        };
        ItemTouchHelper androidItemTouchHelper = new ItemTouchHelper(new DynamicEventsHelper(callback,false));
        androidItemTouchHelper.attachToRecyclerView(mRecyclerView);

        mAdapter = new WidgetsAdapter(this, androidItemTouchHelper);
        mRecyclerView.setAdapter(mAdapter);
    }

    public void showProgressBar(){
        materialProgressBar.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.GONE);
    }

    public void hideProgressBar(){
        materialProgressBar.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        mAdapter.save();
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_widgets, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_activity_widgets_save) {
            mAdapter.save();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
