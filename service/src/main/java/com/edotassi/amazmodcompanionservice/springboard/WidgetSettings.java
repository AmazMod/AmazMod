package com.edotassi.amazmodcompanionservice.springboard;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

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

        // Constructor
        public WidgetSettings (String tag, Context context){
            this.data = null;

            // Get file info
            this.settings_file_name = tag + ".json";
            this.save_directory = context.getExternalFilesDir(null);
            Log.d("AmazMod", "Settings lastChargeDate : " + this.save_directory);

            // Load settings
            this.load();
        }

        private void load() {
            File file = new File (this.save_directory, this.settings_file_name);
            if (file.exists ()) {
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
                    Log.d("AmazMod", "Settings load lastChargeDate: " + data.toString());
                    // Parse to json
                    this.data = new JSONObject(data.toString());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.d("AmazMod", "Settings load exception lastChargeDate : " + e.toString());
                    if (this.data == null) {
                        this.data = new JSONObject();
                    }
                }
            }
            else {
                // No previous settings
                this.data = new JSONObject();
            }
        }

        private void save() {
            File file = new File (this.save_directory, this.settings_file_name);
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(this.data.toString());
                Log.d("AmazMod", "Settings save lastChargeDate : " + this.data.toString());
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("AmazMod", "Settings save exception lastChargeDate : " + e.toString());
            }
        }

        // Data Getter methods
        public String get(String key) {
            return this.getString (key, "");
        }
        public String get(String key, String defvalue) {
            return this.getString (key, defvalue);
        }
        public int get(String key, int defvalue) {
            return this.getInt (key, defvalue);
        }
        public boolean get(String key, boolean defvalue) {
            return this.getBoolean (key, defvalue);
        }
        public long get (String key, long defvalue) {
            return this.getLong (key,defvalue);
        }

        public String getString (String key, String defvalue) {
            String value;
            try {
                value = this.data.getString(key);
            }
            catch(JSONException e) {
                value = defvalue;
            }
            return value;
        }
        public int getInt (String key, int defvalue) {
            int value;
            try {
                value = this.data.getInt(key);
            }
            catch(JSONException e) {
                value = defvalue;
            }
            return value;
        }
        public boolean getBoolean (String key, boolean defvalue) {
            boolean value;
            try {
                value = this.data.getBoolean(key);
            }
            catch(JSONException e) {
                value = defvalue;
            }
            return value;
        }

    public long getLong (String key, long defvalue) {
        long value;
        try {
            value = this.data.getLong(key);
        }
        catch(JSONException e) {
            value = defvalue;
        }
        return value;
    }


        public boolean set (String key, String value) {
            return this.setString(key, value);
        }
        public boolean set (String key, int value) {
            return this.setInt(key, value);
        }
        public boolean set (String key, boolean value) {
            return this.setBoolean(key, value);
        }
    public boolean set (String key, long value) {
        return this.setLong(key, value);
    }
        public boolean setString (String key, String value) {
            // Check if it has the same value
            try {
                if (this.data.getString(key).equals(value)){
                    // Avoid useless writing
                    return true;
                }
            }
            catch(JSONException e) {}

            try {
                this.data.put(key, value);
                this.save();
            }
            catch(JSONException e) {
                return false;
            }
            return true;
        }
        public boolean setInt (String key, int value) {
            // Check if it has the same value
            try {
                if (this.data.getInt(key) == value){
                    // Avoid useless writing
                    return true;
                }
            }
            catch(JSONException e) {}

            try {
                this.data.put(key, value);
                this.save();
            }
            catch(JSONException e) {
                return false;
            }
            return true;
        }
        public boolean setBoolean (String key, boolean value) {
            // Check if it has the same value
            try {
                if (this.data.getBoolean(key) == value){
                    // Avoid useless writing
                    return true;
                }
            }
            catch(JSONException e) {}

            try {
                this.data.put(key, value);
                this.save();
            }
            catch(JSONException e) {
                return false;
            }
            return true;
        }

    public boolean setLong (String key, long value) {
        // Check if it has the same value
        try {
            if (this.data.getLong(key) == value){
                // Avoid useless writing
                return true;
            }
        }
        catch(JSONException e) {}

        try {
            this.data.put(key, value);
            this.save();
        }
        catch(JSONException e) {
            return false;
        }
        return true;
    }

}
