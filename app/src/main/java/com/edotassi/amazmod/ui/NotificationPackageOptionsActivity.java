package com.edotassi.amazmod.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity_Table;
import com.edotassi.amazmod.support.SilenceApplicationHelper;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import amazmod.com.transport.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class NotificationPackageOptionsActivity extends BaseAppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notification_package_options);

        try {
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
            Log.e(Constants.TAG, "FilesExtrasActivity onCreate NullPointerException: " + exception.toString());
        }

        ButterKnife.bind(this);

        Intent intent = getIntent();
        String packageName = intent.getStringExtra("app");

        app = loadApp(packageName);
        if (app == null) {
            //Toast.makeText(this, "Package " + packageName + "is not enabled", Toast.LENGTH_SHORT).show();
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
                silenced_until.setText(SilenceApplicationHelper.getTimeSecondsReadable(app.getSilenceUntil()));
            } catch (PackageManager.NameNotFoundException e) {

                //Toast.makeText(this, "Package " + packageName + "not found", Toast.LENGTH_SHORT).show();
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
        app.setSilenceUntil(0);
        silenced_until.setText("");
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
        return SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle();
    }

}
