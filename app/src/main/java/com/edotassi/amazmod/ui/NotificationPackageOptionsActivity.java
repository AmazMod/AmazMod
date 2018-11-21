package com.edotassi.amazmod.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;

import butterknife.OnCheckedChanged;

public class NotificationPackageOptionsActivity extends AppCompatActivity {

    NotificationPreferencesEntity app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_package_options);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Intent intent = getIntent();
        String pkg = intent.getStringExtra("app");
        Boolean enabled = intent.getBooleanExtra("enabled",false);

        TextView appName = findViewById(R.id.appinfo_appname);
        TextView appPackageName = findViewById(R.id.appinfo_package_name);
        TextView appVersion = findViewById(R.id.appinfo_version);
        ImageView appIcon = findViewById(R.id.appinfo_icon);
        Switch appEnabled = findViewById(R.id.appinfo_enabled);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(pkg,0);
            appName.setText(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
            getSupportActionBar().setTitle("App Options");
            appPackageName.setText(packageInfo.packageName);
            appVersion.setText(packageInfo.versionName);
            appIcon.setImageDrawable(packageInfo.applicationInfo.loadIcon(getPackageManager()));
            appEnabled.setChecked(enabled);

        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this,"Package " + pkg + "not found",Toast.LENGTH_SHORT);
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @OnCheckedChanged(R.id.appinfo_enabled)
    public void onSwitchChanged(Switch switchWidget, boolean checked) {
        if (checked){

        }else{
            deleteCommand();
        }
    }


    public void deleteCommand(NotificationPreferencesEntity app) {
        SQLite
                .delete()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(app.getPackageName()))
                .query();
    }

    private void loadApp() {
        List<NotificationPreferencesEntity> apps = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(app.getPackageName()))
                .queryList();

//        commandHistoryAdapter.clear();
  //      commandHistoryAdapter.addAll(commandHistoryValues);
    //    commandHistoryAdapter.notifyDataSetChanged();
    }

}
