package com.amazmod.service.springboard;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import com.amazmod.service.R;
import com.amazmod.service.helper.SimpleItemTouchHelperCallback;
import com.amazmod.service.springboard.settings.Adapter;
import com.amazmod.service.util.WidgetsUtil;

public class WidgetsReorderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get in and out settings. In is the main setting, which defines the order and state of a page, but does not always contain them all. Out contains them all, but no ordering

        WidgetsUtil.loadSettings(this);
        Adapter adapter = WidgetsUtil.getAdapter(this);

        //Create recyclerview as layout
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        //Setup drag to move using the helper
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
        //Add padding for the watch
        recyclerView.setPadding((int) getResources().getDimension(R.dimen.padding_round_small), 0, (int) getResources().getDimension(R.dimen.padding_round_small), (int) getResources().getDimension(R.dimen.padding_round_large));
        recyclerView.setClipToPadding(false);
        //Set the view
        setContentView(recyclerView);
    }
}
