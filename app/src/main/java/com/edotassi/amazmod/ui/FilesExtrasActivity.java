package com.edotassi.amazmod.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity_Table;
import com.edotassi.amazmod.support.SilenceApplicationHelper;
import com.edotassi.amazmod.util.Permissions;
import com.google.gson.Gson;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class FilesExtrasActivity extends BaseAppCompatActivity {

    @BindView(R.id.activity_files_main_container)
    View filesMainContainer;
    @BindView(R.id.activity_files_progress)
    MaterialProgressBar materialProgressBar;
    @BindView(R.id.activity_files_permission)
    TextView filesPermission;
    @BindView(R.id.activity_files_date_last_backup)
    TextView filesDateLastBackup;
    @BindView(R.id.activity_files_file)
    TextView file;
    @BindView(R.id.activity_files_obs)
    TextView filesOBS;

    @BindView(R.id.activity_files_backup)
    Button backupButton;
    @BindView(R.id.activity_files_restore)
    Button restoreButton;

    private String ENABLED;
    private String DISABLED;
    private String NEVER;
    private String NONE;
    private String DATA;
    private String DOWNLOADS;


    private File saveDirectory;
    private final String bkpDirectory = File.separator + Constants.TAG;
    private String testDirectory;
    private String fileName;
    private boolean useDownloads = false;
    private boolean useFiles = false;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_extras);

        try {
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
            Logger.error("FilesExtrasActivity onCreate NullPointerException: " + exception.toString());
        }

        getSupportActionBar().setTitle(R.string.activity_files_extras);
        ButterKnife.bind(this);

        this.ENABLED = getResources().getString(R.string.enabled);
        this.DISABLED = getResources().getString(R.string.disabled);
        this.NEVER = getResources().getString(R.string.never);
        this.NONE = getResources().getString(R.string.none);
        this.DATA = getResources().getString(R.string.data);
        this.DOWNLOADS = getResources().getString(R.string.downloads);

        this.fileName = Constants.TAG + "_prefs.bkp";
        testDirectory = File.separator + "test" + System.currentTimeMillis();

        if (!Permissions.hasPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar
                    .make(filesMainContainer, R.string.no_storage_permission, Snackbar.LENGTH_LONG)
                    .setAction(R.string.grant, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openPermissions();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateData();
    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_files_backup)
    public void backup() {
        save();
    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_files_restore)
    public void restore() {
        load();

    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_files_permission)
    public void openPermissions() {
        if (!Permissions.hasPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        updateData();
    }

    @SuppressLint("CheckResult")
    private void updateData() {

        String obsText = "";

        materialProgressBar.setVisibility(View.VISIBLE);
        filesMainContainer.setVisibility(View.GONE);

        if (Permissions.hasPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            filesPermission.setText(this.ENABLED.toUpperCase());
            filesPermission.setTextColor(getResources().getColor(R.color.colorCharging));
        } else {
            filesPermission.setText(this.DISABLED.toUpperCase());
            filesPermission.setTextColor(getResources().getColor(R.color.colorAccent));
        }

        final String timeLastSave = Prefs.getString(Constants.PREF_TIME_LAST_SAVE, "null");
        if (timeLastSave.equals("null")) {
            filesDateLastBackup.setText(this.NEVER.toUpperCase());
        } else {
            filesDateLastBackup.setText(timeLastSave);
        }

        if (checkBackupFile()) {
            if (useFiles) {
                file.setText(this.DATA.toUpperCase());
                file.setTextColor(getResources().getColor(R.color.colorCharging));
            } else if (useDownloads) {
                file.setText(this.DOWNLOADS.toUpperCase());
                file.setTextColor(getResources().getColor(R.color.colorCharging));
            }

        } else {
            file.setText(this.NONE.toUpperCase());
            file.setTextColor(getResources().getColor(R.color.colorAccent));
        }

        if (checkWriteDirectory()) {
            if (useFiles) {
                obsText = getResources().getString(R.string.activity_files_backup_obs) + "\n" + this.saveDirectory + "/" + this.fileName;
            } else if (useDownloads) {
                obsText = getResources().getString(R.string.activity_files_backup_downloads) + "\n" + this.saveDirectory + "/" + this.fileName;
            }
        } else {
            obsText = getResources().getString(R.string.activity_files_backup_error);
        }

        filesOBS.setText(obsText);

        materialProgressBar.setVisibility(View.GONE);
        filesMainContainer.setVisibility(View.VISIBLE);

    }

    private void save() {

        Date lastDate = new Date();
        String time = DateFormat.getTimeInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);
        String date = DateFormat.getDateInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(lastDate);

        Prefs.putString(Constants.PREF_TIME_LAST_SAVE, date + " " + time);

        final String packageName = this.getPackageName();

        if (checkWriteDirectory()) {

            boolean success = true;
            try {
                if (!this.saveDirectory.exists()) {
                    success = this.saveDirectory.mkdir();
                }
                if (success) {
                    saveAppsDbToPrefs(); //Restore selected apps to Prefs
                    File data = Environment.getDataDirectory();
                    String currentDBPath = "/data/" + packageName + "/shared_prefs/" + packageName + "_preferences.xml";
                    File currentDB = new File(data, currentDBPath);
                    File backupDB = new File(this.saveDirectory, this.fileName);
                    try {
                        FileChannel source = new FileInputStream(currentDB).getChannel();
                        FileChannel destination = new FileOutputStream(backupDB).getChannel();
                        destination.transferFrom(source, 0, source.size());
                        source.close();
                        destination.close();
                        Toast.makeText(this, getResources().getString(R.string.activity_files_backup_ok), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, getResources().getString(R.string.activity_files_backup_failed), Toast.LENGTH_SHORT).show();
                    }
                    eraseAppsPrefs(); //Overwrite Prefs after backup
                }
            } catch (Exception e) {
                Toast.makeText(this, getResources().getString(R.string.activity_files_file_error), Toast.LENGTH_SHORT).show();
                Logger.error("FilesExtrasActivity save exception: " + e.toString());
            }
            updateData();
        } else {
            Toast.makeText(this, getResources().getString(R.string.activity_files_no_write_permission), Toast.LENGTH_SHORT).show();
        }

    }

    private void load() {

        final String packageName = this.getPackageName();

        boolean success = false;
        if (checkBackupFile()) {
            File backupDB = new File(this.saveDirectory, this.fileName);
            try {
                if (backupDB.exists()) {
                    File data = Environment.getDataDirectory();
                    String currentDBPath = "/data/" + packageName + "/shared_prefs/" + packageName + "_preferences.xml";
                    File currentDB = new File(data, currentDBPath);
                    try {
                        FileChannel source = new FileInputStream(backupDB).getChannel();
                        FileChannel destination = new FileOutputStream(currentDB).getChannel();
                        destination.transferFrom(source, 0, source.size());
                        source.close();
                        destination.close();
                        success = true;
                        Toast.makeText(this, getResources().getString(R.string.activity_files_restore_ok), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        success = false;
                        Toast.makeText(this, getResources().getString(R.string.activity_files_file_error), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                success = false;
                Toast.makeText(this, getResources().getString(R.string.activity_files_restore_failed), Toast.LENGTH_SHORT).show();
                Logger.error("FilesExtrasActivity load exception: " + e.toString());
            }
        } else {
            success = false;
            Toast.makeText(this, getResources().getString(R.string.activity_files_backup_not_found), Toast.LENGTH_SHORT).show();
        }

        if (success) {
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    try {
                        System.exit(2);
                        Logger.info("FilesExtrasActivity load delayed System.exit()");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Logger.error("FilesExtrasActivity load exception: " + e.toString());
                    }
                }
            }, 2000);
        }
    }

    private boolean checkWriteDirectory() {
        this.useDownloads = false;
        this.useFiles = false;
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + testDirectory);
        boolean success = true;
        if (!file.exists()) {
            success = file.mkdir();
        }
        if (success) {
            useDownloads = true;
            this.saveDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + bkpDirectory);
            file.delete();
        } else {
            file = new File(this.getExternalFilesDir(null) + testDirectory);
            success = true;
            if (!file.exists()) {
                success = file.mkdir();
            }
            if (success) {
                useFiles = true;
                this.saveDirectory = this.getExternalFilesDir(null);
                file.delete();
            }
        }
        return useDownloads || useFiles;
    }

    private boolean checkBackupFile() {
        this.useDownloads = false;
        this.useFiles = false;
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + bkpDirectory, fileName);
        if (file.exists()) {
            this.useDownloads = true;
            this.saveDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + bkpDirectory);
        } else {
            file = new File(this.getExternalFilesDir(null) + File.separator, fileName);
            if (file.exists()) {
                this.useFiles = true;
                this.saveDirectory = this.getExternalFilesDir(null);
            }
        }
        return useFiles || useDownloads;
    }

    public static void checkApps(Context context) {

        //Log.d(Constants.TAG,"FilesExtrasActivity checkApps");

        Gson gson = new Gson();
        List<String> packagesList = new ArrayList<>(Arrays.asList(gson
                .fromJson(Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]"), String[].class)));

        if (packagesList.size() > 0)
            migrateNotificationPrefsFromJSON();
        //checkAppsJson(context, packagesList);

        List<NotificationPreferencesEntity> apps = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .queryList();

        if (apps.size() > 0)
            checkAppsSql(context, apps);

    }

    public static void checkAppsJson(Context context, List<String> packagesList) {

        //Log.d(Constants.TAG,"FilesExtrasActivity checkAppsJson packagesList: " + packagesList.toString());

        List<String> installedApps = getInstalledPackagesNames(context);

        List<String> dummy = new ArrayList<>();

        for (String p : packagesList) {

            if (Collections.binarySearch(installedApps, p) < 0) {
                dummy.add(p);
                Logger.info("FilesExtrasActivity checkAppsJson removed app: " + p);
            }
        }

        if (dummy.size() > 0) packagesList.removeAll(dummy);

        String pref = new Gson().toJson(packagesList);

        Prefs.putString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, pref);

    }

    private static void checkAppsSql(Context context, List<NotificationPreferencesEntity> apps) {

        //Log.d(Constants.TAG,"FilesExtrasActivity checkAppsSql");

        List<String> installedApps = getInstalledPackagesNames(context);

        for (NotificationPreferencesEntity p : apps) {

            if (Collections.binarySearch(installedApps, p.getPackageName()) < 0) {
                SQLite
                        .delete()
                        .from(NotificationPreferencesEntity.class)
                        .where(NotificationPreferencesEntity_Table.packageName.eq(p.getPackageName()))
                        .query();

                Logger.info("FilesExtrasActivity checkAppsSql removed app: " + p.getPackageName());
            }
        }

    }

    // TODO: 06/12/2018 remove this in the future
    //Temporary Migration function (old users will have its selected apps migrated from JSON to SQLITE)
    public static void migrateNotificationPrefsFromJSON() {

        //Log.d(Constants.TAG,"FilesExtrasActivity migrateNotificationPrefsFromJSON");

        String packagesJson = Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");

        if (!packagesJson.equals("[]")) {

            String[] packagesList = new Gson().fromJson(packagesJson, String[].class);

            for (String p : packagesList) {
                SilenceApplicationHelper.enablePackage(p);
            }

            Prefs.putString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");
            Logger.info("FilesExtrasActivity migrateNotificationPrefsFromJSON finished");
        }

        String filtersJson = Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES_FILTERS, "[]");
        Logger.debug("FilesExtrasActivity migrateNotificationPrefsFromJSON filters: " + filtersJson);
        if (!filtersJson.equals("[]")) {
            Map<String, String> packagesfilters = new Gson().fromJson(filtersJson, Map.class);

            for (Map.Entry<String, String> pair : packagesfilters.entrySet()) {
                NotificationPreferencesEntity app =
                        SQLite
                                .select()
                                .from(NotificationPreferencesEntity.class)
                                .where(NotificationPreferencesEntity_Table.packageName.eq(pair.getKey()))
                                .querySingle();
                app.setFilter(pair.getValue());
                app.setWhitelist(false);
                FlowManager
                        .getModelAdapter(NotificationPreferencesEntity.class)
                        .update(app);
            }
        }
    }

    private static List<String> getInstalledPackagesNames(Context context) {

        Logger.debug("FilesExtrasActivity getInstalledPackagesNames");

        List<PackageInfo> packagesInstalled = context.getPackageManager().getInstalledPackages(0);
        List<String> packagesInstalledNames = new ArrayList<>();

        for (PackageInfo p : packagesInstalled) {
            packagesInstalledNames.add(p.packageName);
        }

        Collections.sort(packagesInstalledNames);

        return packagesInstalledNames;

    }

    public static void saveAppsDbToPrefs() {

        Logger.debug("FilesExtrasActivity saveAppsDbToPrefs");

        List<NotificationPreferencesEntity> apps = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .queryList();

        if (apps.size() > 0) {

            List<String> dummy = new ArrayList<>();
            Map<String, String> filters = new HashMap<String, String>();

            for (NotificationPreferencesEntity p : apps) {
                dummy.add(p.getPackageName());
                filters.put(p.getPackageName(), p.getFilter());
                //Log.d(Constants.TAG,"FilesExtrasActivity saveAppsDbToPrefs added: " + p.getPackageName());
            }

            if (dummy.size() > 0) {
                String pref = new Gson().toJson(dummy);
                Prefs.putString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, pref);
                String pref_filter = new Gson().toJson(filters);
                Prefs.putString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES_FILTERS, pref_filter);
                Prefs.edit().commit();
            }
        }
    }

    public static void eraseAppsPrefs() {
        Logger.debug("FilesExtrasActivity eraseAppsPrefs");

        Prefs.putString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");
        Prefs.putString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES_FILTERS, "[]");
        Prefs.edit().commit();

    }

    //file
    protected void requestPermission()
    {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        {
            Toast.makeText(FilesExtrasActivity.this, "Read External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    //file
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case 100:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    updateData();
                } else
                {
                    Logger.error("Permission Denied, You cannot use local drive .");
                    Toast.makeText(FilesExtrasActivity.this,"Permission Denied",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

}