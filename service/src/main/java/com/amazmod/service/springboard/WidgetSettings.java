package com.amazmod.service.springboard;

import android.content.Context;
import android.util.Log;

import com.amazmod.service.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

public class WidgetSettings {

    // Settings file name
    private File save_directory;
    private String settings_file_name;
    // Loaded data
    private JSONObject data;

    public static String APP_DELETED = "app_deleted";

    // Constructor
    public WidgetSettings(String tag, Context context) {
        this.data = null;

        // Get file info
        this.settings_file_name = tag + ".json";
        this.save_directory = context.getExternalFilesDir(null);
        Log.d(Constants.TAG, "WidgetSettings: " + context + " \\ " + this.save_directory);

        // Load settings
        this.load();
    }

    private void load() {
        File file = new File(this.save_directory, this.settings_file_name);
        if (file.exists()) {
            try {
                // Read text from file
                StringBuilder data = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    data.append(line);
                    data.append('\n');
                }
                reader.close();
                Log.d(Constants.TAG, "WidgetSettings load: " + data.toString());
                // Parse to json
                this.data = new JSONObject(data.toString());
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(Constants.TAG, "WidgetSettings load exception: " + e.toString());
                if (this.data == null) {
                    this.data = new JSONObject();
                }
            }
        } else {
            // No previous settings
            this.data = new JSONObject();
        }
    }

    public void save() {
        File file = new File(this.save_directory, this.settings_file_name);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(this.data.toString());
            Log.d(Constants.TAG, "WidgetSettings save: " + this.data.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Constants.TAG, "WidgetSettings save exception: " + e.toString());
        }
    }

    // Force reload of data
    public boolean reload() {
        load();
        return (!this.data.toString().isEmpty());
    }

    // Data Getter methods
    public String get(String key) {
        //this.load();
        return this.getString(key, "");
    }

    public String get(String key, String defvalue) {
        //this.load();
        return this.getString(key, defvalue);
    }

    public int get(String key, int defvalue) {
        //this.load();
        return this.getInt(key, defvalue);
    }

    public boolean get(String key, boolean defvalue) {
        //this.load();
        return this.getBoolean(key, defvalue);
    }

    public long get(String key, long defvalue) {
        //this.load();
        //Log.d(Constants.TAG, "WidgetSettings get lastChargeDate: " + key);
        return this.getLong(key, defvalue);
    }

    private String getString(String key, String defvalue) {
        String value;
        try {
            value = this.data.getString(key);
        } catch (JSONException e) {
            value = defvalue;
            Log.d(Constants.TAG, "WidgetSettings getString exception: " + e.toString());
        }
        return value;
    }

    private int getInt(String key, int defvalue) {
        int value;
        try {
            value = this.data.getInt(key);
            //Log.d(Constants.TAG, "WidgetSettings getInt: " + value);
        } catch (JSONException e) {
            value = defvalue;
            Log.d(Constants.TAG, "WidgetSettings getInt exception: " + e.toString());
        }
        return value;
    }

    private boolean getBoolean(String key, boolean defvalue) {
        boolean value;
        try {
            value = this.data.getBoolean(key);
        } catch (JSONException e) {
            value = defvalue;
            Log.d(Constants.TAG, "WidgetSettings getBoolean exception: " + e.toString());
        }
        return value;
    }

    private long getLong(String key, long defvalue) {
        long value;
        try {
            value = this.data.getLong(key);
            //Log.d(Constants.TAG, "WidgetSettings getLong: " + value);
        } catch (JSONException e) {
            value = defvalue;
            Log.d(Constants.TAG, "WidgetSettings getLong exception: " + e.toString());
        }
        return value;
    }

    public boolean set(String key, String value) {
        return this.setString(key, value);
    }

    public boolean set(String key, int value) {
        return this.setInt(key, value);
    }

    public boolean set(String key, boolean value) {
        return this.setBoolean(key, value);
    }

    public boolean set(String key, long value) {
        return this.setLong(key, value);
    }

    public JSONObject getData() {
        return this.data;
    }

    public boolean hasKey(String key) {
        return data.has(key);
    }

    private boolean setString(String key, String value) {
        // Check if it has the same value
        try {
            if (this.data.getString(key).equals(value)) {
                // Avoid useless writing
                return true;
            }
        } catch (JSONException e) {
            Log.d(Constants.TAG, "WidgetSettings setString exception: " + e.toString());
        }

        try {
            this.data.put(key, value);
            this.save();
        } catch (JSONException e) {
            Log.d(Constants.TAG, "WidgetSettings setString exception: " + e.toString());
            return false;
        }
        return true;
    }

    private boolean setInt(String key, int value) {
        // Check if it has the same value
        try {
            if (this.data.getInt(key) == value) {
                // Avoid useless writing
                return true;
            }
        } catch (JSONException e) {
            Log.d(Constants.TAG, "WidgetSettings setInt exception: " + e.toString());
        }

        try {
            this.data.put(key, value);
            this.save();
        } catch (JSONException e) {
            Log.d(Constants.TAG, "WidgetSettings setInt exception: " + e.toString());
            return false;
        }
        return true;
    }

    private boolean setBoolean(String key, boolean value) {
        // Check if it has the same value
        try {
            if (this.data.getBoolean(key) == value) {
                // Avoid useless writing
                return true;
            }
        } catch (JSONException e) {
            Log.d(Constants.TAG, "WidgetSettings setBoolean exception: " + e.toString());
        }

        try {
            this.data.put(key, value);
            this.save();
        } catch (JSONException e) {
            Log.d(Constants.TAG, "WidgetSettings setBoolean exception: " + e.toString());
            return false;
        }
        return true;
    }

    private boolean setLong(String key, long value) {
        // Check if it has the same value
        try {
            if (this.data.getLong(key) == value) {
                // Avoid useless writing
                return true;
            }
        } catch (JSONException e) {
            Log.d(Constants.TAG, "WidgetSettings setLong exception: " + e.toString());
        }

        try {
            this.data.put(key, value);
            this.save();
        } catch (JSONException e) {
            Log.d(Constants.TAG, "WidgetSettings setLong exception: " + e.toString());
            return false;
        }
        return true;
    }

}
