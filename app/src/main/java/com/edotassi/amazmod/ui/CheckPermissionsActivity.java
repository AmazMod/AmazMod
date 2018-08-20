package com.edotassi.amazmod.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.edotassi.amazmod.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.util.Permissions;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class CheckPermissionsActivity extends AppCompatActivity {

    @BindView(R.id.activity_permissions_main_container)
    View permissionsMainContainer;
    @BindView(R.id.activity_permissions_progress)
    MaterialProgressBar materialProgressBar;
    @BindView(R.id.activity_permissions_write)
    TextView permissionWrite;
    @BindView(R.id.activity_permissions_calendar)
    TextView permissionCalendar;
    @BindView(R.id.activity_permissions_notifications)
    TextView permissionNotification;

    @BindView(R.id.activity_permissions_open_permissions)
    Button openSettingsButton;

    private final Map<String, String > REQUIRED_PERMISSIONS = new HashMap<String, String>(){{
            put(Manifest.permission.READ_CALENDAR, "activity_permissions_calendar");
            put(Manifest.permission.WRITE_EXTERNAL_STORAGE, "activity_permissions_write");
    }};

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
        }

        getSupportActionBar().setTitle(R.string.activity_permissions);
        ButterKnife.bind(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        checkPermissions();
    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_permissions_open_permissions)
    public void openPermissions() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_permissions_notifications)
    public void openNotificationsAccess() {
        if (!Permissions.hasNotificationAccess(getApplicationContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } else {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        }
    }

    @SuppressLint("CheckResult")
    private void checkPermissions() {

        final String ENABLED = getResources().getString(R.string.enabled);
        final String DISABLED = getResources().getString(R.string.disabled);
        materialProgressBar.setVisibility(View.VISIBLE);
        permissionsMainContainer.setVisibility(View.GONE);

        for (Map.Entry<String, String> entry : REQUIRED_PERMISSIONS.entrySet()) {

            Log.d(Constants.TAG, "CheckPermissionsActivity permission: " + entry.getKey() + " / " + entry.getValue());

            int id = getResources().getIdentifier(entry.getValue(), "id", "com.edotassi.amazmod");
            TextView textView = findViewById(id);

            if (Permissions.hasPermission(getApplicationContext(), entry.getKey())){
                textView.setText(ENABLED.toUpperCase());
                textView.setTextColor(getResources().getColor(R.color.colorCharging));
            } else {
                textView.setText(DISABLED.toUpperCase());
                textView.setTextColor(getResources().getColor(R.color.colorAccent));
            }
        }

        if (Permissions.hasNotificationAccess(getApplicationContext())){
            permissionNotification.setText(ENABLED.toUpperCase());
            permissionNotification.setTextColor(getResources().getColor(R.color.colorCharging));
        } else {
            permissionNotification.setText(DISABLED.toUpperCase());
            permissionNotification.setTextColor(getResources().getColor(R.color.colorAccent));
        }

        materialProgressBar.setVisibility(View.GONE);
        permissionsMainContainer.setVisibility(View.VISIBLE);

    }

}