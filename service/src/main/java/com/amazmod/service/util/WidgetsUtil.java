package com.amazmod.service.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.MainService;
import com.amazmod.service.R;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.springboard.SpringboardItem;
import com.amazmod.service.springboard.settings.BaseSetting;
import com.amazmod.service.springboard.settings.ButtonSetting;
import com.amazmod.service.springboard.settings.HeaderSetting;
import com.amazmod.service.springboard.settings.SpringboardSetting;
import com.amazmod.service.springboard.settings.SpringboardWidgetAdapter;
import com.amazmod.service.springboard.settings.TextSetting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WidgetsUtil {

    private static ArrayList<BaseSetting> settingList;
    //Countdown timer to prevent saving too often
    private static CountDownTimer countDownTimer;

    public static void syncWidgets(final Context context){
        loadSettings(context,true);
    }

    public static void loadWidgetList(final Context context){
        loadSettings(context,false);
    }

    private static void loadSettings(final Context context, boolean saveOriginalList) {

        SettingsManager settingsManager = new SettingsManager(context);
        boolean amazModFirstWidget = settingsManager.getBoolean(Constants.PREF_AMAZMOD_FIRST_WIDGET, true);
        String savedOrder = settingsManager.getString(Constants.PREF_SPRINGBOARD_ORDER, "");
        String last_widget_order_in = settingsManager.getString(Constants.PREF_AMAZMOD_OFFICIAL_WIDGETS_ORDER, "");

        boolean isAmazmodWidgetMissing = true;
        int amazmodPosition = SystemProperties.isVerge()?0:1;
        if (SystemProperties.isStratos())
            amazmodPosition = 2;

        final SpringboardItem amazmodWidget = new SpringboardItem(Constants.SERVICE_NAME, Constants.LAUNCHER_CLASSNAME, true);

        // Get in and out settings.
        // In is the main setting, which defines the order and state of a page, but does not always contain them all.
        String widget_order_in = DeviceUtil.systemGetString(context, Constants.WIDGET_ORDER_IN);

        // Out contains them all, but no ordering.
        String widget_order_out = DeviceUtil.systemGetString(context, Constants.WIDGET_ORDER_OUT);

        Logger.debug("WidgetsUtil loadSettings widget_order_in  : " + widget_order_in.substring(0, Math.min(widget_order_in.length(), 352)));
        Logger.debug("WidgetsUtil loadSettings widget_order_out : " + widget_order_out.substring(0, Math.min(widget_order_out.length(), 352)));
        Logger.debug("WidgetsUtil loadSettings savedOrder : " + savedOrder.substring(0, Math.min(savedOrder.length(), 352)));
        Logger.debug("WidgetsUtil loadSettings widget_order_last : " + last_widget_order_in.substring(0, Math.min(last_widget_order_in.length(), 352)));

        // Backup the original list to a variable
        if (saveOriginalList)
            saveOfficialAppOrder(context, widget_order_in);

        // Apply user saved list
        if (!savedOrder.isEmpty() && amazModFirstWidget)
            widget_order_in = savedOrder;
        // Old save code
        /* Disabled for testing purposes
        // if last order_in is equal to current one no change was done via Amazfit Watch official App, so reapply saved order
        if (widget_order_in.equals(last_widget_order_in)){
            Logger.debug("WidgetsUtil loadSettings : current order is equal to last one");
            if (!savedOrder.isEmpty()) {
                Logger.debug("WidgetsUtil loadSettings using saved_order : " + savedOrder);
                widget_order_in = savedOrder;
            }
        }
        */

        // Find and populate the widgets list
        settingList = new ArrayList<>(); // Create empty list
        try {
            // Get data from widget_order_in
            //Parse JSON
            JSONObject root = new JSONObject(widget_order_in);
            JSONArray data = root.getJSONArray("data");

            List<String> addedComponents = new ArrayList<>();
            //Data array contains all the elements
            for (int x = 0; x < data.length(); x++) {
                //Get item
                JSONObject item = data.getJSONObject(x);

                //Check if AmazMod widget found
                if (item.getString("pkg").equals(Constants.SERVICE_NAME))
                    isAmazmodWidgetMissing = false;

                //srl is the position, stored as a string for some reason
                int srl = Integer.parseInt(item.getString("srl"));
                //State is stored as an integer when it would be better as a boolean so convert it
                boolean enable = item.getInt("enable") == 1;
                //Create springboard item with the package name, class name and state
                final SpringboardItem springboardItem = new SpringboardItem(item.getString("pkg"), item.getString("cls"), enable);
                //Create a setting (extending switch) with the relevant data and a callback
                SpringboardSetting springboardSetting = new SpringboardSetting(null, getTitle(springboardItem.getPackageName(), context),
                        formatComponentName(springboardItem.getClassName()), new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        Logger.debug("WidgetsUtil loadSettings.onCheckedChanged b: " + b);
                        //Ignore on create to reduce load
                        if (!compoundButton.isPressed()) return;
                        //Update state
                        springboardItem.setEnabled(b);
                        //Save
                        save(context);
                    }
                }, springboardItem.isEnable(), springboardItem);
                //Store component name for later
                addedComponents.add(springboardItem.getClassName());
                try {
                    //Attempt to add at position, may cause exception
                    settingList.add(srl, springboardSetting);
                } catch (IndexOutOfBoundsException e) {
                    //Add at top as position won't work
                    settingList.add(springboardSetting);
                }
            }

            // Get data from widget_order_out (to find any missing)
            //Parse JSON
            JSONObject rootOut = new JSONObject(widget_order_out);
            JSONArray dataOut = rootOut.getJSONArray("data");
            //Loop through main data array
            for (int x = 0; x < data.length(); x++) {
                //Get item
                //Checks if item exists at out array
                if (!dataOut.isNull(x)) {
                    JSONObject item = dataOut.getJSONObject(x);
                    //Get component name to check list
                    String componentName = item.getString("cls");
                    if (!addedComponents.contains(componentName)) {
                        //Get if item is enabled, this time stored as a string (why?)
                        boolean enable = item.getString("enable").equals("true");
                        //Create item with the package name, class name and state
                        SpringboardItem springboardItem = new SpringboardItem(item.getString("pkg"), item.getString("cls"), enable);
                        //Add class name to list to prevent it being adding more than once
                        addedComponents.add(springboardItem.getClassName());

                        //Always show amazmod as first when swiping left (index 2, second item) if its defined in preferences
                        if (item.getString("pkg").equals(Constants.SERVICE_NAME) && amazModFirstWidget) {
                            //Make sure it is enabled
                            springboardItem.setEnabled(true);
                            //Create setting with all the relevant data
                            SpringboardSetting springboardSetting = addSpringboardSetting(context, springboardItem);
                            //Add amazmod as first one
                            settingList.add(amazmodPosition, springboardSetting);
                            isAmazmodWidgetMissing = false;
                        } else {
                            //Create setting with all the relevant data
                            SpringboardSetting springboardSetting = addSpringboardSetting(context, springboardItem);
                            //Add setting to main list
                            settingList.add(springboardSetting);
                        }
                    }
                }
            }

            if (isAmazmodWidgetMissing && amazModFirstWidget) {
                SpringboardSetting amazmodSetting = addSpringboardSetting(context, amazmodWidget);
                settingList.add((settingList.size()>=amazmodPosition)?amazmodPosition:settingList.size(), amazmodSetting);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Empty settings list can be confusing to the user, and is quite common, so we'll add the FAQ to save them having to read the OP (oh the horror)
        if (settingList.size() == 0) {
            //Add error message
            settingList.add(new TextSetting(context.getString(R.string.error_loading), null));
        } else {
            //Add main header to top (pos 0)
            settingList.add(0, new HeaderSetting("Widgets", new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Logger.debug("WidgetsUtil loadSettings.onLongClick");
                    MainService.setWasSpringboardSaved(true);
                    save(context, true, true);
                    return true;
                }
            }));

            // Add save button
            settingList.add(new ButtonSetting("Save order", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Logger.debug("WidgetsUtil SaveSettings.onClick");
                    MainService.setWasSpringboardSaved(true);
                    save(context, true, true);
                }
            }));
        }

        // Add option to clear saved sorted order
        settingList.add(new ButtonSetting("Clear Saved", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.debug("WidgetsUtil clearSettings.onClick");
                new AlertDialog.Builder(context)
                        .setTitle("Clear Saved Order")
                        .setMessage(context.getResources().getString(R.string.confirmation))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                MainService.setWasSpringboardSaved(false);
                                settingsManager.putString(Constants.PREF_SPRINGBOARD_ORDER, "");
                                Toast.makeText(context, "List Cleared", Toast.LENGTH_SHORT).show();
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
            }
        }));
        MainService.setWasSpringboardSaved(true);

        //Save initial config (to keep amazmod in first position)
        save(context,false, false);
    }

    public static JSONArray getWidgetsLists(Context context, boolean searchForCustomWidgets){
        // Get in and out settings.
        // In is the main setting, which defines the order and state of a page, but does not always contain them all.
        String widget_order_in = DeviceUtil.systemGetString(context, Constants.WIDGET_ORDER_IN);
        // Out contains them all, but no ordering.
        String widget_order_out = DeviceUtil.systemGetString(context, Constants.WIDGET_ORDER_OUT);

        // The returned value
        JSONArray widgets_list = new JSONArray();
        List<String> foundComponents = new ArrayList<>();
        int max_position = 0;

        try {
            // Get data from widget_order_in
            //Parse JSON
            JSONObject root = new JSONObject(widget_order_in);
            JSONArray data = root.getJSONArray("data");

            //Data array contains all the elements
            for (int x = 0; x < data.length(); x++) {
                // Get item
                JSONObject item = data.getJSONObject(x);

                // - Widget data -
                // srl is the position, stored as a string for some reason
                int srl = Integer.parseInt(item.getString("srl"));
                // State is stored as an integer when it would be better as a boolean so convert it
                //boolean enable = item.getInt("enable") == 1;
                // package name
                //String str = item.getString("pkg");
                // class name
                //String componentName = item.getString("cls");

                // fix the int-to-string conversion problem
                item.put("enable", item.getInt("enable"));

                //Store component name for later
                foundComponents.add(item.getString("cls"));

                // Save widget in the list
                widgets_list.put(widgets_list.length(), item);
                // Set max position
                if (srl>max_position)
                    max_position = srl;

                Logger.debug("In widget found: " + item.toString());
            }

            // Get data from widget_order_out (to find any missing)
            //Parse JSON
            JSONObject rootOut = new JSONObject(widget_order_out);
            JSONArray dataOut = rootOut.getJSONArray("data");
            // Loop through dataOut array
            for (int x = 0; x < dataOut.length(); x++) {
                // Get item
                JSONObject item = dataOut.getJSONObject(x);
                // Get component (class name) name to check list
                String componentName = item.getString("cls");
                if (!foundComponents.contains(componentName)) {
                    // - Widget data -
                    // Get if item is enabled, this time stored as a string (why?)
                    //boolean enable = item.getString("enable").equals("true");
                    // srl is the position, stored as a string for some reason
                    int srl = Integer.parseInt(item.getString("srl"));
                    // package name
                    //String str = item.getString("pkg");

                    // fix the int-to-string conversion problem
                    item.put("enable", item.getString("enable").equals("true")?1:0);

                    //Store component name for later
                    foundComponents.add(componentName);

                    // Save widget in the list
                    widgets_list.put(widgets_list.length(), item);
                    // Set max position
                    if (srl>max_position)
                        max_position = srl;


                    Logger.debug("Out widget found: " + item.toString());
                }
            }

        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }

        // Find custom widgets
        if(searchForCustomWidgets) {
            // Get the list of installed apps.
            final PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            // Loop through installed apps
            for (ApplicationInfo packageInfo : packages) {
                //Log.d(TAG, "Installed package :" + packageInfo.packageName);
                //Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
                //Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
                Bundle bundle = packageInfo.metaData;
                if (bundle == null) continue;
                try {
                    // boolean metaData = bundle.containsKey("com.huami.watch.launcher.springboard.PASSAGER_TARGET");
                    if ( bundle.containsKey("com.huami.watch.launcher.springboard.PASSAGER_TARGET") ) {
                        // Get component name to check list
                        int id = bundle.getInt("com.huami.watch.launcher.springboard.PASSAGER_TARGET");
                        Resources resources = context.getPackageManager().getResourcesForApplication(packageInfo.packageName);
                        String[] inArray = resources.getStringArray(id);
                        String[] strarray = inArray[0].split("/");
                        String componentName = strarray[strarray.length-1];

                        if (!foundComponents.contains(componentName)) {
                            JSONObject item = new JSONObject();

                            // - Widget data -
                            // Since it is not in the in/out lists, it is disabled = 0
                            item.put("enable", 0);
                            // Position, stored as a string for some reason (we are assigning a position now)
                            max_position++;
                            item.put("srl", max_position+"");
                            // Package name
                            item.put("pkg", packageInfo.packageName);
                            // Name (proper app name)
                            item.put("name", packageInfo.loadLabel(pm).toString());
                            // Class
                            item.put("cls", componentName);

                            // Save widget in the list
                            widgets_list.put(widgets_list.length(), item);

                            //Store component name for later
                            //foundComponents.add(componentName); // no need

                            Logger.debug("New widget found: " + item.toString());
                        }
                    } else
                        Logger.debug("App: " + packageInfo.packageName + " is not a widget");
                } catch (Exception e) {
                    Logger.error("Searching for widgets error: " + e);
                }
            }
        }

        Logger.debug("All widgets: " + widgets_list.toString());

        return widgets_list;
    }

    private static SpringboardSetting addSpringboardSetting(final Context context, final SpringboardItem springboardItem) {

        return new SpringboardSetting(null, getTitle(springboardItem.getPackageName(), context),
                formatComponentName(springboardItem.getClassName()), new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Logger.debug("WidgetsUtil addSpringboardSetting.onCheckedChanged b: " + b);
                if (!compoundButton.isPressed()) return;
                springboardItem.setEnabled(b);
                save(context);
            }
        }, springboardItem.isEnable(), springboardItem);

    }

    public static SpringboardWidgetAdapter getAdapter(final Context context) {
        return new SpringboardWidgetAdapter(context, settingList, new SpringboardWidgetAdapter.ChangeListener() {
            @Override
            public void onChange() {
                Logger.debug("WidgetsUtil getAdapter.onChange");
                checkSave(context);
            }
        });
    }

    //Get an app name from the package name
    private static String getTitle(String pkg, Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return String.valueOf(packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return context.getString(R.string.unknown);
        }
    }

    //Get last part of component name
    private static String formatComponentName(String componentName) {
        //Ignore if no . and just return component name
        if (!componentName.contains(".")) return componentName;
        //Return just the last section of component name
        return componentName.substring(componentName.lastIndexOf(".") + 1);
    }

    private static void checkSave(final Context context) {

        Logger.debug("WidgetsUtil checkSave");

        //Create timer if not already, for 2 seconds. Call save after completion
        if (countDownTimer == null) countDownTimer = new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                save(context);
            }
        };
        //Cancel and start timer. This means that this method must be called ONCE in 2 seconds before save will be called, it prevents save from being called more than once every 2 seconds (buffers moving)
        countDownTimer.cancel();
        countDownTimer.start();
    }

    private static void save(Context context) {
        MainService.setWasSpringboardSaved(true);
        save(context, true, false);
    }

    private static void save(Context context, boolean showToast, boolean saveLocal) {

        Logger.debug("WidgetsUtil save showToast: " + showToast);

        //Create a blank array
        JSONArray data = new JSONArray();
        //Hold position for use as srl
        int pos = 0;

        for (BaseSetting springboardSetting : settingList) {
            //Ignore if not a springboard setting
            if (!(springboardSetting instanceof SpringboardSetting)) continue;
            //Get item
            SpringboardItem springboardItem = ((SpringboardSetting) springboardSetting).getSpringboardItem();
            JSONObject item = new JSONObject();
            //Setup item with data from the item
            try {
                item.put("pkg", springboardItem.getPackageName());
                item.put("cls", springboardItem.getClassName());
                item.put("srl", String.valueOf(pos));
                item.put("enable", springboardItem.isEnable() ? "1" : "0");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Add to list and increment position
            data.put(item);
            pos++;
        }
        //Add to root object
        JSONObject root = new JSONObject();
        try {
            root.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Save setting
        if (saveLocal) {
            SettingsManager settingsManager = new SettingsManager(context);
            settingsManager.putString(Constants.PREF_SPRINGBOARD_ORDER, root.toString());
            Logger.debug("WidgetsUtil save PREF_SPRINGBOARD_ORDER: " + root.toString().substring(0, Math.min(root.toString().length(), 352)));
        } else {
            DeviceUtil.systemPutString(context, Constants.WIDGET_ORDER_IN, root.toString());
            Logger.debug("WidgetsUtil save widget_order_in: " + root.toString().substring(0, Math.min(root.toString().length(), 352)));
        }
        //Notify user
        if (showToast) {
            if (saveLocal)
                Toast.makeText(context, "List Saved", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show();
        }
    }

    private static void saveOfficialAppOrder(Context context, String order_in){
        Logger.debug("WidgetsUtil saveOfficialAppOrder: " + order_in.substring(0, Math.min(order_in.length(), 352)));
        SettingsManager settingsManager = new SettingsManager(context);
        settingsManager.putString(Constants.PREF_AMAZMOD_OFFICIAL_WIDGETS_ORDER, order_in);
    }
}
