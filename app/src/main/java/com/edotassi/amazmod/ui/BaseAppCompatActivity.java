package com.edotassi.amazmod.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.edotassi.amazmod.R;
import com.edotassi.amazmod.util.LocaleUtils;
import com.edotassi.amazmod.util.Screen;

public abstract class BaseAppCompatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Screen.isDarkTheme()) {
            setTheme(R.style.AppThemeDark);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.onAttach(newBase));
    }
}
