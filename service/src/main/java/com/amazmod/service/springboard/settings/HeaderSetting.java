package com.amazmod.service.springboard.settings;

import android.view.View;

/**
 * Created by Kieron on 20/01/2018.
 */

public class HeaderSetting extends BaseSetting {

    //Simple setting with only a title

    String title;
    View.OnLongClickListener onLongClickListener;

    public HeaderSetting(String title) {
        this.title = title;
        this.onLongClickListener = null;
    }

    public HeaderSetting(String title, View.OnLongClickListener onLongClickListener) {
        this.title = title;
        this.onLongClickListener = onLongClickListener;
    }

}