package com.edotassi.amazmod.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.edotassi.amazmod.R
import com.edotassi.amazmod.util.LocaleUtils
import com.edotassi.amazmod.util.Screen

abstract class BaseAppCompatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Screen.isDarkTheme() || MainActivity.systemThemeIsDark) {
            setTheme(R.style.AppThemeDark)
        } else {
            setTheme(R.style.AppTheme)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleUtils.onAttach(newBase))
    }
}