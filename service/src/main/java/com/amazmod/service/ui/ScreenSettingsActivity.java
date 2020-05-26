package com.amazmod.service.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.amazmod.service.R;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.ExecCommand;
import com.amazmod.service.util.SystemProperties;

import org.tinylog.Logger;

public class ScreenSettingsActivity extends Activity {

    TextView header, message, densityLabel, fontSizeLabel, invertedLabel;
    Button save;
    Spinner densitySpinner, fontSpinner, invertedSpinner;

    private Context mContext;
    private Vibrator mVibrator;

    private int densityChosen, fontSizeChosen, invertedChoosen;
    private int initialFontSize = 99, initialDensity = 99;

    /* Now use string from array.xml
    private String[] densities = {"Normal", "Low", "High", "Unknown"};
    private String[] fontSizes = {"Small", "Normal", "Big", "Huge"};
    private String[] inverted = {"No", "Yes"};
    */
    private String[] labels = { "(238)", "(248)", "(148)", "",
                                "(0.90f)", "(1.00f)", "(1.18f)", "(1.30f)",
                                "(Off)", "(On)"};

    private static final String[] DENSITY_COMMANDS = {  "wm density reset; wm size reset",
                                                        "wm density 258",
                                                        "wm density 148"};

    private static final String KILL_LAUNCHER = "am force-stop com.huami.watch.launcher";
    private static final String SET_INVERTED = "settings put secure accessibility_display_inversion_enabled %s";
    private static final String SYSTEM_HIGH_CONTRAST = "high_contrast";

    private static String defaultDensity;

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

        if (SystemProperties.isVerge()) {
            defaultDensity = "240";
            labels[0] = "(240)";
        } else
            defaultDensity = "238";

        setAdapter(densitySpinner, getResources().getStringArray(R.array.densities), 0);
        setAdapter(fontSpinner, getResources().getStringArray(R.array.fontSizes), 1);
        setAdapter(invertedSpinner, getResources().getStringArray(R.array.inverted), 2);

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

        header.setText(getString(R.string.screen_settings));
        message.setText(getString(R.string.screen_setting_summary));
        densityLabel.setText(getString(R.string.density));
        fontSizeLabel.setText(getString(R.string.font));
        invertedLabel.setText(getString(R.string.inverted));
        save.setText(getString(R.string.save));

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
                ((TextView) parent.getChildAt(0)).setText(getResources().getStringArray(R.array.densities)[position]);
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
                ((TextView) parent.getChildAt(0)).setText(getResources().getStringArray(R.array.fontSizes)[position]);
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
                ((TextView) parent.getChildAt(0)).setText(getResources().getStringArray(R.array.inverted)[position]);
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
            new ExecCommand(ExecCommand.ADB, String.format("adb shell %s", command));
        }
    }

    private boolean isInversionModeEnabled() {
        boolean isInversionEnabled =  false;
        int accessibilityEnabled;

        try {
            accessibilityEnabled = Settings.Secure.getInt(this.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);
        } catch (Exception e) {
            Logger.error("ScreenSettingsActivity isInversionModeEnabled SettingNotFoundException: {}", e.getMessage());
            accessibilityEnabled = DeviceUtil.systemGetInt(mContext, SYSTEM_HIGH_CONTRAST, 0);
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

        final float fontScale = DeviceUtil.systemGetFloat(getBaseContext(), Settings.System.FONT_SCALE, 1.0f);

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
            getResources().getStringArray(R.array.fontSizes)[initialFontSize] = getResources().getStringArray(R.array.fontSizes)[initialFontSize] + " *";
        }
    }

    private void saveFontScale() {

        //Configuration config = new Configuration();
        mVibrator = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        switch (fontSizeChosen) {

            case 0:
                //config.fontScale = 0.9f;
                DeviceUtil.systemPutFloat(getBaseContext(),
                        Settings.System.FONT_SCALE,0.9f);
                break;

            case 1:
                //config.fontScale = 1.0f;
                DeviceUtil.systemPutFloat(getBaseContext(),
                        Settings.System.FONT_SCALE,1.0f);
                break;

            case 2:
                //config.fontScale = 1.18f;
                DeviceUtil.systemPutFloat(getBaseContext(),
                        Settings.System.FONT_SCALE,1.18f);
                break;

            case 3:
                //config.fontScale = 1.30f;
                DeviceUtil.systemPutFloat(getBaseContext(),
                        Settings.System.FONT_SCALE,1.30f);
                break;

            default:
                Logger.error("ScreenSettingsActivity saveFontScale error fontSizeChosen: " + fontSizeChosen);

        }

        mVibrator.vibrate(50);
        //getResources().getConfiguration().setTo(config);
        Toast.makeText(mContext, getString(R.string.restart_needed), Toast.LENGTH_LONG).show();

        new Handler().postDelayed(new Runnable() {
            public void run() {
                new ExecCommand("reboot");
            }
        }, 3000);

    }

    private void getCurrentDensity() {

        final ExecCommand execCommand = new ExecCommand("wm density");
        final String result = execCommand.getOutput();

        Logger.debug("ScreenSettingsActivity getCurrentDensity result: {} | error: {}", result, execCommand.getError());

        if (result != null) {
            if (result.contains(defaultDensity) && !result.toLowerCase().contains("override"))
                initialDensity = 0;
            else if (result.contains("258") && result.toLowerCase().contains("override"))
                initialDensity = 1;
            else if (result.contains("148") && result.toLowerCase().contains("override"))
                initialDensity = 2;
            else
                initialDensity = 3;
        } else
            initialDensity = 3;

        getResources().getStringArray(R.array.densities)[initialDensity] = getResources().getStringArray(R.array.densities)[initialDensity] + " *";
        densitySpinner.setSelection(initialDensity);

    }

    private void saveDensity() {

        runCommand(KILL_LAUNCHER + ";" + DENSITY_COMMANDS[densityChosen]);
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

        getResources().getStringArray(R.array.inverted)[invertedChoosen] = getResources().getStringArray(R.array.inverted)[invertedChoosen] + " *";
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