package com.edotassi.amazmod.ui;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

import com.edotassi.amazmod.util.LocaleUtils;

public abstract class BaseAppCompatActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.onAttach(newBase));
    }
}
