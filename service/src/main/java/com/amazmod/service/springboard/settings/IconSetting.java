package com.amazmod.service.springboard.settings;

import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Created by Kieron on 20/01/2018.
 */

public class IconSetting extends BaseSetting {

    //Setting with a click listener, two strings and an icon

    View.OnClickListener onClickListener;
    String title;
    String subtitle;
    Drawable icon;

    public IconSetting(Drawable icon, String title, String subtitle, View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
    }
}