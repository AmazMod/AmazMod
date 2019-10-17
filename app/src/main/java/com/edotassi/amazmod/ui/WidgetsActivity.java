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

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.ResultWidgets;
import com.edotassi.amazmod.support.AppInfo;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;

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

    @BindView(R.id.activity_widgets_selector_list)
    ListView listView;
    @BindView(R.id.activity_widgets_selector_progress)
    MaterialProgressBar materialProgressBar;

    Context mContext;

    private WidgetsAdapter widgetsAdapter;
    private List<AppInfo> widgetsList;

    private boolean mSortable = false;
    private AppInfo mDragWidget;
    private int initalPosition;
    private int mPosition = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getResources().getString(R.string.widgets));
        }

        ButterKnife.bind(this);

        mContext = this;

        widgetsAdapter = new WidgetsAdapter(this, R.layout.row_drag_widgets, new ArrayList<>());
        listView.setAdapter(widgetsAdapter);

        // Get widgets
        requestWidgets(false);


        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!mSortable) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
                        //int position = itemNum - listView.getFirstVisiblePosition();
                        if (position < 0) {
                            break;
                        }
                        if (position != mPosition) {
                            Logger.debug("WidgetsActivity move mPosition: " + mPosition +" \\ position: " + position);
                            if (mPosition != -1) {
                                if (position > mPosition) {
                                    if (position - mPosition == 1)
                                        Collections.swap(widgetsList, mPosition, position);
                                    else {
                                        Collections.swap(widgetsList, mPosition, (position-1));
                                        Collections.swap(widgetsList, position, (position-1));
                                    }
                                } else {
                                    if (mPosition - position == 1)
                                        Collections.swap(widgetsList, mPosition, position);
                                    else {
                                        Collections.swap(widgetsList, mPosition, (position+1));
                                        Collections.swap(widgetsList, position, (position+1));
                                    }
                                }
                            }

                            mPosition = position;
                            widgetsAdapter.remove(mDragWidget);
                            widgetsAdapter.insert(mDragWidget, mPosition);
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE: {
                        Logger.debug("WidgetsActivity cancel initialPosition: " + initalPosition +
                                " \\ mPosition: " + mPosition);
                        stopDrag();
                        return true;
                    }
                }
                return false;
            }
        });

        // Sync-Save button
        /*
        action_activity_widgets_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestWidgets(true);
            }
        });
         */
    }

    public void startDrag(AppInfo widget) {
        mPosition = -1;
        mSortable = true;
        mDragWidget = widget;
        widgetsAdapter.notifyDataSetChanged();
    }

    public void stopDrag() {
        mPosition = -1;
        mSortable = false;
        mDragWidget = null;
        widgetsAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void requestWidgets(boolean sent_data) {
        // Put data to bundle
        WidgetsData widgetsData = new WidgetsData();
        String widget_list = "";

        // List enabled widgets
        if(sent_data){
            JSONArray widget_list_array = new JSONArray();
            int position = 0;
            for (AppInfo widget : widgetsList) {
                try {
                    JSONObject item = new JSONObject();

                    // - Widget data -
                    // Package name
                    item.put("pkg", widget.getPackageName());
                    // Class
                    item.put("cls", widget.getActivity());
                    // Position, stored as a string for some reason (we are assigning a position now)
                    item.put("srl", String.valueOf(position++));
                    // Since it is not in the in/out lists, it is disabled = 0, need as string because this is how it is set in the service
                    item.put("enable", widget.isEnabled() ? "1" : "0");
                    // Name (proper app name)
                    item.put("title", widget.getAppName());

                    // Save widget in the list
                    widget_list_array.put(item);
                } catch (Exception e) {
                    Logger.error("Widget " + widget.getPackageName() + " error: "+e);
                }
            }

            if(widget_list_array.length()>0) {
                // Add to root object
                JSONObject root = new JSONObject();
                try {
                    root.put("data", widget_list_array);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                widget_list = root.toString();
            }else
                widget_list = WIDGETS_LIST_EMPTY_CODE;
            Logger.debug("Widgets to send: "+widget_list);
        }
        widgetsData.setPackages(widget_list);

        // Get widgets
        Watch.get().sendWidgetsData(widgetsData).continueWith(new Continuation<ResultWidgets, Object>() {
            @Override
            public Object then(@NonNull Task<ResultWidgets> task) throws Exception {
                if (task.isSuccessful()) {
                    ResultWidgets returedData = task.getResult();
                    WidgetsData widgetsData = returedData.getWidgetsData();
                    String widgetsJSON = widgetsData.getPackages();

                    // Request widgets or save?
                    if(widgetsJSON.equals(WIDGETS_LIST_SAVED_CODE)) {
                        Logger.debug(task.getException(), "Widgets saved");
                        Snacky.builder()
                                .setActivity(WidgetsActivity.this)
                                .setText(getResources().getString(R.string.widgets_changed))
                                .setDuration(Snacky.LENGTH_LONG)
                                .build().show();
                    }else{
                        Logger.debug("Widgets data: " + widgetsJSON);
                        loadApps(widgetsJSON);
                    }
                } else {
                    Logger.error(task.getException(), "failed reading widgets");
                    Snacky.builder()
                            .setActivity(WidgetsActivity.this)
                            .setText(getResources().getString(R.string.problem_sending_data))
                            .setDuration(Snacky.LENGTH_LONG)
                            .build().show();
                }
                return null;
            }
        });
    }

    private void loadApps(String widgetsJSON) {
        materialProgressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        widgetsList = new ArrayList<>();

        // Extract data from JSON
        boolean error = false;
        try {
            JSONArray data = new JSONArray(widgetsJSON);
            for (int x = 0; x < data.length(); x++) {
                // "Try" again to contain the crash in each widget item
                try {
                    // Get item
                    JSONObject item = data.getJSONObject(x);

                    String pkg = null, name = null, activity = null;
                    int position = 99;
                    boolean enabled = false;

                    if (item.has("pkg"))
                        pkg = item.getString("pkg");
                    if (item.has("srl"))
                        position = item.getInt("srl");
                    if (item.has("enable"))
                        enabled = item.getInt("enable") == 1;
                    if (item.has("cls"))
                        activity = item.getString("cls");
                    if (item.has("title"))
                        name = item.getString("title");
                    else {
                        if (activity != null && !activity.isEmpty()) {
                            String[] activity_components = activity.split("\\.");
                            name = activity_components[activity_components.length - 1];
                        } else {
                            name = pkg;
                        }
                    }

                    if (pkg != null && name != null && activity != null) {
                        // Change names that confuse users
                        if (name.equals("天气")) {
                            name = "Weather";
                        }
                        widgetsList.add(createAppInfo(pkg, name, position, enabled, activity));
                    }
                } catch (Exception e) {
                    Logger.debug("Widget No"+x+" error: "+e);
                }
            }
        } catch (JSONException e) {
            Logger.error("Widgets JSON error");
            Snacky.builder()
                    .setActivity(WidgetsActivity.this)
                    .setText(getResources().getString(R.string.error_loading_widgets))
                    .setDuration(Snacky.LENGTH_LONG)
                    .build().show();
            error = true;
        }

        // Fill List with widgetsList
        if(!widgetsList.isEmpty()) {
            widgetsAdapter.clear();
            widgetsAdapter.addAll(widgetsList);
            widgetsAdapter.notifyDataSetChanged();
        }else if(!error){
            Snacky.builder()
                    .setActivity(WidgetsActivity.this)
                    .setText(getResources().getString(R.string.no_widgets_found))
                    .setDuration(Snacky.LENGTH_LONG)
                    .build().show();
        }

        materialProgressBar.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    private AppInfo createAppInfo(String pkg, String name, int position, boolean enabled, String activity) {
        AppInfo appInfo = new AppInfo();
        appInfo.setPackageName(pkg);
        appInfo.setAppName(name);
        appInfo.setPosition(position);
        appInfo.setActivity(activity);
        //appInfo.setVersionName(packageInfo.versionName);
        //appInfo.setIcon(packageInfo.applicationInfo.loadIcon(getPackageManager()));
        appInfo.setEnabled(enabled);
        return appInfo;
    }

    public void onEnableWidget(AppInfo appInfo) {
        appInfo.setEnabled(!appInfo.isEnabled());
        widgetsAdapter.clear();
        widgetsAdapter.addAll(widgetsList);
        widgetsAdapter.notifyDataSetChanged();
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
            requestWidgets(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public class WidgetsAdapter extends ArrayAdapter<AppInfo> {

        public WidgetsAdapter(Context context, int resource, @NonNull List<AppInfo> objects) {
            super(context, resource, objects);
        }

        @SuppressLint("ClickableViewAccessibility")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View listItem = convertView;
            if (listItem == null)
                listItem = LayoutInflater.from(getContext()).inflate(R.layout.row_drag_widgets, parent, false);

            final AppInfo appInfo = getItem(position);

            ViewHolder viewHolder = new ViewHolder(appInfo);
            ButterKnife.bind(viewHolder, listItem);

            viewHolder.name.setText(appInfo.getAppName());
            viewHolder.pkg.setText(appInfo.getPackageName());
            viewHolder.on_off_switch.setChecked(appInfo.isEnabled());

            viewHolder.handle.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    initalPosition = listView.pointToPosition((int) view.getX(), (int) view.getY());
                    startDrag(appInfo);
                    return true;
                }
            });

            if (mDragWidget != null && mDragWidget == appInfo) {
                listItem.setBackgroundColor(ThemeHelper.getThemeColorAccent(getContext()));
            } else {
                listItem.setBackgroundColor(Color.TRANSPARENT);
            }

            return listItem;
        }

        public class ViewHolder {

            @BindView(R.id.row_widget_name)
            TextView name;
            @BindView(R.id.row_widget_pkg)
            TextView pkg;
            @BindView(R.id.row_widget_switch)
            Switch on_off_switch;
            @BindView(R.id.row_handle)
            TextView handle;

            private AppInfo appInfo;

            protected ViewHolder(AppInfo appInfo) {
                this.appInfo = appInfo;
            }

            @OnClick(R.id.row_widget_switch)
            public void onEnableClick() { onEnableWidget(appInfo); }
        }
    }

}
