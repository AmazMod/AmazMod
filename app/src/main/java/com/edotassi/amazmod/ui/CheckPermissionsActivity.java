package com.edotassi.amazmod.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.databinding.ActivityPermissionsBinding;
import com.edotassi.amazmod.util.Permissions;

import org.tinylog.Logger;

public class CheckPermissionsActivity extends BaseAppCompatActivity {

    private ActivityPermissionsBinding binding;

    private final ArrayMap<String, String > REQUIRED_PERMISSIONS = new ArrayMap<String, String>(){{
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
        binding = ActivityPermissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
        }

        getSupportActionBar().setTitle(R.string.activity_permissions);

        binding.activityPermissionsOpenPermissions.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        binding.activityPermissionsNotifications.setOnClickListener(v -> {
            if (!Permissions.hasNotificationAccess(getApplicationContext())) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        checkPermissions();
    }

    @SuppressLint("CheckResult")
    private void checkPermissions() {

        final String ENABLED = getResources().getString(R.string.enabled);
        final String DISABLED = getResources().getString(R.string.disabled);
        binding.activityPermissionsProgress.setVisibility(View.VISIBLE);
        binding.activityPermissionsMainContainer.setVisibility(View.GONE);

        for (ArrayMap.Entry<String, String> entry : REQUIRED_PERMISSIONS.entrySet()) {

            Logger.debug("CheckPermissionsActivity permission: " + entry.getKey() + " / " + entry.getValue());

            int id = getResources().getIdentifier(entry.getValue(), "id", "com.edotassi.amazmod");
            TextView textView = findViewById(id);

            if (Permissions.hasPermission(getApplicationContext(), entry.getKey())){
                textView.setText(ENABLED.toUpperCase());
                textView.setTextColor(getResources().getColor(R.color.colorCharging, getTheme()));
            } else {
                textView.setText(DISABLED.toUpperCase());
                textView.setTextColor(getResources().getColor(R.color.colorAccent, getTheme()));
            }
        }

        if (Permissions.hasNotificationAccess(getApplicationContext())){
            binding.activityPermissionsNotifications.setText(ENABLED.toUpperCase());
            binding.activityPermissionsNotifications.setTextColor(getResources().getColor(R.color.colorCharging, getTheme()));
        } else {
            binding.activityPermissionsNotifications.setText(DISABLED.toUpperCase());
            binding.activityPermissionsNotifications.setTextColor(getResources().getColor(R.color.colorAccent, getTheme()));
        }

        binding.activityPermissionsProgress.setVisibility(View.GONE);
        binding.activityPermissionsMainContainer.setVisibility(View.VISIBLE);
    }

}