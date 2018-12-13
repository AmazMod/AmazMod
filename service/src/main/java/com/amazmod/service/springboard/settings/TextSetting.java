package com.amazmod.service.springboard.settings;

import android.view.View;

/**
 * Created by Kieron on 25/02/2018.
 */

public class TextSetting extends BaseSetting {

    View.OnClickListener onClickListener;
    String text;

    public TextSetting(String text, View.OnClickListener onClickListener) {
        this.text = text;
        this.onClickListener = onClickListener;
    }
}
