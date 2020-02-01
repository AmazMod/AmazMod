package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.NotificationRepliesAdapter;
import com.edotassi.amazmod.adapters.WidgetsAdapter;
import com.edotassi.amazmod.event.ResultWidgets;
import com.edotassi.amazmod.helpers.DynamicEventsHelper;
import com.edotassi.amazmod.support.AppInfo;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import amazmod.com.transport.data.WidgetsData;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static amazmod.com.transport.Constants.WIDGETS_LIST_EMPTY_CODE;
import static amazmod.com.transport.Constants.WIDGETS_LIST_SAVED_CODE;

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
        ItemTouchHelper androidItemTouchHelper = new ItemTouchHelper(new DynamicEventsHelper(callback));
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
