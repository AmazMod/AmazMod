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

public class FilesExtrasActivity extends AppCompatActivity {

    @BindView(R.id.activity_files_main_container)
    View filesMainContainer;
    @BindView(R.id.activity_files_progress)
    MaterialProgressBar materialProgressBar;
    @BindView(R.id.activity_files_permission)
    TextView filesPermission;
    @BindView(R.id.activity_files_date_last_backup)
    TextView filesDateLastBackup;

    @BindView(R.id.activity_files_backup)
    Button backupButton;
    @BindView(R.id.activity_files_restore)
    Button restoreButton;

    private final Map<String, String > REQUIRED_PERMISSIONS = new HashMap<String, String>(){{
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
        setContentView(R.layout.activity_files_extras);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
        }

        getSupportActionBar().setTitle(R.string.activity_files_extras);
        ButterKnife.bind(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateData();
    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_files_backup)
    public void backup() {

    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_files_restore)
    public void restore() {

    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_files_permission)
    public void openPermissions() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @SuppressLint("CheckResult")
    private void updateData() {

        materialProgressBar.setVisibility(View.VISIBLE);
        filesMainContainer.setVisibility(View.GONE);

        final String ENABLED = getResources().getString(R.string.enabled);
        final String DISABLED = getResources().getString(R.string.disabled);
        final String NEVER = getResources().getString(R.string.never);

        if (Permissions.hasPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            filesPermission.setText(ENABLED.toUpperCase());
            filesPermission.setTextColor(getResources().getColor(R.color.colorCharging));
        } else {
            filesPermission.setText(DISABLED.toUpperCase());
            filesPermission.setTextColor(getResources().getColor(R.color.colorAccent));
        }

        filesDateLastBackup.setText(NEVER.toUpperCase());

        materialProgressBar.setVisibility(View.GONE);
        filesMainContainer.setVisibility(View.VISIBLE);

    }

}