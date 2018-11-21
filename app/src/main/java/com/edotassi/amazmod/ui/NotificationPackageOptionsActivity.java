package com.edotassi.amazmod.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity_Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NotificationPackageOptionsActivity extends AppCompatActivity {

    NotificationPreferencesEntity app;
    PackageInfo packageInfo;

    @BindView(R.id.edittext_filter)
    EditText filter_edittext;

    @BindView(R.id.activity_notifopts_appinfo_appname)
    TextView appname_edittext;

    @BindView(R.id.appinfo_package_name)
    TextView appinfoPackageNameEditText;

    @BindView(R.id.appinfo_version)
    TextView appVersion;

    @BindView(R.id.appinfo_icon)
    ImageView appIcon;

    @BindView(R.id.silenced_until)
    EditText silenced_until;

    @BindView(R.id.current_timestamp)
    TextView current_timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_package_options);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        String packageName = intent.getStringExtra("app");

        app = loadApp(packageName);
        if (app == null) {
            Toast.makeText(this, "Package " + packageName + "is not enabled", Toast.LENGTH_SHORT);
            finish();
        } else {
            try {
                packageInfo = getPackageManager().getPackageInfo(packageName, 0);
                appname_edittext.setText(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString());
                getSupportActionBar().setTitle("App Options");
                appinfoPackageNameEditText.setText(packageInfo.packageName);
                appVersion.setText(packageInfo.versionName);
                appIcon.setImageDrawable(packageInfo.applicationInfo.loadIcon(getPackageManager()));
                filter_edittext.setText(app.getFilter());
                silenced_until.setText(String.valueOf(app.getSilenceUntil()));
                Long tsLong = System.currentTimeMillis()/1000;
                current_timestamp.setText(tsLong.toString());
            } catch (PackageManager.NameNotFoundException e) {
                ;
                Toast.makeText(this, "Package " + packageName + "not found", Toast.LENGTH_SHORT);
                finish();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        save();
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        save();
        finish();
        super.onBackPressed();
    }

    @OnClick(R.id.cancel_button)
    public void onCancelClick() {
        silenced_until.setText("0");
    }

    private void save() {
        updatePackage(app);
    }

    private void updatePackage(NotificationPreferencesEntity app) {
        boolean insert = (app == null);
        if (insert) {
            app = new NotificationPreferencesEntity();
        }
        app.setPackageName(packageInfo.packageName);
        app.setFilter(filter_edittext.getText().toString());
        app.setSilenceUntil(Long.valueOf(silenced_until.getText().toString()));
        app.setWhitelist(false);
        if (insert) {
            Log.d(Constants.TAG, "STORING " + packageInfo.packageName + " in AmazmodDB.NotificationPreferences");
            FlowManager
                    .getModelAdapter(NotificationPreferencesEntity.class)
                    .insert(app);
        } else {
            Log.d(Constants.TAG, "UPDATING " + packageInfo.packageName + " in AmazmodDB.NotificationPreferences");
            FlowManager
                    .getModelAdapter(NotificationPreferencesEntity.class)
                    .update(app);
        }
    }

    private NotificationPreferencesEntity loadApp(String packageName) {
        NotificationPreferencesEntity app = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle();
        return app;
    }

}
