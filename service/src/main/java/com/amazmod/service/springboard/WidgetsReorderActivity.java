package com.amazmod.service.springboard;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazmod.service.R;
import com.amazmod.service.helper.SimpleItemTouchHelperCallback;
import com.amazmod.service.springboard.settings.SpringboardWidgetAdapter;
import com.amazmod.service.util.WidgetsUtil;

public class WidgetsReorderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.onStateNotSaved();

        WidgetsUtil.loadWidgetList(this);
        SpringboardWidgetAdapter adapter = WidgetsUtil.getAdapter(this);

        //Create recyclerview as layout
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setSaveEnabled(false);
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
