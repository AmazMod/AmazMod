package com.edotassi.amazmod.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.adapters.WidgetsAdapter;
import com.edotassi.amazmod.event.ResultWidgets;
import com.edotassi.amazmod.support.AppInfo;
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
import de.mateware.snacky.Snacky;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class WidgetsActivity extends BaseAppCompatActivity
        implements WidgetsAdapter.Bridge{

    @BindView(R.id.activity_widgets_selector_list)
    ListView listView;
    @BindView(R.id.activity_widgets_selector_progress)
    MaterialProgressBar materialProgressBar;

    Context mContext;

    private WidgetsAdapter widgetsAdapter;
    private List<AppInfo> widgetsList;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Widgets"); // TODO translate
        }

        ButterKnife.bind(this);

        mContext = this;

        widgetsAdapter = new WidgetsAdapter(this, R.layout.row_drag_widgets, new ArrayList<>());
        listView.setAdapter(widgetsAdapter);

        // Get widgets
        requestWidgets();

        // Sync-Save button
        /*
        action_activity_widgets_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestWidgets();
            }
        });
        */
    }

    private void requestWidgets() {
        // Put data to bundle
        WidgetsData widgetsData = new WidgetsData();
        widgetsData.setPackages("");

        // Get widgets
        Watch.get().sendWidgetsData(widgetsData).continueWith(new Continuation<ResultWidgets, Object>() {
            @Override
            public Object then(@NonNull Task<ResultWidgets> task) throws Exception {
                if (task.isSuccessful()) {
                    ResultWidgets returedData = task.getResult();
                    WidgetsData widgetsData = returedData.getWidgetsData();
                    String widgetsJSON = widgetsData.getPackages();

                    // Request widgets or save?
                    String save_successful_code = "widgets_saved";
                    if(!widgetsJSON.equals(save_successful_code)) {
                        //Logger.debug("Widgets: " + widgetsJSON);
                        loadApps(widgetsJSON);
                    }else{
                        Logger.error(task.getException(), "failed reading widgets");
                        Snacky.builder()
                                .setActivity(WidgetsActivity.this)
                                .setText("Widgets saved") // TODO translate
                                .setDuration(Snacky.LENGTH_LONG)
                                .build().show();
                    }
                } else {
                    Logger.error(task.getException(), "failed reading widgets");
                    Snacky.builder()
                            .setActivity(WidgetsActivity.this)
                            .setText("Problem sending data") // TODO translate
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

        List<AppInfo> widgetsList = new ArrayList<>();

        // Extract data from JSON
        JSONObject widgets;
        try {
            widgets = new JSONObject(widgetsJSON);
            Logger.debug("JSON widgets breakdown: #"+widgetsJSON+"#");
            if (widgets.has("widgets")){
                //Logger.debug("JSON widgets found");
                JSONArray jsonArray = (JSONArray) widgets.get("widgets");
                for (int i = 0; i <jsonArray.length(); i++) {
                    JSONObject obj = (JSONObject) jsonArray.get(i);

                    String pkg=null, name = null, activity=null;
                    int position = 99;
                    boolean enabled = false;

                    if (obj.has("pkg"))
                        pkg = obj.getString("pkg");
                    if (obj.has("pkg"))
                        name = obj.getString("name");
                    if (obj.has("position"))
                        position = obj.getInt("position");
                    if (obj.has("enabled"))
                        enabled = obj.getBoolean("enabled");
                    if (obj.has("activity"))
                        activity = obj.getString("activity");

                    if(pkg!=null && name!=null && activity!=null) {
                        // Change names that confuse users
                        if(name.equals("天气")){
                            name = "Weather widget";
                        }
                        widgetsList.add(createAppInfo(pkg, name, position, enabled, activity));
                    }
                }

                sortWidgets(widgetsList);
                WidgetsActivity.this.widgetsList = widgetsList;
            }else{
                Logger.error("No widgets to extract form JSON");
                Snacky.builder()
                        .setActivity(WidgetsActivity.this)
                        .setText("Error loading widgets") // TODO translate
                        .setDuration(Snacky.LENGTH_LONG)
                        .build().show();
            }
        }
        catch (JSONException e) {
            Logger.error("Widgets JSON error");
            Snacky.builder()
                    .setActivity(WidgetsActivity.this)
                    .setText("Error loading widgets") // TODO translate
                    .setDuration(Snacky.LENGTH_LONG)
                    .build().show();
        }

        // Fill List with widgetsList
        widgetsAdapter.clear();
        widgetsAdapter.addAll(widgetsList);
        widgetsAdapter.notifyDataSetChanged();

        materialProgressBar.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onAppInfoStatusChange() {
        sortWidgets(widgetsList);
        widgetsAdapter.clear();
        widgetsAdapter.addAll(widgetsList);
        widgetsAdapter.notifyDataSetChanged();
    }

    private AppInfo createAppInfo(String pkg, String name, int position, boolean enabled, String acivity) {
        AppInfo appInfo = new AppInfo();
        appInfo.setPackageName(pkg);
        appInfo.setAppName(name);
        appInfo.setPosition(position);
        appInfo.setAcivity(acivity);
        //appInfo.setVersionName(packageInfo.versionName);
        //appInfo.setIcon(packageInfo.applicationInfo.loadIcon(getPackageManager()));
        appInfo.setEnabled(enabled);
        return appInfo;
    }

    private void sortWidgets(List<AppInfo> appInfoList) {
        Collections.sort(appInfoList, (o1, o2) -> {
            if (o1.isEnabled() && !o2.isEnabled()) {
                return -1;
            } else if (!o1.isEnabled() && o2.isEnabled()) {
                return 1;
            } else if ((!o1.isEnabled() && !o2.isEnabled()) || (o1.isEnabled() && o2.isEnabled())) {
                //return o1.getAppName().compareTo(o2.getAppName());
                return (o1.getPosition()<o2.getPosition())?-1:1;
            }
            //return o1.getAppName().compareTo(o2.getAppName());
            return (o1.getPosition()<o2.getPosition())?-1:1;
        });
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
            requestWidgets();
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

}
