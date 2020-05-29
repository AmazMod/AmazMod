package com.edotassi.amazmod.adapters;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.event.ResultWidgets;
import com.edotassi.amazmod.ui.WidgetsActivity;
import com.edotassi.amazmod.util.Screen;
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

import amazmod.com.models.Widget;
import amazmod.com.transport.data.WidgetsData;

import static amazmod.com.transport.Constants.WIDGETS_LIST_EMPTY_CODE;
import static amazmod.com.transport.Constants.WIDGETS_LIST_SAVED_CODE;

public class WidgetsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ItemTouchHelper androidItemTouchHelper;

    private List<Widget> mList;
    private final WidgetsActivity mActivity;

    public WidgetsAdapter(WidgetsActivity mActivity, ItemTouchHelper androidItemTouchHelper) {
        this.androidItemTouchHelper = androidItemTouchHelper;
        this.mActivity = mActivity;
        mList = new ArrayList<>();
        update();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = View.inflate(mActivity, R.layout.row_drag_widgets, null);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof MyViewHolder) {
            final MyViewHolder holder = (MyViewHolder) viewHolder;

            Widget mWidget = mList.get(i);

            holder.setWidget(mWidget);
            holder.name.setText(mWidget.getName());
            holder.pkg.setText(mWidget.getPackageName());
            holder.on_off_switch.setChecked(mWidget.isEnabled());
            holder.handle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (androidItemTouchHelper != null)
                        androidItemTouchHelper.startDrag(holder);
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }


    public void update() {
        update(false);
    }

    private void update(boolean sendWidgetListToWatch) {
        // Put data to bundle
        WidgetsData widgetsData = new WidgetsData();
        String widget_list = "";

        // List enabled widgets
        if (sendWidgetListToWatch) {
            JSONArray widget_list_array = new JSONArray();
            int position = 0;
            for (Widget widget : mList) {
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
                    item.put("title", widget.getName());

                    // Save widget in the list
                    widget_list_array.put(item);
                } catch (Exception e) {
                    Logger.error("Widget " + widget.getPackageName() + " error: " + e);
                }
            }

            if (widget_list_array.length() > 0) {
                // Add to root object
                JSONObject root = new JSONObject();
                try {
                    root.put("data", widget_list_array);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                widget_list = root.toString();
            } else
                widget_list = WIDGETS_LIST_EMPTY_CODE;
            Logger.debug("Widgets to send: " + widget_list);
        }
        widgetsData.setPackages(widget_list);

        // Get widgets
        Watch.get().sendWidgetsData(widgetsData).continueWith(new Continuation<ResultWidgets, Object>() {
            @Override
            public Object then(@NonNull Task<ResultWidgets> task) {
                if (task.isSuccessful()) {
                    ResultWidgets returedData = task.getResult();
                    WidgetsData widgetsData = returedData.getWidgetsData();
                    String widgetsJSON = widgetsData.getPackages();

                    // Request widgets or save?
                    if (widgetsJSON.equals(WIDGETS_LIST_SAVED_CODE)) {
                        Logger.debug(task.getException(), "Widgets saved");
                        Toast.makeText(mActivity.getBaseContext(),R.string.widgets_changed,Toast.LENGTH_LONG).show();
                    } else {
                        Logger.debug("Widgets data: " + widgetsJSON);
                        //When new widgets are received, update list and notify dataset was changed
                        mList = loadWidgets(widgetsJSON);
                        notifyDataSetChanged();
                    }
                } else {
                    Logger.error(task.getException(), "failed reading widgets");
                    Toast.makeText(mActivity.getBaseContext(),R.string.problem_sending_data,Toast.LENGTH_LONG).show();

                }
                return null;
            }
        });
    }

    private List<Widget> loadWidgets(String widgetsJSON) {
        mActivity.showProgressBar();
        List<Widget> widgetList = new ArrayList<>();

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
                        // Keep left side positions empty
                        if(position<3 && Screen.isStratos())
                            position = 3;
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
                        if (activity.equals("com.amazmod.service.springboard.AmazModLauncher")) {
                            name = "AmazMod";
                        }else if (activity.equals("com.huami.watch.health.widget.StepLauncherView")) {
                            name = mActivity.getResources().getString(R.string.widget_step);
                            if(Screen.isStratos()){
                                position = 0;
                                name = (position+1)+". "+name;
                            }
                        }else if (activity.equals("com.huami.watch.deskclock.countdown.CountdownWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_countdown);
                        }else if (activity.equals("com.huami.watch.deskclock.stopwatch.StopWatchWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_stopwatch);
                        }else if (activity.equals("watch.huami.com.mediaplayer.widget.MusicLauncerView")) {
                            name = mActivity.getResources().getString(R.string.widget_music);
                        }else if (activity.equals("com.huami.watch.health.widget.SleepWidgetView")) {
                            name = mActivity.getResources().getString(R.string.widget_sleep);
                        }else if (activity.equals("com.huami.watch.hmcalendar.widget.CalendarLauncherView") || activity.equals("com.dinodevs.pacecalendarwidget.widget")) {
                            name = mActivity.getResources().getString(R.string.widget_calendar);
                        }else if (activity.equals("com.huami.watch.compass.CompassWidgetView")) {
                            name = mActivity.getResources().getString(R.string.widget_compass);
                        }else if (activity.equals("com.huami.watch.health.widget.HeartLauncherView")) {
                            name = mActivity.getResources().getString(R.string.widget_heart);
                        }else if (activity.equals("com.huami.watch.hmdevices.widget.HmDeviceFindLauncherView")) {
                            name = mActivity.getResources().getString(R.string.widget_find_phone);
                        }else if (activity.equals("com.huami.btcall.widget.BtCallWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_call);
                        }else if (activity.equals("com.huami.watch.newsport.widget.SportHistoryWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_activity);
                            if(Screen.isStratos()){
                                position = 1;
                                name = (position+1)+". "+name;
                            }
                        }else if (activity.equals("com.huami.watch.weather.WeatherWidgetView") || activity.equals("com.huami.watch.weather.Everest2WeatherWidgetView")) {
                            name = mActivity.getResources().getString(R.string.widget_weather);
                        }else if (activity.equals("com.huami.watch.deskclock.AlarmClockWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_alarm);
                        }else if (activity.equals("com.huami.watch.newsport.motionstate.widget.SportMotionStateWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_exercise_state);
                        }else if (activity.equals("com.huami.watch.newsport.widget.SportWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_sport);
                        }else if (activity.equals("com.huami.watch.newsport.widget.SportLauncherWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_sport_launcher);
                            if(Screen.isStratos()){
                                position = 2;
                                name = (position+1)+". "+name;
                            }
                        }else if (activity.equals("com.alipay.android.hmwatch.hmwidget.PayCodeView")) {
                            name = mActivity.getResources().getString(R.string.widget_alipay);
                        }else if (activity.equals("com.huami.watch.ximalayasound.XimalayaLauncherView")) {
                            name = mActivity.getResources().getString(R.string.widget_ximalaya);
                        }else if (activity.equals("com.huami.watch.location.ui.widget.LocationWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_location);
                        }else if (activity.equals("com.huami.watch.wallet.ui.widget.WalletWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_wallet);
                        }else if (activity.equals("com.huami.watch.compass.BarAltitudeWidgetView")) {
                            name = mActivity.getResources().getString(R.string.widget_barometer);
                        }else if (activity.equals("com.huami.watch.pin.PinWidgetView")) {
                            name = mActivity.getResources().getString(R.string.widget_miui_unlock);
                        }else if (activity.equals("com.huami.watch.health.pressure.widget.PressureWidget")) {
                            name = mActivity.getResources().getString(R.string.widget_stress);
                        }else if (pkg.equals("com.huami.watch.hmtvhelper")) {
                            name = mActivity.getResources().getString(R.string.widget_mi_box_remote);
                        }else if (pkg.equals("com.huami.watch.hmtvhelper.widget.TvLauncherView")) {
                            name = mActivity.getResources().getString(R.string.widget_mi_tv_remote);
                        }else if (activity.equals("ard")) {
                            name = mActivity.getResources().getString(R.string.widget_hm_tv);
                        }else if (activity.equals("com.dinodevs.pacecalendarwidget.widgetTimeline")) {
                            name = mActivity.getResources().getString(R.string.widget_timeline);
                        }

                        widgetList.add(createWidget(pkg, name, position, enabled, activity));
                    }
                } catch (Exception e) {
                    Logger.debug("Widget No" + x + " error: " + e);
                }
            }
        } catch (JSONException e) {
            Logger.error("Widgets JSON error");
            Toast.makeText(mActivity.getBaseContext(),R.string.error_loading_widgets,Toast.LENGTH_LONG).show();
        }
        Collections.sort(widgetList);
        mActivity.hideProgressBar();
        return widgetList;
    }

    private Widget createWidget(String pkg, String name, int position, boolean enabled, String activity) {
        Widget widget = new Widget();
        widget.setPackageName(pkg);
        widget.setName(name);
        widget.setPosition(position);
        widget.setActivity(activity);
        widget.setEnabled(enabled);
        return widget;
    }

    public void onEnableWidget(Widget widget) {
        widget.setEnabled(!widget.isEnabled());
        notifyItemChanged(mList.indexOf(widget));
    }

    public void onItemMove(final int initialPosition, final int finalPosition) {
        if (initialPosition < mList.size() && finalPosition < mList.size()) {
            if (initialPosition < finalPosition) {
                for (int i = initialPosition; i < finalPosition; i++) {
                    Collections.swap(mList, i, i + 1);
                }
            } else {
                for (int i = initialPosition; i > finalPosition; i--) {
                    Collections.swap(mList, i, i - 1);
                }
            }
            notifyItemMoved(initialPosition, finalPosition);
        }
    }

    public void save() {
        update(true);
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView pkg;
        Switch on_off_switch;
        TextView handle;


        private Widget widget;

        MyViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.row_widget_name);
            pkg = itemView.findViewById(R.id.row_widget_pkg);
            on_off_switch = itemView.findViewById(R.id.row_widget_switch);
            handle = itemView.findViewById(R.id.row_handle);

            on_off_switch.setOnClickListener(v -> {
                onEnableWidget(widget);
            });

        }

        public void setWidget(Widget widget) {
            this.widget = widget;
        }
    }
}
