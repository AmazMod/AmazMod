package com.amazmod.service.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazmod.service.Constants;
import com.amazmod.service.R;

import org.tinylog.Logger;

import static android.view.View.inflate;

public class ScreenSettingsActivity extends Activity {

    TextView header, message, densityLabel, fontSizeLabel, invertedLabel;
    Button save;
    Spinner densitySpinner, fontSpinner, invertedSpinner;

    private Context mContext;
    private Handler mHandler;
    private Vibrator mVibrator;

    private int densityChosen, fontSizeChosen, invertedChoosen;
    private int initialFontSize = 99, initialDensity = 99;

    private String[] densities = {"Normal", "Low", "High", "Unknown"};
    private String[] fontSizes = {"Small", "Normal", "Big", "Huge"};
    private String[] inverted = {"No", "Yes"};

    private String[] labels = { "(238)", "(248)", "(148)", "",
                                "(0.90f)", "(1.00f)", "(1.18f)", "(1.30f)",
                                "(Off)", "(On)"};

    private static final String[] DENSITY_COMMANDS = {  "wm density reset; wm size reset;exit",
                                                        "wm density 258;exit",
                                                        "wm density 148;exit"};

    private static final String KILL_LAUNCHER = "am force-stop com.huami.watch.launcher;exit";
    private static final String SET_INVERTED = "settings put secure accessibility_display_inversion_enabled %s;exit";
    private static final String SYSTEM_HIGH_CONTRAST = "high_contrast";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("ScreenSettingsActivity onCreate");

        this.mContext = this;
        setContentView(R.layout.activity_screen_settings);

        header = findViewById(R.id.activity_screen_settings_header);
        message = findViewById(R.id.activity_screen_settings_message);
        densityLabel = findViewById(R.id.activity_screen_settings_density_label);
        fontSizeLabel = findViewById(R.id.activity_screen_settings_font_size_label);
        invertedLabel = findViewById(R.id.activity_screen_settings_inversion_label);

        save = findViewById(R.id.activity_screen_settings_button_save);

        densitySpinner = findViewById(R.id.activity_screen_settings_spinner_density);
        fontSpinner = findViewById(R.id.activity_screen_settings_spinner_font_size);
        invertedSpinner = findViewById(R.id.activity_screen_settings_spinner_inversion);

        setLabels();

        setAdapter(densitySpinner, densities, 0);
        setAdapter(fontSpinner, fontSizes, 1);
        setAdapter(invertedSpinner, inverted, 2);

        getCurrentDensity();
        getCurrentFontScale();
        getCurrentInvertedMode();

        updateContent();

    }

    @Override
    public void onResume() {
        super.onResume();

        Logger.info("ScreenSettingsActivity onResume");
        updateContent();
    }

    private void setLabels() {

        header.setText("Screen Settings");
        message.setText("Changing Font Size will restart watch");
        densityLabel.setText("Density:");
        fontSizeLabel.setText("Font:");
        invertedLabel.setText("Inverted:");
        save.setText("Save");

        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18.0f);
        message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f);
        densityLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
        fontSizeLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
        invertedLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
        save.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);

    }

    private void setAdapter(Spinner spinner, final String[] items,final  int mode) {

        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(mContext, R.layout.wearable_spinner_item, items) {
            @Override
            public boolean isEnabled(int position) {
                if (mode == 0)
                    return position != 3;
                else
                    return true;
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View mView = super.getView(position, convertView, parent);
                TextView mTextView = (TextView) mView;
                mTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
                mTextView.setText(String.format("%s %s", items[position], labels[mode* 4 + position]));
                return mView;
            }

            @Override
            public View getDropDownView(int position, View convertView,
                                        @NonNull ViewGroup parent) {
                View mView = super.getDropDownView(position, convertView, parent);
                TextView mTextView = (TextView) mView;
                mTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
                mTextView.setText(String.format("%s %s", items[position], labels[mode* 4 + position]));
                if (mode == 0 && position == 3) {
                    mTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }
                return mView;
            }
        };

        mAdapter.setDropDownViewResource(R.layout.wearable_spinner_dropdown_item);

        spinner.setAdapter(mAdapter);

    }

    private void updateContent() {

        densitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
                ((TextView) parent.getChildAt(0)).setText(densities[position]);
                Logger.debug("ScreenSettingsActivity udpateContent Density: " + parent.getItemAtPosition(position));
                densityChosen = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Desfault method
            }
        });

        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
                ((TextView) parent.getChildAt(0)).setText(fontSizes[position]);
                Logger.debug("ScreenSettingsActivity updateContent Font: " + parent.getItemAtPosition(position));
                fontSizeChosen = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Default method
            }
        });

        invertedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
                ((TextView) parent.getChildAt(0)).setText(inverted[position]);
                Logger.debug("ScreenSettingsActivity updateContent Inverted: " + parent.getItemAtPosition(position));
                if (invertedChoosen != position) {
                    invertedChoosen = position;
                    runCommand(String.format(SET_INVERTED, String.valueOf(invertedChoosen)));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Default method
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveScreenSettings();
            }
        });


    }

    private void runCommand(String command) {

        Logger.debug("ScreenSettingsActivity runCommand: " + command);
        if (!command.isEmpty()) {
            try {
                Runtime.getRuntime().exec(new String[]{"adb", "shell", command},
                        null, Environment.getExternalStorageDirectory());
            } catch (Exception e) {
                Logger.error(e,"ScreenSettingsActivity runCommand exception: " + e.toString());
            }
        }
    }

    private boolean isInversionModeEnabled() {
        boolean isInversionEnabled =  false;
        int accessibilityEnabled;

        try {
            accessibilityEnabled = Settings.Secure.getInt(this.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);
        } catch (Exception e) {
            Logger.error("ScreenSettingsActivity isInversionModeEnabled SettingNotFoundException: " + e.getMessage());
            accessibilityEnabled = Settings.System.getInt(getContentResolver(), SYSTEM_HIGH_CONTRAST, 0);
        }

        if (accessibilityEnabled == 1) {
            Logger.debug("ScreenSettingsActivity isInversionModeEnabled: true");
            isInversionEnabled = true;
        } else {
            Logger.debug("ScreenSettingsActivity isInversionModeEnabled: false");
        }

        return isInversionEnabled;
    }

    private void getCurrentFontScale() {

        final float fontScale = Settings.System.getFloat(getBaseContext().getContentResolver(),
                Settings.System.FONT_SCALE, 1.0f);

        Logger.debug("ScreenSettingsActivity getCurrentFontScale: " + String.valueOf(fontScale));

        if ( fontScale == 0.9f )
            initialFontSize = 0;
        else if ( fontScale == 1.0f )
            initialFontSize = 1;
        else if ( fontScale == 1.18f )
            initialFontSize = 2;
        else if ( fontScale == 1.30f )
            initialFontSize = 3;

        if (initialFontSize != 99) {
            fontSpinner.setSelection(initialFontSize);
            fontSizes[initialFontSize] = fontSizes[initialFontSize] + " *";
        }
    }

    private void saveFontScale() {

        //Configuration config = new Configuration();
        mVibrator = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        switch (fontSizeChosen) {

            case 0:
                //config.fontScale = 0.9f;
                Settings.System.putFloat(getBaseContext().getContentResolver(),
                        Settings.System.FONT_SCALE,0.9f);
                break;

            case 1:
                //config.fontScale = 1.0f;
                Settings.System.putFloat(getBaseContext().getContentResolver(),
                        Settings.System.FONT_SCALE,1.0f);
                break;

            case 2:
                //config.fontScale = 1.18f;
                Settings.System.putFloat(getBaseContext().getContentResolver(),
                        Settings.System.FONT_SCALE,1.18f);
                break;

            case 3:
                //config.fontScale = 1.30f;
                Settings.System.putFloat(getBaseContext().getContentResolver(),
                        Settings.System.FONT_SCALE,1.30f);
                break;

            default:
                Logger.error("ScreenSettingsActivity saveFontScale error fontSizeChosen: " + fontSizeChosen);

        }

        mVibrator.vibrate(50);
        //getResources().getConfiguration().setTo(config);
        Toast.makeText(mContext, "Watch will restart\nto apply changes ;)", Toast.LENGTH_LONG).show();

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                try {
                    Runtime.getRuntime().exec("reboot");
                } catch (Exception e) {
                    Logger.error("ScreenSettingsActivity saveFontScale exception: " + e.toString());
                }
            }
        }, 3000);

    }

    private void getCurrentDensity() {

        //StringBuilder outputLog = new StringBuilder();
        String outputLog = null;
        int returnValue = 0;

        /* Disabled because it sometimes hangs (why?)
        *
        final String[] args = new String[]{"adb", "shell", "wm density"};
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {

            Process process = processBuilder.start();
            String line;

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while ((line = bufferedReader.readLine()) != null) {
                    //outputLog.append(line).append("\n");
                    outputLog = line;
                    Log.d(Constants.TAG, "ScreenSettingsActivity getCurrentDensity line: " + outputLog);
                }
                returnValue = process.waitFor();
            }

        } catch (Exception ex) {
            Log.e(Constants.TAG, ex.getMessage(), ex);
        }
        *
        */

        //final String result = outputLog.toString();
        final String result = outputLog;

        Logger.debug("ScreenSettingsActivity getCurrentDensity returnValue: " + returnValue + " | result: " + result);

        if (result != null) {
            if (result.contains("238"))
                initialDensity = 0;
            else if (result.contains("258"))
                initialDensity = 1;
            else if (result.contains("148"))
                initialDensity = 2;
            else
                initialDensity = 3;
        } else
            initialDensity = 3;

        densities[initialDensity] = densities[initialDensity] + " *";
        densitySpinner.setSelection(initialDensity);

    }

    private void saveDensity() {

        runCommand(KILL_LAUNCHER);
        runCommand(DENSITY_COMMANDS[densityChosen]);
        SystemClock.sleep(1000);

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.huami.watch.launcher");
        if (launchIntent != null) {
            startActivity(launchIntent);
        }

    }

    private void getCurrentInvertedMode() {

        if (isInversionModeEnabled()) {
            invertedChoosen = 1;
        } else {
            invertedChoosen = 0;
        }

        inverted[invertedChoosen] = inverted[invertedChoosen] + " *";
        invertedSpinner.setSelection(invertedChoosen);

    }

    private void saveScreenSettings() {

        if ((initialDensity != densityChosen) && (densityChosen != 3))
            saveDensity();

        if (initialFontSize != fontSizeChosen)
            saveFontScale();

        finish();

    }

}